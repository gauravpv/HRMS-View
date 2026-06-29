-- Optional: run once if EDITOR still exists in your roles catalog.
-- Maps user assignments from EDITOR to USER (adjust schema/table names if yours differ).

-- UPDATE users_roles ur
-- JOIN roles r_editor ON ur.role_id = r_editor.role_id AND UPPER(r_editor.role_name) = 'EDITOR'
-- JOIN roles r_user ON UPPER(r_user.role_name) = 'USER'
-- SET ur.role_id = r_user.role_id;

-- DELETE FROM roles WHERE UPPER(role_name) = 'EDITOR';
