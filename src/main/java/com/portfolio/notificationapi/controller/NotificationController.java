package com.portfolio.notificationapi.controller;

import com.portfolio.notificationapi.dto.NotificationRequest;
import com.portfolio.notificationapi.dto.NotificationResponse;
import com.portfolio.notificationapi.entity.NotificationStatus;
import com.portfolio.notificationapi.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Schedule and manage notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Create (and optionally schedule) a notification")
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/notifications/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "List notifications, optionally filtering by status")
    public ResponseEntity<Page<NotificationResponse>> findAll(
            @RequestParam(required = false) NotificationStatus status,
            Pageable pageable) {

        Page<NotificationResponse> page = status != null
                ? notificationService.findByStatus(status, pageable)
                : notificationService.findAll(pageable);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification by id")
    public ResponseEntity<NotificationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.findById(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending or scheduled notification")
    public ResponseEntity<NotificationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a notification")
    public void delete(@PathVariable Long id) {
        notificationService.delete(id);
    }
}
