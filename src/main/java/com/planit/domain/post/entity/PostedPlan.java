package com.planit.domain.post.entity;

import com.planit.domain.post.entity.Post;
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
 * posted_plans 테이블을 JPA 엔티티로 매핑합니다.
 */
@Entity // JPA에서 관리되는 엔티티 클래스임을 선언
@Table(name = "posted_plans") // 실제 DB 테이블 이름을 지정
public class PostedPlan {

    @Id // 게시 플랜 고유 식별자
    @GeneratedValue(strategy = GenerationType.IDENTITY) // IDENTITY 전략으로 자동 증가
    @Column(name = "posted_plan_id") // 컬럼 이름 명시
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Post 엔티티와 N:1 관계, 지연 로딩
    @JoinColumn(name = "post_id", nullable = false) // 외래키 컬럼 이름과 NOT NULL 제약
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Trip 엔티티와 N:1 관계
    @JoinColumn(name = "trip_id", nullable = false) // 외래키 컬럼 설정
    private Trip trip;

    protected PostedPlan() { // JPA가 내부적으로 호출하는 기본 생성자
    }

    public PostedPlan(Post post, Trip trip) { // 외부에서 사용 가능한 생성자
        this.post = post;
        this.trip = trip;
    }

    public Long getId() { // 식별자 반환
        return id;
    }

    public Post getPost() { // 연관된 Post 반환
        return post;
    }

    public Trip getTrip() { // 연관된 Trip 반환
        return trip;
    }
}
