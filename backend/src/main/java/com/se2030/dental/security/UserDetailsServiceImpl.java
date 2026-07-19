package com.se2030.dental.security;

import com.se2030.dental.model.AppUser;
import com.se2030.dental.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository userRepository;

    public UserDetailsServiceImpl(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username must not be empty");
        }

        AppUser user = userRepository.findByUsername(username.trim().toLowerCase(java.util.Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new AppUserDetails(user);
    }
}
