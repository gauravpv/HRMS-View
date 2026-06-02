package com.app.dto;

import com.app.model.Users;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserListItem {
    private final Users user;
    private final String roleName;

    public String getRoleCssClass() {
        if (roleName != null && roleName.equalsIgnoreCase("ADMIN")) {
            return "hrms-role-pill--admin";
        }
        if (roleName != null && roleName.equalsIgnoreCase("EDITOR")) {
            return "hrms-role-pill--editor";
        }
        return "hrms-role-pill--user";
    }
}
