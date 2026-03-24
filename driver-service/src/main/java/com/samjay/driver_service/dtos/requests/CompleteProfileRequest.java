package com.samjay.driver_service.dtos.requests;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteProfileRequest {

    @NotNull(message = "Profile picture is required")
    private MultipartFile profilePicture;

    @NotBlank(message = "Phone number is required")
    @Length(min = 11, max = 11, message = "Phone number must be exactly 11 digits long")
    @Digits(fraction = 0, integer = 11, message = "Your Phone number is incorrect!")
    private String phoneNumber;

    @NotBlank(message = "License plate number is required")
    private String licensePlateNumber;

    @NotBlank(message = "Identification number is required")
    private String identifiationNumber;
}
