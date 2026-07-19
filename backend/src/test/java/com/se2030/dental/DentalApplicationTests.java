package com.se2030.dental;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;

import com.se2030.dental.repository.AppUserRepository;
import com.se2030.dental.repository.RoleRepository;
import com.se2030.dental.service.AppUserService;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
    "spring.jpa.auditing.enabled=false"
})
class DentalApplicationTests {

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AppUserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private AppUserService userService;

    @Test
    void contextLoads() {
    }
}
