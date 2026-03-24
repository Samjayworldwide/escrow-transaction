package com.samjay.driver_service.services.implementations;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.samjay.driver_service.exceptions.ApplicationException;
import com.samjay.driver_service.services.interfaces.MediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaUploadServiceImplementation implements MediaUploadService {

    private final BlobContainerClient blobContainerClient;

    @Override
    public String upload(MultipartFile file) {

        try {

            String originalFilename = file.getOriginalFilename();

            String extension = getExtension(originalFilename);

            String blobName = String.format("driver/%s.%s", UUID.randomUUID(), extension);

            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

            blobClient.upload(file.getInputStream(), file.getSize(), true);

            String blobUrl = blobClient.getBlobUrl();

            log.info("Uploaded media url {}",  blobUrl);

            return blobUrl;

        } catch (IOException e) {

            log.error("Failed to upload media {}: ", e.getMessage(), e);

            throw new ApplicationException("Failed to upload item media. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(String blobUrl) {

        String blobName = extractBlobName(blobUrl);

        log.info("Attempting to delete blob with name: {}", blobName);

        blobContainerClient.getBlobClient(blobName).deleteIfExists();
    }

    private String getExtension(String filename) {

        if (filename == null || !filename.contains(".")) return "bin";

        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String extractBlobName(String blobUrl) {

        String decoded = URLDecoder.decode(blobUrl, StandardCharsets.UTF_8);

        int index = decoded.indexOf("orders/");

        if (index == -1) {

            log.error("Could not extract blob name from URL: {}", blobUrl);

            throw new IllegalArgumentException("Invalid blob URL format: " + blobUrl);
        }

        return decoded.substring(index);
    }
}
