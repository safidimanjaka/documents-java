package com.example.confdoc.service;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.confdoc.model.Document;
import com.example.confdoc.model.Role;
import com.example.confdoc.model.User;
import com.example.confdoc.repository.DocumentRepository;
import com.example.confdoc.repository.UserRepository;

import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    @Value("${app.supabase.bucket}")
    private String bucket;

    @Value("${app.supabase.secure.path}")
    private String securePath;

    private final DocumentRepository documentRepository;
    private final UserRepository userRepo;
    private final CryptoService cryptoService;
    private final SupabaseClientService supabaseClientService;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepo, CryptoService cryptoService, SupabaseClientService supabaseClientService) {
        this.documentRepository = documentRepository;
        this.userRepo = userRepo;
        this.cryptoService = cryptoService;
        this.supabaseClientService = supabaseClientService;
    }

    @Transactional
    public Document upload(byte[] data, String filename, String username) throws Exception {
        User owner = userRepo.findByUsername(username).orElseThrow();
        Document d = new Document();
        d.setFilename(filename);
        d.setOwner(owner);
        d.setDepartment(owner.getDepartment());
        d.setCreatedAt(Instant.now());
        d.setSize(Math.round(data.length / 1024.0 * 100.0) / 100.0);

        cryptoService.encryptAndStoreWithWrappedKey(data, filename, (filePath, wrapped) -> {
            d.setFilePath(filePath);
            d.setWrappedKey(wrapped);
            documentRepository.save(d);
        });

        return d;
    }

    public byte[] download(Long docId, String username) throws Exception {
        Optional<Document> opt = documentRepository.findById(docId);
        if (opt.isEmpty()) throw new IllegalArgumentException("Document introuvable");
        Document d = opt.get();
        User requester = userRepo.findByUsername(username).orElseThrow();
        if (!canRead(d, requester)) throw new SecurityException("Accès refusé");
        return cryptoService.decryptFromFile(d.getFilePath(), d.getWrappedKey());
    }

    public List<Document> listDocuments(String username) {
        User requester = userRepo.findByUsername(username).orElseThrow();
        Role role = requester.getRole();
        if (role == Role.DIRECTOR) {
            return documentRepository.findAll();
        } else if (role == Role.DEPT_HEAD) {
            if (requester.getDepartment() == null) return new ArrayList<>();
            return documentRepository.findByDepartment(requester.getDepartment());
        } else if (role == Role.EMPLOYEE) {
            return documentRepository.findByOwner(requester);
        } else { // USER
            return documentRepository.findByOwner(requester);
        }
    }

    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }

    public Page<Document> listDocuments(Long ownerId, Long departmentId, Pageable pageable) {
        Specification<Document> documentSpecification = DocumentSpecification.rechercheParCriteres(ownerId, departmentId);
        return documentRepository.findAll(documentSpecification, pageable);
    }

    @Transactional
    public Document updateMetadata(Long id, String newFilename, String username) {
        Document d = documentRepository.findById(id).orElseThrow();
        User requester = userRepo.findByUsername(username).orElseThrow();
        if (!canModify(d, requester)) throw new SecurityException("Accès refusé");
        if (newFilename != null) d.setFilename(newFilename);
        return documentRepository.save(d);
    }

    @Transactional
    public void delete(Long id, String username) throws Exception {
        Document d = documentRepository.findById(id).orElseThrow();
        User requester = userRepo.findByUsername(username).orElseThrow();
        if (!canModify(d, requester)) throw new SecurityException("Accès refusé");
        if (StringUtils.isNotBlank(d.getFilePath())) {
            try {
               supabaseClientService.delete(bucket, securePath + "/" + d.getFilePath());
            } catch (Exception e) {
                throw new Exception("Erreur lors de la suppréssion", e);
            }
        }
        documentRepository.delete(d);
    }

    private boolean canRead(Document d, User u) {
        Role r = u.getRole();
        if (r == Role.DIRECTOR) return true;
        if (r == Role.DEPT_HEAD) {
            return u.getDepartment() != null && d.getDepartment() != null && u.getDepartment().getId().equals(d.getDepartment().getId());
        }
        return d.getOwner() != null && d.getOwner().getId().equals(u.getId());
    }

    private boolean canModify(Document d, User u) {
        Role r = u.getRole();
        if (r == Role.DIRECTOR) return true;
        if (r == Role.DEPT_HEAD) {
            return u.getDepartment() != null && d.getDepartment() != null && u.getDepartment().getId().equals(d.getDepartment().getId());
        }
        if (r == Role.EMPLOYEE) {
            return d.getOwner() != null && d.getOwner().getId().equals(u.getId());
        }
        return false;
    }
}