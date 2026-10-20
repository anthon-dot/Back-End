import { ROLES } from '../config/constants.js';

/**
 * RBAC Middleware: Enforces that authenticated caller has one of the required roles.
 */
export function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        error: 'Authentication required prior to authorization check.'
      });
    }

    const userRole = req.user.role;
    const isAllowed = allowedRoles.includes(userRole) || userRole === ROLES.ADMIN;

    if (!isAllowed) {
      return res.status(403).json({
        success: false,
        error: `Access denied. Action requires one of: [${allowedRoles.join(', ')}]. Caller role: ${userRole}`
      });
    }

    next();
  };
}

/**
 * Staff Guard: Permits any municipal staff actor (Supervisor, Mayor, Treasurer, Collector/Inspector, Admin).
 */
export function requireStaff(req, res, next) {
  const staffRoles = [
    ROLES.MARKET_SUPERVISOR,
    ROLES.MAYOR,
    ROLES.TREASURER,
    ROLES.COLLECTOR_INSPECTOR,
    ROLES.ADMIN
  ];
  return requireRole(...staffRoles)(req, res, next);
}

/**
 * Record Ownership Guard: Ensures caller owns the target resource or is municipal staff.
 */
export function requireOwnerOrStaff(getOwnerUserIdFn) {
  return async (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ success: false, error: 'Authentication required.' });
    }

    // Staff always permitted
    const staffRoles = [
      ROLES.MARKET_SUPERVISOR,
      ROLES.MAYOR,
      ROLES.TREASURER,
      ROLES.COLLECTOR_INSPECTOR,
      ROLES.ADMIN
    ];
    if (staffRoles.includes(req.user.role)) {
      return next();
    }

    try {
      const ownerUserId = await getOwnerUserIdFn(req);
      if (ownerUserId && String(ownerUserId) === String(req.user.id)) {
        return next();
      }

      return res.status(403).json({
        success: false,
        error: 'Access denied. You do not have permission to view or modify this record.'
      });
    } catch (err) {
      return res.status(500).json({
        success: false,
        error: 'Error evaluating record ownership authorization.'
      });
    }
  };
}
