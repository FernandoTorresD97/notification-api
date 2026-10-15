package com.portfolio.notificationapi.config;
import org.springframework.context.annotation.*;
import java.time.Clock;
@Configuration public class DeliveryClock { @Bean public Clock clock(){return Clock.systemDefaultZone();} }
