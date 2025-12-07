package com.example.confdoc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.confdoc.model.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

}