package com.hungng3011.vdtecomberefresh.media;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("v1/media")
@Tag(name = "Media", description = "Media management APIs")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @GetMapping
    public String index() {
        return "Media API is running";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "Upload a file to Cloudinary")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resourceType", defaultValue = "image") String resourceType) {
        // Check if file is valid before proceeding
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("File must not be empty");
        }
        
        try {
            Map<String, Object> result = mediaService.upload(file, resourceType);
            if (result == null || result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to upload file");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading file: " + e.getMessage());
        }
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete file", description = "Delete a file from Cloudinary")
    public ResponseEntity<?> delete(
            @PathVariable String publicId,
            @RequestParam(value = "resourceType", defaultValue = "image") String resourceType) {
        Map<String, Object> result = mediaService.delete(publicId, resourceType);
        if (result == null || result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete file");
        }
        return ResponseEntity.ok(result);
    }
}