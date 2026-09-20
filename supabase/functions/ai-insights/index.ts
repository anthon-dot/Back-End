// Supabase Edge Function: ai-insights
// Built with TypeScript & Hono framework
// Replaces NotificationAIService.java and ReportAIService.java using Google Gemini for automated market intelligence

import { Hono } from "npm:hono"
import { cors } from "npm:hono/cors"
import { createClient } from "npm:@supabase/supabase-js@2"

const app = new Hono()

app.use(
  "*",
  cors({
    origin: "*",
    allowHeaders: ["authorization", "x-client-info", "apikey", "content-type"],
    allowMethods: ["POST", "GET", "OPTIONS"],
  })
)

function getSupabaseClient() {
  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
  return createClient(supabaseUrl, serviceRoleKey)
}

// Fetch live market data snapshot
async function fetchMarketSnapshot(supabase: any) {
  const [
    { count: totalStalls },
    { count: occupiedStalls },
    { data: overdueBillings },
    { data: pendingApplications },
    { data: activeContracts },
  ] = await Promise.all([
    supabase.from("stalls").select("*", { count: "exact", head: true }),
    supabase.from("stalls").select("*", { count: "exact", head: true }).eq("status", "OCCUPIED"),
    supabase.from("billings").select("id, billing_no, balance, due_date").eq("status", "OVERDUE"),
    supabase.from("stakeholders").select("id, business_name, application_status").eq("application_status", "FOR_APPROVAL"),
    supabase.from("contracts").select("id, contract_no, end_date").eq("status", "ACTIVE"),
  ])

  const total = totalStalls || 0
  const occupied = occupiedStalls || 0
  const occupancyRate = total > 0 ? ((occupied / total) * 100).toFixed(1) + "%" : "0%"
  const totalOverdue = overdueBillings?.reduce((sum: number, b: any) => sum + Number(b.balance || 0), 0) || 0

  return {
    totalStalls: total,
    occupiedStalls: occupied,
    vacantStalls: Math.max(0, total - occupied),
    occupancyRate,
    overdueInvoicesCount: overdueBillings?.length || 0,
    totalOverdueBalance: totalOverdue,
    pendingApplicationsCount: pendingApplications?.length || 0,
    activeContractsCount: activeContracts?.length || 0,
  }
}

// Call Google Gemini API
async function generateGeminiInsight(prompt: string, marketData: any, apiKey: string) {
  if (!apiKey) {
    return `Market Occupancy stands at ${marketData.occupancyRate} with ${marketData.occupiedStalls}/${marketData.totalStalls} stalls leased. Overdue balance totals PHP ${marketData.totalOverdueBalance.toLocaleString()}. (Set GEMINI_API_KEY for deep AI analysis).`
  }

  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`

  const response = await fetch(endpoint, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [
        {
          parts: [
            {
              text: `You are an expert economic advisor and municipal market management consultant for Manticao Public Market.
Data snapshot:
${JSON.stringify(marketData, null, 2)}

User request:
${prompt}

Provide a concise, professional executive briefing highlighting key operational risks, revenue collection opportunities, and 3 actionable administrative recommendations. Keep it under 250 words.`,
            },
          ],
        },
      ],
    }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(`Gemini API error: ${errorText}`)
  }

  const data = await response.json()
  return data.candidates?.[0]?.content?.parts?.[0]?.text || "Unable to generate insights at this time."
}

// ==============================================================================
// 1. GET /ai-insights/summary - Automated Market Briefing
// ==============================================================================
app.get("/ai-insights/summary", async (c) => {
  try {
    const supabase = getSupabaseClient()
    const geminiKey = Deno.env.get("GEMINI_API_KEY") || ""

    const snapshot = await fetchMarketSnapshot(supabase)
    const prompt = "Generate a daily administrative briefing summarizing market occupancy, overdue collections, and urgent action items."
    const insight = await generateGeminiInsight(prompt, snapshot, geminiKey)

    return c.json({
      success: true,
      timestamp: new Date().toISOString(),
      marketSnapshot: snapshot,
      aiAnalysis: insight,
    })
  } catch (error: any) {
    return c.json({ error: error.message }, 500)
  }
})

// ==============================================================================
// 2. POST /ai-insights/generate - Custom Executive Report
// ==============================================================================
app.post("/ai-insights/generate", async (c) => {
  try {
    const supabase = getSupabaseClient()
    const geminiKey = Deno.env.get("GEMINI_API_KEY") || ""
    const { prompt } = await c.req.json()

    if (!prompt) {
      return c.json({ error: "Prompt is required" }, 400)
    }

    const snapshot = await fetchMarketSnapshot(supabase)
    const insight = await generateGeminiInsight(prompt, snapshot, geminiKey)

    // Save generated report to notifications or audit log
    await supabase.from("notifications").insert({
      title: "AI Market Report Generated",
      message: insight.slice(0, 300) + "...",
      priority: "MEDIUM",
      notification_type: "AI_INSIGHT",
    })

    return c.json({
      success: true,
      prompt,
      report: insight,
      snapshot,
    })
  } catch (error: any) {
    return c.json({ error: error.message }, 500)
  }
})

app.all("/ai-insights", async (c) => {
  // Alias for root invocation
  return app.request("/ai-insights/summary", c.req.raw)
})

Deno.serve(app.fetch)
