package com.se2030.dental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.dental.dto.LoginRequest;
import com.se2030.dental.model.AppUser;
import com.se2030.dental.model.Role;
import com.se2030.dental.repository.AppUserRepository;
import com.se2030.dental.repository.RoleRepository;
import com.se2030.dental.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_REAL_DB_AUTH_TESTS", matches = "true")
@org.springframework.test.context.ContextConfiguration(initializers = RealDatabaseAuthTests.SafetyInitializer.class)
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/dental_management_dev}",
    "spring.datasource.username=${DB_USERNAME:dental_user}",
    "spring.datasource.password=${DB_PASSWORD:dental_pass}",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class RealDatabaseAuthTests {

    public static class SafetyInitializer implements org.springframework.context.ApplicationContextInitializer<org.springframework.context.ConfigurableApplicationContext> {
        @Override
        public void initialize(org.springframework.context.ConfigurableApplicationContext context) {
            org.springframework.core.env.ConfigurableEnvironment env = context.getEnvironment();

            String runFlag = env.getProperty("RUN_REAL_DB_AUTH_TESTS");
            if (!"true".equalsIgnoreCase(runFlag)) {
                throw new IllegalStateException("Safety check failed: RUN_REAL_DB_AUTH_TESTS must be set to 'true'");
            }

            String approvedDb = env.getProperty("REAL_DB_TEST_DATABASE");
            if (approvedDb == null || !approvedDb.equals("dental_management_dev")) {
                throw new IllegalStateException("Safety check failed: REAL_DB_TEST_DATABASE must be set to 'dental_management_dev'");
            }

            String dbUrl = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String password = env.getProperty("spring.datasource.password");

            if (dbUrl == null || username == null || password == null) {
                throw new IllegalStateException("Safety check failed: DB_URL, DB_USERNAME, or DB_PASSWORD environment variable is missing");
            }

            boolean isLocal = dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1");
            if (!isLocal) {
                throw new IllegalStateException("Safety check failed: Datasource URL does not target localhost or 127.0.0.1: " + dbUrl);
            }

            if (!dbUrl.contains("dental_management_dev")) {
                throw new IllegalStateException("Safety check failed: Datasource URL does not target approved schema 'dental_management_dev': " + dbUrl);
            }

            if (dbUrl.toLowerCase().contains("prod") || dbUrl.toLowerCase().contains("production")) {
                throw new IllegalStateException("Safety check failed: Datasource URL looks like a production database: " + dbUrl);
            }

            String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto");
            if (!"validate".equalsIgnoreCase(ddlAuto)) {
                throw new IllegalStateException("Safety check failed: Hibernate DDL mode must be 'validate' but was: " + ddlAuto);
            }
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AppUserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private org.flywaydb.core.Flyway flyway;

    @org.junit.jupiter.api.BeforeEach
    void verifySafetyChecks() throws Exception {
        String approvedDb = System.getenv("REAL_DB_TEST_DATABASE");
        if (approvedDb == null || !approvedDb.equals("dental_management_dev")) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                "Skipping real-database test: REAL_DB_TEST_DATABASE must be set to 'dental_management_dev'");
            return;
        }

        String dbUrl;
        try (java.sql.Connection conn = dataSource.getConnection()) {
            dbUrl = conn.getMetaData().getURL();
        }
        if (dbUrl == null) {
            fail("Datasource URL is null");
        }

        boolean isLocal = dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1");
        if (!isLocal) {
            fail("Safety check failed: Datasource URL does not target localhost/127.0.0.1: " + dbUrl);
        }

        if (!dbUrl.contains("dental_management_dev")) {
            fail("Safety check failed: Datasource URL does not target the approved schema 'dental_management_dev': " + dbUrl);
        }

        if (dbUrl.toLowerCase().contains("prod") || dbUrl.toLowerCase().contains("production")) {
            fail("Safety check failed: Datasource URL looks like a production database: " + dbUrl);
        }

        try {
            flyway.validate();
        } catch (Exception e) {
            fail("Safety check failed: Flyway validation is not healthy: " + e.getMessage());
        }
    }

    @Test
    void testRealDatabaseLoginMeLogout() throws Exception {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not seeded by Flyway"));

        long initialCount = userRepository.count();

        String username = "temp_smoke_user_" + System.currentTimeMillis();
        String email = username + "@se2030.com";
        String password = "TempSecurePassword123!";

        AppUser user = userService.createUser(username, email, password, "ADMIN", true);
        assertNotNull(user.getId());

        try {
            mockMvc.perform(get("/api/auth/csrf"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());

            LoginRequest loginRequest = new LoginRequest(username, password);
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
            assertNotNull(session);

            mockMvc.perform(get("/api/auth/me")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username));

            mockMvc.perform(post("/api/auth/logout")
                            .session(session)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            assertTrue(session.isInvalid());

            mockMvc.perform(get("/api/auth/me")
                            .session(session))
                    .andExpect(status().isUnauthorized());

        } finally {
            userRepository.delete(user);
            long finalCount = userRepository.count();
            assertEquals(initialCount, finalCount, "User count did not return to its starting value!");
        }
    }
}
