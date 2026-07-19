package com.se2030.dental.repository;

import com.se2030.dental.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    org.springframework.data.domain.Page<AppUser> findByRole_NameIn(java.util.List<String> roleNames, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM AppUser u WHERE u.role.name = 'ADMIN' AND u.active = true")
    java.util.List<AppUser> findActiveAdminsForUpdate();
}
