package com.cinema.testcinema.service;

public interface EmailService {
    void sendEmailVerificationCode(String email, String code);
}
