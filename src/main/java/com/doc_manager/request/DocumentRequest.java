package com.doc_manager.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class DocumentRequest {
    private MultipartFile file;

    @NotBlank
    private String title;

    private String description;

    private Set<String> allowedUser;
}