package com.minthantoo.appointmentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cached_patient") // to define custom table name
public class CachedPatient {
    @Id
    private UUID id;
    private String fullName; // since these data comes from another microservice, not from a random client request, its sure data is good and no need for extra validation
    private String email;
    private Instant updatedAt; // Shows when the cached record was last updated (to detect staleness and refresh when needed)

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
