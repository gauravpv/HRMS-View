package com.app.support;

/**
 * Values stored in {@code users.IS_ENABLED}.
 */
public final class UserAccountStatus {

    public static final int ACTIVE = 0;
    public static final int DEACTIVATED = 1;
    /** New user: may sign in once; app use blocked until an admin activates the account. */
    public static final int PENDING_APPROVAL = 2;

    private UserAccountStatus() {
    }

    public static boolean isActive(Integer isEnabled) {
        return isEnabled != null && isEnabled == ACTIVE;
    }

    public static boolean isPendingApproval(Integer isEnabled) {
        return isEnabled != null && isEnabled == PENDING_APPROVAL;
    }

    public static boolean canAuthenticate(Integer isEnabled) {
        return isActive(isEnabled) || isPendingApproval(isEnabled);
    }
}
