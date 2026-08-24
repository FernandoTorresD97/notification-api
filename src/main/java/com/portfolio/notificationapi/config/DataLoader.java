package com.portfolio.notificationapi.config;

import com.portfolio.notificationapi.entity.Channel;
import com.portfolio.notificationapi.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with the default set of channels on application startup,
 * so the API is usable immediately without manual setup.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ChannelRepository channelRepository;

    @Override
    public void run(String... args) {
        if (channelRepository.count() > 0) {
            return;
        }

        channelRepository.saveAll(java.util.List.of(
                Channel.builder().name("EMAIL").description("Email notifications").active(true).build(),
                Channel.builder().name("SMS").description("SMS text notifications").active(true).build(),
                Channel.builder().name("PUSH").description("Mobile push notifications").active(true).build()
        ));
    }
}
