package com.example.model;

import javax.persistence.*;

@Entity
@Table(name = "event_images")
public class EventImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "is_banner")
    private boolean isBanner = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    public EventImage() {}

    public EventImage(String imageUrl, boolean isBanner, Event event) {
        this.imageUrl = imageUrl;
        this.isBanner = isBanner;
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isBanner() {
        return isBanner;
    }

    public void setBanner(boolean banner) {
        isBanner = banner;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
