-- Create a test user for local development
-- Password: 'password123' (BCrypt encoded)

-- Insert the test user
INSERT INTO users (USER_NAME, PASSWORD, EMAIL, IS_ENABLED, IS_ACTIVE)
VALUES ('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsQ0fEnxRiT3.SOXbC', 'testuser@example.com', 0, 1);

-- Get the user ID of the newly created user
SET @user_id = LAST_INSERT_ID();

-- Create admin role if not exists
INSERT IGNORE INTO roles (ROLE_NAME) VALUES ('ADMIN');
INSERT IGNORE INTO roles (ROLE_NAME) VALUES ('USER');

-- Get role IDs
SET @admin_role_id = (SELECT ROLE_ID FROM roles WHERE ROLE_NAME = 'ADMIN');
SET @user_role_id = (SELECT ROLE_ID FROM roles WHERE ROLE_NAME = 'USER');

-- Assign USER role to the test user (change to @admin_role_id for admin access)
INSERT INTO users_roles (USER_ID, ROLE_ID) VALUES (@user_id, @user_role_id);

-- Verify the user was created
SELECT u.USER_ID, u.USER_NAME, u.EMAIL, u.IS_ENABLED, u.IS_ACTIVE, r.ROLE_NAME
FROM users u
LEFT JOIN users_roles ur ON u.USER_ID = ur.USER_ID
LEFT JOIN roles r ON ur.ROLE_ID = r.ROLE_ID
WHERE u.USER_NAME = 'testuser';
