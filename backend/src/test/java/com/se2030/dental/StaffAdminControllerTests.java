package com.se2030.dental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.dental.dto.*;
import com.se2030.dental.model.AppUser;
import com.se2030.dental.model.Role;
import com.se2030.dental.repository.AppUserRepository;
import com.se2030.dental.repository.RoleRepository;
import com.se2030.dental.service.AppUserService;
import com.se2030.dental.service.StaffAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.auditing.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:staffdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
@Transactional
class StaffAdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

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

    private Role adminRole;
    private Role receptionistRole;
    private Role dentistRole;
    private Role assistantRole;
    private Role patientRole;

    private AppUser mainAdmin;
    private AppUser secondaryAdmin;
    private AppUser receptionistUser;
    private AppUser patientUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        adminRole = roleRepository.save(new Role("ADMIN", "Admin"));
        receptionistRole = roleRepository.save(new Role("RECEPTIONIST", "Receptionist"));
        dentistRole = roleRepository.save(new Role("DENTIST", "Dentist"));
        assistantRole = roleRepository.save(new Role("DENTAL_ASSISTANT", "Assistant"));
        patientRole = roleRepository.save(new Role("PATIENT", "Patient"));

        mainAdmin = userService.createUser("admin1", "admin1@se2030.com", "adminpassword", "ADMIN", true);
        secondaryAdmin = userService.createUser("admin2", "admin2@se2030.com", "adminpassword", "ADMIN", true);
        receptionistUser = userService.createUser("receptionist", "receptionist@se2030.com", "recpassword", "RECEPTIONIST", true);
        patientUser = userService.createUser("patient1", "patient1@se2030.com", "patpassword", "PATIENT", true);
    }

    private MockHttpSession getSession(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    // ==========================================
    // AUTHORIZATION TESTS
    // ==========================================

    @Test
    void endpoints_whenUnauthenticated_returnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/staff-users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required to access this resource"));

        mockMvc.perform(post("/api/admin/staff-users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpoints_whenAuthenticatedAsNonAdmin_returnForbidden() throws Exception {
        MockHttpSession recSession = getSession("receptionist", "recpassword");

        mockMvc.perform(get("/api/admin/staff-users")
                        .session(recSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied or invalid CSRF token"));

        mockMvc.perform(post("/api/admin/staff-users")
                        .session(recSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void endpoints_whenAuthenticatedAsAdmin_allowAccess() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");

        mockMvc.perform(get("/api/admin/staff-users")
                        .session(adminSession))
                .andExpect(status().isOk());
    }

    @Test
    void mutationEndpoints_withoutCsrf_returnForbidden() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");
        StaffCreateRequest request = new StaffCreateRequest();
        request.setUsername("newStaff");
        request.setEmail("newstaff@se2030.com");
        request.setPassword("password123");
        request.setRole("DENTIST");

        mockMvc.perform(post("/api/admin/staff-users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // STAFF USER CREATION TESTS
    // ==========================================

    @Test
    void createStaff_withValidData_succeedsAndEncodesPassword() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");
        StaffCreateRequest request = new StaffCreateRequest();
        request.setUsername("DrDentist");
        request.setEmail("drdentist@SE2030.com");
        request.setPassword("securePassword99");
        request.setRole("DENTIST");

        mockMvc.perform(post("/api/admin/staff-users")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("drdentist"))
                .andExpect(jsonPath("$.email").value("drdentist@se2030.com"))
                .andExpect(jsonPath("$.role").value("DENTIST"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        AppUser dbUser = userRepository.findByUsername("drdentist").orElseThrow();
        assertTrue(passwordEncoder.matches("securePassword99", dbUser.getPasswordHash()));
        assertEquals("admin1", dbUser.getCreatedBy());
    }

    @Test
    void createStaff_withPatientRole_returnsBadRequest() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");
        StaffCreateRequest request = new StaffCreateRequest();
        request.setUsername("patientAdminCreated");
        request.setEmail("patadmin@se2030.com");
        request.setPassword("password123");
        request.setRole("PATIENT");

        mockMvc.perform(post("/api/admin/staff-users")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid staff role: PATIENT"));
    }

    @Test
    void createStaff_withDuplicateUsernameOrEmail_returnsConflict() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");

        // Duplicate username check
        StaffCreateRequest req1 = new StaffCreateRequest();
        req1.setUsername("admin2"); // already exists
        req1.setEmail("different@se2030.com");
        req1.setPassword("password123");
        req1.setRole("RECEPTIONIST");

        mockMvc.perform(post("/api/admin/staff-users")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));

        // Duplicate email check
        StaffCreateRequest req2 = new StaffCreateRequest();
        req2.setUsername("differentUser");
        req2.setEmail("admin2@se2030.com"); // already exists
        req2.setPassword("password123");
        req2.setRole("RECEPTIONIST");

        mockMvc.perform(post("/api/admin/staff-users")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    // ==========================================
    // PAGINATION AND INSPECTION TESTS
    // ==========================================

    @Test
    void listStaffUsers_returnsDeterministicPaginationAndExcludesPatients() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");

        // Seed dentist and assistant
        userService.createUser("dentistUser", "dentist@se2030.com", "pw", "DENTIST", true);
        userService.createUser("assistantUser", "assistant@se2030.com", "pw", "DENTAL_ASSISTANT", true);

        mockMvc.perform(get("/api/admin/staff-users?page=0&size=10")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5)) // admin1, admin2, receptionist, dentistUser, assistantUser
                .andExpect(jsonPath("$.content[0].username").value("admin1"))
                .andExpect(jsonPath("$.content[1].username").value("admin2"))
                .andExpect(jsonPath("$.content[2].username").value("assistantuser"))
                .andExpect(jsonPath("$.content[3].username").value("dentistuser"))
                .andExpect(jsonPath("$.content[4].username").value("receptionist"))
                .andExpect(jsonPath("$.content[*].role").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("PATIENT"))));
    }

    @Test
    void inspectStaffUser_returnsSanitizedDetails_orNotFound() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");

        mockMvc.perform(get("/api/admin/staff-users/" + receptionistUser.getId())
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("receptionist"))
                .andExpect(jsonPath("$.role").value("RECEPTIONIST"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // 404 for patient inspection through staff API
        mockMvc.perform(get("/api/admin/staff-users/" + patientUser.getId())
                        .session(adminSession))
                .andExpect(status().isNotFound());

        // 404 for invalid ID
        mockMvc.perform(get("/api/admin/staff-users/99999")
                        .session(adminSession))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // STATUS MUTATION TESTS
    // ==========================================

    @Test
    void updateStatus_deactivatesUserSuccessfully() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");
        StaffStatusUpdateRequest request = new StaffStatusUpdateRequest();
        request.setActive(false);

        mockMvc.perform(patch("/api/admin/staff-users/" + receptionistUser.getId() + "/status")
                        .session(adminSession)
                        .with(user("admin1").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        AppUser dbUser = userRepository.findById(receptionistUser.getId()).orElseThrow();
        assertFalse(dbUser.isActive());
        assertEquals("admin1", dbUser.getUpdatedBy());

        // Deactivated user cannot authenticate
        LoginRequest loginRequest = new LoginRequest("receptionist", "recpassword");
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateStatus_rejectsSelfDeactivation() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");
        StaffStatusUpdateRequest request = new StaffStatusUpdateRequest();
        request.setActive(false);

        mockMvc.perform(patch("/api/admin/staff-users/" + mainAdmin.getId() + "/status")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Administrators cannot deactivate their own currently authenticated account"));
    }

    @Test
    void updateStatus_rejectsDeactivationOfFinalActiveAdmin() {
        // Deactivate admin2 first
        staffAdminService.updateStatus(secondaryAdmin.getId(), false, "admin1");

        // Deactivating admin1 (the final active admin) must throw FinalActiveAdminException
        assertThrows(com.se2030.dental.exception.FinalActiveAdminException.class, () -> {
            staffAdminService.updateStatus(mainAdmin.getId(), false, "admin2");
        });
    }

    // ==========================================
    // ROLE MUTATION TESTS
    // ==========================================

    @Test
    void updateRole_changesStaffRoleSuccessfully() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");
        StaffRoleUpdateRequest request = new StaffRoleUpdateRequest();
        request.setRole("DENTIST");

        mockMvc.perform(patch("/api/admin/staff-users/" + receptionistUser.getId() + "/role")
                        .session(adminSession)
                        .with(user("admin1").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DENTIST"));

        AppUser dbUser = userRepository.findById(receptionistUser.getId()).orElseThrow();
        assertEquals("DENTIST", dbUser.getRole().getName());
        assertEquals("admin1", dbUser.getUpdatedBy());
    }

    @Test
    void updateRole_rejectsDemotingToPatient() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");
        StaffRoleUpdateRequest request = new StaffRoleUpdateRequest();
        request.setRole("PATIENT");

        mockMvc.perform(patch("/api/admin/staff-users/" + receptionistUser.getId() + "/role")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid staff role: PATIENT"));
    }

    @Test
    void updateRole_rejectsSelfDemotion() throws Exception {
        MockHttpSession adminSession = getSession("admin1", "adminpassword");
        StaffRoleUpdateRequest request = new StaffRoleUpdateRequest();
        request.setRole("RECEPTIONIST");

        mockMvc.perform(patch("/api/admin/staff-users/" + mainAdmin.getId() + "/role")
                        .session(adminSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Administrators cannot demote themselves or modify their own role during their current session"));
    }

    @Test
    void updateRole_rejectsDemotionOfFinalActiveAdmin() {
        // Deactivate admin2 first
        staffAdminService.updateStatus(secondaryAdmin.getId(), false, "admin1");

        // Demoting admin1 (the final active admin) must throw FinalActiveAdminException
        assertThrows(com.se2030.dental.exception.FinalActiveAdminException.class, () -> {
            staffAdminService.updateRole(mainAdmin.getId(), "DENTIST", "admin2");
        });
    }
}
