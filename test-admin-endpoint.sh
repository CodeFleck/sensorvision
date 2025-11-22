#!/bin/bash
# Test admin dashboard endpoint with proper authentication

echo "Testing admin dashboard endpoint..."
echo ""

# First, try without auth
echo "1. Without authentication:"
curl -s -w "\nHTTP Status: %{http_code}\n" http://35.88.65.186.nip.io:8080/api/v1/admin/dashboard/stats
echo ""

# The endpoint requires authentication, so you'll need to:
# 1. Login to get a JWT token
# 2. Use that token to access the admin dashboard

echo "To test with authentication, you need to:"
echo "1. Login via the frontend"
echo "2. Get your JWT token from browser DevTools > Application > Local Storage"
echo "3. Run: curl -H 'Authorization: Bearer YOUR_TOKEN' http://35.88.65.186.nip.io:8080/api/v1/admin/dashboard/stats"
