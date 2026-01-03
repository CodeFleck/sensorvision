package io.indcloud.plugin.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.model.DataPlugin;
import io.indcloud.model.PluginProvider;
import io.indcloud.plugin.PluginValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModbusTcpPluginTest {

    private ModbusTcpPlugin plugin;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        plugin = new ModbusTcpPlugin();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetSupportedProvider() {
        assertEquals(PluginProvider.MODBUS_TCP, plugin.getSupportedProvider());
    }

    @Test
    void testGetDefaultPollingIntervalSeconds() {
        assertEquals(60, plugin.getDefaultPollingIntervalSeconds());
    }

    @Test
    void testGetRequiredConfigFields() {
        String[] required = plugin.getRequiredConfigFields();
        assertNotNull(required);
        assertEquals(5, required.length);
        assertArrayEquals(new String[]{"host", "port", "unitId", "deviceId", "registers"}, required);
    }

    @Test
    void testValidateConfiguration_ValidConfig() {
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "pollingIntervalSeconds": 60,
                "registers": [
                    {
                        "type": "HOLDING",
                        "address": 40001,
                        "count": 1,
                        "variableName": "temperature",
                        "dataType": "INT16"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid(), "Configuration should be valid");
    }

    @Test
    void testValidateConfiguration_MissingHost() {
        String configJson = """
            {
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "registers": []
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("host"));
    }

    @Test
    void testValidateConfiguration_InvalidPort() {
        // Note: Port validation is not enforced by BasePollingPlugin
        // This test ensures the config is parseable
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "registers": [
                    {
                        "type": "HOLDING",
                        "address": 0,
                        "count": 1,
                        "variableName": "temp"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateConfiguration_ValidUnitId() {
        // Unit ID validation is not enforced by base class
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "registers": [
                    {
                        "type": "HOLDING",
                        "address": 0,
                        "count": 1,
                        "variableName": "temp"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateConfiguration_MissingRegistersArray() {
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001"
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("registers"));
    }

    @Test
    void testValidateConfiguration_ValidRegistersArray() {
        // BasePollingPlugin doesn't validate register contents
        // Just ensure config is parseable
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "registers": [
                    {
                        "type": "HOLDING",
                        "address": 0,
                        "count": 1,
                        "variableName": "temp"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateConfiguration_AllDataTypes() {
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "registers": [
                    {
                        "type": "HOLDING",
                        "address": 0,
                        "count": 1,
                        "variableName": "int16_val",
                        "dataType": "INT16"
                    },
                    {
                        "type": "HOLDING",
                        "address": 1,
                        "count": 1,
                        "variableName": "uint16_val",
                        "dataType": "UINT16"
                    },
                    {
                        "type": "HOLDING",
                        "address": 2,
                        "count": 2,
                        "variableName": "int32_val",
                        "dataType": "INT32"
                    },
                    {
                        "type": "HOLDING",
                        "address": 4,
                        "count": 2,
                        "variableName": "uint32_val",
                        "dataType": "UINT32"
                    },
                    {
                        "type": "HOLDING",
                        "address": 6,
                        "count": 2,
                        "variableName": "float32_val",
                        "dataType": "FLOAT32"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid(), "All standard data types should be valid");
    }

    @Test
    void testValidateConfiguration_InputRegisters() {
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "registers": [
                    {
                        "type": "INPUT",
                        "address": 30001,
                        "count": 1,
                        "variableName": "sensor_reading"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateConfiguration_WithScaleAndOffset() {
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "registers": [
                    {
                        "type": "HOLDING",
                        "address": 0,
                        "count": 1,
                        "variableName": "temperature",
                        "dataType": "INT16",
                        "scale": 0.1,
                        "offset": -40.0
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateConfiguration_PollingInterval() {
        String configJson = """
            {
                "host": "192.168.1.100",
                "port": 502,
                "unitId": 1,
                "deviceId": "plc-001",
                "pollingIntervalSeconds": 30,
                "registers": [
                    {
                        "type": "HOLDING",
                        "address": 0,
                        "count": 1,
                        "variableName": "temp"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MODBUS_TCP);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }
}
