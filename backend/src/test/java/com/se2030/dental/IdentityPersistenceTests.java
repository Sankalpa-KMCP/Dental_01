package com.se2030.dental;

import com.se2030.dental.config.AuditingConfig;
import com.se2030.dental.config.SecurityConfig;
import com.se2030.dental.model.AppUser;
import com.se2030.dental.model.Role;
import com.se2030.dental.repository.AppUserRepository;
import com.se2030.dental.repository.RoleRepository;
import com.se2030.dental.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({AuditingConfig.class, SecurityConfig.class, AppUserService.class})
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.auditing.enabled=true"
})
class IdentityPersistenceTests {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AppUserService userService;

    @Test
    void persistRole_andFindByName() {
        Role role = new Role("TEST_ROLE", "A test role description");
        Role saved = roleRepository.save(role);

        assertNotNull(saved.getId());
        assertEquals("TEST_ROLE", saved.getName());

        Optional<Role> found = roleRepository.findByName("TEST_ROLE");
        assertTrue(found.isPresent());
        assertEquals("A test role description", found.get().getDescription());
    }

    @Test
    void persistUser_appliesAuditingAndRelations() {
        Role role = roleRepository.save(new Role("ADMIN", "System Admin"));

        AppUser user = new AppUser("testuser", "test@se2030.com", "hashed_pw", role, true);
        AppUser saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("testuser", saved.getUsername());
        assertEquals("ADMIN", saved.getRole().getName());
        assertTrue(saved.isActive());

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("SYSTEM", saved.getCreatedBy());
        assertEquals("SYSTEM", saved.getUpdatedBy());
    }

    @Test
    void persistUser_uniqueConstraints_throwsException() {
        Role role = roleRepository.save(new Role("PATIENT", "Patient User"));

        AppUser user1 = new AppUser("dupuser", "dup1@se2030.com", "pw", role, true);
        userRepository.saveAndFlush(user1);

        AppUser user2 = new AppUser("dupuser", "dup2@se2030.com", "pw", role, true);
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2));

        AppUser user3 = new AppUser("otheruser", "dup1@se2030.com", "pw", role, true);
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user3));
    }

    @Test
    void userService_createUser_encodesPasswordAndValidates() {
        roleRepository.save(new Role("DENTIST", "Dentist Role"));

        AppUser created = userService.createUser("dentist_john", "john@se2030.com", "myPlainPassword", "DENTIST", true);

        assertNotNull(created.getId());
        assertEquals("dentist_john", created.getUsername());
        assertNotEquals("myPlainPassword", created.getPasswordHash());
        assertTrue(created.getPasswordHash().startsWith("$2a$"));
        assertEquals("DENTIST", created.getRole().getName());

        assertThrows(IllegalArgumentException.class, () -> 
            userService.createUser("dentist_john", "different@se2030.com", "pw", "DENTIST", true)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            userService.createUser("different", "john@se2030.com", "pw", "DENTIST", true)
        );
    }
}
