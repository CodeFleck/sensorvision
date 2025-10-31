package org.sensorvision.regression;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.sensorvision.dto.*;
import org.sensorvision.entity.*;
import org.sensorvision.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Full System Regression Test Suite
 *
 * This test suite validates the entire SensorVision platform including:
 * - User authentication and authorization
 * - Device management lifecycle
 * - Telemetry ingestion and storage
 * - Rules engine and alerting
 * - Synthetic variables calculation
 * - Dashboard and analytics
 * - Multi-tenancy isolation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullSystemRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SyntheticVariableRepository syntheticVariableRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Long organizationId;
    private Long deviceId;
    private String deviceToken;

    @BeforeAll
    void setupTestData() throws Exception {
        // Create organization
        Organization org = new Organization();
        org.setName("Test Organization");
        org.setCreatedAt(LocalDateTime.now());
        org = organizationRepository.save(org);
        organizationId = org.getId();

        // Create admin user
        User admin = new User();
        admin.setUsername("test_admin");
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        admin.setRole(UserRole.ADMIN);
        admin.setOrganization(org);
        admin.setActive(true);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);

        // Create regular user
        User user = new User();
        user.setUsername("test_user");
        user.setEmail("user@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setOrganization(org);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Login as admin
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername("test_admin");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        Map<String, Object> loginResponse = objectMapper.readValue(response, Map.class);
        adminToken = (String) loginResponse.get("token");

        // Login as user
        loginRequest.setUsername("test_user");
        loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        response = loginResult.getResponse().getContentAsString();
        loginResponse = objectMapper.readValue(response, Map.class);
        userToken = (String) loginResponse.get("token");
    }

    @Test
    @Order(1)
    @DisplayName("REGRESSION: Authentication System")
    void testAuthenticationSystem() throws Exception {
        // Test successful login (already done in setup)
        assertNotNull(adminToken);
        assertNotNull(userToken);

        // Test invalid credentials
        LoginRequestDto invalidLogin = new LoginRequestDto();
        invalidLogin.setUsername("invalid_user");
        invalidLogin.setPassword("wrong_password");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());

        // Test protected endpoint without token
        mockMvc.perform(get("/api/v1/devices"))
                .andExpect(status().isUnauthorized());

        // Test protected endpoint with valid token
        mockMvc.perform(get("/api/v1/devices")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("REGRESSION: Device Lifecycle")
    void testDeviceLifecycle() throws Exception {
        // 1. Create device
        DeviceCreationDto deviceDto = new DeviceCreationDto();
        deviceDto.setName("Test Smart Meter");
        deviceDto.setDeviceId("test-meter-001");
        deviceDto.setDescription("Test smart meter for regression testing");
        deviceDto.setActive(true);

        MvcResult createResult = mockMvc.perform(post("/api/v1/devices")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value("test-meter-001"))
                .andExpect(jsonPath("$.name").value("Test Smart Meter"))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Map<String, Object> deviceResponse = objectMapper.readValue(createResponse, Map.class);
        deviceId = Long.valueOf(deviceResponse.get("id").toString());

        // 2. Generate device token
        MvcResult tokenResult = mockMvc.perform(post("/api/v1/device-tokens/" + deviceId + "/generate")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(deviceId))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String tokenResponse = tokenResult.getResponse().getContentAsString();
        Map<String, Object> tokenData = objectMapper.readValue(tokenResponse, Map.class);
        deviceToken = (String) tokenData.get("token");

        // 3. Retrieve device
        mockMvc.perform(get("/api/v1/devices/" + deviceId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deviceId))
                .andExpect(jsonPath("$.deviceId").value("test-meter-001"));

        // 4. List all devices
        mockMvc.perform(get("/api/v1/devices")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].deviceId").value("test-meter-001"));

        // 5. Update device
        deviceDto.setName("Updated Smart Meter");
        mockMvc.perform(put("/api/v1/devices/" + deviceId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Smart Meter"));
    }

    @Test
    @Order(3)
    @DisplayName("REGRESSION: Telemetry Ingestion")
    void testTelemetryIngestion() throws Exception {
        // Create telemetry payload
        Map<String, Object> telemetryData = new HashMap<>();
        telemetryData.put("voltage", 220.5);
        telemetryData.put("current", 5.2);
        telemetryData.put("power", 1146.6);
        telemetryData.put("temperature", 35.8);

        // Send telemetry via simple ingestion endpoint
        mockMvc.perform(post("/api/v1/ingest/test-meter-001")
                .header("Authorization", "Bearer " + deviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(telemetryData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Verify data was stored
        Thread.sleep(500); // Allow async processing

        mockMvc.perform(get("/api/v1/devices/" + deviceId + "/telemetry")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].voltage").value(220.5))
                .andExpect(jsonPath("$[0].current").value(5.2));

        // Send multiple data points
        for (int i = 0; i < 5; i++) {
            telemetryData.put("voltage", 220.0 + i);
            telemetryData.put("current", 5.0 + i * 0.1);

            mockMvc.perform(post("/api/v1/ingest/test-meter-001")
                    .header("Authorization", "Bearer " + deviceToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(telemetryData)))
                    .andExpect(status().isOk());
        }

        Thread.sleep(1000); // Allow processing

        // Verify multiple records
        long recordCount = telemetryRepository.count();
        assertTrue(recordCount >= 6, "Should have at least 6 telemetry records");
    }

    @Test
    @Order(4)
    @DisplayName("REGRESSION: Rules Engine")
    void testRulesEngine() throws Exception {
        // Create alert rule (temperature > 40)
        RuleCreationDto ruleDto = new RuleCreationDto();
        ruleDto.setDeviceId(deviceId);
        ruleDto.setName("High Temperature Alert");
        ruleDto.setVariableName("temperature");
        ruleDto.setOperator(RuleOperator.GT);
        ruleDto.setThreshold(40.0);
        ruleDto.setSeverity(AlertSeverity.WARNING);
        ruleDto.setActive(true);

        MvcResult ruleResult = mockMvc.perform(post("/api/v1/rules")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ruleDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("High Temperature Alert"))
                .andReturn();

        String ruleResponse = ruleResult.getResponse().getContentAsString();
        Map<String, Object> ruleData = objectMapper.readValue(ruleResponse, Map.class);
        Long ruleId = Long.valueOf(ruleData.get("id").toString());

        // Send telemetry that triggers the rule
        Map<String, Object> triggeringData = new HashMap<>();
        triggeringData.put("temperature", 45.0);
        triggeringData.put("voltage", 220.0);

        mockMvc.perform(post("/api/v1/ingest/test-meter-001")
                .header("Authorization", "Bearer " + deviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(triggeringData)))
                .andExpect(status().isOk());

        Thread.sleep(1000); // Allow rule evaluation

        // Verify alert was created
        mockMvc.perform(get("/api/v1/alerts")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.ruleId == " + ruleId + ")]").exists());

        // Send normal temperature (should not trigger)
        triggeringData.put("temperature", 30.0);
        mockMvc.perform(post("/api/v1/ingest/test-meter-001")
                .header("Authorization", "Bearer " + deviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(triggeringData)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("REGRESSION: Synthetic Variables")
    void testSyntheticVariables() throws Exception {
        // Create synthetic variable (power_calculated = voltage * current)
        SyntheticVariableCreationDto syntheticDto = new SyntheticVariableCreationDto();
        syntheticDto.setDeviceId(deviceId);
        syntheticDto.setName("power_calculated");
        syntheticDto.setExpression("voltage * current");
        syntheticDto.setActive(true);

        MvcResult syntheticResult = mockMvc.perform(post("/api/v1/synthetic-variables")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(syntheticDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("power_calculated"))
                .andReturn();

        // Send telemetry with voltage and current
        Map<String, Object> telemetryData = new HashMap<>();
        telemetryData.put("voltage", 230.0);
        telemetryData.put("current", 10.0);

        mockMvc.perform(post("/api/v1/ingest/test-meter-001")
                .header("Authorization", "Bearer " + deviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(telemetryData)))
                .andExpect(status().isOk());

        Thread.sleep(1000); // Allow calculation

        // Verify synthetic variable was calculated (230 * 10 = 2300)
        MvcResult telemetryResult = mockMvc.perform(get("/api/v1/devices/" + deviceId + "/telemetry")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "1"))
                .andExpect(status().isOk())
                .andReturn();

        String telemetryResponse = telemetryResult.getResponse().getContentAsString();
        // Verify power_calculated exists in synthetic values
        assertTrue(telemetryResponse.contains("power_calculated") ||
                   telemetryResponse.contains("2300"));
    }

    @Test
    @Order(6)
    @DisplayName("REGRESSION: Analytics and Aggregations")
    void testAnalytics() throws Exception {
        // Get analytics for device
        mockMvc.perform(get("/api/v1/analytics/devices/" + deviceId)
                .header("Authorization", "Bearer " + adminToken)
                .param("variableName", "voltage")
                .param("aggregation", "AVG")
                .param("interval", "1h")
                .param("start", LocalDateTime.now().minusHours(1).toString())
                .param("end", LocalDateTime.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Test different aggregations
        String[] aggregations = {"MIN", "MAX", "AVG", "SUM"};
        for (String agg : aggregations) {
            mockMvc.perform(get("/api/v1/analytics/devices/" + deviceId)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("variableName", "voltage")
                    .param("aggregation", agg)
                    .param("interval", "1h"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Order(7)
    @DisplayName("REGRESSION: Dashboard and Widgets")
    void testDashboards() throws Exception {
        // Create dashboard
        Map<String, Object> dashboardDto = new HashMap<>();
        dashboardDto.put("name", "Test Dashboard");
        dashboardDto.put("description", "Regression test dashboard");

        MvcResult dashboardResult = mockMvc.perform(post("/api/v1/dashboards")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dashboardDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Dashboard"))
                .andReturn();

        String dashboardResponse = dashboardResult.getResponse().getContentAsString();
        Map<String, Object> dashboardData = objectMapper.readValue(dashboardResponse, Map.class);
        Long dashboardId = Long.valueOf(dashboardData.get("id").toString());

        // List dashboards
        mockMvc.perform(get("/api/v1/dashboards")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Get specific dashboard
        mockMvc.perform(get("/api/v1/dashboards/" + dashboardId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dashboardId));
    }

    @Test
    @Order(8)
    @DisplayName("REGRESSION: Multi-Tenancy Isolation")
    void testMultiTenancyIsolation() throws Exception {
        // Create second organization
        Organization org2 = new Organization();
        org2.setName("Test Organization 2");
        org2.setCreatedAt(LocalDateTime.now());
        org2 = organizationRepository.save(org2);

        // Create user in second organization
        User user2 = new User();
        user2.setUsername("test_user2");
        user2.setEmail("user2@test.com");
        user2.setPasswordHash(passwordEncoder.encode("password123"));
        user2.setRole(UserRole.USER);
        user2.setOrganization(org2);
        user2.setActive(true);
        user2.setCreatedAt(LocalDateTime.now());
        userRepository.save(user2);

        // Login as user from org2
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername("test_user2");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        Map<String, Object> loginResponse = objectMapper.readValue(response, Map.class);
        String org2Token = (String) loginResponse.get("token");

        // Try to access devices from org1 (should be empty/forbidden)
        MvcResult devicesResult = mockMvc.perform(get("/api/v1/devices")
                .header("Authorization", "Bearer " + org2Token))
                .andExpect(status().isOk())
                .andReturn();

        String devicesResponse = devicesResult.getResponse().getContentAsString();
        assertTrue(devicesResponse.equals("[]") || !devicesResponse.contains("test-meter-001"),
                "User from org2 should not see devices from org1");

        // Try to access specific device from org1 (should be forbidden)
        mockMvc.perform(get("/api/v1/devices/" + deviceId)
                .header("Authorization", "Bearer " + org2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("REGRESSION: Device Health Score")
    void testDeviceHealthScore() throws Exception {
        mockMvc.perform(get("/api/v1/devices/" + deviceId + "/health")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthScore").exists())
                .andExpect(jsonPath("$.healthScore").isNumber());
    }

    @Test
    @Order(10)
    @DisplayName("REGRESSION: System Health Check")
    void testSystemHealth() throws Exception {
        // Check actuator health endpoints
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @AfterAll
    void cleanup() {
        // Cleanup is handled by @Transactional rollback
        System.out.println("✅ Full System Regression Test Suite Completed Successfully");
    }
}
