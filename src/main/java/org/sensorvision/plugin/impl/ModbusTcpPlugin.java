package org.sensorvision.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import org.sensorvision.model.PluginProvider;
import org.sensorvision.plugin.BasePollingPlugin;
import org.sensorvision.plugin.PluginProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Modbus TCP Polling Plugin.
 * Connects to Modbus TCP devices and polls holding/input registers.
 *
 * Configuration format:
 * {
 *   "host": "192.168.1.100",
 *   "port": 502,
 *   "unitId": 1,
 *   "deviceId": "plc-001",
 *   "pollingIntervalSeconds": 60,
 *   "registers": [
 *     {
 *       "type": "HOLDING",      // HOLDING or INPUT
 *       "address": 40001,       // Modbus address
 *       "count": 1,             // Number of registers to read
 *       "variableName": "temperature",
 *       "scale": 0.1,           // Optional: multiply value by this
 *       "offset": 0             // Optional: add this to value
 *     },
 *     {
 *       "type": "INPUT",
 *       "address": 30001,
 *       "count": 1,
 *       "variableName": "pressure"
 *     }
 *   ]
 * }
 */
@Component
public class ModbusTcpPlugin extends BasePollingPlugin {

    private static final Logger log = LoggerFactory.getLogger(ModbusTcpPlugin.class);

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.MODBUS_TCP;
    }

    public String getConfigurationSchema() {
        return """
            {
              "type": "object",
              "required": ["host", "port", "unitId", "deviceId", "registers"],
              "properties": {
                "host": {
                  "type": "string",
                  "title": "Modbus Server IP",
                  "description": "IP address of the Modbus TCP server",
                  "example": "192.168.1.100"
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
                  "description": "Modbus unit/slave ID (usually 1)",
                  "default": 1,
                  "minimum": 0,
                  "maximum": 255
                },
                "deviceId": {
                  "type": "string",
                  "title": "Device ID",
                  "description": "SensorVision device ID for this Modbus device",
                  "example": "plc-001"
                },
                "pollingIntervalSeconds": {
                  "type": "integer",
                  "title": "Polling Interval (seconds)",
                  "description": "How often to poll the device",
                  "default": 60,
                  "minimum": 1,
                  "maximum": 86400
                },
                "timeout": {
                  "type": "integer",
                  "title": "Connection Timeout (ms)",
                  "description": "Modbus connection timeout",
                  "default": 3000,
                  "minimum": 100,
                  "maximum": 30000
                },
                "registers": {
                  "type": "array",
                  "title": "Register Mappings",
                  "description": "Map Modbus registers to SensorVision variables",
                  "items": {
                    "type": "object",
                    "required": ["type", "address", "count", "variableName"],
                    "properties": {
                      "type": {
                        "type": "string",
                        "title": "Register Type",
                        "enum": ["HOLDING", "INPUT"],
                        "description": "Holding Register (read/write) or Input Register (read-only)"
                      },
                      "address": {
                        "type": "integer",
                        "title": "Start Address",
                        "description": "Modbus register address (1-based or 0-based depending on device)",
                        "minimum": 0,
                        "maximum": 65535
                      },
                      "count": {
                        "type": "integer",
                        "title": "Register Count",
                        "description": "Number of consecutive registers to read",
                        "default": 1,
                        "minimum": 1,
                        "maximum": 125
                      },
                      "variableName": {
                        "type": "string",
                        "title": "Variable Name",
                        "description": "SensorVision variable name for this register"
                      },
                      "scale": {
                        "type": "number",
                        "title": "Scale Factor",
                        "description": "Multiply register value by this (e.g., 0.1 for tenths)",
                        "default": 1.0
                      },
                      "offset": {
                        "type": "number",
                        "title": "Offset",
                        "description": "Add this to the scaled value",
                        "default": 0.0
                      },
                      "dataType": {
                        "type": "string",
                        "title": "Data Type",
                        "enum": ["INT16", "UINT16", "INT32", "UINT32", "FLOAT32"],
                        "default": "INT16",
                        "description": "How to interpret the register data"
                      }
                    }
                  },
                  "minItems": 1
                }
              }
            }
            """;
    }

    @Override
    protected String[] getRequiredConfigFields() {
        return new String[]{"host", "port", "unitId", "deviceId", "registers"};
    }

    @Override
    protected int getDefaultPollingIntervalSeconds() {
        return 60;
    }

    @Override
    public PollingResult poll(JsonNode config) throws PluginProcessingException {
        String host = config.get("host").asText();
        int port = config.get("port").asInt(502);
        int unitId = config.get("unitId").asInt(1);
        String deviceId = config.get("deviceId").asText();
        int timeout = config.get("timeout").asInt(3000);

        ModbusTCPMaster master = null;
        Map<String, BigDecimal> variables = new HashMap<>();

        try {
            // Connect to Modbus TCP device
            master = new ModbusTCPMaster(host, port);
            master.setTimeout(timeout);
            master.connect();

            log.info("Connected to Modbus TCP device {}:{} (unit {})", host, port, unitId);

            // Read all configured registers
            JsonNode registers = config.get("registers");
            for (JsonNode registerConfig : registers) {
                try {
                    BigDecimal value = readRegister(master, unitId, registerConfig);
                    String variableName = registerConfig.get("variableName").asText();
                    variables.put(variableName, value);

                    log.debug("Read Modbus register: {} = {}", variableName, value);
                } catch (Exception e) {
                    log.error("Failed to read register {}: {}",
                        registerConfig.get("variableName").asText(), e.getMessage());
                    // Continue with other registers even if one fails
                }
            }

            if (variables.isEmpty()) {
                throw new PluginProcessingException("Failed to read any registers from Modbus device");
            }

            return new PollingResult(deviceId, Instant.now(), variables);

        } catch (Exception e) {
            throw new PluginProcessingException("Modbus TCP polling failed: " + e.getMessage(), e);
        } finally {
            if (master != null && master.isConnected()) {
                master.disconnect();
            }
        }
    }

    private BigDecimal readRegister(ModbusTCPMaster master, int unitId, JsonNode config) throws Exception {
        String type = config.get("type").asText("HOLDING");
        int address = config.get("address").asInt();
        int count = config.get("count").asInt(1);
        double scale = config.get("scale").asDouble(1.0);
        double offset = config.get("offset").asDouble(0.0);
        String dataType = config.get("dataType").asText("INT16");

        // Modbus addresses are often specified in different conventions
        // Some use 1-based (40001 for holding, 30001 for input)
        // Others use 0-based. We'll auto-detect and adjust.
        int adjustedAddress = adjustModbusAddress(address, type);

        // Read the register(s)
        if ("HOLDING".equalsIgnoreCase(type)) {
            Register[] registers = master.readMultipleRegisters(unitId, adjustedAddress, count);
            return parseRegisters(registers, dataType, scale, offset);
        } else if ("INPUT".equalsIgnoreCase(type)) {
            InputRegister[] registers = master.readInputRegisters(unitId, adjustedAddress, count);
            return parseInputRegisters(registers, dataType, scale, offset);
        } else {
            throw new PluginProcessingException("Unknown register type: " + type);
        }
    }

    private int adjustModbusAddress(int address, String type) {
        // If address is in Modicon convention (40001+, 30001+), convert to 0-based
        if (type.equalsIgnoreCase("HOLDING") && address >= 40001 && address <= 49999) {
            return address - 40001; // 40001 becomes 0
        } else if (type.equalsIgnoreCase("INPUT") && address >= 30001 && address <= 39999) {
            return address - 30001; // 30001 becomes 0
        }
        // Otherwise assume it's already 0-based or use as-is
        return address;
    }

    private BigDecimal parseRegisters(Register[] registers, String dataType, double scale, double offset) {
        double rawValue = switch (dataType.toUpperCase()) {
            case "INT16" -> (short) registers[0].getValue(); // Signed 16-bit
            case "UINT16" -> registers[0].getValue() & 0xFFFF; // Unsigned 16-bit
            case "INT32" -> {
                if (registers.length < 2) throw new IllegalArgumentException("INT32 requires 2 registers");
                // Preserve sign bit by treating as signed int
                int high = registers[0].getValue();  // Already signed 16-bit from register
                int low = registers[1].getValue() & 0xFFFF;  // Unsigned low word
                yield (high << 16) | low;  // Sign extends properly
            }
            case "UINT32" -> {
                if (registers.length < 2) throw new IllegalArgumentException("UINT32 requires 2 registers");
                long high = registers[0].getValue() & 0xFFFF;
                long low = registers[1].getValue() & 0xFFFF;
                yield (high << 16) | low;
            }
            case "FLOAT32" -> {
                if (registers.length < 2) throw new IllegalArgumentException("FLOAT32 requires 2 registers");
                int high = registers[0].getValue() & 0xFFFF;
                int low = registers[1].getValue() & 0xFFFF;
                int bits = (high << 16) | low;
                yield Float.intBitsToFloat(bits);
            }
            default -> (short) registers[0].getValue();
        };

        double scaledValue = (rawValue * scale) + offset;
        return BigDecimal.valueOf(scaledValue);
    }

    private BigDecimal parseInputRegisters(InputRegister[] registers, String dataType, double scale, double offset) {
        // Convert InputRegister[] to Register[] for reuse
        Register[] regs = new Register[registers.length];
        for (int i = 0; i < registers.length; i++) {
            final int value = registers[i].getValue();
            regs[i] = new Register() {
                @Override
                public int getValue() {
                    return value;
                }

                @Override
                public int toUnsignedShort() {
                    return value & 0xFFFF;
                }

                @Override
                public short toShort() {
                    return (short) value;
                }

                @Override
                public byte[] toBytes() {
                    return new byte[]{(byte) (value >> 8), (byte) value};
                }

                @Override
                public void setValue(int v) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setValue(short s) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setValue(byte[] bytes) {
                    throw new UnsupportedOperationException();
                }
            };
        }
        return parseRegisters(regs, dataType, scale, offset);
    }
}
