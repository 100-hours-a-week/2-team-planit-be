package com.planit.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 게시물과 장소를 연결하는 posted_places 테이블 표현
 */
@Entity
@Table(name = "posted_places")
public class PostedPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "google_place_id", length = 255)
    private String googlePlaceId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    protected PostedPlace() {
    }

    public PostedPlace(Post post, Long placeId, String googlePlaceId, Integer rating) {
        this.post = post;
        this.placeId = placeId;
        this.googlePlaceId = googlePlaceId;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public Integer getRating() {
        return rating;
    }
}
