package com.code_space.code_space.service;

import com.code_space.code_space.entity.Room;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${mailjet.api.key}")
    private String apiKey;

    @Value("${mailjet.secret.key}")
    private String secretKey;

    @Value("${mailjet.from.email}")
    private String fromEmail;

    @Value("${mailjet.from.name}")
    private String fromName;

    @Value("${app.base.url}")
    private String baseUrl;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    private MailjetClient mailjetClient;

    // Initialize the client when Spring injects values
    @Autowired
    public void init() {
        if (apiKey != null && secretKey != null) {
            this.mailjetClient = new MailjetClient(ClientOptions.builder()
                    .apiKey(apiKey)
                    .apiSecretKey(secretKey)
                    .build());
            logger.info("Mailjet client initialized");
        } else {
            logger.warn("Mailjet client not initialized: API key or secret key is null");
        }
    }

    public void sendMeetingInvitation(String email, Room room) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send meeting invitation to: {}", email);
            return;
        }

        try {
            Context context = createEmailContext(room);
            String htmlContent = templateEngine.process("emails/meeting-invitation", context);
            String subject = "Meeting Invitation: " + room.getTitle();

            sendEmail(email, subject, htmlContent, "meeting-invitation");
            logger.info("Meeting invitation sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send meeting invitation to {}: {}", email, e.getMessage());
        }
    }

    public void sendGuestMeetingInvitation(String email, Room room) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send guest invitation to: {}", email);
            return;
        }

        try {
            Context context = createEmailContext(room);
            context.setVariable("joinUrl", baseUrl + "/guest/join/" + room.getInvitationLink());

            String htmlContent = templateEngine.process("emails/guest-meeting-invitation", context);
            String subject = "You're invited to join: " + room.getTitle();

            sendEmail(email, subject, htmlContent, "guest-invitation");
            logger.info("Guest meeting invitation sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send guest invitation to {}: {}", email, e.getMessage());
        }
    }

    public void sendMeetingReminder(String email, Room room) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send meeting reminder to: {}", email);
            return;
        }

        try {
            Context context = createEmailContext(room);
            String htmlContent = templateEngine.process("emails/meeting-reminder", context);
            String subject = "Reminder: " + room.getTitle() + " starts in 15 minutes";

            sendEmail(email, subject, htmlContent, "meeting-reminder");
            logger.info("Meeting reminder sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send meeting reminder to {}: {}", email, e.getMessage());
        }
    }

    public void sendMeetingStarted(List<String> emails, Room room) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send meeting started notification to {} recipients", emails.size());
            return;
        }

        for (String email : emails) {
            try {
                Context context = createEmailContext(room);
                String htmlContent = templateEngine.process("emails/meeting-started", context);
                String subject = room.getTitle() + " has started";

                sendEmail(email, subject, htmlContent, "meeting-started");
                logger.info("Meeting started notification sent to: {}", email);
            } catch (Exception e) {
                logger.error("Failed to send meeting started notification to {}: {}", email, e.getMessage());
            }
        }
    }

    public void sendMeetingCancellation(String email, Room room) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send meeting cancellation to: {}", email);
            return;
        }

        try {
            Context context = createEmailContext(room);
            String htmlContent = templateEngine.process("emails/meeting-cancelled", context);
            String subject = "Meeting Cancelled: " + room.getTitle();

            sendEmail(email, subject, htmlContent, "meeting-cancelled");
            logger.info("Meeting cancellation sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send meeting cancellation to {}: {}", email, e.getMessage());
        }
    }

    public void sendMeetingUpdated(String email, Room room, String updateDetails) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send meeting update to: {}", email);
            return;
        }

        try {
            Context context = createEmailContext(room);
            context.setVariable("updateDetails", updateDetails);
            String htmlContent = templateEngine.process("emails/meeting-updated", context);
            String subject = "Meeting Updated: " + room.getTitle();

            sendEmail(email, subject, htmlContent, "meeting-updated");
            logger.info("Meeting update notification sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send meeting update to {}: {}", email, e.getMessage());
        }
    }

    private void sendEmail(String toEmail, String subject, String htmlContent, String templateType) {
        if (mailjetClient == null) {
            logger.error("Mailjet client not initialized. Cannot send email.");
            return;
        }

        try {
            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", fromEmail)
                                            .put("Name", fromName))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", toEmail)))
                                    .put(Emailv31.Message.SUBJECT, subject)
                                    .put(Emailv31.Message.HTMLPART, htmlContent)
                                    .put("CustomID", templateType)));

            MailjetResponse response = mailjetClient.post(request);
            if (response.getStatus() == 200) {
                logger.info("Email sent successfully via Mailjet to: {}", toEmail);
            } else {
                logger.error("Failed to send email via Mailjet: Status {}, Data: {}",
                        response.getStatus(), response.getData().toString());
            }
        } catch (MailjetException e) {
            logger.error("Mailjet API error: {}", e.getMessage());
            throw new RuntimeException("Failed to send email via Mailjet", e);
        } catch (Exception e) {
            logger.error("Failed to send email via Mailjet: {}", e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // Bulk email sending for notifications
    public void sendBulkNotification(List<String> emails, String subject, String templateName, Context context) {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send bulk notification to {} recipients", emails.size());
            return;
        }

        try {
            String htmlContent = templateEngine.process("emails/" + templateName, context);

            // Send individual emails
            for (String email : emails) {
                sendEmail(email, subject, htmlContent, templateName);
            }

            logger.info("Bulk email sent successfully to {} recipients", emails.size());
        } catch (Exception e) {
            logger.error("Exception while sending bulk email: {}", e.getMessage());
        }
    }

    private Context createEmailContext(Room room) {
        Context context = new Context();
        context.setVariable("room", room);
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("hostName", room.getHost().getFirstName() + " " + room.getHost().getLastName());
        context.setVariable("joinUrl", baseUrl + "/join/" + room.getInvitationLink());
        context.setVariable("meetingCode", room.getRoomCode());

        if (room.getScheduledStartTime() != null) {
            context.setVariable("startTime", room.getScheduledStartTime().format(
                    DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a")));
            context.setVariable("startTimeISO", room.getScheduledStartTime().toString());
        }

        return context;
    }

    // Utility method to test email configuration
    public boolean testEmailConfiguration() {
        if (mailjetClient == null) {
            logger.error("Mailjet client not initialized. Cannot send test email.");
            return false;
        }

        try {
            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", fromEmail)
                                            .put("Name", fromName))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", fromEmail)))
                                    .put(Emailv31.Message.SUBJECT, "CodeSpace Email Configuration Test")
                                    .put(Emailv31.Message.TEXTPART, "Email is working!")
                                    .put(Emailv31.Message.HTMLPART, "<h1>Email is working!</h1><p>Your Mailjet integration is configured correctly.</p>")));

            MailjetResponse response = mailjetClient.post(request);
            logger.info("Test email sent, status: {}", response.getStatus());
            return response.getStatus() == 200;
        } catch (Exception e) {
            logger.error("Failed to send test email: {}", e.getMessage());
            return false;
        }
    }
}