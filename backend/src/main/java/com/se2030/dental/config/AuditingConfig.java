package com.se2030.dental.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
@ConditionalOnProperty(name = "spring.jpa.auditing.enabled", havingValue = "true", matchIfMissing = true)
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // Temporary system auditor. In the next S4 substep, this will read from 
        // SecurityContextHolder.getContext().getAuthentication().getName()
        return () -> Optional.of("SYSTEM");
    }
}
