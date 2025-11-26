package com.cinema.testcinema.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendEmailVerificationCode(String email, String code) {
        log.info("Sending email verification code {} to {}", code, email);
    }
}
