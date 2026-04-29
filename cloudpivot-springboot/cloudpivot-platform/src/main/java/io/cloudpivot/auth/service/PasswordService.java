package io.cloudpivot.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword != null
                && passwordHash != null
                && passwordEncoder.matches(rawPassword, passwordHash);
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
