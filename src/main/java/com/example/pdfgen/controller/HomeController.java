package com.example.pdfgen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles top-level page routes.
 *
 * - "/" → static/index.html (phone login page)
 * - "/app/index.html" is served directly by Spring's static resource handler
 */
@Controller
public class HomeController {

    /** Login page served at the root. */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
