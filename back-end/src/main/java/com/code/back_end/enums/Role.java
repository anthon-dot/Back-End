package com.code.back_end.enums;

public enum Role {
    ROLE_ADMIN,
    ROLE_STAKEHOLDER,
    ROLE_ENDORSEMENT_OFFICE,
    ROLE_MARKET_SUPERVISOR,
    ROLE_BPLO;

    public String roleName() {
        return name();
    }

    public String authority() {
        return name();
    }

    public String roleWithoutPrefix() {
        return name().startsWith("ROLE_") ? name().substring(5) : name();
    }

    public static Role fromString(String rawRole) {
        if (rawRole == null || rawRole.trim().isEmpty()) {
            return null;
        }
        String normalized = rawRole.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        switch (normalized) {
            case "ROLE_ADMIN":
            case "ROLE_TREASURER":
                return ROLE_ADMIN;
            case "ROLE_STAKEHOLDER":
            case "ROLE_TENANT":
            case "ROLE_APPLICANT":
                return ROLE_STAKEHOLDER;
            case "ROLE_MARKET_SUPERVISOR":
            case "ROLE_MARKETSUPERVISOR":
            case "ROLE_SUPERVISOR":
                return ROLE_MARKET_SUPERVISOR;
            case "ROLE_BPLO":
            case "ROLE_BPLO_OFFICE":
            case "ROLE_BPLOOFFICE":
                return ROLE_BPLO;
            case "ROLE_ENDORSEMENT_OFFICE":
            case "ROLE_ENDORSING_OFFICE":
            case "ROLE_ENDORISING_OFFICE":
            case "ROLE_ENDORSINGOFFICE":
            case "ROLE_ENDORSING_OFFICER":
                return ROLE_ENDORSEMENT_OFFICE;
            default:
                try {
                    return Role.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }
}
