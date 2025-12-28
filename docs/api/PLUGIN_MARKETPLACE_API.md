# Plugin Marketplace API Documentation

**Version**: 1.0.0
**Base URL**: `/api/v1/plugins`
**Authentication**: Required (JWT Bearer token)

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Endpoints](#endpoints)
4. [Models](#models)
5. [Error Responses](#error-responses)
6. [Examples](#examples)

---

## Overview

The Plugin Marketplace API provides endpoints for:
- Browsing available plugins
- Installing and managing plugins
- Configuring plugin settings
- Rating and reviewing plugins
- Querying plugin statistics

All endpoints require authentication and operate within the context of the authenticated user's organization.

---

## Authentication

All API requests must include a valid JWT authentication token in the `Authorization` header:

```http
Authorization: Bearer <your-jwt-token>
```

### Obtaining a Token

```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "your-password"
  }'

# Response includes token
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": { ... }
}
```

---

## Endpoints

### Plugin Registry

#### 1. Get All Plugins

Retrieve all available plugins from the marketplace.

**Request:**
```http
GET /api/v1/plugins
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `category` | string | No | Filter by category (PROTOCOL_PARSER, NOTIFICATION, etc.) |
| `search` | string | No | Search by name, description, or author |
| `official` | boolean | No | Filter official plugins only |
| `verified` | boolean | No | Filter verified plugins only |

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "pluginKey": "lorawan-ttn",
    "name": "LoRaWAN TTN Integration",
    "description": "Connect your LoRaWAN devices from The Things Network (TTN) v3...",
    "category": "PROTOCOL_PARSER",
    "version": "1.0.0",
    "author": "Industrial Cloud Team",
    "authorUrl": "https://github.com/CodeFleck/indcloud",
    "iconUrl": "https://www.thethingsnetwork.org/docs/lorawan/icon.svg",
    "repositoryUrl": "https://github.com/CodeFleck/indcloud",
    "documentationUrl": "https://github.com/CodeFleck/indcloud/blob/main/docs/LORAWAN_TTN_INTEGRATION.md",
    "minSensorvisionVersion": "1.0.0",
    "maxSensorvisionVersion": null,
    "isOfficial": true,
    "isVerified": true,
    "installationCount": 25,
    "ratingAverage": 4.5,
    "ratingCount": 10,
    "pluginProvider": "LORAWAN_TTN",
    "pluginType": "PROTOCOL_PARSER",
    "configSchema": {
      "type": "object",
      "required": ["applicationId", "apiKey", "region"],
      "properties": {
        "applicationId": {
          "type": "string",
          "title": "Application ID",
          "description": "TTN Application ID"
        },
        "apiKey": {
          "type": "string",
          "title": "API Key",
          "format": "password"
        },
        "region": {
          "type": "string",
          "enum": ["eu1", "nam1", "au1"],
          "default": "eu1"
        }
      }
    },
    "tags": ["lorawan", "ttn", "iot", "protocol"],
    "screenshots": ["https://example.com/screenshot.png"],
    "changelog": "v1.0.0 - Initial release",
    "publishedAt": "2025-01-01T00:00:00Z",
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-01-01T00:00:00Z"
  }
]
```

**Example Requests:**
```bash
# Get all plugins
curl -X GET http://localhost:8080/api/v1/plugins \
  -H "Authorization: Bearer <token>"

# Filter by category
curl -X GET "http://localhost:8080/api/v1/plugins?category=NOTIFICATION" \
  -H "Authorization: Bearer <token>"

# Search plugins
curl -X GET "http://localhost:8080/api/v1/plugins?search=lorawan" \
  -H "Authorization: Bearer <token>"

# Official plugins only
curl -X GET "http://localhost:8080/api/v1/plugins?official=true" \
  -H "Authorization: Bearer <token>"
```

---

#### 2. Get Plugin by Key

Retrieve detailed information about a specific plugin.

**Request:**
```http
GET /api/v1/plugins/{pluginKey}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pluginKey` | string | Yes | Unique plugin identifier |

**Response: 200 OK**
```json
{
  "id": 1,
  "pluginKey": "lorawan-ttn",
  "name": "LoRaWAN TTN Integration",
  "description": "Connect your LoRaWAN devices from The Things Network (TTN) v3...",
  "category": "PROTOCOL_PARSER",
  "version": "1.0.0",
  "author": "Industrial Cloud Team",
  "isOfficial": true,
  "isVerified": true,
  "installationCount": 25,
  "ratingAverage": 4.5,
  "ratingCount": 10,
  "configSchema": { ... },
  "tags": ["lorawan", "ttn"],
  "createdAt": "2025-01-01T00:00:00Z"
}
```

**Response: 404 Not Found**
```json
{
  "error": "Plugin not found",
  "message": "Plugin with key 'invalid-key' does not exist",
  "timestamp": "2025-11-14T12:00:00Z"
}
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/plugins/lorawan-ttn \
  -H "Authorization: Bearer <token>"
```

---

#### 3. Search Plugins

Search plugins by keyword (name, description, author).

**Request:**
```http
GET /api/v1/plugins/search
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | Yes | Search term |

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "pluginKey": "lorawan-ttn",
    "name": "LoRaWAN TTN Integration",
    ...
  }
]
```

**Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/plugins/search?query=lorawan" \
  -H "Authorization: Bearer <token>"
```

---

#### 4. Get Plugins by Category

Filter plugins by category.

**Request:**
```http
GET /api/v1/plugins/category/{category}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `category` | enum | Yes | PROTOCOL_PARSER, NOTIFICATION, INTEGRATION, DATA_SOURCE |

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "pluginKey": "lorawan-ttn",
    "category": "PROTOCOL_PARSER",
    ...
  },
  {
    "id": 5,
    "pluginKey": "modbus-tcp",
    "category": "PROTOCOL_PARSER",
    ...
  }
]
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/plugins/category/PROTOCOL_PARSER \
  -H "Authorization: Bearer <token>"
```

---

#### 5. Get Popular Plugins

Get plugins sorted by installation count.

**Request:**
```http
GET /api/v1/plugins/popular
```

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "pluginKey": "slack-notifications",
    "installationCount": 150,
    ...
  },
  {
    "id": 2,
    "pluginKey": "lorawan-ttn",
    "installationCount": 100,
    ...
  }
]
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/plugins/popular \
  -H "Authorization: Bearer <token>"
```

---

#### 6. Get Top Rated Plugins

Get plugins sorted by average rating.

**Request:**
```http
GET /api/v1/plugins/top-rated
```

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "pluginKey": "lorawan-ttn",
    "ratingAverage": 4.8,
    "ratingCount": 25,
    ...
  }
]
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/plugins/top-rated \
  -H "Authorization: Bearer <token>"
```

---

#### 7. Get Recent Plugins

Get recently published plugins (sorted by publishedAt DESC).

**Request:**
```http
GET /api/v1/plugins/recent
```

**Response: 200 OK**
```json
[
  {
    "id": 6,
    "pluginKey": "http-webhook",
    "publishedAt": "2025-01-15T00:00:00Z",
    ...
  }
]
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/plugins/recent \
  -H "Authorization: Bearer <token>"
```

---

### Plugin Installation

#### 8. Install Plugin

Install a plugin for your organization.

**Request:**
```http
POST /api/v1/plugins/{pluginKey}/install
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pluginKey` | string | Yes | Plugin to install |

**Request Body:**
```json
{
  "applicationId": "my-ttn-app",
  "apiKey": "NNSXS.XXXXXXXXXXXXX",
  "region": "eu1",
  "webhookEnabled": true
}
```

**Note**: Request body must match the plugin's `configSchema` requirements.

**Response: 200 OK**
```json
{
  "id": 1,
  "pluginKey": "lorawan-ttn",
  "organization": {
    "id": 1,
    "name": "My Organization"
  },
  "status": "INACTIVE",
  "configuration": {
    "applicationId": "my-ttn-app",
    "region": "eu1",
    "webhookEnabled": true
  },
  "installedAt": "2025-11-14T12:00:00Z",
  "activatedAt": null,
  "lastUsedAt": null
}
```

**Response: 400 Bad Request**
```json
{
  "error": "Validation failed",
  "message": "Required field 'apiKey' is missing",
  "timestamp": "2025-11-14T12:00:00Z"
}
```

**Response: 409 Conflict**
```json
{
  "error": "Plugin already installed",
  "message": "Plugin 'lorawan-ttn' is already installed for this organization",
  "timestamp": "2025-11-14T12:00:00Z"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/plugins/lorawan-ttn/install \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "my-ttn-app",
    "apiKey": "NNSXS.XXXXXXXXXXXXX",
    "region": "eu1",
    "webhookEnabled": true
  }'
```

---

#### 9. Get Installed Plugin

Get installation details for a specific plugin.

**Request:**
```http
GET /api/v1/plugins/{pluginKey}/installation
```

**Response: 200 OK**
```json
{
  "id": 1,
  "pluginKey": "lorawan-ttn",
  "organization": {
    "id": 1,
    "name": "My Organization"
  },
  "status": "ACTIVE",
  "configuration": { ... },
  "installedAt": "2025-11-14T12:00:00Z",
  "activatedAt": "2025-11-14T12:05:00Z",
  "lastUsedAt": "2025-11-14T14:30:00Z"
}
```

**Response: 404 Not Found**
```json
{
  "error": "Plugin not installed",
  "message": "Plugin 'lorawan-ttn' is not installed for this organization",
  "timestamp": "2025-11-14T12:00:00Z"
}
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/plugins/lorawan-ttn/installation \
  -H "Authorization: Bearer <token>"
```

---

#### 10. Get All Installed Plugins

Get all plugins installed for your organization.

**Request:**
```http
GET /api/v1/plugins/installed
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | enum | No | Filter by status (ACTIVE, INACTIVE, ERROR) |

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "pluginKey": "lorawan-ttn",
    "status": "ACTIVE",
    "installedAt": "2025-11-14T12:00:00Z"
  },
  {
    "id": 2,
    "pluginKey": "slack-notifications",
    "status": "INACTIVE",
    "installedAt": "2025-11-14T13:00:00Z"
  }
]
```

**Example:**
```bash
# All installed plugins
curl -X GET http://localhost:8080/api/v1/plugins/installed \
  -H "Authorization: Bearer <token>"

# Active plugins only
curl -X GET "http://localhost:8080/api/v1/plugins/installed?status=ACTIVE" \
  -H "Authorization: Bearer <token>"
```

---

#### 11. Activate Plugin

Activate an installed plugin to start processing.

**Request:**
```http
POST /api/v1/plugins/{pluginKey}/activate
```

**Response: 200 OK**
```json
{
  "id": 1,
  "pluginKey": "lorawan-ttn",
  "status": "ACTIVE",
  "activatedAt": "2025-11-14T12:30:00Z",
  ...
}
```

**Response: 404 Not Found**
```json
{
  "error": "Plugin not installed",
  "message": "Plugin must be installed before activation",
  "timestamp": "2025-11-14T12:30:00Z"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/plugins/lorawan-ttn/activate \
  -H "Authorization: Bearer <token>"
```

---

#### 12. Deactivate Plugin

Deactivate a plugin to temporarily stop processing.

**Request:**
```http
POST /api/v1/plugins/{pluginKey}/deactivate
```

**Response: 200 OK**
```json
{
  "id": 1,
  "pluginKey": "lorawan-ttn",
  "status": "INACTIVE",
  "activatedAt": "2025-11-14T12:30:00Z",
  ...
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/plugins/lorawan-ttn/deactivate \
  -H "Authorization: Bearer <token>"
```

---

#### 13. Update Plugin Configuration

Update configuration for an installed plugin.

**Request:**
```http
PUT /api/v1/plugins/{pluginKey}/configuration
```

**Request Body:**
```json
{
  "applicationId": "updated-app-id",
  "apiKey": "NNSXS.NEWAPIKEY",
  "region": "nam1",
  "webhookEnabled": false
}
```

**Response: 200 OK**
```json
{
  "id": 1,
  "pluginKey": "lorawan-ttn",
  "configuration": {
    "applicationId": "updated-app-id",
    "region": "nam1",
    "webhookEnabled": false
  },
  "updatedAt": "2025-11-14T13:00:00Z"
}
```

**Response: 400 Bad Request**
```json
{
  "error": "Validation failed",
  "message": "Invalid configuration: region must be one of [eu1, nam1, au1]",
  "timestamp": "2025-11-14T13:00:00Z"
}
```

**Example:**
```bash
curl -X PUT http://localhost:8080/api/v1/plugins/lorawan-ttn/configuration \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "updated-app-id",
    "apiKey": "NNSXS.NEWAPIKEY",
    "region": "nam1"
  }'
```

---

#### 14. Uninstall Plugin

Completely remove a plugin from your organization.

**Request:**
```http
DELETE /api/v1/plugins/{pluginKey}/uninstall
```

**Response: 204 No Content**

**Response: 404 Not Found**
```json
{
  "error": "Plugin not installed",
  "message": "Cannot uninstall plugin that is not installed",
  "timestamp": "2025-11-14T13:30:00Z"
}
```

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/v1/plugins/lorawan-ttn/uninstall \
  -H "Authorization: Bearer <token>"
```

---

### Plugin Ratings

#### 15. Rate Plugin

Submit or update a rating for a plugin.

**Request:**
```http
POST /api/v1/plugins/{pluginKey}/rate
```

**Request Body:**
```json
{
  "rating": 5,
  "reviewText": "Excellent plugin! Easy to set up and works flawlessly."
}
```

**Request Body Schema:**
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `rating` | integer | Yes | 1-5 | Star rating |
| `reviewText` | string | No | Max 1000 chars | Review text |

**Response: 200 OK**
```json
{
  "id": 1,
  "pluginKey": "lorawan-ttn",
  "organization": {
    "id": 1,
    "name": "My Organization"
  },
  "rating": 5,
  "reviewText": "Excellent plugin! Easy to set up and works flawlessly.",
  "createdAt": "2025-11-14T14:00:00Z",
  "updatedAt": "2025-11-14T14:00:00Z"
}
```

**Response: 400 Bad Request**
```json
{
  "error": "Validation failed",
  "message": "Rating must be between 1 and 5",
  "timestamp": "2025-11-14T14:00:00Z"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/plugins/lorawan-ttn/rate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "reviewText": "Excellent plugin! Easy to set up and works flawlessly."
  }'
```

---

#### 16. Get Plugin Ratings

Get all ratings and reviews for a plugin.

**Request:**
```http
GET /api/v1/plugins/{pluginKey}/ratings
```

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "organization": {
      "id": 1,
      "name": "My Organization"
    },
    "rating": 5,
    "reviewText": "Excellent plugin!",
    "createdAt": "2025-11-14T14:00:00Z"
  },
  {
    "id": 2,
    "organization": {
      "id": 2,
      "name": "Another Org"
    },
    "rating": 4,
    "reviewText": "Good, but could use more features.",
    "createdAt": "2025-11-13T10:00:00Z"
  }
]
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/plugins/lorawan-ttn/ratings \
  -H "Authorization: Bearer <token>"
```

---

## Models

### PluginRegistry

```typescript
{
  id: number;
  pluginKey: string;           // Unique identifier
  name: string;                // Display name
  description: string;         // Long description
  category: PluginCategory;    // PROTOCOL_PARSER, NOTIFICATION, etc.
  version: string;             // Semantic version (1.0.0)
  author: string;              // Author name
  authorUrl?: string;          // Author website
  iconUrl?: string;            // Plugin icon
  repositoryUrl?: string;      // Source code repository
  documentationUrl?: string;   // Documentation link
  minSensorvisionVersion?: string;  // Minimum required version
  maxSensorvisionVersion?: string;  // Maximum compatible version
  isOfficial: boolean;         // Official Industrial Cloud plugin
  isVerified: boolean;         // Verified by team
  installationCount: number;   // Total installations
  ratingAverage: number;       // Average rating (0-5)
  ratingCount: number;         // Number of ratings
  pluginProvider: string;      // Provider enum value
  pluginType: string;          // Type enum value
  configSchema: object;        // JSON Schema for configuration
  tags: string[];              // Search tags
  screenshots?: string[];      // Screenshot URLs
  changelog?: string;          // Version changelog
  publishedAt?: Date;          // Publication date
  createdAt: Date;
  updatedAt: Date;
}
```

### InstalledPlugin

```typescript
{
  id: number;
  pluginKey: string;
  organization: {
    id: number;
    name: string;
  };
  status: PluginInstallationStatus;  // ACTIVE, INACTIVE, ERROR
  configuration: object;             // Plugin-specific config (encrypted secrets)
  installedAt: Date;
  activatedAt?: Date;
  lastUsedAt?: Date;
  errorMessage?: string;
}
```

### PluginRating

```typescript
{
  id: number;
  pluginKey: string;
  organization: {
    id: number;
    name: string;
  };
  rating: number;       // 1-5
  reviewText?: string;
  createdAt: Date;
  updatedAt: Date;
}
```

### Enums

**PluginCategory:**
- `PROTOCOL_PARSER` - Protocol decoder/parser
- `NOTIFICATION` - Notification channel
- `INTEGRATION` - Third-party integration
- `DATA_SOURCE` - Data ingestion source
- `ANALYTICS` - Analytics/processing
- `TRANSFORMATION` - Data transformation

**PluginInstallationStatus:**
- `ACTIVE` - Plugin is running
- `INACTIVE` - Plugin installed but not active
- `ERROR` - Plugin encountered error

---

## Error Responses

### Standard Error Format

All error responses follow this format:

```json
{
  "error": "Error type",
  "message": "Detailed error message",
  "timestamp": "2025-11-14T12:00:00Z",
  "path": "/api/v1/plugins/invalid-key"
}
```

### HTTP Status Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created |
| 204 | No Content | Deletion successful |
| 400 | Bad Request | Validation failed, invalid parameters |
| 401 | Unauthorized | Missing or invalid authentication token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Plugin or installation not found |
| 409 | Conflict | Plugin already installed |
| 500 | Internal Server Error | Server-side error |

---

## Examples

### Complete Installation Workflow

```bash
# 1. Browse available plugins
curl -X GET http://localhost:8080/api/v1/plugins \
  -H "Authorization: Bearer <token>"

# 2. Get plugin details
curl -X GET http://localhost:8080/api/v1/plugins/slack-notifications \
  -H "Authorization: Bearer <token>"

# 3. Install plugin
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/install \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://hooks.slack.com/services/XXX",
    "channel": "#alerts",
    "username": "Industrial Cloud",
    "iconEmoji": ":robot_face:",
    "mentionChannel": false,
    "includeMetadata": true
  }'

# 4. Activate plugin
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/activate \
  -H "Authorization: Bearer <token>"

# 5. Verify installation
curl -X GET http://localhost:8080/api/v1/plugins/slack-notifications/installation \
  -H "Authorization: Bearer <token>"

# 6. Rate plugin
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/rate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "reviewText": "Perfect for our team notifications!"
  }'
```

### Search and Filter

```bash
# Search for protocol parsers
curl -X GET "http://localhost:8080/api/v1/plugins?category=PROTOCOL_PARSER&search=modbus" \
  -H "Authorization: Bearer <token>"

# Get official plugins only
curl -X GET "http://localhost:8080/api/v1/plugins?official=true" \
  -H "Authorization: Bearer <token>"

# Get popular notification plugins
curl -X GET http://localhost:8080/api/v1/plugins/popular \
  -H "Authorization: Bearer <token>" \
  | jq '.[] | select(.category == "NOTIFICATION")'
```

### Configuration Management

```bash
# Get current configuration
curl -X GET http://localhost:8080/api/v1/plugins/lorawan-ttn/installation \
  -H "Authorization: Bearer <token>" \
  | jq '.configuration'

# Update configuration
curl -X PUT http://localhost:8080/api/v1/plugins/lorawan-ttn/configuration \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "new-app-id",
    "apiKey": "NNSXS.NEWKEY",
    "region": "nam1",
    "webhookEnabled": true
  }'

# Deactivate before major config change (optional)
curl -X POST http://localhost:8080/api/v1/plugins/lorawan-ttn/deactivate \
  -H "Authorization: Bearer <token>"

# Reactivate after config update
curl -X POST http://localhost:8080/api/v1/plugins/lorawan-ttn/activate \
  -H "Authorization: Bearer <token>"
```

---

## Rate Limiting

**Current**: No rate limiting implemented

**Planned (Future Release)**:
- 100 requests/minute per organization
- 10 install/uninstall operations per hour
- 429 Too Many Requests response when exceeded

---

## Webhooks (Future Feature)

**Planned Events:**
- `plugin.installed`
- `plugin.activated`
- `plugin.deactivated`
- `plugin.configuration.updated`
- `plugin.uninstalled`
- `plugin.error`

---

## Support

For API issues or questions:
- **Documentation**: https://github.com/CodeFleck/indcloud/tree/main/docs
- **GitHub Issues**: https://github.com/CodeFleck/indcloud/issues
- **Email**: api-support@indcloud.io

---

**API Version**: 1.0.0
**Last Updated**: 2025-11-14
