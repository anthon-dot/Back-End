// Supabase Edge Function: ai-insights
// Replaces NotificationAIService.java and ReportAIService.java
// Generates intelligent administrative recommendations using Google Gemini

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

    const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY') || ''

    // 1. Fetch current market summary statistics
    const [
      { count: totalStalls },
      { count: occupiedStalls },
      { data: overdueBillings },
      { data: pendingApplications }
    ] = await Promise.all([
      supabaseClient.from('stalls').select('*', { count: 'exact', head: true }),
      supabaseClient.from('stalls').select('*', { count: 'exact', head: true }).eq('status', 'OCCUPIED'),
      supabaseClient.from('billings').select('id, billing_no, balance, due_date, occupant_id').eq('status', 'UNPAID'),
      supabaseClient.from('business_applications').select('id, business_name, application_status, applied_on').eq('application_status', 'PENDING')
    ])

    const marketSummary = {
      totalStalls: totalStalls || 0,
      occupiedStalls: occupiedStalls || 0,
      occupancyRate: totalStalls ? (((occupiedStalls || 0) / totalStalls) * 100).toFixed(1) + '%' : '0%',
      overdueInvoicesCount: overdueBillings?.length || 0,
      totalOverdueBalance: overdueBillings?.reduce((sum, b) => sum + Number(b.balance || 0), 0) || 0,
      pendingApplicationsCount: pendingApplications?.length || 0
    }

    let aiInsightText = "AI analysis skipped (GEMINI_API_KEY not configured)."

    // 2. Call Gemini API if key is present
    if (GEMINI_API_KEY) {
      const prompt = `
You are an intelligent Generative AI assistant for the Public Market Stall Management System of Manticao.
Analyze the following market statistics:
${JSON.stringify(marketSummary, null, 2)}

Provide a concise, professional executive briefing:
1. Operational Risk Assessment (e.g. overdue balances, occupancy)
2. Revenue Opportunities
3. Immediate actionable recommendations for market administrators.

Keep response structured, concise, and professional.
`
      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${GEMINI_API_KEY}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }]
        })
      })

      if (response.ok) {
        const json = await response.json()
        aiInsightText = json?.candidates?.[0]?.content?.parts?.[0]?.text || "No insight generated."
      }
    }

    // 3. Save as an administrative notification
    await supabaseClient.from('notifications').insert({
      title: 'Weekly AI Market Intelligence Briefing',
      message: `Occupancy at ${marketSummary.occupancyRate} with ${marketSummary.pendingApplicationsCount} pending applications and ${marketSummary.overdueInvoicesCount} overdue bills.`,
      explanation: aiInsightText,
      priority: marketSummary.overdueInvoicesCount > 5 ? 'HIGH' : 'MEDIUM',
      notification_type: 'AI_INSIGHT',
      ai_generated: true
    })

    return new Response(JSON.stringify({ success: true, summary: marketSummary, insight: aiInsightText }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200
    })
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 400
    })
  }
})
