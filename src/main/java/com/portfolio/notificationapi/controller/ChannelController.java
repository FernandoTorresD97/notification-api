package com.portfolio.notificationapi.controller;

import com.portfolio.notificationapi.dto.ChannelRequest;
import com.portfolio.notificationapi.dto.ChannelResponse;
import com.portfolio.notificationapi.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
@Tag(name = "Channels", description = "Manage notification delivery channels")
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    @Operation(summary = "Create a new channel")
    public ResponseEntity<ChannelResponse> create(@Valid @RequestBody ChannelRequest request) {
        ChannelResponse response = channelService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/channels/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "List all channels")
    public ResponseEntity<List<ChannelResponse>> findAll() {
        return ResponseEntity.ok(channelService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a channel by id")
    public ResponseEntity<ChannelResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(channelService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing channel")
    public ResponseEntity<ChannelResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ChannelRequest request) {
        return ResponseEntity.ok(channelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a channel")
    public void delete(@PathVariable Long id) {
        channelService.delete(id);
    }
}
