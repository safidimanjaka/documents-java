package com.example.confdoc.repository;

import java.util.List;
import java.util.Optional;

import com.example.confdoc.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.confdoc.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByDepartment(Department department);
}