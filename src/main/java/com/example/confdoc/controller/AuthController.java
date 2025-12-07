package com.example.confdoc.controller;

import java.util.Map;

import com.example.confdoc.model.User;
import com.example.confdoc.repository.UserRepository;
import com.example.confdoc.security.JwtUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepo, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) throws Exception {
        String username = body.get("username");
        String password = body.get("password");
        var opt = userRepo.findByUsername(username);
        if (opt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error","Identifiants invalides\n"));
        User user = opt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error","Identifiants invalides\n"));
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getDepartment());
        return ResponseEntity.ok(Map.of("token", token));
    }

}