# Plugin Marketplace User Guide

**Version**: 1.0.0
**Last Updated**: 2025-11-14
**Audience**: End Users, System Administrators

---

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Browsing Plugins](#browsing-plugins)
4. [Installing Plugins](#installing-plugins)
5. [Configuring Plugins](#configuring-plugins)
6. [Managing Plugins](#managing-plugins)
7. [Plugin Categories](#plugin-categories)
8. [Official vs Community Plugins](#official-vs-community-plugins)
9. [Rating and Reviewing](#rating-and-reviewing)
10. [Troubleshooting](#troubleshooting)

---

## Overview

The **Plugin Marketplace** extends Industrial Cloud's capabilities with pre-built integrations and tools. Discover, install, and manage plugins to connect your devices, send notifications, parse protocols, and integrate with third-party platforms.

### Key Features

- **6 Official Plugins** - Pre-built, verified integrations ready to use
- **One-Click Installation** - Simple wizard-based setup process
- **Dynamic Configuration** - Auto-generated forms based on plugin requirements
- **Instant Activation** - Enable/disable plugins without uninstalling
- **Community Ecosystem** - Browse and contribute plugins (coming soon)

### Available Plugin Types

| Type | Description | Examples |
|------|-------------|----------|
| **Protocol Parser** | Decode IoT protocol data | LoRaWAN, Modbus, Sigfox |
| **Notification** | Send alerts to external services | Slack, Discord, Email |
| **Integration** | Connect to third-party platforms | HTTP Webhooks, AWS IoT, Azure IoT |
| **Data Source** | Ingest data from external systems | MQTT bridges, REST APIs |

---

## Getting Started

### Accessing the Plugin Marketplace

1. **Login** to Industrial Cloud with your credentials
2. **Navigate** to the Plugin Marketplace:
   - Click the **Store** icon (🏪) in the left sidebar
   - Or go directly to: `https://your-instance.com/plugin-marketplace`

### Marketplace Layout

The Plugin Marketplace has **two main tabs**:

#### 1. Marketplace Tab
Browse all available plugins, search, and filter by category.

**Features:**
- Search bar for finding plugins by name or description
- Category filter (All, Protocol Parser, Notification, Integration, etc.)
- Plugin cards showing:
  - Plugin icon and name
  - Short description
  - Author and version
  - Official/Verified badges
  - Rating (stars) and installation count

#### 2. My Plugins Tab
View and manage your installed plugins.

**Features:**
- List of all installed plugins
- Status indicators (Active/Inactive)
- Quick actions (Activate, Deactivate, Configure, Uninstall)
- Installation date

---

## Browsing Plugins

### Search for Plugins

**By Keyword:**
1. Type in the search bar at the top of the Marketplace tab
2. Search matches plugin names, descriptions, and authors
3. Example searches:
   - "lorawan" - Find LoRaWAN integrations
   - "slack" - Find Slack notification plugin
   - "modbus" - Find Modbus protocol parser

**By Category:**
1. Click the **Category** dropdown
2. Select a category:
   - **All** - Show all plugins
   - **Protocol Parser** - Device protocol decoders
   - **Notification** - Alert notification channels
   - **Integration** - Third-party platform integrations
   - **Data Source** - External data ingestion tools

### View Plugin Details

**Click on any plugin card** to see full details:

- **Overview**: Comprehensive description and features
- **Configuration**: Required and optional settings
- **Screenshots**: Visual preview (if available)
- **Metadata**:
  - Version number
  - Author and website
  - Installation count
  - Average rating
  - Compatible Industrial Cloud versions
- **External Links**:
  - Documentation
  - Source code repository
  - Author website

### Understanding Badges

Plugins display badges to indicate quality and trust:

| Badge | Meaning |
|-------|---------|
| **Official** (Blue) | Built and maintained by Industrial Cloud team |
| **Verified** (Green ✓) | Code reviewed, tested, and approved |
| **Popular** | High installation count |
| **Top Rated** | 4+ star average rating |

---

## Installing Plugins

### Installation Process

**Step 1: Select Plugin**
- Browse or search for the plugin you want
- Click on the plugin card to view details
- Review requirements and configuration needs

**Step 2: Click Install**
- Click the **Install** button
- Configuration modal will open

**Step 3: Configure Plugin**
- Fill in required fields (marked with asterisk *)
- Optional fields can be left blank (defaults will be used)
- Example configuration (Slack Notifications):
  ```
  Webhook URL*: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
  Channel: #alerts
  Username: Industrial Cloud
  Icon Emoji: :robot_face:
  Mention @channel: ☐ (checkbox)
  Include Metadata: ☑ (checkbox)
  ```

**Step 4: Save Configuration**
- Click **Save** or **Install**
- Plugin will be installed with status "Inactive"
- Success notification will appear

**Step 5: Activate Plugin**
- Switch to **My Plugins** tab
- Find the newly installed plugin
- Click **Activate** button
- Plugin is now active and running

### Installation Example: LoRaWAN TTN Integration

Let's walk through installing the LoRaWAN TTN Integration plugin:

**1. Find the Plugin**
- Go to Plugin Marketplace
- Search for "lorawan" or filter by "Protocol Parser"
- Click on "LoRaWAN TTN Integration" card

**2. Review Details**
- Description: "Connect your LoRaWAN devices from The Things Network (TTN) v3"
- Requirements:
  - TTN Application ID
  - TTN API Key
  - Region selection (EU1, NAM1, AU1)

**3. Install and Configure**
- Click **Install**
- Enter your TTN credentials:
  - **Application ID**: `my-ttn-app`
  - **API Key**: `NNSXS.XXXXXXXXXXXXX` (your TTN API key)
  - **Region**: Select `eu1` (or your region)
  - **Enable Webhook**: ☑ (checked)
- Click **Save**

**4. Activate**
- Go to **My Plugins** tab
- Find "LoRaWAN TTN Integration" (status: Inactive)
- Click **Activate**
- Plugin is now processing TTN messages

**5. Test Integration**
- Send a test message from a TTN device
- Check device telemetry in Industrial Cloud dashboard
- Verify data is being parsed correctly

---

## Configuring Plugins

### View Current Configuration

1. Go to **My Plugins** tab
2. Find the plugin you want to configure
3. Click **Configure** button
4. Configuration modal opens with current values

### Update Configuration

**Edit Fields:**
- Modify any configuration field
- Required fields cannot be empty
- Optional fields can be cleared

**Field Types:**

| Type | Input | Example |
|------|-------|---------|
| **Text** | Text box | Application ID, username |
| **Password/Secret** | Masked input | API keys, tokens, passwords |
| **Number** | Number input | Port (502), timeout (30) |
| **Boolean** | Checkbox | Enable webhook, mention channel |
| **Dropdown** | Select menu | Region (EU1, NAM1, AU1) |
| **Multi-line** | Text area | JSON configuration, custom script |

**Save Changes:**
- Click **Save** to apply changes
- Plugin will reload with new configuration
- No need to deactivate/reactivate

### Configuration Validation

Plugins validate configuration on save:

- **Required fields**: Must be filled
- **Format validation**: Email, URL, number ranges
- **Custom validation**: Plugin-specific rules

**Validation Errors:**
If configuration is invalid, you'll see error messages:
- "API Key is required"
- "Port must be between 1 and 65535"
- "Invalid webhook URL format"

Fix errors and save again.

---

## Managing Plugins

### Activate Plugin

**Purpose**: Enable plugin to start processing data/sending notifications

**Steps:**
1. Go to **My Plugins** tab
2. Find plugin with status "Inactive"
3. Click **Activate** button
4. Status changes to "Active"
5. Plugin starts functioning immediately

**What Happens:**
- Plugin registers with Industrial Cloud engine
- Starts receiving events (telemetry, alerts, etc.)
- Begins executing configured actions

### Deactivate Plugin

**Purpose**: Temporarily disable plugin without uninstalling

**Steps:**
1. Go to **My Plugins** tab
2. Find plugin with status "Active"
3. Click **Deactivate** button
4. Status changes to "Inactive"
5. Plugin stops processing immediately

**What Happens:**
- Plugin unregisters from Industrial Cloud engine
- Stops receiving events
- Configuration is preserved
- Can be reactivated anytime

**Use Cases:**
- Troubleshooting issues
- Temporarily pausing notifications
- Switching between different configurations
- Testing alternative plugins

### Uninstall Plugin

**Purpose**: Completely remove plugin from your organization

**Steps:**
1. Go to **My Plugins** tab
2. Find the plugin to remove
3. Click **Uninstall** button
4. Confirm uninstall in dialog
5. Plugin is removed from list

**What Happens:**
- Plugin is deactivated (if active)
- Configuration is deleted
- Installation record is removed
- Can be re-installed later with fresh configuration

**⚠️ Warning**: Uninstalling is permanent and cannot be undone. Configuration must be re-entered if reinstalled.

---

## Plugin Categories

### Protocol Parser Plugins

**Purpose**: Decode proprietary IoT protocol data into Industrial Cloud telemetry format

**Available Plugins:**

#### 1. LoRaWAN TTN Integration
- **What it does**: Connects to The Things Network v3, parses LoRaWAN uplink messages
- **Use case**: LoRaWAN sensor networks (temperature, humidity, GPS trackers)
- **Configuration**: TTN Application ID, API Key, Region
- **Documentation**: [TTN Integration Guide](https://github.com/CodeFleck/indcloud/blob/main/docs/LORAWAN_TTN_INTEGRATION.md)

#### 2. Modbus TCP Integration
- **What it does**: Polls Modbus TCP devices, reads holding/input registers
- **Use case**: Industrial sensors, PLCs, SCADA systems
- **Configuration**: Host IP, port, unit ID, register mapping
- **Documentation**: [Modbus Guide](https://modbus.org/docs/Modbus_Application_Protocol_V1_1b3.pdf)

#### 3. Sigfox Protocol Parser
- **What it does**: Receives and decodes Sigfox device callbacks
- **Use case**: Sigfox IoT devices (asset tracking, environmental monitoring)
- **Configuration**: Callback URL, payload format, location handling
- **Documentation**: [Sigfox Callbacks](https://support.sigfox.com/docs/callbacks)

### Notification Plugins

**Purpose**: Send alert notifications to external messaging platforms

**Available Plugins:**

#### 1. Slack Notifications
- **What it does**: Sends real-time alerts to Slack channels or DMs
- **Use case**: Team collaboration, incident response
- **Configuration**: Webhook URL, channel, username, icon
- **Features**:
  - Rich message formatting
  - Mention @channel for critical alerts
  - Include device metadata
  - Custom bot icon and name

#### 2. Discord Notifications
- **What it does**: Sends alerts to Discord channels via webhooks
- **Use case**: Community-driven monitoring, gaming server infrastructure
- **Configuration**: Webhook URL, username, avatar, embed color
- **Features**:
  - Rich embeds with color coding
  - Mention @everyone for critical alerts
  - Custom bot avatar
  - Timestamp and metadata

### Integration Plugins

**Purpose**: Connect Industrial Cloud to external platforms and services

**Available Plugins:**

#### 1. HTTP Webhook Receiver
- **What it does**: Generic HTTP endpoint for receiving data from any system
- **Use case**: Custom integrations, third-party platforms
- **Configuration**: Endpoint path, device ID field, data field mapping
- **Features**:
  - Flexible JSON field mapping
  - Optional authentication token
  - Timestamp parsing
  - Auto-provisioning devices

---

## Official vs Community Plugins

### Official Plugins (Blue Badge)

**Characteristics:**
- Built and maintained by Industrial Cloud team
- Comprehensive documentation
- Extensive testing (>80% code coverage)
- Security audited
- Production-ready
- Regular updates and support

**All 6 current plugins are official:**
1. LoRaWAN TTN Integration
2. Modbus TCP Integration
3. Sigfox Protocol Parser
4. Slack Notifications
5. Discord Notifications
6. HTTP Webhook Receiver

### Verified Plugins (Green ✓ Badge)

**Criteria for Verification:**
- Official plugin status
- Used in production by 10+ organizations
- Average rating >4.0 stars
- Active maintenance (<30 days since last update)
- No critical security issues

### Community Plugins (Coming Soon)

**Future Feature:**
- User-contributed plugins
- Open-source submissions
- Community ratings and reviews
- Plugin development toolkit
- Template repository

**How to Contribute:**
- See [Plugin Development Guide](https://github.com/CodeFleck/indcloud/blob/main/docs/PLUGIN_DEVELOPMENT_GUIDE.md)
- Submit pull request with plugin code
- Pass code review and testing
- Get published to marketplace

---

## Rating and Reviewing

### Rate a Plugin

**Steps:**
1. Go to **My Plugins** tab (must have plugin installed)
2. Click on plugin card or **Configure** button
3. Scroll to **Rating** section in details
4. Click star rating (1-5 stars):
   - ⭐ - Poor
   - ⭐⭐ - Fair
   - ⭐⭐⭐ - Good
   - ⭐⭐⭐⭐ - Very Good
   - ⭐⭐⭐⭐⭐ - Excellent
5. Optionally add review text
6. Click **Submit Rating**

### Write a Review

**Best Practices:**
- Be specific about what you liked/disliked
- Mention your use case
- Note any issues encountered
- Suggest improvements
- Be constructive and respectful

**Example Review:**
```
⭐⭐⭐⭐⭐ Excellent plugin!

We're using the LoRaWAN TTN Integration to monitor 50+ environmental
sensors across our campus. The setup was straightforward, and the
automatic device provisioning saved us hours of manual configuration.
Payload parsing works flawlessly, and we haven't had any issues in
3 months of production use.

One suggestion: It would be great to have support for downlink commands
in a future version.

Highly recommended for anyone using The Things Network!
```

### Update Your Rating

- Can update rating/review anytime
- Click on plugin again and modify stars/text
- Previous rating is replaced (not averaged)

---

## Troubleshooting

### Plugin Installation Issues

**Problem: "Plugin already installed" error**

**Cause**: Plugin is already installed for your organization

**Solution**:
1. Go to **My Plugins** tab
2. Check if plugin is already there
3. If you want fresh install:
   - Uninstall existing plugin
   - Install again with new configuration

---

**Problem: Configuration validation fails**

**Cause**: Required fields missing or invalid format

**Solution**:
1. Check error messages carefully
2. Ensure all required fields (marked with *) are filled
3. Verify format:
   - URLs start with `https://`
   - Ports are numbers 1-65535
   - API keys are correct format
4. Consult plugin documentation for field requirements

---

**Problem: Installation succeeds but plugin doesn't work**

**Cause**: Plugin installed but not activated

**Solution**:
1. Go to **My Plugins** tab
2. Check plugin status
3. If "Inactive", click **Activate**
4. Wait a few seconds for activation
5. Test plugin functionality

---

### Plugin Activation Issues

**Problem: "Activation failed" error**

**Cause**: Invalid configuration or service unavailable

**Solution**:
1. Verify configuration is correct:
   - Click **Configure**
   - Test external service credentials
   - Update if needed
2. Check plugin documentation
3. Deactivate and reactivate
4. Check application logs (if admin)

---

**Problem: Plugin activates but doesn't process data**

**Cause**: Configuration error or service connectivity

**Solution for Protocol Parsers:**
1. Verify device is sending data to correct endpoint
2. Check webhook URL configuration
3. Test with manual data transmission
4. Review plugin documentation

**Solution for Notifications:**
1. Test webhook URL manually (curl/Postman)
2. Verify channel/room exists
3. Check API key permissions
4. Ensure alerts are being triggered

---

### Configuration Issues

**Problem: Can't save configuration changes**

**Cause**: Validation error or required fields missing

**Solution**:
1. Read error messages
2. Fill all required fields
3. Check field formats
4. Try minimal configuration first
5. Add optional fields later

---

**Problem: Lost configuration after update**

**Cause**: This shouldn't happen (configuration persists)

**Solution**:
1. Check **My Plugins** tab
2. Click **Configure** to view current values
3. If truly lost (bug), reconfigure and contact support
4. Keep backup of important credentials

---

### Performance Issues

**Problem: Plugin causing slow performance**

**Cause**: High data volume or configuration issue

**Solution**:
1. Deactivate plugin temporarily
2. Check if performance improves
3. Review configuration:
   - Reduce polling interval (Modbus)
   - Filter unnecessary data
   - Optimize register mapping
4. Contact support if persistent

---

### Getting Help

**Resources:**
- **Documentation**: [Plugin Development Guide](https://github.com/CodeFleck/indcloud/blob/main/docs/PLUGIN_DEVELOPMENT_GUIDE.md)
- **GitHub Issues**: https://github.com/CodeFleck/indcloud/issues
- **GitHub Discussions**: https://github.com/CodeFleck/indcloud/discussions
- **Email Support**: support@indcloud.io

**When Reporting Issues:**
Include:
- Plugin name and version
- Your Industrial Cloud version
- Configuration (sanitize secrets!)
- Error messages
- Steps to reproduce
- Expected vs actual behavior

---

## Best Practices

### Plugin Selection

1. **Start with official plugins** - Verified and tested
2. **Read documentation** - Understand requirements and limitations
3. **Check ratings** - Learn from other users' experiences
4. **Test in staging** - Before production deployment

### Configuration Management

1. **Document configurations** - Keep backup of settings
2. **Use secure secrets** - Don't share API keys publicly
3. **Test after changes** - Verify plugin still works
4. **Monitor alerts** - Watch for configuration issues

### Security

1. **Rotate API keys** - Periodically update credentials
2. **Limit permissions** - Use least-privilege access
3. **Review installed plugins** - Remove unused plugins
4. **Keep updated** - Install plugin updates when available

### Performance

1. **Deactivate unused plugins** - Don't leave idle plugins active
2. **Optimize polling** - Set reasonable intervals
3. **Filter data** - Process only what you need
4. **Monitor resource usage** - Watch for performance impact

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `/` | Focus search bar |
| `Esc` | Close modal |
| `Enter` | Submit form (when focused) |
| `Tab` | Navigate form fields |

---

## FAQ

**Q: How many plugins can I install?**
A: No limit. Install as many as you need.

**Q: Can I install the same plugin multiple times?**
A: No. One installation per organization. Use configuration to manage multiple instances if needed (future feature).

**Q: What happens if I uninstall a plugin that's processing data?**
A: Plugin stops immediately. Data in transit may be lost. Deactivate first, verify no data loss, then uninstall.

**Q: Can I develop my own plugins?**
A: Yes! See the [Plugin Development Guide](https://github.com/CodeFleck/indcloud/blob/main/docs/PLUGIN_DEVELOPMENT_GUIDE.md) to get started.

**Q: Are plugin configurations encrypted?**
A: Yes. Sensitive fields (passwords, API keys) are encrypted in the database.

**Q: Can I export/import plugin configurations?**
A: Not yet. This feature is planned for a future release.

**Q: What happens during Industrial Cloud updates?**
A: Plugins remain installed. May need reconfiguration if plugin schema changes (rare).

---

**Need more help?** Contact support@indcloud.io or visit our [GitHub Discussions](https://github.com/CodeFleck/indcloud/discussions).
