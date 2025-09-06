package com.doc_manager.controller;

import com.doc_manager.model.Document;
import com.doc_manager.entity.User;
import com.doc_manager.request.DocumentRequest;
import com.doc_manager.repository.DocumentRepository;
import com.doc_manager.repository.UserRepository;
import com.doc_manager.service.MinioService;
import com.doc_manager.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final MinioService minioService;
    private final DocumentRepository docRepo;
    private final UserRepository userRepo;
    private final ScanService scanService;

    @Value("${app.upload.max-size-bytes}") private long maxSize;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@Valid @ModelAttribute DocumentRequest request) throws Exception {
        String username = getCurrentUsername();
        if(request.getFile()==null || request.getFile().isEmpty())
            return ResponseEntity.badRequest().body("File trống");
        if(request.getFile().getSize() > maxSize)
            return ResponseEntity.status(413).body("File quá lớn");

        String name = request.getFile().getOriginalFilename();
        if(!name.toLowerCase().endsWith(".pdf") && !name.toLowerCase().endsWith(".docx"))
            return ResponseEntity.badRequest().body("Chỉ PDF hoặc DOCX");

        String objectName = UUID.randomUUID()+"-"+name;
        try(InputStream is = request.getFile().getInputStream()){
            minioService.putFile(objectName,is,request.getFile().getSize(),request.getFile().getContentType());
        }

        Set<User> allowedUsers = new HashSet<>();
        if(request.getAllowedUser()!=null){
            for(String user : request.getAllowedUser()){
                userRepo.findByUsername(user).ifPresent(allowedUsers::add);
            }
        }

        Document doc = Document.builder()
                .objectName(objectName)
                .filename(name)
                .title(request.getTitle())
                .description(request.getDescription())
                .size(request.getFile().getSize())
                .uploadedBy(username)
                .uploadedAt(java.time.Instant.now())
                .status("UPLOADED")
                .allowedUsers(allowedUsers)
                .build();

        docRepo.save(doc);
        scanService.scanDocumentAsync(doc.getId());

        return ResponseEntity.ok(Map.of("message","Uploaded","documentId",doc.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id){
        String username = getCurrentUsername();
        return docRepo.findById(id).map(doc -> {
            if(doc.getUploadedBy().equals(username)
                    || doc.getAllowedUsers().stream().anyMatch(u -> u.getUsername().equals(username))
                    || isAdmin())
                return ResponseEntity.ok(doc);
            else
                return ResponseEntity.status(403).body("Không có quyền");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody DocumentRequest request){
        String username = getCurrentUsername();
        return docRepo.findById(id).map(doc -> {
            if(!doc.getUploadedBy().equals(username) && !isAdmin())
                return ResponseEntity.status(403).body("Không có quyền");
            doc.setTitle(request.getTitle());
            doc.setDescription(request.getDescription());

            Set<User> allowedUsers = new HashSet<>();
            if(request.getAllowedUser()!=null){
                for(String user : request.getAllowedUser()){
                    userRepo.findByUsername(user).ifPresent(allowedUsers::add);
                }
            }
            doc.setAllowedUsers(allowedUsers);

            docRepo.save(doc);
            return ResponseEntity.ok(doc);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){
        String username = getCurrentUsername();
        return docRepo.findById(id).map(doc -> {
            if(!doc.getUploadedBy().equals(username) && !isAdmin())
                return ResponseEntity.status(403).body("Không có quyền");
            try { minioService.removeFile(doc.getObjectName()); }
            catch(Exception e){ return ResponseEntity.status(500).body("Xóa file thất bại"); }
            docRepo.delete(doc);
            return ResponseEntity.ok(Map.of("message","Deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable Long id) {
        String username = getCurrentUsername();

        return docRepo.findById(id).map(doc -> {
            boolean allowed = doc.getUploadedBy().equals(username)
                    || doc.getAllowedUsers().stream().anyMatch(u -> u.getUsername().equals(username))
                    || isAdmin();

            if (!allowed) return ResponseEntity.status(403).body("Không có quyền");

            try (InputStream is = minioService.getFileStream(doc.getObjectName())) {
                byte[] bytes = is.readAllBytes();
                // Lấy content type từ MinIO
                String contentType = minioService.getContentType(doc.getObjectName());
                if (contentType == null) contentType = "application/octet-stream";
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + doc.getFilename() + "\"")
                        .header("Content-Type", contentType)
                        .body(bytes);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Lấy file thất bại");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-documents")
    public ResponseEntity<?> listUserDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        String username = getCurrentUsername();
        Page<Document> docs = docRepo.findByUploadedByOrAllowedUsersUsername(username, username, pageable);
        return ResponseEntity.ok(docs);
    }
}
