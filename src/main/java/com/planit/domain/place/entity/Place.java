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
}
