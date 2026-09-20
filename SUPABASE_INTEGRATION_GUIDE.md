# Frontend Integration Guide: Connecting Directly to Supabase

This guide explains how to connect your frontend (React, Vue, Vite, Next.js, or plain JS) directly to **Supabase**, completely replacing the Spring Boot Java backend endpoints.

---

## 1. Setup in Supabase Dashboard

1. Log into [supabase.com](https://supabase.com) and open your project.
2. **Execute Database Schema**:
   - Go to the **SQL Editor** tab (left sidebar).
   - Click **New Query**.
   - Copy and paste the entire contents of [`supabase/schema.sql`](./supabase/schema.sql).
   - Click **Run**.
3. **Configure Storage**:
   - In the **SQL Editor**, open another query tab.
   - Copy and paste [`supabase/storage.sql`](./supabase/storage.sql) and click **Run**.
   - Verify under **Storage** that the `uploads` bucket exists and is public.
4. **Get API Keys**:
   - Go to **Project Settings** -> **API**.
   - Copy your **Project URL** (`https://<project-id>.supabase.co`).
   - Copy your **`anon` `public` key**.

---

## 2. Install Supabase Client in Frontend

In your frontend project folder:
```bash
npm install @supabase/supabase-js
```

Create a Supabase client helper (`src/supabaseClient.js` or `.ts`):
```typescript
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://your-project.supabase.co'
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'your-anon-key'

export const supabase = createClient(supabaseUrl, supabaseAnonKey)
```

---

## 3. Replacing Spring Boot API Endpoints

### A. Authentication & Registration
*Old: `POST /api/auth/login` and `POST /api/auth/register`*

**User Registration:**
```javascript
import { supabase } from './supabaseClient'

export async function registerUser(email, password, username, fullName) {
  const { data, error } = await supabase.auth.signUp({
    email,
    password,
    options: {
      data: {
        username: username,
        name: fullName,
        role: 'USER'
      }
    }
  })
  if (error) throw error
  return data
}
```

**User Login:**
```javascript
export async function loginUser(email, password) {
  const { data, error } = await supabase.auth.signInWithPassword({
    email,
    password
  })
  if (error) throw error
  return data // Contains session and user details
}
```

**Get Current User Profile:**
```javascript
export async function getCurrentProfile() {
  const { data: { user } } = await supabase.auth.getUser()
  if (!user) return null

  const { data: profile, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', user.id)
    .single()

  return profile
}
```

---

### B. Browsing Stalls
*Old: `GET /api/stalls` and `GET /api/stall-types`*

```javascript
// Fetch all available stalls
export async function getAvailableStalls() {
  const { data, error } = await supabase
    .from('stalls')
    .select('*')
    .eq('status', 'AVAILABLE')
    .order('stall_no', { ascending: true })

  if (error) throw error
  return data
}

// Fetch stall types
export async function getStallTypes() {
  const { data, error } = await supabase
    .from('stall_types')
    .select('*')
    .eq('status', 'ACTIVE')

  if (error) throw error
  return data
}
```

---

### C. Submitting Business Applications
*Old: `POST /api/business-applications`*

```javascript
export async function submitApplication(applicationData) {
  const { data: { user } } = await supabase.auth.getUser()
  if (!user) throw new Error("Must be logged in to submit application")

  const { data, error } = await supabase
    .from('business_applications')
    .insert({
      user_id: user.id,
      business_name: applicationData.businessName,
      business_type: applicationData.businessType,
      first_name: applicationData.firstName,
      last_name: applicationData.lastName,
      contact: applicationData.contact,
      email: user.email,
      address: applicationData.address,
      selected_stall_id: applicationData.stallId
    })
    .select()
    .single()

  if (error) throw error
  return data
}
```

---

### D. Uploading Documents to Supabase Storage
*Old: `POST /api/stakeholder-documents/upload`*

```javascript
export async function uploadStakeholderDocument(file, stakeholderId, documentType) {
  const fileExt = file.name.split('.').pop()
  const fileName = `${stakeholderId}_${Date.now()}.${fileExt}`
  const filePath = `documents/${fileName}`

  // 1. Upload to Supabase Storage bucket 'uploads'
  const { error: uploadError } = await supabase.storage
    .from('uploads')
    .upload(filePath, file)

  if (uploadError) throw uploadError

  // 2. Get Public URL
  const { data: { publicUrl } } = supabase.storage
    .from('uploads')
    .getPublicUrl(filePath)

  // 3. Save reference in stakeholder_documents table
  const { data, error: dbError } = await supabase
    .from('stakeholder_documents')
    .insert({
      stakeholder_id: stakeholderId,
      document_type: documentType,
      file_name: file.name,
      file_path: publicUrl
    })
    .select()
    .single()

  if (dbError) throw dbError
  return data
}
```

---

### E. Realtime Notifications
*Old: Polling `GET /api/notifications`*

Listen for live notifications without refreshing:
```javascript
export function subscribeToNotifications(stakeholderId, onNewNotification) {
  return supabase
    .channel(`notifications-${stakeholderId}`)
    .on(
      'postgres_changes',
      {
        event: 'INSERT',
        schema: 'public',
        table: 'notifications',
        filter: `stakeholder_id=eq.${stakeholderId}`
      },
      (payload) => {
        onNewNotification(payload.new)
      }
    )
    .subscribe()
}
```

---

### F. Triggering Supabase Edge Functions (Hono-Powered)
*Replaces complex multi-stage Java service workflows and AI report generators.*

#### 1. Treasurer Approval (Advance Payment)
```javascript
export async function treasurerApprove({ stakeholderId, amount, totalAdvanceAmount, referenceNo }) {
  const { data, error } = await supabase.functions.invoke('approval-workflow/treasurer-approve', {
    body: { stakeholderId, amount, totalAdvanceAmount, referenceNo }
  })
  if (error) throw error
  return data
}
```

#### 2. Stall Assignment & Lease Contract Creation
```javascript
export async function assignStallAndContract({ stakeholderId, stallId, startDate, endDate, terms }) {
  const { data, error } = await supabase.functions.invoke('approval-workflow/assign-stall', {
    body: { stakeholderId, stallId, startDate, endDate, terms }
  })
  if (error) throw error
  return data
}
```

#### 3. BPLO Approval
```javascript
export async function bploApprove({ stakeholderId, remarks }) {
  const { data, error } = await supabase.functions.invoke('approval-workflow/bplo-approve', {
    body: { stakeholderId, remarks }
  })
  if (error) throw error
  return data
}
```

#### 4. Final Endorsement
```javascript
export async function finalEndorse({ stakeholderId, remarks }) {
  const { data, error } = await supabase.functions.invoke('approval-workflow/final-endorse', {
    body: { stakeholderId, remarks }
  })
  if (error) throw error
  return data
}
```

#### 5. Permit Payment & Tenant Activation
```javascript
export async function permitPaymentAndActivate({ stakeholderId, amount, referenceNo }) {
  const { data, error } = await supabase.functions.invoke('approval-workflow/permit-payment', {
    body: { stakeholderId, amount, referenceNo }
  })
  if (error) throw error
  return data
}
```

#### 6. Reject Application
```javascript
export async function rejectApplication({ stakeholderId, stage, remarks }) {
  const { data, error } = await supabase.functions.invoke('approval-workflow/reject', {
    body: { stakeholderId, stage, remarks }
  })
  if (error) throw error
  return data
}
```

#### 7. AI Market Intelligence Briefing (Google Gemini)
```javascript
export async function getMarketAIBriefing() {
  const { data, error } = await supabase.functions.invoke('ai-insights/summary')
  if (error) throw error
  return data
}
```

---

## 4. Deployment via CLI or GitHub Actions

### Using NPM Scripts (Local CLI):
```bash
# 1. Login and link project
npm run supabase:login
npm run supabase:link

# 2. Push all database migrations
npm run supabase:db:push

# 3. Deploy all Edge Functions (approval-workflow + ai-insights)
npm run supabase:functions:deploy

# Or deploy everything at once:
npm run supabase:deploy
```

### Automated CI/CD (GitHub Actions):
Any push to `main` triggers `.github/workflows/deploy-supabase.yml` automatically deploying migrations and edge functions.

