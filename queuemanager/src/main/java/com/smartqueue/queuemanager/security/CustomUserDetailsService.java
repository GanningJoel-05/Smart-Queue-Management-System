package com.smartqueue.queuemanager.security;

import com.smartqueue.queuemanager.entity.User;
import com.smartqueue.queuemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Spring Security expects roles prefixed with "ROLE_"
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getIsActive(),   // enabled
                true,                 // accountNonExpired
                true,                 // credentialsNonExpired
                true,                 // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}