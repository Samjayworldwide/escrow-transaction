package com.samjay.driver_service.services.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface MediaUploadService {

    String upload(MultipartFile file);

    void delete(String blobUrl);
}
