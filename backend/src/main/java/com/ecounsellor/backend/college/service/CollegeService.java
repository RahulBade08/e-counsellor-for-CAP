package com.ecounsellor.backend.college.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ecounsellor.backend.college.dto.CollegeDTO;
import com.ecounsellor.backend.core.entity.College;
import com.ecounsellor.backend.core.repository.CollegeRepository;

@Service
public class CollegeService {

    private final CollegeRepository repo;

    public CollegeService(CollegeRepository repo) {
        this.repo = repo;
    }

    // ✅ CREATE (ADMIN ONLY)
    public CollegeDTO create(CollegeDTO dto) {
        // Check if college code already exists
        if (repo.findByCollegeCode(dto.getCollegeCode()).isPresent()) {
            throw new RuntimeException("College with code " + dto.getCollegeCode() + " already exists");
        }

        College college = new College();
        college.setCollegeCode(dto.getCollegeCode());
        college.setCollegeName(dto.getCollegeName());
        college.setCourseUniversity(dto.getCourseUniversity());

        College saved = repo.save(college);
        return toDTO(saved);
    }

    // ✅ READ ALL
    public List<CollegeDTO> getAll() {
        return repo.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ READ ONE BY ID
    public CollegeDTO getById(Long id) {
        College college = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("College not found with id: " + id));
        return toDTO(college);
    }

    // ✅ READ ONE BY COLLEGE CODE
    public CollegeDTO getByCode(String collegeCode) {
        College college = repo.findByCollegeCode(collegeCode)
                .orElseThrow(() -> new RuntimeException("College not found with code: " + collegeCode));
        return toDTO(college);
    }

    // ✅ SEARCH BY NAME
    public List<CollegeDTO> searchByName(String name) {
        return repo.findAll()
                .stream()
                .filter(c -> c.getCollegeName().toLowerCase().contains(name.toLowerCase()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ UPDATE (ADMIN ONLY)
    public CollegeDTO update(Long id, CollegeDTO dto) {
        College college = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("College not found with id: " + id));

        // Check if trying to update to an existing college code
        if (dto.getCollegeCode() != null && !dto.getCollegeCode().equals(college.getCollegeCode())) {
            if (repo.findByCollegeCode(dto.getCollegeCode()).isPresent()) {
                throw new RuntimeException("College with code " + dto.getCollegeCode() + " already exists");
            }
            college.setCollegeCode(dto.getCollegeCode());
        }

        if (dto.getCollegeName() != null) {
            college.setCollegeName(dto.getCollegeName());
        }

        if (dto.getCourseUniversity() != null) {
            college.setCourseUniversity(dto.getCourseUniversity());
        }

        College updated = repo.save(college);
        return toDTO(updated);
    }

    // ✅ DELETE (ADMIN ONLY)
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("College not found with id: " + id);
        }
        repo.deleteById(id);
    }

    // ✅ COUNT
    public long count() {
        return repo.count();
    }

    // 🔄 Helper method to convert Entity to DTO
    private CollegeDTO toDTO(College college) {
        return new CollegeDTO(
            college.getCollegeId(),
            college.getCollegeCode(),
            college.getCollegeName(),
            college.getCourseUniversity()
        );
    }
}