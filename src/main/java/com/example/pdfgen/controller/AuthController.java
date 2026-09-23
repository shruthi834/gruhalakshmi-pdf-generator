package com.example.pdfgen.controller;

import com.example.pdfgen.service.AuthService;
import com.example.pdfgen.service.AuthService.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for phone-number-based authentication.
 *
 * <pre>
 * POST /api/auth/login   { "phone": "9876543210" }
 *   → 200 { "token": "uuid..." }
 *   → 404 { "message": "..." }  (phone not registered)
 *   → 400 { "message": "..." }  (bad input)
 * </pre>
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String phone = body == null ? null : body.get("phone");

        if (phone == null || phone.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Phone number is required."));
        }

        // Sanitise: keep only digits, must be exactly 10
        phone = phone.replaceAll("\\D", "");
        if (phone.length() != 10) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Please provide a valid 10-digit phone number."));
        }

        try {
            String token = authService.login(phone);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (UserNotFoundException ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", ex.getMessage()));
        }
    }
}
