package com.app.dto;

import com.app.model.Users;
import com.app.security.HrmsAuthorities;
import com.app.support.UserAccountStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserListItem {
    private final Users user;
    private final String roleName;

    public String getRoleCssClass() {
        if (HrmsAuthorities.ADMIN.equalsIgnoreCase(roleName)) {
            return "hrms-role-pill--admin";
        }
        return "hrms-role-pill--user";
    }

    public boolean isAccountActive() {
        return UserAccountStatus.isActive(user.getIsEnabled());
    }

    public boolean canDeactivate() {
        return isAccountActive();
    }

    public boolean canActivate() {
        return !isAccountActive();
    }

    public String getAccountStatusLabel() {
        Integer enabled = user.getIsEnabled();
        if (enabled == null) {
            return "Unknown";
        }
        return switch (enabled) {
            case UserAccountStatus.ACTIVE -> "Active";
            case UserAccountStatus.DEACTIVATED -> "Inactive";
            case UserAccountStatus.PENDING_APPROVAL -> "Pending approval";
            default -> "Unknown";
        };
    }

    public String getAccountStatusCssClass() {
        Integer enabled = user.getIsEnabled();
        if (enabled == null) {
            return "hrms-user-status--unknown";
        }
        return switch (enabled) {
            case UserAccountStatus.ACTIVE -> "hrms-user-status--active";
            case UserAccountStatus.DEACTIVATED -> "hrms-user-status--inactive";
            case UserAccountStatus.PENDING_APPROVAL -> "hrms-user-status--pending";
            default -> "hrms-user-status--unknown";
        };
    }
}
