package com.samjay.order_service.services.implementations;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.samjay.order_service.dtos.responses.UploadResult;
import com.samjay.order_service.enumerations.MediaType;
import com.samjay.order_service.exceptions.ApplicationException;
import com.samjay.order_service.services.interfaces.MediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaUploadServiceImplementation implements MediaUploadService {

    private final BlobContainerClient blobContainerClient;

    private static final Map<String, MediaType> ALLOWED_MEDIA_TYPES = Map.of(
            "image/jpeg", MediaType.IMAGE,
            "image/png", MediaType.IMAGE,
            "image/webp", MediaType.IMAGE,
            "video/mp4", MediaType.VIDEO,
            "video/quicktime", MediaType.VIDEO
    );

    @Override
    public UploadResult upload(MultipartFile file, String orderReferenceNumber) {

        String originalFilename = file.getOriginalFilename();

        String extension = getExtension(originalFilename);

        String blobName = String.format("orders/%s/%s.%s",
                orderReferenceNumber,
                UUID.randomUUID(),
                extension);

        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        try {

            blobClient.upload(file.getInputStream(), file.getSize(), true);

        } catch (IOException e) {

            log.error("Failed to upload media for order {}: {}", orderReferenceNumber, e.getMessage());

            throw new ApplicationException("Failed to upload item media. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        MediaType mediaType = ALLOWED_MEDIA_TYPES.get(file.getContentType());

        String blobUrl = blobClient.getBlobUrl();

        log.info("Uploaded media for order {}: {}", orderReferenceNumber, blobUrl);

        return new UploadResult(blobUrl, mediaType);
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

        String decoded = java.net.URLDecoder.decode(blobUrl, java.nio.charset.StandardCharsets.UTF_8);

        int index = decoded.indexOf("orders/");

        if (index == -1) {

            log.error("Could not extract blob name from URL: {}", blobUrl);

            throw new IllegalArgumentException("Invalid blob URL format: " + blobUrl);
        }

        return decoded.substring(index);
    }
}
