package com.doc_manager.repository;

import com.doc_manager.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByUploadedByOrAllowedUsersUsername(String uploadedBy, String username, Pageable pageable);

}
