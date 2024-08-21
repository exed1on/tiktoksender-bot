package com.exed1ons.bottiktokdownloader.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckController {

    @GetMapping("/")
    public ResponseEntity<Void> checkForUpdates() {
        return ResponseEntity.ok().build();
    }
}
