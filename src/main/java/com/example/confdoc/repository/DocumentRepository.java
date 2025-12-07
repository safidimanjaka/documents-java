package com.example.confdoc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.confdoc.model.Document;
import com.example.confdoc.model.Department;
import com.example.confdoc.model.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    List<Document> findByOwner(User owner);
    List<Document> findByDepartment(Department department);

    @Transactional
    @Modifying
    @Query("UPDATE Document d SET d.department = :dept WHERE d.owner.id = :ownerId")
    int updateDepartmentByOwnerId(@Param("ownerId") Long ownerId, @Param("dept") Department dept);

}