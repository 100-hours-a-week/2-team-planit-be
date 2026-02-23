package com.planit.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.trip.repository.WantedPlaceRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripCreateApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private WantedPlaceRepository wantedPlaceRepository;

    @Autowired
    private ItineraryDayRepository itineraryDayRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .loginId("user1")
                .password("hashed")
                .nickname("nick1")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @WithMockUser(username = "user1")
    void createTrip_happyPath_persistsTripAndItinerary() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "title", "부산 2박 3일",
                "arrivalDate", LocalDate.of(2024, 8, 15),
                "arrivalTime", LocalTime.of(9, 0),
                "departureDate", LocalDate.of(2024, 8, 17),
                "departureTime", LocalTime.of(18, 0),
                "travelCity", "부산",
                "totalBudget", 120000,
                "travelTheme", List.of("맛집탐방", "힐링"),
                "wantedPlace", List.of("place-id-1", "place-id-2")
        );

        // When
        MvcResult result = mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tripId").isNumber())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Number tripIdValue = JsonPath.parse(responseBody).read("$.data.tripId", Number.class);
        Long tripId = tripIdValue.longValue();

        Trip trip = tripRepository.findById(tripId).orElseThrow();
        assertThat(trip.getTitle()).isEqualTo("부산 2박 3일");
        assertThat(trip.getUser().getId()).isEqualTo(user.getId());
        assertThat(trip.getThemes()).hasSize(2);
        assertThat(wantedPlaceRepository.findByTripId(tripId)).hasSize(2);
        assertThat(itineraryDayRepository.findByTripIdOrderByDayIndex(tripId)).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "user1")
    void createTrip_validationError_returns400AndNoTripSaved() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "title", "",
                "arrivalDate", LocalDate.of(2024, 8, 15),
                "arrivalTime", LocalTime.of(9, 0),
                "departureDate", LocalDate.of(2024, 8, 17),
                "departureTime", LocalTime.of(18, 0),
                "travelCity", "부산",
                "totalBudget", 120000,
                "travelTheme", List.of("맛집탐방")
        );

        long before = tripRepository.count();

        // When & Then
        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("COMMON_001"))
                .andExpect(jsonPath("$.error.code").value("COMMON_001"))
                .andExpect(jsonPath("$.error.message").value("잘못된 요청입니다"));

        assertThat(tripRepository.count()).isEqualTo(before);
    }
}
