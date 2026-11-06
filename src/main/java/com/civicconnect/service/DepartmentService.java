package com.civicconnect.service;

import com.civicconnect.model.Department;
import com.civicconnect.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public List<Department> findActive() {
        return departmentRepository.findByActiveTrue();
    }

    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }
}
