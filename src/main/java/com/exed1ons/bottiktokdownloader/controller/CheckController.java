package com.exed1ons.bottiktokdownloader.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckController {

    @GetMapping("/")
    public ResponseEntity<String> checkForUpdates() {
        return ResponseEntity.ok("Server is running!");
    }
}
