package com.portfolio.notificationapi.config;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
@Component @ConfigurationProperties("notification.delivery") @Validated @Getter @Setter
public class DeliveryProperties {
    @Min(1) @Max(10) private int maxAttempts=3;
    @Min(1) @Max(86400) private long initialDelaySeconds=5;
    @Min(1) @Max(86400) private long maxDelaySeconds=300;
    @Min(1) @Max(1000) private int batchSize=100;
    @NotBlank private String from="demo@example.invalid";
    public long backoffSeconds(int attempt) { return Math.min(maxDelaySeconds,initialDelaySeconds*(1L << Math.min(20,Math.max(0,attempt-1)))); }
}
