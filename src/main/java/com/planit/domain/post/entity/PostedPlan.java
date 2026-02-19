package com.planit.domain.post.entity;

import com.planit.domain.trip.entity.Trip;
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
 * posted_plans 테이블을 표현한 JPA 엔티티
 */
@Entity // JPA가 관리하도록 지정
@Table(name = "posted_plans") // 테이블 명시
public class PostedPlan {

    @Id // PK 필드
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT 전략
    @Column(name = "posted_plan_id") // 컬럼 명시
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Post와 N:1 관계 (지연 로딩)
    @JoinColumn(name = "post_id", nullable = false) // FK 컬럼
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Trip과 N:1 관계
    @JoinColumn(name = "trip_id", nullable = false) // FK 설정
    private Trip trip;

    protected PostedPlan() { // JPA용 기본 생성자
    }

    public PostedPlan(Post post, Trip trip) { // 생성자
        this.post = post;
        this.trip = trip;
    }

    public Long getId() { // getter
        return id;
    }

    public Post getPost() { // Post 반환
        return post;
    }

    public Trip getTrip() { // Trip 반환
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }
}
