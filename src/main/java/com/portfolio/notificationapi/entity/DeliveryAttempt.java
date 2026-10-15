package com.portfolio.notificationapi.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="delivery_attempts", uniqueConstraints=@UniqueConstraint(columnNames={"notification_id","attempt_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryAttempt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="notification_id",nullable=false) private Notification notification;
    @Column(name="attempt_number",nullable=false) private int attemptNumber;
    @Column(nullable=false,length=30) private String outcome;
    @Column(name="error_code",length=80) private String errorCode;
    @Column(name="attempted_at",nullable=false) private LocalDateTime attemptedAt;
}
