-- Run once after importing production dumps into local Docker MySQL:
--   docker exec -i hrms-mysql mysql -uroot -phrmsroot < src/main/resources/db/local-mysql/fix-definer.sql
--
-- Procedures are created with DEFINER `brehrmsproddb0admin`@`%` which does not exist locally.

CREATE USER IF NOT EXISTS 'brehrmsproddb0admin'@'%' IDENTIFIED BY 'localdev';
GRANT ALL PRIVILEGES ON decision_rules_hrmsbre.* TO 'brehrmsproddb0admin'@'%';
FLUSH PRIVILEGES;
