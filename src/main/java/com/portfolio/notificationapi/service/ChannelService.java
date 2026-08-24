package com.portfolio.notificationapi.service;

import com.portfolio.notificationapi.dto.ChannelRequest;
import com.portfolio.notificationapi.dto.ChannelResponse;
import com.portfolio.notificationapi.entity.Channel;
import com.portfolio.notificationapi.exception.BusinessException;
import com.portfolio.notificationapi.exception.ResourceNotFoundException;
import com.portfolio.notificationapi.mapper.NotificationMapper;
import com.portfolio.notificationapi.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final NotificationMapper mapper;

    @Transactional
    public ChannelResponse create(ChannelRequest request) {
        channelRepository.findByNameIgnoreCase(request.name()).ifPresent(c -> {
            throw new BusinessException("A channel named '" + request.name() + "' already exists");
        });

        Channel channel = Channel.builder()
                .name(request.name())
                .description(request.description())
                .active(request.active())
                .build();

        return mapper.toResponse(channelRepository.save(channel));
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> findAll() {
        return channelRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChannelResponse findById(Long id) {
        return mapper.toResponse(getChannelOrThrow(id));
    }

    @Transactional
    public ChannelResponse update(Long id, ChannelRequest request) {
        Channel channel = getChannelOrThrow(id);
        channel.setName(request.name());
        channel.setDescription(request.description());
        channel.setActive(request.active());
        return mapper.toResponse(channel);
    }

    @Transactional
    public void delete(Long id) {
        Channel channel = getChannelOrThrow(id);
        channelRepository.delete(channel);
    }

   public Channel getChannelOrThrow(Long id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found with id " + id));
    }
}
