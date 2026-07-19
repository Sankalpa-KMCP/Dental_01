package com.se2030.dental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.dental.dto.LoginRequest;
import com.se2030.dental.dto.StaffCreateRequest;
import com.se2030.dental.dto.StaffRoleUpdateRequest;
import com.se2030.dental.dto.StaffStatusUpdateRequest;
import com.se2030.dental.model.AppUser;
import com.se2030.dental.repository.AppUserRepository;
import com.se2030.dental.service.AppUserService;
import com.se2030.dental.service.StaffAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_REAL_DB_ADMIN_TESTS", matches = "true")
@ContextConfiguration(initializers = RealDatabaseStaffAdminTests.SafetyInitializer.class)
@TestPropertySource(properties = {
    "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/dental_management_dev}",
    "spring.datasource.username=${DB_USERNAME:dental_user}",
    "spring.datasource.password=${DB_PASSWORD:dental_pass}",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class RealDatabaseStaffAdminTests {

    public static class SafetyInitializer implements org.springframework.context.ApplicationContextInitializer<org.springframework.context.ConfigurableApplicationContext> {
        @Override
        public void initialize(org.springframework.context.ConfigurableApplicationContext context) {
            org.springframework.core.env.ConfigurableEnvironment env = context.getEnvironment();

            String runFlag = env.getProperty("RUN_REAL_DB_ADMIN_TESTS");
            if (!"true".equalsIgnoreCase(runFlag)) {
                throw new IllegalStateException("Safety check failed: RUN_REAL_DB_ADMIN_TESTS must be set to 'true'");
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
    private AppUserService userService;

    @Autowired
    private StaffAdminService staffAdminService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.flywaydb.core.Flyway flyway;

    private AppUser tempAdmin;
    private AppUser tempAdmin2;
    private Long tempStaffUserId;
    private long startingCount;

    @BeforeEach
    void setUp() throws Exception {
        flyway.validate();

        startingCount = userRepository.count();

        tempAdmin = userService.createUser("tempadminadmin", "tempadminadmin@se2030.com", "tempAdminPassword123", "ADMIN", true);
    }

    private MockHttpSession getSession() throws Exception {
        LoginRequest loginRequest = new LoginRequest("tempadminadmin", "tempAdminPassword123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void testRealDatabaseStaffAdminLifecycle() throws Exception {
        MockHttpSession adminSession = getSession();

        StaffCreateRequest request = new StaffCreateRequest();
        request.setUsername("tempdentiststaff");
        request.setEmail("tempdentiststaff@se2030.com");
        request.setPassword("tempDentistPassword123");
        request.setRole("DENTIST");

        MvcResult createResult = mockMvc.perform(post("/api/admin/staff-users")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("tempdentiststaff"))
                .andExpect(jsonPath("$.role").value("DENTIST"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        tempStaffUserId = objectMapper.readTree(body).get("id").asLong();

        AppUser staffUser = userRepository.findById(tempStaffUserId).orElseThrow();
        assertTrue(passwordEncoder.matches("tempDentistPassword123", staffUser.getPasswordHash()));

        mockMvc.perform(get("/api/admin/staff-users/" + tempStaffUserId)
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tempdentiststaff"));

        mockMvc.perform(get("/api/admin/staff-users?page=0&size=20")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + tempStaffUserId + ")]").exists());

        StaffRoleUpdateRequest roleRequest = new StaffRoleUpdateRequest();
        roleRequest.setRole("RECEPTIONIST");

        mockMvc.perform(patch("/api/admin/staff-users/" + tempStaffUserId + "/role")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RECEPTIONIST"));

        StaffStatusUpdateRequest statusRequest = new StaffStatusUpdateRequest();
        statusRequest.setActive(false);

        mockMvc.perform(patch("/api/admin/staff-users/" + tempStaffUserId + "/status")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        LoginRequest loginRequest = new LoginRequest("tempdentiststaff", "tempDentistPassword123");
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testConcurrentAdminDeactivation() throws Exception {
        // Create second active administrator via application services
        tempAdmin2 = userService.createUser("tempadminadmin2", "tempadminadmin2@se2030.com", "tempAdminPassword456", "ADMIN", true);

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicReference<Throwable> error1 = new AtomicReference<>();
        AtomicReference<Throwable> error2 = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                barrier.await();
                staffAdminService.updateStatus(tempAdmin2.getId(), false, tempAdmin.getUsername());
            } catch (Throwable t) {
                error1.set(t);
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                barrier.await();
                staffAdminService.updateStatus(tempAdmin.getId(), false, tempAdmin2.getUsername());
            } catch (Throwable t) {
                error2.set(t);
            }
        });

        t1.start();
        t2.start();

        t1.join(10000);
        t2.join(10000);

        boolean t1Succeeded = (error1.get() == null);
        boolean t2Succeeded = (error2.get() == null);

        // Exactly one concurrent deactivation must succeed, and the other must fail with FinalActiveAdminException
        assertTrue(t1Succeeded != t2Succeeded, "Exactly one concurrent deactivation should succeed");

        Throwable failedException = t1Succeeded ? error2.get() : error1.get();
        assertNotNull(failedException);

        // Find the root cause to handle Spring transaction wrappers if any
        Throwable root = failedException;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        assertTrue(root instanceof com.se2030.dental.exception.FinalActiveAdminException,
                "Expected FinalActiveAdminException but got " + root.getClass().getName());

        // Verify at least one active ADMIN remains
        long activeAdminsCount = userRepository.findAll().stream()
                .filter(u -> u.getRole().getName().equals("ADMIN") && u.isActive())
                .count();
        assertTrue(activeAdminsCount >= 1, "At least one active ADMIN must remain after concurrent deactivations");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (tempStaffUserId != null) {
            try {
                userRepository.deleteById(tempStaffUserId);
            } catch (Exception ignored) {}
        }
        if (tempAdmin2 != null) {
            try {
                userRepository.delete(tempAdmin2);
            } catch (Exception ignored) {}
        }
        if (tempAdmin != null) {
            try {
                userRepository.delete(tempAdmin);
            } catch (Exception ignored) {}
        }

        assertEquals(startingCount, userRepository.count());
    }
}
