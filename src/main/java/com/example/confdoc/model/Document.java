package com.example.confdoc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    private String filePath; // chemin du fichier chiffré sur disque

    @Lob
    private byte[] wrappedKey; // clé AES wrapée par la clé publique RSA

    @ManyToOne
    private User owner;

    @ManyToOne
    private Department department;

    private Instant createdAt;

    private double size;
}