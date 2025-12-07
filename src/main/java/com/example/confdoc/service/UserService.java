package com.example.confdoc.service;

import java.util.Objects;
import java.util.Optional;

import com.example.confdoc.model.Department;
import com.example.confdoc.model.Role;
import com.example.confdoc.model.User;
import com.example.confdoc.repository.DepartmentRepository;
import com.example.confdoc.repository.DocumentRepository;
import com.example.confdoc.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DocumentRepository documentRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       DocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.documentRepository = documentRepository;
    }

    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    @Transactional
    public User createUser(User dto, Authentication auth) {
        requireDirector(auth);

        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom d'utilisateur obligatoire");
        }

        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom d'utilisateur existe déjà");
        }

        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe obligatoire");
        }

        User u = new User();
        u.setUsername(dto.getUsername().trim());
        u.setPassword(passwordEncoder.encode(dto.getPassword()));
        u.setRole(dto.getRole() != null ? dto.getRole() : Role.USER);

        if (dto.getDepartment() != null && dto.getDepartment().getId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartment().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Département introuvable"));
            u.setDepartment(dept);
        }

        return userRepository.save(u);
    }

    @Transactional
    public User updateUser(Long id, User dto, Authentication auth) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        boolean isDirector = hasRole(auth, Role.DIRECTOR);
        boolean isDeptHead = hasRole(auth, Role.DEPT_HEAD) && authIsDeptHeadOf(auth, existing.getDepartment());
        String currentUsername = getAuthUsername(auth);
        boolean isSelf = currentUsername != null && currentUsername.equals(existing.getUsername());

        if (!(isDirector || isDeptHead || isSelf)) {
            throw new AccessDeniedException("Accès refusé");
        }

        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && !dto.getUsername().equals(existing.getUsername())) {
            if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom d'utilisateur existe déjà");
            }
            existing.setUsername(dto.getUsername().trim());
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getRole() != null && dto.getRole() != existing.getRole()) {
            if (!isDirector) {
                throw new AccessDeniedException("Accès refusé");
            }
            existing.setRole(dto.getRole());
        }

        Department newDepartment = dto.getDepartment();
        if (!Objects.equals(newDepartment, existing.getDepartment())) {
            if (!isDirector && !isDeptHead) {
                throw new AccessDeniedException("Accès refusé");
            }
            if (newDepartment != null && newDepartment.getId() != null) {
                Department dept = departmentRepository.findById(dto.getDepartment().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Département introuvable"));
                existing.setDepartment(dept);
            } else {
                existing.setDepartment(null);
            }
        }
        User user = userRepository.save(existing);

        if (!Objects.equals(newDepartment, existing.getDepartment())) {
            documentRepository.updateDepartmentByOwnerId(existing.getId(), newDepartment);
        }
        return user;
    }

    @Transactional
    public void deleteUser(Long id, Authentication auth) {
        requireDirector(auth);
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        userRepository.delete(u);
    }

    private void requireDirector(Authentication auth) {
        if (!hasRole(auth, Role.DIRECTOR)) {
            throw new AccessDeniedException("Accès refusé");
        }
    }

    private boolean hasRole(Authentication auth, Role role) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
    }

    private String getAuthUsername(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof String) return (String) principal;
        try {
            var ud = (org.springframework.security.core.userdetails.User) principal;
            return ud.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean authIsDeptHeadOf(Authentication auth, Department dept) {
        if (auth == null || dept == null) return false;
        String username = getAuthUsername(auth);
        if (username == null) return false;
        Optional<User> me = userRepository.findByUsername(username);
        return me.isPresent()
                && me.get().getRole() == Role.DEPT_HEAD
                && me.get().getDepartment() != null
                && me.get().getDepartment().getId().equals(dept.getId());
    }
}