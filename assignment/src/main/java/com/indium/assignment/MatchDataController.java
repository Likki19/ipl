package com.indium.assignment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/matches")
public class MatchDataController {

    @Autowired
    private MatchDataService matchDataService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMatchData(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }

        try {
            // Pass MultipartFile directly to the service for handling
            matchDataService.uploadJsonFile(file);
            return ResponseEntity.status(HttpStatus.OK).body("File uploaded and data saved successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the file: " + e.getMessage());
        } catch (Exception e) {
            // Catching more general exceptions to debug issues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error: " + e.getMessage());
        }
    }
}
