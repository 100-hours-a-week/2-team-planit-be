package com.planit.domain.trip.entity;

import com.planit.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trip_group_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_trip_group_member", columnNames = {"group_id", "user_id"})
)
public class TripGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private TripGroup group;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private GroupMemberRole role;

    @Column(name = "submitted", nullable = false)
    private boolean submitted;

    @Column(name = "themes_json", columnDefinition = "TEXT")
    private String themesJson;

    @Column(name = "wanted_places_json", columnDefinition = "TEXT")
    private String wantedPlacesJson;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    protected TripGroupMember() {
    }

    public TripGroupMember(
            TripGroup group,
            User user,
            GroupMemberRole role,
            boolean submitted,
            String themesJson,
            String wantedPlacesJson,
            LocalDateTime submittedAt
    ) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.submitted = submitted;
        this.themesJson = themesJson;
        this.wantedPlacesJson = wantedPlacesJson;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public TripGroup getGroup() {
        return group;
    }

    public User getUser() {
        return user;
    }

    public GroupMemberRole getRole() {
        return role;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public String getThemesJson() {
        return themesJson;
    }

    public String getWantedPlacesJson() {
        return wantedPlacesJson;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void submit(String themesJson, String wantedPlacesJson, LocalDateTime submittedAt) {
        this.submitted = true;
        this.themesJson = themesJson;
        this.wantedPlacesJson = wantedPlacesJson;
        this.submittedAt = submittedAt;
    }
}
