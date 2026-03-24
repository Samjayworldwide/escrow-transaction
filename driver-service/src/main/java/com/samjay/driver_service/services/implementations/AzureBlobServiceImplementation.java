package com.samjay.driver_service.services.implementations;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.samjay.driver_service.services.interfaces.AzureBlobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AzureBlobServiceImplementation implements AzureBlobService {

    private final BlobContainerClient containerClient;

    @Value("${azure.storage.sas-expiry-minutes:10}")
    private int sasExpiryMinutes;

    @Override
    public String generateSasUrl(String blobName) {

        BlobClient blobClient = containerClient.getBlobClient(blobName);

        BlobSasPermission permission = new BlobSasPermission()
                .setWritePermission(true)
                .setCreatePermission(true);

        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(sasExpiryMinutes),
                permission
        );

        String sasToken = blobClient.generateSas(sasValues);

        log.info("Generated SAS Token: {}", sasToken);

        String sasUrl = blobClient.getBlobUrl() + "?" + sasToken;

        log.info("Generated SAS URL: {}", sasUrl);

        return sasUrl;
    }

    @Override
    public String buildBlobName(String prefix) {

        return prefix + "/" + UUID.randomUUID();

    }

    @Override
    public void deleteBlob(String blobName) {

        containerClient.getBlobClient(blobName).deleteIfExists();

    }

    @Override
    public boolean blobExists(String blobName) {

        return containerClient.getBlobClient(blobName).exists();

    }
}
