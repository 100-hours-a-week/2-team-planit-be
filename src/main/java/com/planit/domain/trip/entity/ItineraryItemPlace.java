package com.planit.domain.trip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "itinerary_item_places")
public class ItineraryItemPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "itinerary_item_id", nullable = false)
    private ItineraryItem itineraryItem;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "event_order", nullable = false)
    private Integer eventOrder;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_time", nullable = false)
    private LocalTime durationTime;

    @Column(name = "cost", nullable = false)
    private BigDecimal cost;

    @Column(name = "place_name", length = 255)
    private String placeName;

    @Column(name = "google_map_url", length = 500)
    private String googleMapUrl;

    @Column(name = "position_lat", length = 255)
    private String positionLat;

    @Column(name = "position_lng", length = 255)
    private String positionLng;

    //memo 필드 추가

    protected ItineraryItemPlace() {
    }

    public ItineraryItemPlace(
            ItineraryItem itineraryItem,
            String placeId,
            Integer eventOrder,
            LocalTime startTime,
            LocalTime durationTime,
            BigDecimal cost,
            String placeName,
            String googleMapUrl,
            String positionLat,
            String positionLng
    ) {
        this.itineraryItem = itineraryItem;
        this.placeId = placeId;
        this.eventOrder = eventOrder;
        this.startTime = startTime;
        this.durationTime = durationTime;
        this.cost = cost;
        this.placeName = placeName;
        this.googleMapUrl = googleMapUrl;
        this.positionLat = positionLat;
        this.positionLng = positionLng;
    }

    public Long getId() {
        return id;
    }

    public String getPlaceId() {
        return placeId;
    }

    public Integer getEventOrder() {
        return eventOrder;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getDurationTime() {
        return durationTime;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getGoogleMapUrl() {
        return googleMapUrl;
    }

    public String getPositionLat() {
        return positionLat;
    }

    public String getPositionLng() {
        return positionLng;
    }
}
