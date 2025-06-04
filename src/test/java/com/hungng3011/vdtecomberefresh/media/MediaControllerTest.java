package com.hungng3011.vdtecomberefresh.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.hungng3011.vdtecomberefresh.config.SecurityConfig;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for testing
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class MediaControllerTest {

    @BeforeEach
    void initMocks() {
        org.mockito.MockitoAnnotations.openMocks(this);
    }

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    @Autowired
    private MockMvc mockMvc;

    private Map<String, Object> uploadResult;
    private Map<String, Object> deleteResult;

    @BeforeEach
    void setUp() {
        uploadResult = new HashMap<>();
        uploadResult.put("public_id", "sample_id");
        uploadResult.put("url", "http://example.com/image.jpg");

        deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");
    }

    @Test
    void index_ShouldReturnWelcomeMessage() throws Exception {
        mockMvc.perform(get("/v1/media"))
                .andExpect(status().isOk())
                .andExpect(content().string("Media API is running"));
    }

    @Test
    void upload_WithValidFile_ShouldReturnCreatedStatus() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(mediaService.upload(any(), eq("image"))).thenReturn(uploadResult);

        // Act & Assert
        mockMvc.perform(multipart("/v1/media")
                        .file(file)
                        .param("resourceType", "image"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.public_id").value("sample_id"))
                .andExpect(jsonPath("$.url").value("http://example.com/image.jpg"));

        verify(mediaService).upload(any(), eq("image"));
    }

    @Test
    void upload_WhenServiceReturnsNull_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(mediaService.upload(any(), eq("image"))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(multipart("/v1/media")
                        .file(file)
                        .param("resourceType", "image"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to upload file"));
    }

    @Test
    void delete_WithValidPublicId_ShouldReturnOkStatus() throws Exception {
        // Arrange
        when(mediaService.delete(eq("sample_id"), eq("image"))).thenReturn(deleteResult);

        // Act & Assert
        mockMvc.perform(delete("/v1/media/sample_id")
                        .param("resourceType", "image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ok"));

        verify(mediaService).delete("sample_id", "image");
    }

    @Test
    void delete_WhenServiceReturnsNull_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(mediaService.delete(eq("sample_id"), eq("image"))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(delete("/v1/media/sample_id")
                        .param("resourceType", "image"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to delete file"));
    }
}