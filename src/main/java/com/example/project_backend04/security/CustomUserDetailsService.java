package com.example.project_backend04.security;

import com.example.project_backend04.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthRepository authRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return authRepository.findByUsername(username)
                .map(user -> new CustomUserDetails(
                        user,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().getName()
                        ))
                ))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User không tồn tại")
                );
    }
}
