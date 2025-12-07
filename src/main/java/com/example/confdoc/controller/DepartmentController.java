package com.example.confdoc.controller;

import java.util.List;
import java.util.Map;

import com.example.confdoc.model.Department;
import com.example.confdoc.model.Document;
import com.example.confdoc.service.DepartmentService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<Object> createDepartment(@RequestBody Department dept, Authentication auth) {
        Department created = departmentService.createDepartment(dept, auth);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> renameDepartment(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        String newName = body.get("name");
        Department updated = departmentService.renameDepartment(id, newName, auth);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteDepartment(@PathVariable Long id, Authentication auth) {
        departmentService.deleteDepartment(id, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<Department>> listDepartments(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(departmentService.listDepartments(pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Department>> allDepartments() {
        return ResponseEntity.ok(departmentService.listDepartments());
    }
}