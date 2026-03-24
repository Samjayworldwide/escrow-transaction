package com.samjay.email_service.services.implementations;

import com.samjay.email_service.dtos.requests.EmailRequestDto;
import com.samjay.email_service.services.interfaces.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImplementation implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${email.username}")
    private String emailSender;

    @Override
    public void sendEmail(EmailRequestDto emailRequestDto) {

        try {

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

            mimeMessageHelper.setFrom(emailSender);

            mimeMessageHelper.setTo(emailRequestDto.getRecipient());

            mimeMessageHelper.setText(emailRequestDto.getMessageBody(), true);

            mimeMessageHelper.setSubject(emailRequestDto.getSubject());

            javaMailSender.send(mimeMessage);

        } catch (Exception ex) {

            log.error("Error sending email to: {} with exception {}", emailRequestDto.getRecipient(), ex.getMessage(), ex);

            throw new RuntimeException("Failed to send email to: " + emailRequestDto.getRecipient(), ex);
        }
    }
}
