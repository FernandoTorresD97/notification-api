package com.portfolio.notificationapi.delivery;
import com.portfolio.notificationapi.config.DeliveryProperties;
import com.portfolio.notificationapi.entity.*;
import com.portfolio.notificationapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.MailException;
import java.time.*;
import java.util.List;
@Service @RequiredArgsConstructor @Slf4j
public class DeliveryWorker {
    public static final List<NotificationStatus> READY=List.of(NotificationStatus.PENDING,NotificationStatus.SCHEDULED,NotificationStatus.RETRYING);
    private final NotificationRepository notifications;
    private final DeliveryAttemptRepository attempts;
    private final SmtpDelivery smtp;
    private final DeliveryProperties properties;
    private final Clock clock;

    // The row lock serializes delivery, cancellation and deletion for this id.
    // SMTP timeouts bound this transaction; a crash after SMTP acceptance but
    // before commit can still cause a duplicate email (documented at-least-once).
    @Transactional
    public void process(Long id) {
        Notification n=notifications.lockById(id).orElse(null);
        LocalDateTime now=LocalDateTime.now(clock);
        if(n==null || !READY.contains(n.getStatus())) return;
        if(n.getScheduledAt()!=null && n.getScheduledAt().isAfter(now)) return;
        if(n.getNextAttemptAt()!=null && n.getNextAttemptAt().isAfter(now)) return;
        int number=n.getAttemptCount()+1;
        n.setAttemptCount(number);
        String error=null;
        if(!n.getChannel().isActive()) error="CHANNEL_INACTIVE";
        else if(!"EMAIL".equalsIgnoreCase(n.getChannel().getName())) error="UNSUPPORTED_CHANNEL";
        else {
            try { smtp.send(n); }
            catch(MailException ex) { error="SMTP_SEND_FAILED"; }
        }
        String outcome;
        if(error==null) {
            n.setStatus(NotificationStatus.SENT); n.setSentAt(now);
            n.setLastError(null); n.setNextAttemptAt(null); outcome="SMTP_ACCEPTED";
        } else {
            n.setLastError(error);
            boolean exhausted=number>=properties.getMaxAttempts() || !"SMTP_SEND_FAILED".equals(error);
            n.setStatus(exhausted ? NotificationStatus.FAILED : NotificationStatus.RETRYING);
            n.setNextAttemptAt(exhausted ? null : now.plusSeconds(properties.backoffSeconds(number)));
            n.setDeadLetterPending(exhausted); outcome=exhausted ? "FAILED" : "RETRY_SCHEDULED";
        }
        attempts.save(DeliveryAttempt.builder().notification(n).attemptNumber(number).outcome(outcome).errorCode(error).attemptedAt(now).build());
        log.info("notification={} attempt={} outcome={}",id,number,outcome);
    }
    @Transactional public void acknowledgeDeadLetter(Long id) {
        notifications.lockById(id).ifPresent(n->n.setDeadLetterPending(false));
    }
}
