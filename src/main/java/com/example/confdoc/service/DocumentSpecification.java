// src/main/java/com/example/demo/service/ProduitSpecification.java
package com.example.confdoc.service;

import com.example.confdoc.model.Document;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DocumentSpecification {

    public static Specification<Document> rechercheParCriteres(
            Long ownerId,
            Long departmentId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Condition sur le ownerId (si ownerId non null)
            if (ownerId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("owner").get("id"), ownerId
                ));
            }
            // 1. Condition sur le departmentId (si departmentId non null)
            if (departmentId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("department").get("id"), departmentId
                ));
            }

            // Combinez toutes les conditions avec un AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}