package com.example.coupons.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    private String id;

    private String type; // cart-wise | product-wise | bxgy

    @Lob
    @Column(name = "details_json", columnDefinition = "CLOB")
    private String detailsJson;

    private boolean active = true;

    private Instant createdAt = Instant.now();

    private Instant expiresAt;

    public Coupon() { this.id = UUID.randomUUID().toString(); }

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
