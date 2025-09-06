package com.doc_manager.service;

import com.doc_manager.model.Document;
import com.doc_manager.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final DocumentRepository docRepo;
    private final MinioService minioService;
    private final Tika tika = new Tika();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private final Pattern CMND = Pattern.compile("\\b\\d{9}\\b|\\b\\d{12}\\b");
    private final Pattern TAX = Pattern.compile("\\b\\d{10}\\b");
    private final Pattern BANK = Pattern.compile("(?i)(số tài khoản|stk|account)[:\\s]*([0-9\\- ]{6,30})");

    @Async
    public void scanDocumentAsync(Long docId){
        docRepo.findById(docId).ifPresent(doc -> {
            doc.setStatus("SCANNING");
            docRepo.save(doc);

            try(InputStream is = minioService.getFileStream(doc.getObjectName())){
                String text = tika.parseToString(is);
                List<String> labels = new ArrayList<>();

                Matcher m = EMAIL.matcher(text);
                while(m.find()) labels.add("EMAIL:"+m.group());

                m = CMND.matcher(text);
                while(m.find()) labels.add("CMND:"+m.group());

                m = TAX.matcher(text);
                while(m.find()) labels.add("TAX:"+m.group());

                m = BANK.matcher(text);
                while(m.find()) labels.add("BANK:"+m.group(2));

                if(!labels.isEmpty()){
                    doc.setStatus("FLAGGED");
                    doc.setLabelsJson(objectMapper.writeValueAsString(labels));
                } else {
                    doc.setStatus("SCANNED");
                    doc.setLabelsJson("[]");
                }
            } catch(Exception e){
                doc.setStatus("ERROR");
            } finally {
                docRepo.save(doc);
            }
        });
    }
}