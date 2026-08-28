package com.example.pdfgen.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*")
public class PdfController {

    @GetMapping("/template-defaults")
    public ResponseEntity<Map<String, String>> getTemplateDefaults() {
        return ResponseEntity.ok(Map.of(
                "date", "2023-07-21",
                "displayDate", "21-07-2023",
                "rationCardNumber", "240100159730",
                "name", "ಶಿವಮ್ಮ"
        ));
    }
}
