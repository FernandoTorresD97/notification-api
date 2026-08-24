package com.portfolio.notificationapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a delivery channel through which a notification can be sent
 * (e.g. EMAIL, SMS, PUSH).
 *
 * NOTE: the identifier strategy below is intentionally GenerationType.IDENTITY.
 * The original version of this project used GenerationType.SEQUENCE without
 * declaring a matching @SequenceGenerator, which caused Hibernate to throw
 * an IdentifierGenerationException at startup because it could not resolve
 * the sequence name against PostgreSQL. Switching to IDENTITY (backed by a
 * PostgreSQL GENERATED ALWAYS AS IDENTITY / SERIAL column) fixed it.
 */
@Entity
@Table(name = "channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 255)
    private String description;
}
