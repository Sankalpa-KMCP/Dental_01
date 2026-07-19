package com.se2030.dental;

import com.se2030.dental.controller.HealthController;
import com.se2030.dental.service.HealthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.dental.config.SecurityConfig;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, com.se2030.dental.security.CustomAuthenticationEntryPoint.class, com.se2030.dental.security.CustomAccessDeniedHandler.class})
class HealthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    void health_whenDatabaseHealthy_returns200AndUp() throws Exception {
        Mockito.when(healthService.isDatabaseHealthy()).thenReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.database", is("UP")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void health_whenDatabaseDown_returns503AndErrorResponse() throws Exception {
        Mockito.when(healthService.isDatabaseHealthy()).thenReturn(false);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is(503)))
                .andExpect(jsonPath("$.error", is("SERVICE_UNAVAILABLE")))
                .andExpect(jsonPath("$.message", is("Database connection check failed")))
                .andExpect(jsonPath("$.path", is("/api/health")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void health_unauthorizedEndpoints_areDeniedWith401() throws Exception {
        mockMvc.perform(get("/api/other-endpoint"))
                .andExpect(status().isUnauthorized());
    }
}
