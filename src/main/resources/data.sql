-- ========================================================
-- CivicConnect - Initial Seed Data Script
-- ========================================================

-- Categories
INSERT INTO categories (name, description, icon, active) VALUES
('Road Damage', 'Potholes, cracks, and road surface hazards', 'fas fa-road', TRUE),
('Streetlight', 'Non-functional, broken, or flickering street lights', 'fas fa-lightbulb', TRUE),
('Garbage', 'Garbage overflow, uncollected trash, and waste accumulation', 'fas fa-trash', TRUE),
('Water Leakage', 'Main pipe leaks, burst lines, and clean water wastage', 'fas fa-tint', TRUE),
('Drainage', 'Blocked storm drains, sewage overflow, and waterlogging', 'fas fa-water', TRUE),
('Public Safety', 'Open manholes, fallen electrical lines, and hazards', 'fas fa-shield-alt', TRUE),
('Other', 'General community issues and municipal queries', 'fas fa-ellipsis-h', TRUE)
ON DUPLICATE KEY UPDATE name=name;

-- Departments
INSERT INTO departments (name, description, contact_email, active) VALUES
('Public Works', 'Roads, pavements, bridges, and infrastructure repair', 'publicworks@civicconnect.com', TRUE),
('Electrical Services', 'Street illumination, power lines, and electrical safety', 'electrical@civicconnect.com', TRUE),
('Sanitation & Waste', 'Garbage disposal, landfill management, and public hygiene', 'sanitation@civicconnect.com', TRUE),
('Water & Sewerage', 'Water supply, leak fixing, and storm water drainage', 'water@civicconnect.com', TRUE),
('Public Safety & Traffic', 'Emergency municipal hazards and traffic obstruction', 'safety@civicconnect.com', TRUE)
ON DUPLICATE KEY UPDATE name=name;
