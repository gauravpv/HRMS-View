-- Stored procedures (SP_MOVE_* / SP_MOVE_*_MAIN) log failures to sp_error_log.
-- This table is not always included in HRMS dumps; create it before granting privileges.
-- Run as MySQL admin. Adjust the GRANT user if your app account differs.

USE hrms_bre;

CREATE TABLE IF NOT EXISTS sp_error_log (
    ID INT NOT NULL AUTO_INCREMENT,
    Error TEXT,
    Added_on TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Many procedures write to infosec_bre; create only if that database exists on your server.
CREATE DATABASE IF NOT EXISTS infosec_bre;

USE infosec_bre;

CREATE TABLE IF NOT EXISTS sp_error_log (
    ID INT NOT NULL AUTO_INCREMENT,
    Error TEXT,
    Added_on TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Privileges for the application user (production example)
GRANT INSERT ON hrms_bre.sp_error_log TO 'brehrmsproddb0admin'@'%';
GRANT INSERT ON infosec_bre.sp_error_log TO 'brehrmsproddb0admin'@'%';

FLUSH PRIVILEGES;
