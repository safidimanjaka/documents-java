package com.example.confdoc.controller;

import java.util.List;
import java.util.Map;

import com.example.confdoc.model.Document;
import com.example.confdoc.service.DocumentService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService docService;

    public DocumentController(DocumentService docService) {
        this.docService = docService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Object> upload(@RequestParam("file") MultipartFile file, Authentication auth) throws Exception {
        String username = (String) auth.getPrincipal();
        Document d = docService.upload(file.getBytes(), file.getOriginalFilename(), username);
        return ResponseEntity.ok(d);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Object> download(@PathVariable Long id, Authentication auth) throws Exception {
        String username = (String) auth.getPrincipal();
        byte[] data = docService.download(id, username);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"doc_" + id + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping
    public ResponseEntity<Page<Document>> listDocuments(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Long ownerId,
                                                        @RequestParam(required = false) Long departmentId) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(docService.listDocuments(ownerId, departmentId, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Document>> allDocuments() {
        return ResponseEntity.ok(docService.listDocuments());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        String username = (String) auth.getPrincipal();
        Document updated = docService.updateMetadata(id, body.get("filename"), username);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id, Authentication auth) throws Exception {
        String username = (String) auth.getPrincipal();
        docService.delete(id, username);
        return ResponseEntity.noContent().build();
    }
}