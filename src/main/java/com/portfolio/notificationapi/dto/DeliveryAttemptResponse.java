package com.portfolio.notificationapi.dto;
import java.time.LocalDateTime;
public record DeliveryAttemptResponse(int attemptNumber,String outcome,String errorCode,LocalDateTime attemptedAt) {}
