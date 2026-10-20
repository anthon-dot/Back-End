import { supabase } from '../config/supabase.js';
import { ROLES } from '../config/constants.js';
import { logAudit } from '../middleware/auditLogger.js';

export async function getMe(req, res, next) {
  try {
    return res.json({
      success: true,
      user: req.user
    });
  } catch (err) {
    next(err);
  }
}

export async function register(req, res, next) {
  try {
    const { username, password, email, fullName, phoneNumber, role } = req.body;

    if (!username || !password) {
      return res.status(400).json({ success: false, error: 'Username and password are required.' });
    }

    const cleanUsername = username.trim();
    const cleanEmail = email
      ? email.trim().toLowerCase()
      : (cleanUsername.includes('@')
          ? cleanUsername.toLowerCase()
          : `${cleanUsername.toLowerCase().replace(/[^a-z0-9_.-]/g, '')}@manticao.market`);
    const assignedRole = (role || (cleanUsername.toLowerCase() === 'admin' ? ROLES.ADMIN : ROLES.APPLICANT)).toUpperCase();

    // 1. Try creating user via Supabase Auth Admin API or standard signUp
    let authUser = null;
    let createError = null;

    if (supabase.auth?.admin) {
      const { data, error } = await supabase.auth.admin.createUser({
        email: cleanEmail,
        password: password,
        email_confirm: true,
        user_metadata: {
          username: cleanUsername,
          name: fullName || cleanUsername,
          role: assignedRole
        }
      });
      if (error && !error.message.toLowerCase().includes('already')) {
        createError = error;
      }
      authUser = data?.user;
    }

    if (!authUser && !createError) {
      const { data, error } = await supabase.auth.signUp({
        email: cleanEmail,
        password: password,
        options: {
          data: {
            username: cleanUsername,
            name: fullName || cleanUsername,
            role: assignedRole
          }
        }
      });
      if (error && !error.message.toLowerCase().includes('already')) {
        createError = error;
      }
      authUser = data?.user;
    }

    // 2. Fetch or create user profile
    let userId = authUser?.id;
    if (!userId) {
      const { data: existingProf } = await supabase
        .from('user_profiles')
        .select('id')
        .eq('email', cleanEmail)
        .maybeSingle();
      userId = existingProf?.id;
    }

    if (userId) {
      // Upsert profile
      await supabase.from('user_profiles').upsert({
        id: userId,
        username: cleanUsername,
        full_name: fullName || cleanUsername,
        email: cleanEmail,
        phone_number: phoneNumber || '',
        role: assignedRole,
        is_active: true,
        updated_at: new Date().toISOString()
      }, { onConflict: 'id' });

      // If applicant / vendor, ensure stakeholders record exists
      if (assignedRole === ROLES.APPLICANT || assignedRole === ROLES.VENDOR || assignedRole === 'STAKEHOLDER') {
        const { data: existingSh } = await supabase
          .from('stakeholders')
          .select('id')
          .eq('user_id', userId)
          .maybeSingle();

        if (!existingSh) {
          const names = (fullName || cleanUsername).split(' ');
          const firstName = names[0] || cleanUsername;
          const lastName = names.slice(1).join(' ') || 'Applicant';
          await supabase.from('stakeholders').insert({
            user_id: userId,
            first_name: firstName,
            last_name: lastName,
            business_name: `${firstName}'s Market Business`,
            business_type: 'General Merchandise',
            contact: phoneNumber || '09000000000',
            email: cleanEmail,
            address: 'Manticao, Misamis Oriental',
            application_status: 'NEW',
            onboarding_status: 'NEW'
          });
        }
      }

      await logAudit({
        userId,
        role: assignedRole,
        action: 'USER_REGISTRATION',
        entityName: 'user_profiles',
        entityId: userId,
        details: { username: cleanUsername, role: assignedRole }
      });
    }

    // 3. Immediately obtain authenticated session so client doesn't need to sign in separately
    let token = null;
    let session = null;
    try {
      const { data: authData } = await supabase.auth.signInWithPassword({
        email: cleanEmail,
        password
      });
      if (authData?.session) {
        token = authData.session.access_token;
        session = authData.session;
      }
    } catch (_) {}

    return res.status(201).json({
      success: true,
      message: 'Account created successfully.',
      token,
      session,
      user: {
        id: userId,
        username: cleanUsername,
        email: cleanEmail,
        role: assignedRole
      }
    });
  } catch (err) {
    next(err);
  }
}

export async function login(req, res, next) {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ success: false, error: 'Username and password are required.' });
    }

    const lowerId = username.trim().toLowerCase();
    const MUNICIPAL_MAP = {
      admin: 'admin@manticao.market',
      supervisor: 'supervisor@manticao.gov.ph',
      market_supervisor: 'supervisor@manticao.gov.ph',
      mayor: 'mayor@manticao.gov.ph',
      municipal_mayor: 'mayor@manticao.gov.ph',
      treasurer: 'treasurer@manticao.gov.ph',
      collector: 'collector@manticao.gov.ph',
      inspector: 'collector@manticao.gov.ph',
      collector_inspector: 'collector@manticao.gov.ph',
      vendor: 'vendor@manticao.market',
      applicant: 'applicant@manticao.market'
    };

    let emailToAuth = username.trim();
    if (!emailToAuth.includes('@')) {
      if (MUNICIPAL_MAP[lowerId]) {
        emailToAuth = MUNICIPAL_MAP[lowerId];
      } else {
        const { data: prof } = await supabase
          .from('user_profiles')
          .select('email')
          .ilike('username', username.trim())
          .maybeSingle();
        if (prof?.email) {
          emailToAuth = prof.email;
        } else {
          emailToAuth = `${lowerId.replace(/[^a-z0-9_.-]/g, '')}@manticao.market`;
        }
      }
    }

    const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
      email: emailToAuth,
      password
    });

    if (authError || !authData?.session) {
      return res.status(401).json({ success: false, error: authError?.message || 'Invalid username or password.' });
    }

    const authUser = authData.user;
    const session = authData.session;

    const [profRes, shRes] = await Promise.all([
      supabase.from('user_profiles').select('*').eq('id', authUser.id).maybeSingle(),
      supabase.from('stakeholders').select('id').eq('user_id', authUser.id).maybeSingle()
    ]);

    const profile = profRes?.data;
    const stakeholder = shRes?.data;
    const resolvedRole = profile?.role || authUser.user_metadata?.role || (lowerId === 'admin' ? ROLES.ADMIN : ROLES.APPLICANT);

    await supabase.from('login_history').insert({
      user_id: authUser.id,
      ip_address: req.ip || '127.0.0.1',
      user_agent: req.headers['user-agent'] || 'Web Portal',
      login_time: new Date().toISOString(),
      success: true
    });

    return res.json({
      success: true,
      token: session.access_token,
      user: {
        id: authUser.id,
        email: authUser.email,
        username: profile?.username || username,
        role: resolvedRole
      },
      profile,
      stakeholderId: stakeholder ? String(stakeholder.id) : ''
    });
  } catch (err) {
    next(err);
  }
}

export async function updateProfile(req, res, next) {
  try {
    const { fullName, phoneNumber } = req.body;
    const { data, error } = await supabase
      .from('user_profiles')
      .update({
        full_name: fullName,
        phone_number: phoneNumber,
        updated_at: new Date().toISOString()
      })
      .eq('id', req.user.id)
      .select()
      .single();

    if (error) throw error;
    return res.json({ success: true, profile: data });
  } catch (err) {
    next(err);
  }
}
