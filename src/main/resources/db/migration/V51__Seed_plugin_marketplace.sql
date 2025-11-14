-- Seed Plugin Marketplace with Pre-Built Plugins
-- Migration V51: Add official plugins to marketplace

-- 1. LoRaWAN TTN Integration (Protocol Parser)
INSERT INTO plugin_registry (
    plugin_key, name, description, category, version, author, author_url,
    icon_url, repository_url, documentation_url,
    min_sensorvision_version, max_sensorvision_version,
    is_official, is_verified, installation_count, rating_average, rating_count,
    plugin_provider, plugin_type, config_schema, tags, screenshots, changelog,
    published_at, created_at, updated_at
) VALUES (
    'lorawan-ttn',
    'LoRaWAN TTN Integration',
    'Connect your LoRaWAN devices from The Things Network (TTN) v3. Automatically parse and decode uplink messages, handle device provisioning, and forward downlink commands.',
    'PROTOCOL_PARSER',
    '1.0.0',
    'SensorVision Team',
    'https://github.com/CodeFleck/sensorvision',
    'https://www.thethingsnetwork.org/docs/lorawan/icon.svg',
    'https://github.com/CodeFleck/sensorvision',
    'https://github.com/CodeFleck/sensorvision/blob/main/docs/LORAWAN_TTN_INTEGRATION.md',
    '1.0.0',
    null,
    true,
    true,
    0,
    0.0,
    0,
    'LORAWAN_TTN',
    'PROTOCOL_PARSER',
    '{
        "type": "object",
        "required": ["applicationId", "apiKey", "region"],
        "properties": {
            "applicationId": {
                "type": "string",
                "title": "Application ID",
                "description": "TTN Application ID",
                "placeholder": "my-ttn-app"
            },
            "apiKey": {
                "type": "string",
                "title": "API Key",
                "description": "TTN API Key with device read/write permissions",
                "format": "password"
            },
            "region": {
                "type": "string",
                "title": "TTN Region",
                "description": "The Things Network region",
                "enum": ["eu1", "nam1", "au1"],
                "default": "eu1"
            },
            "webhookEnabled": {
                "type": "boolean",
                "title": "Enable Webhook",
                "description": "Receive uplink messages via webhook",
                "default": true
            }
        }
    }',
    ARRAY['lorawan', 'ttn', 'iot', 'protocol', 'lpwan'],
    ARRAY['https://www.thethingsnetwork.org/docs/lorawan/architecture.png'],
    'v1.0.0 - Initial release with TTN v3 support',
    NOW(),
    NOW(),
    NOW()
);

-- 2. Slack Notifications (Notification Channel)
INSERT INTO plugin_registry (
    plugin_key, name, description, category, version, author, author_url,
    icon_url, repository_url, documentation_url,
    min_sensorvision_version, max_sensorvision_version,
    is_official, is_verified, installation_count, rating_average, rating_count,
    plugin_provider, plugin_type, config_schema, tags, screenshots, changelog,
    published_at, created_at, updated_at
) VALUES (
    'slack-notifications',
    'Slack Notifications',
    'Send real-time alert notifications to Slack channels or direct messages. Supports rich formatting, mentions, and custom templates. Perfect for team collaboration and incident response.',
    'NOTIFICATION',
    '1.0.0',
    'SensorVision Team',
    'https://github.com/CodeFleck/sensorvision',
    'https://cdn.worldvectorlogo.com/logos/slack-new-logo.svg',
    'https://github.com/CodeFleck/sensorvision',
    'https://api.slack.com/messaging/webhooks',
    '1.0.0',
    null,
    true,
    true,
    0,
    0.0,
    0,
    'CUSTOM_PARSER',
    'INTEGRATION',
    '{
        "type": "object",
        "required": ["webhookUrl"],
        "properties": {
            "webhookUrl": {
                "type": "string",
                "title": "Webhook URL",
                "description": "Slack Incoming Webhook URL",
                "placeholder": "https://hooks.slack.com/services/YOUR/WEBHOOK/URL",
                "format": "password"
            },
            "channel": {
                "type": "string",
                "title": "Default Channel",
                "description": "Default channel or user to send messages to (e.g., #alerts or @username)",
                "placeholder": "#alerts"
            },
            "username": {
                "type": "string",
                "title": "Bot Username",
                "description": "Display name for the bot",
                "default": "SensorVision"
            },
            "iconEmoji": {
                "type": "string",
                "title": "Icon Emoji",
                "description": "Emoji to use as bot icon",
                "default": ":robot_face:",
                "placeholder": ":bell:"
            },
            "mentionChannel": {
                "type": "boolean",
                "title": "Mention @channel",
                "description": "Mention @channel for critical alerts",
                "default": false
            },
            "includeMetadata": {
                "type": "boolean",
                "title": "Include Metadata",
                "description": "Include device metadata in notifications",
                "default": true
            }
        }
    }',
    ARRAY['slack', 'notifications', 'alerts', 'messaging', 'collaboration'],
    ARRAY['https://a.slack-edge.com/80588/marketing/img/features/unfurls/unfurls_hero_demo.png'],
    'v1.0.0 - Initial release with Incoming Webhooks support',
    NOW(),
    NOW(),
    NOW()
);

-- 3. Discord Notifications (Notification Channel)
INSERT INTO plugin_registry (
    plugin_key, name, description, category, version, author, author_url,
    icon_url, repository_url, documentation_url,
    min_sensorvision_version, max_sensorvision_version,
    is_official, is_verified, installation_count, rating_average, rating_count,
    plugin_provider, plugin_type, config_schema, tags, screenshots, changelog,
    published_at, created_at, updated_at
) VALUES (
    'discord-notifications',
    'Discord Notifications',
    'Send alert notifications to Discord channels via webhooks. Supports embeds, mentions, and rich formatting. Ideal for community-driven monitoring and gaming server infrastructure.',
    'NOTIFICATION',
    '1.0.0',
    'SensorVision Team',
    'https://github.com/CodeFleck/sensorvision',
    'https://cdn.worldvectorlogo.com/logos/discord-6.svg',
    'https://github.com/CodeFleck/sensorvision',
    'https://discord.com/developers/docs/resources/webhook',
    '1.0.0',
    null,
    true,
    true,
    0,
    0.0,
    0,
    'CUSTOM_PARSER',
    'INTEGRATION',
    '{
        "type": "object",
        "required": ["webhookUrl"],
        "properties": {
            "webhookUrl": {
                "type": "string",
                "title": "Webhook URL",
                "description": "Discord Webhook URL",
                "placeholder": "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN",
                "format": "password"
            },
            "username": {
                "type": "string",
                "title": "Bot Username",
                "description": "Display name for the webhook",
                "default": "SensorVision"
            },
            "avatarUrl": {
                "type": "string",
                "title": "Avatar URL",
                "description": "URL for the bot avatar image",
                "placeholder": "https://example.com/avatar.png"
            },
            "embedColor": {
                "type": "string",
                "title": "Embed Color",
                "description": "Hex color for alert embeds",
                "enum": ["#e74c3c", "#e67e22", "#f39c12", "#3498db", "#2ecc71"],
                "default": "#e74c3c"
            },
            "mentionEveryone": {
                "type": "boolean",
                "title": "Mention @everyone",
                "description": "Mention @everyone for critical alerts",
                "default": false
            },
            "useEmbeds": {
                "type": "boolean",
                "title": "Use Rich Embeds",
                "description": "Send notifications as rich embeds",
                "default": true
            }
        }
    }',
    ARRAY['discord', 'notifications', 'alerts', 'messaging', 'gaming'],
    ARRAY['https://support.discord.com/hc/article_attachments/1500000463501/Screen_Shot_2020-12-15_at_4.51.38_PM.png'],
    'v1.0.0 - Initial release with Webhook support',
    NOW(),
    NOW(),
    NOW()
);

-- 4. Sigfox Protocol Parser (Protocol Parser)
INSERT INTO plugin_registry (
    plugin_key, name, description, category, version, author, author_url,
    icon_url, repository_url, documentation_url,
    min_sensorvision_version, max_sensorvision_version,
    is_official, is_verified, installation_count, rating_average, rating_count,
    plugin_provider, plugin_type, config_schema, tags, screenshots, changelog,
    published_at, created_at, updated_at
) VALUES (
    'sigfox-parser',
    'Sigfox Protocol Parser',
    'Parse Sigfox device messages and callbacks. Decode payload data, handle device location updates, and process downlink commands. Supports both uplink and downlink communication.',
    'PROTOCOL_PARSER',
    '1.0.0',
    'SensorVision Team',
    'https://github.com/CodeFleck/sensorvision',
    'https://www.sigfox.com/themes/custom/sigfox_theme/sigfox-logo.svg',
    'https://github.com/CodeFleck/sensorvision',
    'https://support.sigfox.com/docs/callbacks',
    '1.0.0',
    null,
    true,
    true,
    0,
    0.0,
    0,
    'SIGFOX',
    'PROTOCOL_PARSER',
    '{
        "type": "object",
        "required": ["callbackUrl"],
        "properties": {
            "callbackUrl": {
                "type": "string",
                "title": "Callback URL",
                "description": "Public URL for Sigfox callbacks (auto-generated)",
                "readOnly": true
            },
            "apiKey": {
                "type": "string",
                "title": "API Key",
                "description": "Sigfox API Key for device management",
                "format": "password"
            },
            "parsePayload": {
                "type": "boolean",
                "title": "Auto-Parse Payload",
                "description": "Automatically parse binary payload data",
                "default": true
            },
            "payloadFormat": {
                "type": "string",
                "title": "Payload Format",
                "description": "Expected payload structure",
                "enum": ["raw", "json", "custom"],
                "default": "raw"
            },
            "handleLocation": {
                "type": "boolean",
                "title": "Process Location",
                "description": "Extract and store device location from callbacks",
                "default": true
            },
            "callbackType": {
                "type": "string",
                "title": "Callback Type",
                "description": "Type of Sigfox callback",
                "enum": ["data", "service", "error", "acknowledge"],
                "default": "data"
            }
        }
    }',
    ARRAY['sigfox', 'iot', 'protocol', 'lpwan', '0g'],
    ARRAY['https://www.sigfox.com/sites/default/files/2021-01/Sigfox-Global-Coverage.jpg'],
    'v1.0.0 - Initial release with callback support',
    NOW(),
    NOW(),
    NOW()
);

-- 5. Modbus TCP Integration (Protocol Parser) - Already implemented
INSERT INTO plugin_registry (
    plugin_key, name, description, category, version, author, author_url,
    icon_url, repository_url, documentation_url,
    min_sensorvision_version, max_sensorvision_version,
    is_official, is_verified, installation_count, rating_average, rating_count,
    plugin_provider, plugin_type, config_schema, tags, screenshots, changelog,
    published_at, created_at, updated_at
) VALUES (
    'modbus-tcp',
    'Modbus TCP Integration',
    'Connect to Modbus TCP devices and poll registers. Supports reading holding registers, input registers, coils, and discrete inputs. Configurable polling intervals and register mapping.',
    'PROTOCOL_PARSER',
    '1.0.0',
    'SensorVision Team',
    'https://github.com/CodeFleck/sensorvision',
    'https://upload.wikimedia.org/wikipedia/commons/1/19/Modbus_Logo.svg',
    'https://github.com/CodeFleck/sensorvision',
    'https://modbus.org/docs/Modbus_Application_Protocol_V1_1b3.pdf',
    '1.0.0',
    null,
    true,
    true,
    0,
    0.0,
    0,
    'MODBUS_TCP',
    'PROTOCOL_PARSER',
    '{
        "type": "object",
        "required": ["host", "port", "unitId"],
        "properties": {
            "host": {
                "type": "string",
                "title": "Host Address",
                "description": "IP address or hostname of Modbus device",
                "placeholder": "192.168.1.100"
            },
            "port": {
                "type": "integer",
                "title": "Port",
                "description": "Modbus TCP port (default: 502)",
                "default": 502,
                "minimum": 1,
                "maximum": 65535
            },
            "unitId": {
                "type": "integer",
                "title": "Unit ID",
                "description": "Modbus unit/slave ID",
                "default": 1,
                "minimum": 0,
                "maximum": 255
            },
            "pollingInterval": {
                "type": "integer",
                "title": "Polling Interval (seconds)",
                "description": "How often to poll the device",
                "default": 60,
                "minimum": 5,
                "maximum": 3600
            },
            "timeout": {
                "type": "integer",
                "title": "Connection Timeout (ms)",
                "description": "Timeout for Modbus requests",
                "default": 5000,
                "minimum": 1000,
                "maximum": 30000
            },
            "registers": {
                "type": "string",
                "title": "Register Configuration",
                "description": "JSON configuration for registers to poll",
                "format": "textarea",
                "placeholder": "[{\"address\": 0, \"count\": 10, \"type\": \"holding\"}]"
            }
        }
    }',
    ARRAY['modbus', 'tcp', 'industrial', 'scada', 'plc'],
    ARRAY['https://www.rtautomation.com/wp-content/uploads/2019/06/Modbus-TCP-Communication.jpg'],
    'v1.0.0 - Initial release with TCP support',
    NOW(),
    NOW(),
    NOW()
);

-- 6. HTTP Webhook Generic (Integration) - Already implemented
INSERT INTO plugin_registry (
    plugin_key, name, description, category, version, author, author_url,
    icon_url, repository_url, documentation_url,
    min_sensorvision_version, max_sensorvision_version,
    is_official, is_verified, installation_count, rating_average, rating_count,
    plugin_provider, plugin_type, config_schema, tags, screenshots, changelog,
    published_at, created_at, updated_at
) VALUES (
    'http-webhook',
    'HTTP Webhook Receiver',
    'Generic HTTP webhook endpoint for receiving data from any external system. Supports JSON and form-encoded payloads with flexible field mapping. Perfect for custom integrations.',
    'INTEGRATION',
    '1.0.0',
    'SensorVision Team',
    'https://github.com/CodeFleck/sensorvision',
    'https://cdn-icons-png.flaticon.com/512/2165/2165004.png',
    'https://github.com/CodeFleck/sensorvision',
    'https://github.com/CodeFleck/sensorvision/blob/main/docs/DATA_PLUGINS.md',
    '1.0.0',
    null,
    true,
    true,
    0,
    0.0,
    0,
    'HTTP_WEBHOOK',
    'WEBHOOK',
    '{
        "type": "object",
        "required": ["endpointPath"],
        "properties": {
            "endpointPath": {
                "type": "string",
                "title": "Endpoint Path",
                "description": "Custom path for webhook URL (e.g., my-integration)",
                "placeholder": "my-integration"
            },
            "deviceIdField": {
                "type": "string",
                "title": "Device ID Field",
                "description": "JSON path to device identifier",
                "placeholder": "device.id"
            },
            "dataField": {
                "type": "string",
                "title": "Data Field",
                "description": "JSON path to telemetry data",
                "placeholder": "data"
            },
            "timestampField": {
                "type": "string",
                "title": "Timestamp Field",
                "description": "JSON path to timestamp (optional)",
                "placeholder": "timestamp"
            },
            "authToken": {
                "type": "string",
                "title": "Authentication Token",
                "description": "Optional token for webhook authentication",
                "format": "password"
            }
        }
    }',
    ARRAY['webhook', 'http', 'api', 'integration', 'generic'],
    ARRAY['https://webhooks.fyi/img/webhook-flow.png'],
    'v1.0.0 - Initial release',
    NOW(),
    NOW(),
    NOW()
);
