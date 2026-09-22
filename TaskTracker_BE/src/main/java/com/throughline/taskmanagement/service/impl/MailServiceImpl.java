package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.exception.EmailDeliveryException;
import com.throughline.taskmanagement.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    public void sendCode(String to, String subject, String message, String code, String actionUrl, String actionLabel) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // true = multipart/alternative (a real plain-text part alongside the HTML one).
            // Without it, Gmail's quote/signature-detection heuristics can mistake the
            // trailing block (button + footer note) for quoted content and collapse it
            // behind a "..." — the HTML looked fine in isolation but rendered with the
            // button missing. A genuine plain-text alternative is the standard fix.
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(codeEmailPlainText(message, code, actionUrl, actionLabel), codeEmailHtml(message, code, actionUrl, actionLabel));
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // A malformed message, not a mail-server problem — still surfaced the same way
            // callers already handle a failed send() (best-effort try/catch or
            // EmailDeliveryException), so this doesn't need its own distinct handling.
            throw new EmailDeliveryException("Could not compose the email.");
        }
    }

    private String codeEmailPlainText(String message, String code, String actionUrl, String actionLabel) {
        String cta = (actionUrl == null || actionLabel == null) ? "" : "\n\n" + actionLabel + ": " + actionUrl;
        return message + "\n\nCode: " + code + cta + "\n\nIf you didn't request this, you can safely ignore this email.";
    }

    // Matches the frontend's green brand palette (TaskTracker_FE/src/styles/tokens.css):
    // --accent-strong, --accent-soft, and --accent, in that order below.
    private static final String ACCENT_STRONG = "#1d5f20";
    private static final String ACCENT_SOFT = "#e4f3e4";
    private static final String ACCENT = "#3c9f40";

    /** Plain inline styles only — email clients strip <style> blocks and external CSS/fonts
     *  unpredictably, so every rule has to travel on the element itself. The code renders in
     *  one bordered, letter-spaced box: big enough to read at a glance, and a click-drag or
     *  triple-click still selects exactly the code (letter-spacing is purely visual, it
     *  doesn't insert real space characters). */
    private String codeEmailHtml(String message, String code, String actionUrl, String actionLabel) {
        String cta = (actionUrl == null || actionLabel == null) ? "" : """
                <div style="text-align:center;margin:0 0 28px;">
                  <a href="%s" style="display:inline-block;background:%s;color:#ffffff;text-decoration:none;
                     font-size:14px;font-weight:600;padding:12px 28px;border-radius:6px;">%s</a>
                </div>
                """.formatted(actionUrl, ACCENT_STRONG, actionLabel);

        return """
                <!DOCTYPE html>
                <html>
                  <head><meta charset="utf-8"></head>
                  <body style="margin:0;padding:0;background:#ffffff;">
                    <div style="font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;max-width:480px;
                         margin:0 auto;padding:32px 24px;color:#1f2937;">
                      <p style="margin:0 0 4px;font-size:13px;font-weight:700;letter-spacing:.08em;
                         text-transform:uppercase;color:%s;">Throughline</p>
                      <p style="margin:0 0 24px;font-size:15px;line-height:1.5;color:#374151;">%s</p>
                      <div style="text-align:center;margin:0 0 28px;">
                        <span style="display:inline-block;font-family:'Courier New',monospace;font-size:32px;
                           font-weight:700;letter-spacing:10px;padding:16px 20px 16px 30px;border:1px solid %s;
                           border-radius:8px;background:%s;color:%s;">%s</span>
                      </div>
                      %s
                      <p style="margin:0;font-size:13px;color:#6b7280;">If you didn't request this, you can safely
                         ignore this email.</p>
                    </div>
                  </body>
                </html>
                """.formatted(ACCENT_STRONG, message, ACCENT, ACCENT_SOFT, ACCENT_STRONG, code, cta);
    }
}
