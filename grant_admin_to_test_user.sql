-- Grant ADMIN role to test user
UPDATE users SET role = 'ADMIN' WHERE username = 'test@sensorvision.com';
