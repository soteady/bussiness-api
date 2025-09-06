package com.doc_manager.repository;

import com.doc_manager.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUploadedByOrAllowedUsersUsername(String uploadedBy, String username);
}
