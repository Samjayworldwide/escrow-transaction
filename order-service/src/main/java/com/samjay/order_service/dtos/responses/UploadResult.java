package com.samjay.order_service.dtos.responses;

import com.samjay.order_service.enumerations.MediaType;

public record UploadResult(String url, MediaType mediaType) {
}
