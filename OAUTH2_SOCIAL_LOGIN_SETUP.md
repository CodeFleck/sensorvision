# OAuth2 Social Login Setup Guide

## Overview

This guide explains how to configure and use OAuth2 social login (Google and GitHub) in SensorVision.

## Implementation Status

✅ **Complete** - OAuth2 social login is fully implemented and ready to use.

---

## Prerequisites

Before enabling social login, you need to obtain OAuth2 credentials from Google and GitHub:

### 1. Google OAuth2 Setup

#### Create Google OAuth2 Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Navigate to **APIs & Services** > **Credentials**
4. Click **Create Credentials** > **OAuth client ID**
5. Select **Web application**
6. Configure the OAuth client:
   - **Name**: SensorVision
   - **Authorized JavaScript origins**:
     - `http://localhost:8080`
     - `http://localhost:3001`
   - **Authorized redirect URIs**:
     - `http://localhost:8080/login/oauth2/code/google`
     - `http://localhost:8080/oauth2/callback/google`
7. Click **Create**
8. Copy the **Client ID** and **Client Secret**

#### Enable Required APIs

1. In Google Cloud Console, go to **APIs & Services** > **Library**
2. Search for and enable:
   - Google+ API
   - People API

### 2. GitHub OAuth2 Setup

#### Create GitHub OAuth App

1. Go to [GitHub Settings](https://github.com/settings/developers)
2. Click **OAuth Apps** > **New OAuth App**
3. Fill in the application details:
   - **Application name**: SensorVision
   - **Homepage URL**: `http://localhost:3001`
   - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
4. Click **Register application**
5. Copy the **Client ID**
6. Click **Generate a new client secret**
7. Copy the **Client Secret** (you won't be able to see it again)

---

## Configuration

### 1. Environment Variables

Set the following environment variables with your OAuth2 credentials:

```bash
# Google OAuth2
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"

# GitHub OAuth2
export GITHUB_CLIENT_ID="your-github-client-id"
export GITHUB_CLIENT_SECRET="your-github-client-secret"
```

**Windows (PowerShell):**
```powershell
$env:GOOGLE_CLIENT_ID="your-google-client-id"
$env:GOOGLE_CLIENT_SECRET="your-google-client-secret"
$env:GITHUB_CLIENT_ID="your-github-client-id"
$env:GITHUB_CLIENT_SECRET="your-github-client-secret"
```

**Windows (Command Prompt):**
```cmd
set GOOGLE_CLIENT_ID=your-google-client-id
set GOOGLE_CLIENT_SECRET=your-google-client-secret
set GITHUB_CLIENT_ID=your-github-client-id
set GITHUB_CLIENT_SECRET=your-github-client-secret
```

### 2. Application Configuration

The OAuth2 configuration is already set up in `application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:your-google-client-id}
            client-secret: ${GOOGLE_CLIENT_SECRET:your-google-client-secret}
            scope:
              - email
              - profile
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          github:
            client-id: ${GITHUB_CLIENT_ID:your-github-client-id}
            client-secret: ${GITHUB_CLIENT_SECRET:your-github-client-secret}
            scope:
              - user:email
              - read:user
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
```

---

## How It Works

### Authentication Flow

1. User clicks "Login with Google" or "Login with GitHub" button in frontend
2. Frontend redirects to backend OAuth2 authorization endpoint
3. User is redirected to Google/GitHub for authentication
4. After successful authentication, OAuth2 provider redirects back to backend
5. Backend:
   - Receives OAuth2 user information
   - Creates or finds existing user in database
   - Generates JWT access and refresh tokens
   - Redirects to frontend callback URL with tokens
6. Frontend:
   - Extracts tokens from URL
   - Stores tokens in localStorage
   - Redirects user to dashboard

### OAuth2 Endpoints

| Provider | Authorization URL |
|----------|-------------------|
| Google   | `http://localhost:8080/oauth2/authorization/google` |
| GitHub   | `http://localhost:8080/oauth2/authorization/github` |

### Callback Handling

After successful OAuth2 authentication, the backend redirects to:

```
http://localhost:3001/oauth2/callback?accessToken={token}&refreshToken={token}
```

---

## Frontend Integration

### 1. Create OAuth2 Callback Handler

Create a new component to handle the OAuth2 callback:

**`frontend/src/pages/OAuth2Callback.tsx`:**

```typescript
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export const OAuth2Callback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setUser } = useAuth();

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');

    if (accessToken && refreshToken) {
      // Store tokens
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // Fetch current user
      fetch('/api/v1/auth/me', {
        headers: {
          'Authorization': `Bearer ${accessToken}`
        }
      })
        .then(res => res.json())
        .then(user => {
          setUser(user);
          navigate('/dashboard');
        })
        .catch(error => {
          console.error('Failed to fetch user:', error);
          navigate('/login?error=oauth2_failed');
        });
    } else {
      navigate('/login?error=oauth2_failed');
    }
  }, [searchParams, navigate, setUser]);

  return <div>Completing login...</div>;
};
```

### 2. Add OAuth2 Login Buttons

**`frontend/src/pages/Login.tsx`:**

```typescript
const LoginPage = () => {
  const handleGoogleLogin = () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  const handleGitHubLogin = () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/github';
  };

  return (
    <div>
      {/* Regular login form */}
      <form>
        {/* ... username/password fields ... */}
      </form>

      {/* OAuth2 login buttons */}
      <div className="social-login">
        <button onClick={handleGoogleLogin} type="button">
          <img src="/google-icon.svg" alt="Google" />
          Login with Google
        </button>

        <button onClick={handleGitHubLogin} type="button">
          <img src="/github-icon.svg" alt="GitHub" />
          Login with GitHub
        </button>
      </div>
    </div>
  );
};
```

### 3. Add Route for OAuth2 Callback

**`frontend/src/App.tsx`:**

```typescript
import { OAuth2Callback } from './pages/OAuth2Callback';

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/oauth2/callback" element={<OAuth2Callback />} />
      {/* ... other routes ... */}
    </Routes>
  );
}
```

---

## User Management for OAuth2 Users

### Automatic User Creation

When a user logs in via OAuth2 for the first time:

1. **Username**: Generated as `{email_prefix}_{provider}` (e.g., `john_google`)
   - If username exists, a number is appended (e.g., `john_google1`)
2. **Email**: From OAuth2 provider
3. **Name**: Parsed into firstName and lastName
4. **Password**: Random UUID (OAuth2 users don't use password login)
5. **Organization**: Assigned to "Default Organization"
6. **Email Verification**: Automatically verified (OAuth2 providers verify email)
7. **Roles**: Assigned "ROLE_USER" by default

### Linking OAuth2 to Existing Account

If a user with the same email already exists in the database, the OAuth2 login will use that existing account instead of creating a new one.

---

## Security Considerations

### 1. Redirect URI Validation

- Only configured redirect URIs are allowed
- Prevents authorization code interception attacks

### 2. State Parameter

- Spring Security automatically handles CSRF protection via state parameter
- Prevents cross-site request forgery attacks

### 3. Token Storage

- Access tokens are short-lived (24 hours)
- Refresh tokens allow token renewal without re-authentication
- Tokens are transmitted via HTTPS in production

### 4. Scope Limitations

- **Google**: Only requests `email` and `profile` (minimal permissions)
- **GitHub**: Only requests `user:email` and `read:user` (minimal permissions)

---

## Testing OAuth2 Login

### Local Testing

1. Set environment variables with your OAuth2 credentials
2. Start the backend:
   ```bash
   ./gradlew.bat bootRun
   ```
3. Start the frontend:
   ```bash
   cd frontend && npm run dev
   ```
4. Navigate to `http://localhost:3001/login`
5. Click "Login with Google" or "Login with GitHub"
6. Complete OAuth2 authentication
7. Verify redirection to dashboard with tokens

### Testing Without Frontend

You can test OAuth2 directly by visiting:

- Google: `http://localhost:8080/oauth2/authorization/google`
- GitHub: `http://localhost:8080/oauth2/authorization/github`

The backend will redirect you to the callback URL with tokens in the query parameters.

---

## Production Deployment

### 1. Update Redirect URIs

In Google Cloud Console and GitHub OAuth App settings, add production URLs:

**Google:**
- Authorized JavaScript origins: `https://your-domain.com`
- Authorized redirect URIs: `https://your-domain.com/login/oauth2/code/google`

**GitHub:**
- Authorization callback URL: `https://your-domain.com/login/oauth2/code/github`

### 2. Update Frontend Callback URL

In `OAuth2AuthenticationSuccessHandler.java`, update the target URL:

```java
String targetUrl = UriComponentsBuilder.fromUriString("https://your-domain.com/oauth2/callback")
        .queryParam("accessToken", accessToken)
        .queryParam("refreshToken", refreshToken)
        .build().toUriString();
```

### 3. Use Environment Variables

Never commit OAuth2 credentials to source control. Always use environment variables:

```bash
export GOOGLE_CLIENT_ID="production-client-id"
export GOOGLE_CLIENT_SECRET="production-client-secret"
export GITHUB_CLIENT_ID="production-client-id"
export GITHUB_CLIENT_SECRET="production-client-secret"
```

---

## Troubleshooting

### Common Issues

#### 1. "Error: redirect_uri_mismatch"

**Cause**: The redirect URI in the request doesn't match configured URIs

**Solution**:
- Verify redirect URIs in Google Cloud Console / GitHub OAuth App settings
- Ensure the URL exactly matches (including protocol, domain, port, path)
- Check for trailing slashes

#### 2. "User email not found"

**Cause**: OAuth2 provider didn't return email address

**Solution**:
- Verify email scope is requested (`email` for Google, `user:email` for GitHub)
- Check if user has verified email with OAuth2 provider
- Ensure user grants email permission during OAuth2 consent

#### 3. "OAuth2 authentication failed"

**Cause**: Various issues (invalid credentials, network error, etc.)

**Solution**:
- Check backend logs for detailed error message
- Verify OAuth2 credentials are correct
- Ensure OAuth2 client credentials match environment variables
- Check network connectivity to OAuth2 provider

#### 4. Tokens not appearing in callback URL

**Cause**: OAuth2AuthenticationSuccessHandler not being called

**Solution**:
- Verify SecurityConfig has oauth2Login configured
- Check that CustomOAuth2UserService is being used
- Review backend logs for exceptions

---

## API Reference

### OAuth2 Components

#### CustomOAuth2UserService

Handles OAuth2 user information and creates/updates users in database.

**Location**: `src/main/java/org/sensorvision/security/CustomOAuth2UserService.java`

**Key Methods**:
- `loadUser()`: Loads OAuth2 user and creates/finds database user

#### OAuth2AuthenticationSuccessHandler

Handles successful OAuth2 authentication and generates JWT tokens.

**Location**: `src/main/java/org/sensorvision/security/OAuth2AuthenticationSuccessHandler.java`

**Key Methods**:
- `onAuthenticationSuccess()`: Generates tokens and redirects to frontend

#### CustomOAuth2User

Wrapper for OAuth2User with SensorVision user details.

**Location**: `src/main/java/org/sensorvision/security/CustomOAuth2User.java`

---

## Feature Comparison

| Feature | Username/Password Login | OAuth2 Social Login |
|---------|------------------------|---------------------|
| Email verification | Required | Automatic |
| Password reset | Available | N/A |
| Remember me | Available | Via refresh token |
| Account creation | Manual | Automatic |
| Multi-factor auth | Planned | Provider-dependent |

---

## Next Steps

1. **Get OAuth2 Credentials**: Set up Google and GitHub OAuth2 apps
2. **Configure Environment Variables**: Add credentials to your environment
3. **Implement Frontend**: Add OAuth2 login buttons and callback handler
4. **Test Locally**: Verify OAuth2 login flow works end-to-end
5. **Deploy to Production**: Update redirect URIs and use production credentials

---

## Support

For issues or questions:
- Check the troubleshooting section
- Review backend logs for detailed error messages
- Refer to Spring Security OAuth2 documentation
- Contact the development team

---

## Related Documentation

- `AUTH_IMPLEMENTATION_SUMMARY.md` - Complete authentication system overview
- `COMPLETE_AUTH_IMPLEMENTATION.md` - Detailed implementation guide
- Spring Security OAuth2: https://spring.io/guides/tutorials/spring-boot-oauth2/
- Google OAuth2: https://developers.google.com/identity/protocols/oauth2
- GitHub OAuth2: https://docs.github.com/en/developers/apps/building-oauth-apps
