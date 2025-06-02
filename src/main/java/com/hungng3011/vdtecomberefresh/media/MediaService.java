package com.hungng3011.vdtecomberefresh.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class MediaService {
    private final Cloudinary cloudinary;
//    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Map<String, Object> upload(MultipartFile file, String resourceType) {
        if (file.isEmpty()) {
            log.warn("Attempted to upload an empty file");
            throw new IllegalArgumentException("File must not be empty");
        }

        Map params = ObjectUtils.asMap(
                "resource_type", resourceType,
                "folder", "vdtecomberefresh/media",
                "use_filename", true,
                "unique_filename", true
        );

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            log.info("File uploaded successfully: {}", uploadResult);
            return uploadResult;
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public Map<String, Object> delete(String publicId, String resourceType) {
        if (publicId == null || publicId.isEmpty()) {
            log.warn("Attempted to delete a media with an empty public ID");
            throw new IllegalArgumentException("Public ID must not be empty");
        }

        Map params = ObjectUtils.asMap(
                "resource_type", resourceType,
                "invalidate", true
        );

        try {
            Map<String, Object> deleteResult = cloudinary.uploader().destroy(publicId, params);
            log.info("File deleted successfully: {}", deleteResult);
            return deleteResult;
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}
