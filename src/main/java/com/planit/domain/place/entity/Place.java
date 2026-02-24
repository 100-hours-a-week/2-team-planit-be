package com.planit.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity // 장소 정보를 저장하는 places 테이블과 매핑되는 JPA 엔티티
@Table(name = "places")
@Getter
@NoArgsConstructor
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id; // 장소 PK

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 장소 이름

    @Column(name = "google_place_id", length = 255, unique = true)
    private String googlePlaceId;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 255)
    private String country;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "photo_url")
    private String photoUrl;

    public Place(String name, String googlePlaceId, String city, String country, Double latitude, Double longitude) {
        this.name = name;
        this.googlePlaceId = googlePlaceId;
        this.city = city;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoUrl = null;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
    
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
