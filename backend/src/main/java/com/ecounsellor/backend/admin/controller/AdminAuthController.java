package com.ecounsellor.backend.admin.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.admin.dto.LoginRequest;
import com.ecounsellor.backend.admin.service.AdminAuthService;

@RestController
@RequestMapping("/auth")
public class AdminAuthController {

    private final AdminAuthService service;

    public AdminAuthController(AdminAuthService service){
        this.service = service;
    }

    @PostMapping("/login")
    public Map<String,String> login(
            @RequestBody LoginRequest request){

        String token =
            service.login(
                request.getUsername(),
                request.getPassword()
            );

        return Map.of("token", token);
    }
}

