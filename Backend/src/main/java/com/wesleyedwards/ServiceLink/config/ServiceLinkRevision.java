package com.wesleyedwards.ServiceLink.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import java.util.UUID;

/**
 * Custom Envers revision row. Extends {@link DefaultRevisionEntity} for the id +
 * timestamp columns and adds the acting user, populated by
 * {@link ServiceLinkRevisionListener} at flush time.
 */
@Entity
@Getter
@Setter
@RevisionEntity(ServiceLinkRevisionListener.class)
public class ServiceLinkRevision extends DefaultRevisionEntity {

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "actor_id")
    private UUID actorId;
}
