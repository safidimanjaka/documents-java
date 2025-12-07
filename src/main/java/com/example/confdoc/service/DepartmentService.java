package com.example.confdoc.service;

import java.util.List;
import java.util.Optional;

import com.example.confdoc.model.Department;
import com.example.confdoc.model.Role;
import com.example.confdoc.model.User;
import com.example.confdoc.repository.DepartmentRepository;
import com.example.confdoc.repository.DocumentRepository;
import com.example.confdoc.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             UserRepository userRepository,
                             DocumentRepository documentRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public Department createDepartment(Department dto, Authentication auth) {
        requireDirector(auth);
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de département obligatoire");
        }
        Department dept = new Department();
        dept.setName(dto.getName().trim());
        return departmentRepository.save(dept);
    }

    @Transactional
    public Department renameDepartment(Long id, String newName, Authentication auth) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Département introuvable"));

        if (newName == null || newName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de département obligatoire");
        }

        if (!isDirector(auth) && !isDeptHeadOf(auth, dept)) {
            throw new AccessDeniedException("Accès refusé");
        }

        dept.setName(newName.trim());
        return departmentRepository.save(dept);
    }

    @Transactional
    public void deleteDepartment(Long id, Authentication auth) {
        requireDirector(auth);

        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Département introuvable"));

        List<User> users = userRepository.findByDepartment(dept);
        if (users != null && !users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le département a des utilisateurs");
        }

        var docs = documentRepository.findByDepartment(dept);
        if (docs != null && !docs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le département a des documents");
        }

        departmentRepository.delete(dept);
    }

    public List<Department> listDepartments() {
        return departmentRepository.findAll();
    }

    public Page<Department> listDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable);
    }
    
    private void requireDirector(Authentication auth) {
        if (!isDirector(auth)) {
            throw new AccessDeniedException("Accès refusé");
        }
    }

    private boolean isDirector(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.DIRECTOR.name()));
    }

    private boolean isDeptHeadOf(Authentication auth, Department dept) {
        if (auth == null || dept == null) return false;
        String username = (String) auth.getPrincipal();
        Optional<User> me = userRepository.findByUsername(username);
        return me.isPresent()
                && me.get().getRole() == Role.DEPT_HEAD
                && me.get().getDepartment() != null
                && me.get().getDepartment().getId().equals(dept.getId());
    }
}