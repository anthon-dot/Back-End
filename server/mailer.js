import nodemailer from 'nodemailer'
import fs from 'fs'
import path from 'path'

/**
 * ==============================================================================
 * NODEMAILER EMAIL DISPATCHER
 * ==============================================================================
 * Dispatches official municipal notifications strictly using Nodemailer SMTP.
 */

let transporter = null
let transporterConfig = null
let isTestAccount = false

function getFreshEnv() {
  const env = { ...process.env }
  try {
    const envPath = path.resolve(process.cwd(), '.env')
    if (fs.existsSync(envPath)) {
      const content = fs.readFileSync(envPath, 'utf8')
      content.split('\n').forEach(line => {
        const trimmed = line.trim()
        if (trimmed && !trimmed.startsWith('#')) {
          const eqIdx = trimmed.indexOf('=')
          if (eqIdx !== -1) {
            const k = trimmed.slice(0, eqIdx).trim()
            let v = trimmed.slice(eqIdx + 1).trim()
            if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) {
              v = v.slice(1, -1)
            }
            env[k] = v
          }
        }
      })
    }
  } catch (_) {}
  return env
}

export async function getTransporter(forceRefresh = false) {
  const env = getFreshEnv()
  const host = env.SMTP_HOST || env.VITE_SMTP_HOST || 'smtp.gmail.com'
  const port = Number(env.SMTP_PORT || env.VITE_SMTP_PORT || 587)
  const secure = (env.SMTP_SECURE || env.VITE_SMTP_SECURE) === 'true' || port === 465
  const user = (env.SMTP_USER || env.VITE_SMTP_USER || 'anthony.jamaca@msunaawan.edu.ph').trim()
  const pass = (env.SMTP_PASS || env.VITE_SMTP_PASS || '').trim()

  const hasRealCreds = Boolean(host && user && pass)
  const configChanged = !transporterConfig ||
    transporterConfig.host !== host ||
    transporterConfig.port !== port ||
    transporterConfig.auth?.user !== user ||
    transporterConfig.auth?.pass !== pass

  if (transporter && !forceRefresh && !configChanged && (!isTestAccount || !hasRealCreds)) {
    return { transporter, isTestAccount, config: transporterConfig }
  }

  // 1. If real SMTP credentials are provided, use real SMTP transporter
  if (hasRealCreds) {
    transporterConfig = {
      host,
      port,
      secure,
      auth: { user, pass },
      tls: {
        rejectUnauthorized: false
      }
    }
    transporter = nodemailer.createTransport(transporterConfig)
    isTestAccount = false
    console.log(`[Nodemailer SMTP] Real transporter connected to ${host}:${port} (${user})`)
    return { transporter, isTestAccount, config: transporterConfig }
  }

  // 2. Fallback: Automated Ethereal test mailbox for seamless zero-config local development
  try {
    const testAccount = await nodemailer.createTestAccount()
    transporterConfig = {
      host: testAccount.smtp.host,
      port: testAccount.smtp.port,
      secure: testAccount.smtp.secure,
      auth: {
        user: testAccount.user,
        pass: testAccount.pass
      }
    }
    transporter = nodemailer.createTransport(transporterConfig)
    isTestAccount = true
    console.log(`[Nodemailer] SMTP_PASS not set in .env. Initialized automated test mailbox: ${testAccount.user}`)
    console.log(`[Nodemailer] To deliver real emails to actual recipient inboxes, configure your 16-character App Password in .env as SMTP_PASS.`)
    return { transporter, isTestAccount, config: transporterConfig }
  } catch (err) {
    console.warn('[Nodemailer] Could not create Ethereal test account, using JSON stream transporter:', err.message)
    transporter = nodemailer.createTransport({
      jsonTransport: true
    })
    isTestAccount = true
    return { transporter, isTestAccount, config: { host: 'json-fallback' } }
  }
}

/**
 * Dispatch an email strictly using Nodemailer to a real recipient.
 *
 * @param {Object} options
 * @param {string} options.to - Real recipient email address
 * @param {string} [options.recipientName] - Recipient full name
 * @param {string} options.subject - Email subject
 * @param {string} options.html - Rich HTML email body
 * @param {string} [options.text] - Plaintext fallback
 * @param {string} [options.from] - Custom sender address
 */
export async function sendEmail({ to, recipientName, subject, html, text, from }) {
  if (!to || !to.trim()) {
    throw new Error('Recipient email address ("to") is required.')
  }

  const targetEmail = to.trim()
  const emailSubject = subject || 'Municipal Market Administration Notification'
  const emailHtml = html || `<p>${text || subject}</p>`
  const emailText = text || subject

  const { transporter: activeTransporter, isTestAccount: isTest } = await getTransporter()
  const env = getFreshEnv()

  const defaultFrom = env.SMTP_FROM ||
    env.VITE_SMTP_FROM ||
    `"Office of the Market Supervisor" <${env.SMTP_USER || 'tawoayie@gmail.com'}>`

  const mailOptions = {
    from: from || defaultFrom,
    to: recipientName ? `"${recipientName}" <${targetEmail}>` : targetEmail,
    subject: emailSubject,
    text: emailText,
    html: emailHtml
  }

  const info = await activeTransporter.sendMail(mailOptions)

  let previewUrl = null
  if (isTest && nodemailer.getTestMessageUrl) {
    previewUrl = nodemailer.getTestMessageUrl(info)
    console.log(`[Nodemailer Preview URL]: ${previewUrl}`)
  }

  console.log(`[Nodemailer] Email dispatched to "${targetEmail}" (Message ID: ${info.messageId})${isTest ? ' [Test Account]' : ' [Real Delivery]'}`)

  return {
    success: true,
    messageId: info.messageId,
    previewUrl,
    isTestAccount: isTest,
    sender: mailOptions.from,
    recipient: targetEmail,
    subject: emailSubject,
    transport: isTest ? 'nodemailer-ethereal-test' : 'nodemailer-smtp'
  }
}

/**
 * Verify transporter connectivity
 */
export async function verifyTransporter() {
  const env = getFreshEnv()
  try {
    const { transporter: activeTransporter, isTestAccount: isTest, config } = await getTransporter()
    await activeTransporter.verify()
    return {
      connected: true,
      isTestAccount: isTest,
      host: config.host,
      port: config.port,
      sender: env.SMTP_USER || 'tawoayie@gmail.com',
      mode: isTest ? 'Nodemailer Ethereal Sandbox (Configure SMTP_PASS in .env for real delivery)' : 'Nodemailer SMTP (Real Delivery)'
    }
  } catch (err) {
    return {
      connected: false,
      error: err.message,
      host: env.SMTP_HOST || 'smtp.gmail.com',
      sender: env.SMTP_USER || 'tawoayie@gmail.com'
    }
  }
}

export default {
  getTransporter,
  sendEmail,
  verifyTransporter
}


