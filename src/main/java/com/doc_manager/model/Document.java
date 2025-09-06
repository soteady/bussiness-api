package com.doc_manager.model;

import com.doc_manager.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Builder
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String objectName;
    private String filename;
    private String title;
    private String description;
    private Long size;
    private String uploadedBy; // email
    private Instant uploadedAt;
    private String status; // UPLOADED, SCANNED, FLAGGED, etc.

    @Column(columnDefinition = "text")
    private String labelsJson;

    // Danh sách user được phép xem
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "document_user",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> allowedUsers;
}