package com.throughline.taskmanagement.service;

/** Thin wrapper over JavaMailSender — kept as its own interface so the OTP-verification
 *  work (and anything else that needs to email someone later) depends on this, not on
 *  Spring's mail API directly. */
public interface MailService {
    void send(String to, String subject, String body);
}
