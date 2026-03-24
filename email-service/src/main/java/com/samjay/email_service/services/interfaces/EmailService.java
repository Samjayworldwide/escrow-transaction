package com.samjay.email_service.services.interfaces;

import com.samjay.email_service.dtos.requests.EmailRequestDto;

public interface EmailService {

    void sendEmail(EmailRequestDto emailRequestDto);
}
