package com.samjay.order_service.services.interfaces;

import com.samjay.order_service.dtos.responses.UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface MediaUploadService {

    UploadResult upload(MultipartFile file, String orderReferenceNumber);

    void delete(String blobUrl);
}
