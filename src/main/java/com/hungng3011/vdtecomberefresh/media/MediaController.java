package com.hungng3011.vdtecomberefresh.media;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("v1/media")
@Tag(name = "Media", description = "Media management APIs")
@RequiredArgsConstructor
@Slf4j
public class MediaController {
    private final MediaService mediaService;

    @GetMapping
    public String index() {
        log.info("Media API health check requested");
        return "Media API is running";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "Upload a file to Cloudinary")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resourceType", defaultValue = "image") String resourceType) {
        log.info("File upload requested - filename: {}, size: {} bytes, resourceType: {}", 
                file != null ? file.getOriginalFilename() : "null", 
                file != null ? file.getSize() : 0, 
                resourceType);
        
        // Check if file is valid before proceeding
        if (file == null || file.isEmpty()) {
            log.warn("Upload failed - file is null or empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("File must not be empty");
        }
        
        try {
            Map<String, Object> result = mediaService.upload(file, resourceType);
            if (result == null || result.isEmpty()) {
                log.error("Upload failed - service returned null or empty result for file: {}", file.getOriginalFilename());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to upload file");
            }
            log.info("Successfully uploaded file: {} with public_id: {}", 
                    file.getOriginalFilename(), 
                    result.get("public_id"));
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Error uploading file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading file: " + e.getMessage());
        }
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete file", description = "Delete a file from Cloudinary")
    public ResponseEntity<?> delete(
            @PathVariable String publicId,
            @RequestParam(value = "resourceType", defaultValue = "image") String resourceType) {
        log.info("File deletion requested - publicId: {}, resourceType: {}", publicId, resourceType);
        try {
            Map<String, Object> result = mediaService.delete(publicId, resourceType);
            if (result == null || result.isEmpty()) {
                log.error("Delete failed - service returned null or empty result for publicId: {}", publicId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to delete file");
            }
            log.info("Successfully deleted file with publicId: {}", publicId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error deleting file with publicId: {}", publicId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting file: " + e.getMessage());
        }
    }
}