// Supabase Edge Function: approval-workflow
// Built with TypeScript & Hono framework
// Handles municipal market multi-stage approval workflow securely with Role-Based Access Control (RBAC)

import { Hono } from "npm:hono"
import { cors } from "npm:hono/cors"
import { createClient } from "npm:@supabase/supabase-js@2"

const app = new Hono()

// Enable CORS for all incoming client requests
app.use(
  "*",
  cors({
    origin: "*",
    allowHeaders: ["authorization", "x-client-info", "apikey", "content-type"],
    allowMethods: ["POST", "GET", "OPTIONS"],
  })
)

// Helper: Initialize Supabase client using Service Role key for administrative operations
function getSupabaseClient() {
  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
  return createClient(supabaseUrl, serviceRoleKey)
}

// Helper: Authenticate request & retrieve caller profile with role
async function authenticateCaller(c: any, allowedRoles: string[]) {
  const authHeader = c.req.header("Authorization")
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return { error: "Missing or invalid Authorization header", status: 401 }
  }

  const token = authHeader.replace("Bearer ", "").trim()
  const supabase = getSupabaseClient()

  const { data: { user }, error: authError } = await supabase.auth.getUser(token)
  if (authError || !user) {
    return { error: "Unauthorized: Invalid or expired JWT", status: 401 }
  }

  const { data: profile, error: profileError } = await supabase
    .from("profiles")
    .select("id, email, full_name, role")
    .eq("id", user.id)
    .single()

  if (profileError || !profile) {
    return { error: "User profile not found", status: 403 }
  }

  const userRole = (profile.role || "").toUpperCase()
  const hasAccess = allowedRoles.map(r => r.toUpperCase()).includes(userRole) || userRole === "ADMIN"

  if (!hasAccess) {
    return { error: `Forbidden: Requires one of [${allowedRoles.join(", ")}]`, status: 403 }
  }

  return { user, profile, supabase }
}

// ==============================================================================
// 1. TREASURER APPROVAL (Advance Payment Recording)
// ==============================================================================
app.post("/approval-workflow/treasurer-approve", async (c) => {
  const auth = await authenticateCaller(c, ["TREASURER", "ADMIN"])
  if (auth.error) return c.json({ error: auth.error }, auth.status)

  const { supabase, profile } = auth
  const body = await c.req.json()
  const { stakeholderId, amount, totalAdvanceAmount, referenceNo } = body

  if (!stakeholderId) {
    return c.json({ error: "stakeholderId is required" }, 400)
  }

  // Fetch stakeholder
  const { data: stakeholder, error: stError } = await supabase
    .from("stakeholders")
    .select("*")
    .eq("id", stakeholderId)
    .single()

  if (stError || !stakeholder) {
    return c.json({ error: "Stakeholder not found" }, 404)
  }

  if (stakeholder.treasurer_approved) {
    return c.json({ error: "Treasurer approval already recorded" }, 409)
  }

  const numAmount = Number(amount || 0)
  const numTotalAdvance = Number(totalAdvanceAmount || numAmount)
  let receiptNo = ""

  if (numAmount > 0) {
    receiptNo = `ADV-${Date.now().toString().slice(-8)}`

    // Record Advance Payment
    const { error: payError } = await supabase.from("payments").insert({
      stakeholder_id: stakeholderId,
      amount: numAmount,
      total_advance_amount: numTotalAdvance,
      payment_type: "ADVANCE_PAYMENT",
      reference_no: referenceNo || "",
      receipt_no: receiptNo,
      payment_date: new Date().toISOString(),
    })
    if (payError) return c.json({ error: `Payment failed: ${payError.message}` }, 500)
  }

  const currentCredit = Number(stakeholder.advance_balance || 0)
  const newCredit = currentCredit + numAmount

  // Update stakeholder
  const { data: updated, error: updError } = await supabase
    .from("stakeholders")
    .update({
      treasurer_approved: true,
      advance_payment_paid: numAmount > 0,
      advance_payment_completed: numAmount > 0,
      advance_payment_date: new Date().toISOString().split("T")[0],
      total_advance_amount: numTotalAdvance,
      advance_payment_amount: newCredit,
      advance_balance: newCredit,
      application_status: "PENDING_MARKET_SUPERVISOR_APPROVAL",
      onboarding_status: "FOR_APPROVAL",
    })
    .eq("id", stakeholderId)
    .select()
    .single()

  if (updError) return c.json({ error: updError.message }, 500)

  // Record History, Notifications & Audit Log
  await Promise.all([
    supabase.from("approval_history").insert({
      stakeholder_id: stakeholderId,
      stage: "TREASURER",
      status: "APPROVED",
      approved_by: profile.id,
      remarks: receiptNo ? `Advance payment recorded: ${receiptNo}` : "Treasurer approved application",
    }),
    supabase.from("notifications").insert({
      stakeholder_id: stakeholderId,
      title: "Treasurer Approved",
      message: "Your application has been approved by the Treasurer and forwarded to the Market Supervisor.",
      priority: "MEDIUM",
      notification_type: "APPROVAL_UPDATE",
      related_record_type: "STAKEHOLDER",
      related_record_id: stakeholderId,
    }),
    supabase.from("audit_logs").insert({
      action: "TREASURER_APPROVED",
      entity_name: "Stakeholder",
      entity_id: stakeholderId,
      performed_by: profile.email,
      details: `Treasurer approved stakeholder. Receipt: ${receiptNo || "None"}`,
    }),
  ])

  return c.json({ success: true, data: updated })
})

// ==============================================================================
// 2. MARKET SUPERVISOR APPROVAL (Stall Assignment & Contract Generation)
// ==============================================================================
app.post("/approval-workflow/assign-stall", async (c) => {
  const auth = await authenticateCaller(c, ["MARKET_SUPERVISOR", "ADMIN"])
  if (auth.error) return c.json({ error: auth.error }, auth.status)

  const { supabase, profile } = auth
  const body = await c.req.json()
  const { stakeholderId, stallId, startDate, endDate, terms } = body

  if (!stakeholderId || !stallId) {
    return c.json({ error: "stakeholderId and stallId are required" }, 400)
  }

  const { data: stakeholder } = await supabase.from("stakeholders").select("*").eq("id", stakeholderId).single()
  if (!stakeholder) return c.json({ error: "Stakeholder not found" }, 404)
  if (!stakeholder.treasurer_approved) {
    return c.json({ error: "Treasurer approval is required first" }, 400)
  }

  const { data: stall } = await supabase.from("stalls").select("*").eq("id", stallId).single()
  if (!stall) return c.json({ error: "Stall not found" }, 404)
  if (stall.status !== "AVAILABLE") {
    return c.json({ error: "Stall is not available for assignment" }, 409)
  }

  const contractStart = startDate || new Date().toISOString().split("T")[0]
  const contractEnd = endDate || new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]
  const contractNo = `CNT-${Date.now().toString().slice(-8)}`

  // 1. Create occupant
  const { data: occupant, error: occErr } = await supabase
    .from("occupants")
    .insert({
      stakeholder_id: stakeholderId,
      status: "PENDING",
      occupancy_date: contractStart,
      advance_balance: stakeholder.advance_balance || 0,
    })
    .select()
    .single()

  if (occErr) return c.json({ error: `Occupant creation failed: ${occErr.message}` }, 500)

  // 2. Create contract
  const { data: contract, error: cntErr } = await supabase
    .from("contracts")
    .insert({
      occupant_id: occupant.id,
      stall_id: stall.id,
      contract_no: contractNo,
      start_date: contractStart,
      end_date: contractEnd,
      monthly_rent: stall.monthly_rent,
      billing_frequency: "MONTHLY",
      terms: terms || "Standard Municipal Public Market Lease Terms",
      status: "PENDING_APPROVAL",
    })
    .select()
    .single()

  if (cntErr) return c.json({ error: `Contract creation failed: ${cntErr.message}` }, 500)

  // 3. Update stall to OCCUPIED and link occupant
  await supabase.from("stalls").update({ status: "OCCUPIED", occupant_id: occupant.id }).eq("id", stall.id)

  // 4. Update occupant with contract reference
  await supabase.from("occupants").update({ contract_id: contract.id }).eq("id", occupant.id)

  // 5. Update stakeholder
  const { data: updatedStakeholder, error: stUpdErr } = await supabase
    .from("stakeholders")
    .update({
      selected_stall_id: stall.id,
      market_supervisor_approved: true,
      market_approval_status: "APPROVED",
      application_status: "PENDING_BPLO_APPROVAL",
    })
    .eq("id", stakeholderId)
    .select()
    .single()

  if (stUpdErr) return c.json({ error: stUpdErr.message }, 500)

  // Record History, Notifications & Audit Log
  await Promise.all([
    supabase.from("approval_history").insert({
      stakeholder_id: stakeholderId,
      stage: "MARKET_SUPERVISOR",
      status: "APPROVED",
      approved_by: profile.id,
      remarks: `Assigned stall ${stall.stall_no} with contract ${contractNo}`,
    }),
    supabase.from("notifications").insert({
      stakeholder_id: stakeholderId,
      title: "Stall Assigned",
      message: `Stall ${stall.stall_no} assigned. Contract ${contractNo} prepared for validation.`,
      priority: "HIGH",
      notification_type: "STALL_ASSIGNED",
      related_record_type: "CONTRACT",
      related_record_id: contract.id,
    }),
    supabase.from("audit_logs").insert({
      action: "STALL_ASSIGNED",
      entity_name: "Stall",
      entity_id: stall.id,
      performed_by: profile.email,
      details: `Stall ${stall.stall_no} assigned to stakeholder ${stakeholderId}`,
    }),
  ])

  return c.json({ success: true, data: updatedStakeholder, contract, occupant })
})

// ==============================================================================
// 3. BPLO APPROVAL (Business Permit Validation)
// ==============================================================================
app.post("/approval-workflow/bplo-approve", async (c) => {
  const auth = await authenticateCaller(c, ["BPLO", "ADMIN"])
  if (auth.error) return c.json({ error: auth.error }, auth.status)

  const { supabase, profile } = auth
  const { stakeholderId, remarks } = await c.req.json()

  const { data: stakeholder } = await supabase.from("stakeholders").select("*").eq("id", stakeholderId).single()
  if (!stakeholder) return c.json({ error: "Stakeholder not found" }, 404)
  if (!stakeholder.market_supervisor_approved) {
    return c.json({ error: "Market supervisor approval required first" }, 400)
  }

  const { data: updated, error: updErr } = await supabase
    .from("stakeholders")
    .update({
      bplo_approved: true,
      bplo_status: "APPROVED",
      bplo_approved_by: profile.email,
      approval_date: new Date().toISOString(),
      application_status: "PENDING_ENDORSING_OFFICE_APPROVAL",
    })
    .eq("id", stakeholderId)
    .select()
    .single()

  if (updErr) return c.json({ error: updErr.message }, 500)

  await Promise.all([
    supabase.from("approval_history").insert({
      stakeholder_id: stakeholderId,
      stage: "BPLO",
      status: "APPROVED",
      approved_by: profile.id,
      remarks: remarks || "BPLO verification completed and approved",
    }),
    supabase.from("notifications").insert({
      stakeholder_id: stakeholderId,
      title: "BPLO Approved",
      message: "Your business permit documents have been validated by BPLO.",
      priority: "MEDIUM",
      notification_type: "APPROVAL_UPDATE",
      related_record_type: "STAKEHOLDER",
      related_record_id: stakeholderId,
    }),
    supabase.from("audit_logs").insert({
      action: "BPLO_APPROVED",
      entity_name: "Stakeholder",
      entity_id: stakeholderId,
      performed_by: profile.email,
      details: "BPLO validated business requirements",
    }),
  ])

  return c.json({ success: true, data: updated })
})

// ==============================================================================
// 4. FINAL ENDORSEMENT (Mayor / Endorsement Office)
// ==============================================================================
app.post("/approval-workflow/final-endorse", async (c) => {
  const auth = await authenticateCaller(c, ["ENDORSING_OFFICE", "ENDORSEMENT_OFFICE", "ADMIN"])
  if (auth.error) return c.json({ error: auth.error }, auth.status)

  const { supabase, profile } = auth
  const { stakeholderId, remarks } = await c.req.json()

  const { data: stakeholder } = await supabase.from("stakeholders").select("*").eq("id", stakeholderId).single()
  if (!stakeholder) return c.json({ error: "Stakeholder not found" }, 404)
  if (!stakeholder.bplo_approved) {
    return c.json({ error: "BPLO approval required first" }, 400)
  }

  const { data: updated, error: updErr } = await supabase
    .from("stakeholders")
    .update({
      final_endorsed: true,
      endorsing_approved: true,
      endorsement_status: "APPROVED",
      endorsing_status: "ENDORSED",
      final_status: "APPROVED",
      endorsed_by: profile.email,
      endorsed_at: new Date().toISOString(),
      application_status: "PENDING_BUSINESS_PERMIT_PAYMENT",
    })
    .eq("id", stakeholderId)
    .select()
    .single()

  if (updErr) return c.json({ error: updErr.message }, 500)

  await Promise.all([
    supabase.from("approval_history").insert({
      stakeholder_id: stakeholderId,
      stage: "ENDORSEMENT",
      status: "APPROVED",
      approved_by: profile.id,
      remarks: remarks || "Application officially endorsed",
    }),
    supabase.from("notifications").insert({
      stakeholder_id: stakeholderId,
      title: "Application Endorsed",
      message: "Your application has received final endorsement. Please settle business permit fees with the Treasurer.",
      priority: "HIGH",
      notification_type: "APPROVAL_UPDATE",
      related_record_type: "STAKEHOLDER",
      related_record_id: stakeholderId,
    }),
    supabase.from("audit_logs").insert({
      action: "FINAL_ENDORSEMENT",
      entity_name: "Stakeholder",
      entity_id: stakeholderId,
      performed_by: profile.email,
      details: "Endorsement completed",
    }),
  ])

  return c.json({ success: true, data: updated })
})

// ==============================================================================
// 5. PERMIT PAYMENT & ACTIVATION (Tenant Onboarding Complete)
// ==============================================================================
app.post("/approval-workflow/permit-payment", async (c) => {
  const auth = await authenticateCaller(c, ["TREASURER", "ADMIN"])
  if (auth.error) return c.json({ error: auth.error }, auth.status)

  const { supabase, profile } = auth
  const { stakeholderId, amount, referenceNo } = await c.req.json()

  const { data: stakeholder } = await supabase.from("stakeholders").select("*").eq("id", stakeholderId).single()
  if (!stakeholder) return c.json({ error: "Stakeholder not found" }, 404)
  if (!stakeholder.final_endorsed) {
    return c.json({ error: "Final endorsement required before permit payment" }, 400)
  }

  const receiptNo = `BP-${Date.now().toString().slice(-8)}`

  // 1. Record Payment
  await supabase.from("payments").insert({
    stakeholder_id: stakeholderId,
    amount: Number(amount || 0),
    payment_type: "BUSINESS_PERMIT_PAYMENT",
    reference_no: referenceNo || "",
    receipt_no: receiptNo,
    payment_date: new Date().toISOString(),
  })

  // 2. Fetch occupant & contract to activate
  const { data: occupant } = await supabase.from("occupants").select("*").eq("stakeholder_id", stakeholderId).single()
  if (occupant) {
    await supabase.from("occupants").update({ status: "ACTIVE" }).eq("id", occupant.id)
    if (occupant.contract_id) {
      await supabase.from("contracts").update({ status: "ACTIVE" }).eq("id", occupant.contract_id)

      // Create Initial Billing Invoice
      const { data: contract } = await supabase.from("contracts").select("*").eq("id", occupant.contract_id).single()
      if (contract) {
        const dueDate = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]
        await supabase.from("billings").insert({
          occupant_id: occupant.id,
          contract_id: contract.id,
          billing_no: `INV-${Date.now().toString().slice(-8)}`,
          billing_period: `${new Date().toLocaleString("default", { month: "short" })} ${new Date().getFullYear()}`,
          total_amount: contract.monthly_rent,
          paid_amount: 0,
          balance: contract.monthly_rent,
          due_date: dueDate,
          status: "UNPAID",
        })
      }
    }
  }

  // 3. Mark stakeholder as verified tenant
  const { data: updated, error: updErr } = await supabase
    .from("stakeholders")
    .update({
      application_status: "APPROVED",
      onboarding_status: "COMPLETED",
      verified_tenant: true,
      advance_payment_completed: true,
    })
    .eq("id", stakeholderId)
    .select()
    .single()

  if (updErr) return c.json({ error: updErr.message }, 500)

  await Promise.all([
    supabase.from("approval_history").insert({
      stakeholder_id: stakeholderId,
      stage: "PERMIT_PAYMENT",
      status: "COMPLETED",
      approved_by: profile.id,
      remarks: `Business permit fee settled. Receipt: ${receiptNo}. Tenant fully onboarded.`,
    }),
    supabase.from("notifications").insert({
      stakeholder_id: stakeholderId,
      title: "Welcome! Onboarding Complete",
      message: "Your market stall lease contract is now active. You may now commence business operations.",
      priority: "HIGH",
      notification_type: "ONBOARDING_COMPLETE",
      related_record_type: "STAKEHOLDER",
      related_record_id: stakeholderId,
    }),
  ])

  return c.json({ success: true, data: updated, receiptNo })
})

// ==============================================================================
// 6. REJECT APPLICATION
// ==============================================================================
app.post("/approval-workflow/reject", async (c) => {
  const auth = await authenticateCaller(c, ["ADMIN", "TREASURER", "MARKET_SUPERVISOR", "BPLO", "ENDORSING_OFFICE"])
  if (auth.error) return c.json({ error: auth.error }, auth.status)

  const { supabase, profile } = auth
  const { stakeholderId, stage, remarks } = await c.req.json()

  if (!stakeholderId || !remarks) {
    return c.json({ error: "stakeholderId and remarks are required" }, 400)
  }

  const { data: stakeholder } = await supabase.from("stakeholders").select("*").eq("id", stakeholderId).single()
  if (!stakeholder) return c.json({ error: "Stakeholder not found" }, 404)

  // Release stall if one was selected/occupied
  if (stakeholder.selected_stall_id) {
    await supabase.from("stalls").update({ status: "AVAILABLE", occupant_id: null }).eq("id", stakeholder.selected_stall_id)
  }

  const { data: updated, error: updErr } = await supabase
    .from("stakeholders")
    .update({
      application_status: "REJECTED",
      onboarding_status: "REJECTED",
      rejection_remarks: remarks,
      rejection_stage: stage || "WORKFLOW",
      rejected_by: profile.email,
      rejected_at: new Date().toISOString(),
    })
    .eq("id", stakeholderId)
    .select()
    .single()

  if (updErr) return c.json({ error: updErr.message }, 500)

  await Promise.all([
    supabase.from("approval_history").insert({
      stakeholder_id: stakeholderId,
      stage: stage || "WORKFLOW",
      status: "REJECTED",
      approved_by: profile.id,
      remarks: remarks,
    }),
    supabase.from("notifications").insert({
      stakeholder_id: stakeholderId,
      title: "Application Disapproved",
      message: `Your application was disapproved at stage ${stage || "review"}. Reason: ${remarks}`,
      priority: "HIGH",
      notification_type: "APPLICATION_REJECTED",
      related_record_type: "STAKEHOLDER",
      related_record_id: stakeholderId,
    }),
    supabase.from("audit_logs").insert({
      action: "APPLICATION_REJECTED",
      entity_name: "Stakeholder",
      entity_id: stakeholderId,
      performed_by: profile.email,
      details: `Disapproved at ${stage}. Remarks: ${remarks}`,
    }),
  ])

  return c.json({ success: true, data: updated })
})

// Fallback for default invoke root
app.all("/approval-workflow", (c) => {
  return c.json({ message: "Approval Workflow Edge Function Active. Use specific route endpoints." })
})

Deno.serve(app.fetch)
