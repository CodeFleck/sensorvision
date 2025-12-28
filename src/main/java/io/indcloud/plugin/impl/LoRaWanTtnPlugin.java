package io.indcloud.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.PluginProvider;
import io.indcloud.plugin.BaseWebhookPlugin;
import io.indcloud.plugin.PluginProcessingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * LoRaWAN The Things Network (TTN) Webhook Plugin.
 * Handles webhook data from TTN in their standard uplink format.
 *
 * Expected TTN payload format:
 * {
 *   "end_device_ids": {
 *     "device_id": "my-device-001"
 *   },
 *   "received_at": "2024-01-01T12:00:00.123456Z",
 *   "uplink_message": {
 *     "f_port": 1,
 *     "frm_payload": "AQIDBAUGBwg=",  // base64 encoded
 *     "decoded_payload": {
 *       "temperature": 25.5,
 *       "humidity": 60.0
 *     },
 *     "rx_metadata": [...],
 *     "settings": {...}
 *   }
 * }
 */
@Component
public class LoRaWanTtnPlugin extends BaseWebhookPlugin {

    @Override
    protected String extractDeviceId(JsonNode payload, JsonNode config) throws PluginProcessingException {
        // TTN device ID is in end_device_ids.device_id
        JsonNode endDeviceIds = payload.path("end_device_ids");
        JsonNode deviceId = endDeviceIds.path("device_id");

        if (deviceId.isMissingNode() || deviceId.isNull()) {
            throw new PluginProcessingException("Missing TTN device ID (end_device_ids.device_id)");
        }

        String ttnDeviceId = deviceId.asText();

        // Check if we should use a device ID prefix/suffix from config
        if (config.has("deviceIdPrefix")) {
            ttnDeviceId = config.get("deviceIdPrefix").asText() + ttnDeviceId;
        }
        if (config.has("deviceIdSuffix")) {
            ttnDeviceId = ttnDeviceId + config.get("deviceIdSuffix").asText();
        }

        return ttnDeviceId;
    }

    @Override
    protected Instant extractTimestamp(JsonNode payload, JsonNode config) {
        // TTN timestamp is in received_at
        JsonNode receivedAt = payload.path("received_at");

        if (!receivedAt.isMissingNode() && !receivedAt.isNull()) {
            try {
                return Instant.parse(receivedAt.asText());
            } catch (Exception e) {
                // Fall back to current time
            }
        }

        return Instant.now();
    }

    @Override
    protected Map<String, BigDecimal> extractVariables(JsonNode payload, JsonNode config) throws PluginProcessingException {
        JsonNode uplinkMessage = payload.path("uplink_message");

        if (uplinkMessage.isMissingNode()) {
            throw new PluginProcessingException("Missing uplink_message in TTN payload");
        }

        Map<String, BigDecimal> variables = new HashMap<>();

        // Try to extract from decoded_payload first (preferred)
        JsonNode decodedPayload = uplinkMessage.path("decoded_payload");
        if (!decodedPayload.isMissingNode() && decodedPayload.isObject()) {
            decodedPayload.fields().forEachRemaining(entry -> {
                if (entry.getValue().isNumber()) {
                    variables.put(entry.getKey(), entry.getValue().decimalValue());
                }
            });
        }

        // If no decoded payload, check if there's a custom decoder function in config
        if (variables.isEmpty() && config.has("decoderFunction")) {
            // In a production system, you would execute the custom decoder here
            // For now, we'll throw an exception to indicate this needs to be implemented
            throw new PluginProcessingException(
                    "Custom decoder functions are not yet supported. " +
                    "Please configure your TTN application to include decoded_payload."
            );
        }

        if (variables.isEmpty()) {
            throw new PluginProcessingException("No telemetry data found in TTN payload. " +
                    "Please ensure your TTN application includes decoded_payload.");
        }

        return variables;
    }

    @Override
    protected Map<String, Object> extractMetadata(JsonNode payload, JsonNode config) {
        Map<String, Object> metadata = new HashMap<>();

        JsonNode uplinkMessage = payload.path("uplink_message");

        if (!uplinkMessage.isMissingNode()) {
            // Extract FPort
            JsonNode fPort = uplinkMessage.path("f_port");
            if (!fPort.isMissingNode()) {
                metadata.put("lorawan_fport", fPort.asInt());
            }

            // Extract RSSI from first gateway if available
            JsonNode rxMetadata = uplinkMessage.path("rx_metadata");
            if (rxMetadata.isArray() && rxMetadata.size() > 0) {
                JsonNode firstGateway = rxMetadata.get(0);
                JsonNode rssi = firstGateway.path("rssi");
                JsonNode snr = firstGateway.path("snr");

                if (!rssi.isMissingNode()) {
                    metadata.put("lorawan_rssi", rssi.asDouble());
                }
                if (!snr.isMissingNode()) {
                    metadata.put("lorawan_snr", snr.asDouble());
                }
            }

            // Extract spreading factor and bandwidth if available
            JsonNode settings = uplinkMessage.path("settings");
            if (!settings.isMissingNode()) {
                JsonNode dataRate = settings.path("data_rate");
                if (!dataRate.isMissingNode()) {
                    JsonNode spreadingFactor = dataRate.path("lora").path("spreading_factor");
                    JsonNode bandwidth = dataRate.path("lora").path("bandwidth");

                    if (!spreadingFactor.isMissingNode()) {
                        metadata.put("lorawan_sf", spreadingFactor.asInt());
                    }
                    if (!bandwidth.isMissingNode()) {
                        metadata.put("lorawan_bandwidth", bandwidth.asInt());
                    }
                }
            }
        }

        return metadata;
    }

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.LORAWAN_TTN;
    }
}
