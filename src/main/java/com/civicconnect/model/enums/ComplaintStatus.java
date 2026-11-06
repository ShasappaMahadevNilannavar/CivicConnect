package com.civicconnect.model.enums;

public enum ComplaintStatus {
    SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, IN_PROGRESS,
    RESOLVED_PENDING_VERIFICATION, VERIFIED_RESOLVED;

    public String getDisplayName() {
        String raw = name().replace("_", " ");
        return raw.substring(0, 1) + raw.substring(1).toLowerCase();
    }
}
