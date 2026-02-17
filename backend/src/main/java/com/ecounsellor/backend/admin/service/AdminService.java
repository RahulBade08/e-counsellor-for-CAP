package com.ecounsellor.backend.admin.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecounsellor.backend.admin.entity.Admin;
import com.ecounsellor.backend.admin.repository.AdminRepository;

@Service
public class AdminService {

    private final AdminRepository repo;
    private final PasswordEncoder encoder;

    public AdminService(
        AdminRepository repo,
        PasswordEncoder encoder
    ){
        this.repo = repo;
        this.encoder = encoder;
    }

    // CREATE
    public Admin create(Admin admin){
        admin.setPassword(
            encoder.encode(admin.getPassword())
        );
        return repo.save(admin);
    }

    // READ ALL
    public List<Admin> getAll(){
        return repo.findAll();
    }

    // READ ONE
    public Admin get(Long id){
        return repo.findById(id)
            .orElseThrow();
    }

    // UPDATE
    public Admin update(Long id, Admin updated){
        Admin a = get(id);

        a.setEmail(updated.getEmail());
        a.setUsername(updated.getUsername());

        if(updated.getPassword()!=null){
            a.setPassword(
                encoder.encode(updated.getPassword())
            );
        }

        return repo.save(a);
    }

    // DELETE
    public void delete(Long id){
        repo.deleteById(id);
    }
}
