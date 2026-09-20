// Supabase Edge Function: approval-workflow
// Handles multi-stage stakeholder approval transitions securely using the Service Role Key

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // Verify user authorization from header
    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')
    const { data: { user }, error: authError } = await supabaseClient.auth.getUser(token)

    if (authError || !user) {
      return new Response(JSON.stringify({ error: 'Unauthorized' }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    // Check if user is ADMIN
    const { data: profile } = await supabaseClient
      .from('profiles')
      .select('role')
      .eq('id', user.id)
      .single()

    if (!profile || profile.role !== 'ADMIN') {
      return new Response(JSON.stringify({ error: 'Forbidden: Admin access required' }), {
        status: 403,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    const { stakeholderId, stage, status, remarks } = await req.json()

    // 1. Log approval history
    await supabaseClient.from('approval_history').insert({
      stakeholder_id: stakeholderId,
      stage: stage,
      status: status,
      approved_by: user.id,
      remarks: remarks || ''
    })

    // 2. Update stakeholder status based on stage
    const updateData: Record<string, any> = {}
    if (stage === 'MARKET') {
      updateData.market_approval_status = status
      updateData.market_supervisor_approved = (status === 'APPROVED')
    } else if (stage === 'ENDORSEMENT') {
      updateData.endorsement_status = status
      updateData.endorsed_by = user.email
      updateData.endorsed_at = new Date().toISOString()
    } else if (stage === 'BPLO') {
      updateData.bplo_status = status
      updateData.bplo_approved = (status === 'APPROVED')
      updateData.bplo_approved_by = user.email
      updateData.approval_date = new Date().toISOString()
      if (status === 'APPROVED') {
        updateData.application_status = 'APPROVED'
      }
    }

    const { data: updatedStakeholder, error: updateError } = await supabaseClient
      .from('stakeholders')
      .update(updateData)
      .eq('id', stakeholderId)
      .select()
      .single()

    if (updateError) throw updateError

    // 3. Create notification for the stakeholder
    await supabaseClient.from('notifications').insert({
      stakeholder_id: stakeholderId,
      title: `Application Update: ${stage}`,
      message: `Your application stage ${stage} was updated to ${status}.`,
      priority: 'MEDIUM',
      notification_type: 'APPROVAL_UPDATE',
      related_record_type: 'STAKEHOLDER',
      related_record_id: stakeholderId
    })

    return new Response(JSON.stringify({ success: true, data: updatedStakeholder }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 400,
    })
  }
})
