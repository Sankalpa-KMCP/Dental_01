package com.se2030.dental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.dental.dto.LoginRequest;
import com.se2030.dental.model.AppUser;
import com.se2030.dental.model.Role;
import com.se2030.dental.repository.AppUserRepository;
import com.se2030.dental.repository.RoleRepository;
import com.se2030.dental.service.AppUserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
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
@org.springframework.context.annotation.Import(AuthControllerTests.TestAdminController.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.auditing.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
@Transactional
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AppUserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private Role adminRole;
    private Role patientRole;
    private AppUser activeAdmin;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        adminRole = roleRepository.save(new Role("ADMIN", "System Admin"));
        patientRole = roleRepository.save(new Role("PATIENT", "Patient User"));

        activeAdmin = userService.createUser("adminUser", "admin@se2030.com", "securePassword123", "ADMIN", true);
        userService.createUser("disabledUser", "disabled@se2030.com", "password123", "PATIENT", false);
        userService.createUser("patientUser", "patient@se2030.com", "patientPassword", "PATIENT", true);
    }

    @Test
    void userDetails_mapsRolesToAuthorities() {
        Role receptionistRole = roleRepository.save(new Role("RECEPTIONIST", "Receptionist"));
        AppUser user = new AppUser("receptionistUser", "rep@se2030.com", "hash", receptionistRole, true);
        com.se2030.dental.security.AppUserDetails userDetails = new com.se2030.dental.security.AppUserDetails(user);

        assertEquals("receptionistUser", userDetails.getUsername());
        assertEquals("hash", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());

        org.springframework.security.core.GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertEquals("ROLE_RECEPTIONIST", authority.getAuthority());
    }

    @Test
    void getCsrfToken_returnsHeaderAndToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    void login_withoutCsrf_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied or invalid CSRF token"));
    }

    @Test
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "wrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_withUnknownUser_returnsUnauthorizedAndGenericError() throws Exception {
        LoginRequest loginRequest = new LoginRequest("unknownUser", "password");
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_withDisabledUser_returnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest("disabledUser", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_withValidCredentials_establishesSessionAndReturnsSanitizedUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeAdmin.getId()))
                .andExpect(jsonPath("$.username").value("adminuser"))
                .andExpect(jsonPath("$.email").value("admin@se2030.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertNotNull(session);
    }

    @Test
    void getMe_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required to access this resource"));
    }

    @Test
    void getMe_whenAuthenticated_returnsSanitizedUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeAdmin.getId()))
                .andExpect(jsonPath("$.username").value("adminuser"))
                .andExpect(jsonPath("$.email").value("admin@se2030.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void logout_withoutCsrf_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_withCsrf_invalidatesSession() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertFalse(session.isInvalid());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());

        // Old session should be rejected
        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpoint_remainsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unmatchedProtectedEndpoint_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/other-unmatched-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditing_populatesAuthenticatedUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        org.springframework.security.core.context.SecurityContext context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        org.springframework.security.core.Authentication auth = context.getAuthentication();

        try {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            new com.se2030.dental.security.AppUserDetails(activeAdmin),
                            null,
                            org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ADMIN")
                    );
            context.setAuthentication(authToken);

            AppUser newUser = new AppUser("auditeduser", "audited@se2030.com", "pw_hash", adminRole, true);
            AppUser saved = userRepository.saveAndFlush(newUser);

            assertEquals("adminuser", saved.getCreatedBy());
            assertEquals("adminuser", saved.getUpdatedBy());

        } finally {
            context.setAuthentication(auth);
        }
    }

    @Test
    void login_withInvalidCsrf_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied or invalid CSRF token"));
    }

    @Test
    void logout_withInvalidCsrf_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied or invalid CSRF token"));
    }

    @Test
    void testAdmin_whenAuthenticatedAsPatient_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest("patientUser", "patientPassword");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/test-admin-only"))
                .andExpect(status().isUnauthorized()); // Unauthenticated direct check

        mockMvc.perform(get("/api/auth/test-admin-only")
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied or invalid CSRF token"));
    }

    @Test
    void testAdmin_whenAuthenticatedAsAdmin_returnsOk() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adminUser", "securePassword123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/test-admin-only")
                        .session(session))
                .andExpect(status().isOk());
    }

    @org.springframework.web.bind.annotation.RestController
    @org.springframework.web.bind.annotation.RequestMapping("/api/auth/test-admin-only")
    static class TestAdminController {
        @org.springframework.web.bind.annotation.GetMapping
        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        public String test() {
            return "ok";
        }
    }
}
