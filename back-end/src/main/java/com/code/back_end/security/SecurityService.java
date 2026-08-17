package com.code.back_end.security;

import com.code.back_end.entity.User;
import com.code.back_end.enums.Role;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Set;

@Service
public class SecurityService {

    private static final Set<Role> ADMIN_ROLES = EnumSet.of(Role.ROLE_ADMIN);

    private static final Set<Role> SUPERVISOR_ROLES = EnumSet.of(
            Role.ROLE_MARKET_SUPERVISOR,
            Role.ROLE_BPLO,
            Role.ROLE_ENDORSEMENT_OFFICE
    );

    private static final Set<Role> STAKEHOLDER_ROLES = EnumSet.of(Role.ROLE_STAKEHOLDER);

    private final UserRepository userRepository;
    private final StakeholderRepository stakeholderRepository;

    public SecurityService(
            UserRepository userRepository,
            StakeholderRepository stakeholderRepository
    ) {
        this.userRepository = userRepository;
        this.stakeholderRepository = stakeholderRepository;
    }

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    public Role currentRoleEnum() {
        return Role.fromString(currentUser().getRole());
    }

    public String currentRole() {
        Role role = currentRoleEnum();
        return role != null ? role.roleWithoutPrefix() : normalizeRole(currentUser().getRole());
    }

    public boolean isAdmin() {
        Role role = currentRoleEnum();
        return role != null && ADMIN_ROLES.contains(role);
    }

    public boolean isSupervisor() {
        Role role = currentRoleEnum();
        return role != null && SUPERVISOR_ROLES.contains(role);
    }

    public boolean isTenant() {
        Role role = currentRoleEnum();
        return role != null && STAKEHOLDER_ROLES.contains(role);
    }

    public boolean isApplicant() {
        return isTenant();
    }

    public boolean canManageOperations() {
        return isAdmin() || isSupervisor();
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Admin access is required");
        }
    }

    public void requireSupervisorOrAdmin() {
        if (!canManageOperations()) {
            throw new AccessDeniedException("Supervisor or admin access is required");
        }
    }

    public void requireTreasurerOrAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Treasurer access is required");
        }
    }

    public void requireMarketSupervisorOrAdmin() {
        Role role = currentRoleEnum();
        if (isAdmin() || role == Role.ROLE_MARKET_SUPERVISOR) {
            return;
        }
        throw new AccessDeniedException("Market supervisor access is required");
    }

    public void requireBploOrAdmin() {
        Role role = currentRoleEnum();
        if (isAdmin() || role == Role.ROLE_BPLO) {
            return;
        }
        throw new AccessDeniedException("BPLO access is required");
    }

    public void requireEndorsingOfficerOrAdmin() {
        Role role = currentRoleEnum();
        if (isAdmin() || role == Role.ROLE_ENDORSEMENT_OFFICE) {
            return;
        }
        throw new AccessDeniedException("Endorsing officer access is required");
    }

    public void requireSelfUserOrStaff(Long userId) {
        User user = currentUser();
        if (canManageOperations() || (user.getId() != null && user.getId().equals(userId))) {
            return;
        }
        throw new AccessDeniedException("You can only access your own user record");
    }

    public void requireStakeholderOwnerOrStaff(Long ownerUserId) {
        User user = currentUser();
        if (canManageOperations() || (user.getId() != null && user.getId().equals(ownerUserId))) {
            return;
        }
        throw new AccessDeniedException("You can only access your own records");
    }

    public void requireVerifiedStakeholderOwnerOrStaff(Long ownerUserId) {
        User user = currentUser();
        if (canManageOperations()) {
            return;
        }

        if (user.getId() == null || !user.getId().equals(ownerUserId)) {
            throw new AccessDeniedException("You can only access your own records");
        }

        boolean verified = stakeholderRepository.findByUser_Id(ownerUserId)
                .map(stakeholder -> Boolean.TRUE.equals(stakeholder.getVerified())
                        && Boolean.TRUE.equals(stakeholder.getApplicantFeePaid()))
                .orElse(false);

        if (!verified) {
            throw new AccessDeniedException("Applicant fee payment required before dashboard access.");
        }
    }

    public String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        Role parsed = Role.fromString(role);
        if (parsed != null) {
            return parsed.roleWithoutPrefix();
        }
        return role.replace("ROLE_", "").trim().toUpperCase();
    }
}
