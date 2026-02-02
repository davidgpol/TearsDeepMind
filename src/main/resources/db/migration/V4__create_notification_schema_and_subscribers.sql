-- V4: Create notification schema and subscribers table
CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.subscribers (
    email VARCHAR(255) PRIMARY KEY,
    name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert a default admin subscriber
INSERT INTO notification.subscribers (email, name, is_active)
VALUES ('admin@tearsdeepmind.com', 'Admin User', TRUE);
