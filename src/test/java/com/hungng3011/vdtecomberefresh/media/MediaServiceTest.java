package com.hungng3011.vdtecomberefresh.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MediaServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private MediaService mediaService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;

    private MultipartFile validFile;
    private MultipartFile emptyFile;
    private Map<String, Object> uploadResult;
    private Map<String, Object> deleteResult;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        uploadResult = new HashMap<>();
        uploadResult.put("public_id", "sample_id");
        uploadResult.put("url", "http://example.com/image.jpg");
        uploadResult.put("secure_url", "https://example.com/image.jpg");

        deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");

        // Fix the unnecessary stubbing issue by using lenient
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void upload_WithValidFile_ShouldReturnUploadResult() throws IOException {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        // Act
        Map<String, Object> result = mediaService.upload(validFile, "image");

        // Assert
        assertNotNull(result);
        assertEquals(uploadResult, result);
        verify(uploader).upload(any(byte[].class), paramsCaptor.capture());

        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("image", params.get("resource_type"));
        assertEquals("vdtecomberefresh/media", params.get("folder"));
        assertEquals(true, params.get("use_filename"));
    }

    @Test
    void upload_WithEmptyFile_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            mediaService.upload(emptyFile, "image");
        });

        assertEquals("File must not be empty", exception.getMessage());
        verifyNoInteractions(uploader);
    }

    @Test
    void upload_WithVideoResourceType_ShouldPassCorrectParameters() throws IOException {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        // Act
        mediaService.upload(validFile, "video");

        // Assert
        verify(uploader).upload(any(byte[].class), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("video", params.get("resource_type"));
    }

    @Test
    void upload_WithUploadError_ShouldThrowRuntimeException() throws IOException {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Upload failed"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            mediaService.upload(validFile, "image");
        });

        assertEquals("Failed to upload file", exception.getMessage());
    }

    @Test
    void delete_WithValidPublicId_ShouldReturnDeleteResult() throws IOException {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(deleteResult);

        // Act
        Map<String, Object> result = mediaService.delete("sample_id", "image");

        // Assert
        assertNotNull(result);
        assertEquals(deleteResult, result);
        verify(uploader).destroy(eq("sample_id"), paramsCaptor.capture());

        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("image", params.get("resource_type"));
        assertEquals(true, params.get("invalidate"));
    }

    @Test
    void delete_WithEmptyPublicId_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            mediaService.delete("", "image");
        });

        assertEquals("Public ID must not be empty", exception.getMessage());
        verifyNoInteractions(uploader);
    }

    @Test
    void delete_WithNullPublicId_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            mediaService.delete(null, "image");
        });

        assertEquals("Public ID must not be empty", exception.getMessage());
        verifyNoInteractions(uploader);
    }

    @Test
    void delete_WithDeleteError_ShouldThrowRuntimeException() throws IOException {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("Delete failed"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            mediaService.delete("sample_id", "image");
        });

        assertEquals("Failed to delete file", exception.getMessage());
    }
}