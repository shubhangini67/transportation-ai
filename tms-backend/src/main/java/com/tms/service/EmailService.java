package com.tms.service;

import com.tms.entity.Booking;
import com.tms.entity.LorryReceipt;
import com.tms.entity.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@tms.com}")
    private String fromEmail;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Async
    public void sendBookingConfirmation(Booking booking) {
        if (!emailEnabled || booking.getCustomerEmail() == null) return;
        String subject = "Booking Confirmed — #" + booking.getId();
        String body = String.format(
                "<h2>Booking Confirmation</h2>" +
                "<p>Dear %s,</p>" +
                "<p>Your booking has been confirmed.</p>" +
                "<ul><li>Booking ID: %d</li><li>Seats: %d</li><li>Vehicle: %s</li></ul>" +
                "<p>Thank you for choosing TMS!</p>",
                booking.getCustomerName(), booking.getId(), booking.getSeatCount(),
                booking.getTrip().getVehicle().getVehicleNumber());
        sendHtml(booking.getCustomerEmail(), subject, body);
    }

    @Async
    public void sendTripStatusUpdate(Trip trip, String oldStatus) {
        if (!emailEnabled) return;
        log.info("Trip {} status changed: {} → {}", trip.getId(), oldStatus, trip.getStatus());
        // In a real system, notify all booked customers
    }

    @Async
    public void sendLrDispatchAlert(LorryReceipt lr) {
        if (!emailEnabled) return;
        log.info("LR {} dispatched: {} → {}", lr.getLrNumber(), lr.getOrigin(), lr.getDestination());
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}

