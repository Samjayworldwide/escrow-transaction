package com.samjay.order_service.dtos.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.samjay.order_service.validations.ValidMedia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDetailsRequest {

    @NotBlank(message = "Item name is required")
    private String name;

    @NotBlank(message = "Item description cannot be blank")
    private String description;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @NotNull(message = "Enter a valid price for this item")
    private BigDecimal price;

    @NotNull(message = "Picture or video of item is required")
    @ValidMedia(maxSizeInMB = 10, allowedContentTypes = {"image/jpeg", "image/png"}, message = "Only JPEG/PNG/MP4 file under 10MB are allowed.")
    @JsonIgnore
    private MultipartFile itemMedia;
}
