package com.example.confdoc;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.confdoc.model.Department;
import com.example.confdoc.model.Role;
import com.example.confdoc.model.User;
import com.example.confdoc.service.KeyStoreService;
import com.example.confdoc.repository.DepartmentRepository;
import com.example.confdoc.repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class ConfidentialDocumentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfidentialDocumentApplication.class, args);
    }

    @Bean
    public CommandLineRunner init(KeyStoreService keyStoreService,
                                  DepartmentRepository deptRepo,
                                  UserRepository userRepo) {
        return args -> {
            // Ensure keystore exists and RSA key pair created
            keyStoreService.ensureKeyStoreAndKey();

            // Create demo data if absent
            if (deptRepo.count() == 0) {
                Department d1 = new Department();
                d1.setName("IT");
                deptRepo.save(d1);

                Department d2 = new Department();
                d2.setName("Finance");
                deptRepo.save(d2);

                if (userRepo.count() == 0) {
                    BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword(enc.encode("adminpass"));
                    admin.setRole(Role.DIRECTOR);
                    admin.setDepartment(d1);
                    userRepo.save(admin);

                    User head = new User();
                    head.setUsername("depthead");
                    head.setPassword(enc.encode("headpass"));
                    head.setRole(Role.DEPT_HEAD);
                    head.setDepartment(d1);
                    userRepo.save(head);

                    User emp = new User();
                    emp.setUsername("employee");
                    emp.setPassword(enc.encode("emppass"));
                    emp.setRole(Role.EMPLOYEE);
                    emp.setDepartment(d1);
                    userRepo.save(emp);

                    User user = new User();
                    user.setUsername("user");
                    user.setPassword(enc.encode("userpass"));
                    user.setRole(Role.USER);
                    user.setDepartment(d2);
                    userRepo.save(user);
                }
            }
        };
    }
}