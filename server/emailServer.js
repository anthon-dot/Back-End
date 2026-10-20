import express from 'express'
import cors from 'cors'
import { sendEmail, verifyTransporter } from './mailer.js'

const app = express()
const PORT = process.env.EMAIL_PORT || 3001

app.use(cors())
app.use(express.json({ limit: '10mb' }))

// Health check and transporter status
app.get('/api/email/status', async (req, res) => {
  try {
    const status = await verifyTransporter()
    res.json({
      status: 'online',
      service: 'Manticao Stakeholder Email Service (Nodemailer)',
      ...status
    })
  } catch (err) {
    res.status(500).json({ error: err.message })
  }
})

// Send stakeholder email notification endpoint
app.post('/api/email/send', async (req, res) => {
  const { to, recipientName, subject, html, text, from, notificationType, stakeholderId, metadata } = req.body

  if (!to) {
    return res.status(400).json({
      success: false,
      error: 'Recipient email address ("to") is required.'
    })
  }

  try {
    const result = await sendEmail({
      to,
      recipientName,
      subject,
      html,
      text,
      from
    })

    return res.json({
      success: true,
      ...result,
      notificationType,
      stakeholderId,
      metadata
    })
  } catch (err) {
    console.error('[emailServer] Failed to dispatch email:', err)
    return res.status(500).json({
      success: false,
      error: err.message || 'Failed to dispatch email notification via Nodemailer.'
    })
  }
})

// Send test email
app.post('/api/email/test', async (req, res) => {
  const { to } = req.body
  const targetEmail = to || process.env.SMTP_USER || 'test@example.com'

  try {
    const result = await sendEmail({
      to: targetEmail,
      recipientName: 'Manticao Stakeholder Test',
      subject: 'Manticao Public Market - Test Stakeholder Notification',
      html: `
        <div style="font-family: sans-serif; padding: 20px; color: #1e293b;">
          <h2>Manticao Market Stakeholder Notification Test</h2>
          <p>This is a test notification verifying that the <strong>Nodemailer</strong> email dispatch system is operational.</p>
          <p>Time: ${new Date().toISOString()}</p>
        </div>
      `,
      text: 'This is a test stakeholder notification confirming Nodemailer is working properly.'
    })
    res.json({ success: true, message: `Test email dispatched to ${targetEmail}`, ...result })
  } catch (err) {
    res.status(500).json({ success: false, error: err.message })
  }
})

app.listen(PORT, () => {
  console.log(`[EmailServer] Nodemailer Stakeholder Service running on http://localhost:${PORT}`)
})

export default app
