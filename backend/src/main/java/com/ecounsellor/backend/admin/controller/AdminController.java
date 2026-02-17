package com.ecounsellor.backend.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.ecounsellor.backend.admin.entity.Admin;
import com.ecounsellor.backend.admin.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service){
        this.service = service;
    }

    // 🔹 TEST
    @GetMapping("/test")
    public String test(){
        return "Admin secured works";
    }

    // 🔹 CREATE ADMIN
    @PostMapping
    public Admin create(@RequestBody Admin admin){
        return service.create(admin);
    }

    // 🔹 GET ALL ADMINS
    @GetMapping("/all")
    public List<Admin> getAll(){
        return service.getAll();
    }

    // 🔹 GET ONE ADMIN
    @GetMapping("/{id}")
    public Admin get(@PathVariable Long id){
        return service.get(id);
    }

    // 🔹 UPDATE ADMIN
    @PutMapping("/{id}")
    public Admin update(
            @PathVariable Long id,
            @RequestBody Admin admin
    ){
        return service.update(id, admin);
    }

    // 🔹 DELETE ADMIN
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }
}

