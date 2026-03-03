package com.planit.domain.trip.entity;

import com.planit.domain.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "group_id")
    private Long groupId;

    @Column(nullable = false, length = 15)
    private String title;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "travel_city", length = 50)
    private String travelCity;

    @Column(name = "total_budget")
    private Integer totalBudget;

    @Column(name = "head_count")
    private Integer headCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TripStatus status;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TripTheme> themes = new ArrayList<>();

    protected Trip() {
    }

    public Trip(
            User user,
            String title,
            LocalDate arrivalDate,
            LocalDate departureDate,
            LocalTime arrivalTime,
            LocalTime departureTime,
            String travelCity,
            Integer totalBudget
    ) {
        this(user, title, arrivalDate, departureDate, arrivalTime, departureTime, travelCity, totalBudget, null, TripStatus.GENERATING);
    }

    public Trip(
            User user,
            String title,
            LocalDate arrivalDate,
            LocalDate departureDate,
            LocalTime arrivalTime,
            LocalTime departureTime,
            String travelCity,
            Integer totalBudget,
            Integer headCount,
            TripStatus status
    ) {
        this.user = user;
        this.title = title;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.travelCity = travelCity;
        this.totalBudget = totalBudget;
        this.headCount = headCount;
        this.status = status == null ? TripStatus.GENERATING : status;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public String getTravelCity() {
        return travelCity;
    }

    public Integer getTotalBudget() {
        return totalBudget;
    }

    public Integer getHeadCount() {
        return headCount;
    }

    public TripStatus getStatus() {
        return status;
    }

    public List<TripTheme> getThemes() {
        return themes;
    }

    public void addTheme(TripTheme theme) {
        themes.add(theme);
    }

    public void updateStatus(TripStatus status) {
        this.status = status;
    }

    public void assignGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
