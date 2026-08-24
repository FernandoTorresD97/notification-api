package com.portfolio.notificationapi.repository;

import com.portfolio.notificationapi.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    Optional<Channel> findByNameIgnoreCase(String name);
}
