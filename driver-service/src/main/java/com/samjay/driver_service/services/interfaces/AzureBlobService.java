package com.samjay.driver_service.services.interfaces;

public interface AzureBlobService {

    String generateSasUrl(String blobName);

    String buildBlobName(String prefix);

    void deleteBlob(String blobName);

    boolean blobExists(String blobName);
}
