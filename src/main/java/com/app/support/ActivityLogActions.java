package com.app.support;

public final class ActivityLogActions {

    public static final String CREATED = "CREATED";
    public static final String UPLOAD = "UPLOAD";
    public static final String MOVE_TO_MASTER = "MOVE_TO_MASTER";
    public static final String MOVE_TO_MAIN = "MOVE_TO_MAIN";
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";

    private ActivityLogActions() {
    }

    public static String labelFor(String action) {
        if (action == null) {
            return "Activity";
        }
        return switch (action) {
            case CREATED -> "Data submitted";
            case UPLOAD -> "Bulk upload";
            case MOVE_TO_MASTER -> "Moved to master";
            case MOVE_TO_MAIN -> "Moved to main";
            case LOGIN -> "Signed in";
            case LOGOUT -> "Signed out";
            default -> action.replace('_', ' ').toLowerCase();
        };
    }

    public static String iconFor(String action) {
        if (action == null) {
            return "info";
        }
        return switch (action) {
            case LOGIN -> "login";
            case LOGOUT -> "logout";
            case UPLOAD -> "cloud_upload";
            case MOVE_TO_MASTER, MOVE_TO_MAIN -> "swap_horiz";
            case CREATED -> "edit_note";
            default -> "history";
        };
    }
}
