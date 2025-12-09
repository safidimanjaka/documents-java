package com.example.confdoc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Mappe la racine ("/") directement au nom du fichier statique.
     * Spring Boot trouvera index.html dans src/main/resources/static/
     * car nous retournons le nom exact du fichier sans l'extension.
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}