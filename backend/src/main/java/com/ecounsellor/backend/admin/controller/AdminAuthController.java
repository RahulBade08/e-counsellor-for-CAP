package com.ecounsellor.backend.admin.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam String username,
            @RequestParam String password){

        String token = service.login(username, password);

        return Map.of("token", token);
    }
    
   

}
