package com.throughline.taskmanagement.service;

/** Thin wrapper over JavaMailSender — kept as its own interface so the OTP-verification
 *  work (and anything else that needs to email someone later) depends on this, not on
 *  Spring's mail API directly. */
public interface MailService {
    void send(String to, String subject, String body);

    /** Same delivery as send(), but rendered as a styled HTML email with the code shown
     *  large or in a bordered, letter-spaced box) so it's easy to read and copy — used for
     *  every one-time code (sign-up, password reset). actionUrl/actionLabel are optional
     *  (either both null or both set) and add a button linking straight to where the code
     *  gets entered. */
    void sendCode(String to, String subject, String message, String code, String actionUrl, String actionLabel);
}
