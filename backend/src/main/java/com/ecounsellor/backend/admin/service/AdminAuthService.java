package com.ecounsellor.backend.admin.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecounsellor.backend.admin.entity.Admin;
import com.ecounsellor.backend.admin.repository.AdminRepository;
import com.ecounsellor.backend.admin.util.JwtUtil;

@Service
public class AdminAuthService {

    private final AdminRepository repo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AdminAuthService(
        AdminRepository repo,
        PasswordEncoder encoder,
        JwtUtil jwtUtil
    ){
        this.repo = repo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public String login(
        String username,
        String password
    ){

        Admin admin = repo.findByUsername(username)
            .orElseThrow(() ->
                new RuntimeException("User not found")
            );

        if(!encoder.matches(password,
                            admin.getPassword())){
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateToken(admin.getUsername());
    }
}
