package com.code.back_end.security;

import com.code.back_end.entity.User;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class SecurityService {

    private static final Set<String> ADMIN_ROLES =
            Set.of("ADMIN", "TREASURER");

    private static final Set<String> SUPERVISOR_ROLES =
            Set.of(
                    "SUPERVISOR",
                    "MARKETSUPERVISOR",
                    "MARKET_SUPERVISOR",
                    "BPLO_OFFICE",
                    "BPLOOFFICE",
                    "BPLO",
                    "ENDORSINGOFFICE",
                    "ENDORSING_OFFICE",
                    "ENDORSING_OFFICER",
                    "ENDORISING_OFFICE"
            );

    private static final Set<String> TENANT_ROLES =
            Set.of("TENANT", "STAKEHOLDER");

    private static final Set<String> APPLICANT_ROLES =
            Set.of("APPLICANT", "STAKEHOLDER");

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
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (
                authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated user not found"
                        )
                );
    }

    public String currentRole() {
        return normalizeRole(
                currentUser().getRole()
        );
    }

    public boolean isAdmin() {
        return ADMIN_ROLES.contains(currentRole());
    }

    public boolean isSupervisor() {
        return SUPERVISOR_ROLES.contains(currentRole());
    }

    public boolean isTenant() {
        return TENANT_ROLES.contains(currentRole());
    }

    public boolean isApplicant() {
        return APPLICANT_ROLES.contains(currentRole());
    }

    public boolean canManageOperations() {
        return isAdmin() || isSupervisor();
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException(
                    "Admin access is required"
            );
        }
    }

    public void requireSupervisorOrAdmin() {
        if (!canManageOperations()) {
            throw new AccessDeniedException(
                    "Supervisor or admin access is required"
            );
        }
    }

    public void requireTreasurerOrAdmin() {
        String role = currentRole();

        if ("ADMIN".equals(role) || "TREASURER".equals(role)) {
            return;
        }

        throw new AccessDeniedException(
                "Treasurer access is required"
        );
    }

    public void requireMarketSupervisorOrAdmin() {
        String role = currentRole();

        if (
                "ADMIN".equals(role)
                        || "MARKETSUPERVISOR".equals(role)
                        || "MARKET_SUPERVISOR".equals(role)
                        || "SUPERVISOR".equals(role)
        ) {
            return;
        }

        throw new AccessDeniedException(
                "Market supervisor access is required"
        );
    }

    public void requireBploOrAdmin() {
        String role = currentRole();

        if (
                "ADMIN".equals(role)
                        || "BPLO".equals(role)
                        || "BPLO_OFFICE".equals(role)
                        || "BPLOOFFICE".equals(role)
        ) {
            return;
        }

        throw new AccessDeniedException(
                "BPLO access is required"
        );
    }

    public void requireEndorsingOfficerOrAdmin() {
        String role = currentRole();

        if (
                "ADMIN".equals(role)
                        || "ENDORSING_OFFICE".equals(role)
                        || "ENDORISING_OFFICE".equals(role)
                        || "ENDORSING_OFFICER".equals(role)
                        || "ENDORSINGOFFICE".equals(role)
        ) {
            return;
        }

        throw new AccessDeniedException(
                "Endorsing officer access is required"
        );
    }

    public void requireSelfUserOrStaff(Long userId) {
        User user = currentUser();

        if (
                canManageOperations() ||
                user.getId().equals(userId)
        ) {
            return;
        }

        throw new AccessDeniedException(
                "You can only access your own user record"
        );
    }

    public void requireStakeholderOwnerOrStaff(Long ownerUserId) {
        User user = currentUser();

        if (
                canManageOperations() ||
                user.getId().equals(ownerUserId)
        ) {
            return;
        }

        throw new AccessDeniedException(
                "You can only access your own records"
        );
    }

    public void requireVerifiedStakeholderOwnerOrStaff(Long ownerUserId) {
        User user = currentUser();

        if (canManageOperations()) {
            return;
        }

        if (!user.getId().equals(ownerUserId)) {
            throw new AccessDeniedException(
                    "You can only access your own records"
            );
        }

        boolean verified =
                stakeholderRepository.findByUser_Id(ownerUserId)
                        .map(stakeholder ->
                                Boolean.TRUE.equals(stakeholder.getVerified())
                                        && Boolean.TRUE.equals(stakeholder.getApplicantFeePaid())
                        )
                        .orElse(false);

        if (!verified) {
            throw new AccessDeniedException(
                    "Applicant fee payment required before dashboard access."
            );
        }
    }

    public String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        return role
                .replace("ROLE_", "")
                .trim()
                .toUpperCase();
    }
}
