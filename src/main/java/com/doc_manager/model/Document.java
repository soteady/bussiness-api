package com.doc_manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String objectName;
    private String filename;
    private Long size;
    private String contentType;
    private String uploadedBy;
    private Instant uploadedAt;
    private String status;
    @Column(columnDefinition="text")
    private String labelsJson;
}
