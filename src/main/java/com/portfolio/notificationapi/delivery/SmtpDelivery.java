package com.portfolio.notificationapi.delivery;
import com.portfolio.notificationapi.entity.Notification;
import com.portfolio.notificationapi.config.DeliveryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class SmtpDelivery {
    private final JavaMailSender sender;
    private final DeliveryProperties properties;
    public void send(Notification notification) {
        SimpleMailMessage mail=new SimpleMailMessage();
        mail.setFrom(properties.getFrom()); mail.setTo(notification.getRecipient());
        mail.setSubject(notification.getTitle()); mail.setText(notification.getMessage());
        sender.send(mail);
    }
}
