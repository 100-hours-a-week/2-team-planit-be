package com.planit.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripStatus;
import com.planit.domain.trip.repository.ItineraryDayRepository;
import com.planit.domain.trip.repository.TripGroupRepository;
import com.planit.domain.trip.repository.TripRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripGroupFlowApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripGroupRepository tripGroupRepository;

    @Autowired
    private ItineraryDayRepository itineraryDayRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .loginId("leader")
                .password("hashed")
                .nickname("leader-nick")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        userRepository.save(User.builder()
                .loginId("member1")
                .password("hashed")
                .nickname("member-nick")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void createGroupTrip_waitingStateAndInviteCodeReturned_enqueueDelayed() throws Exception {
        Map<String, Object> request = Map.ofEntries(
                Map.entry("title", "부산 같이가기"),
                Map.entry("arrivalDate", LocalDate.of(2024, 8, 15)),
                Map.entry("arrivalTime", LocalTime.of(9, 0)),
                Map.entry("departureDate", LocalDate.of(2024, 8, 17)),
                Map.entry("departureTime", LocalTime.of(18, 0)),
                Map.entry("travelCity", "부산"),
                Map.entry("totalBudget", 120000),
                Map.entry("travelTheme", List.of("맛집탐방", "힐링")),
                Map.entry("wantedPlace", List.of("place-a")),
                Map.entry("travelMode", "GROUP"),
                Map.entry("headCount", 2)
        );

        MvcResult result = mockMvc.perform(post("/trips")
                        .with(user("leader"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tripId").isNumber())
                .andExpect(jsonPath("$.data.inviteCode").isString())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Long tripId = JsonPath.parse(body).read("$.data.tripId", Number.class).longValue();
        String inviteCode = JsonPath.parse(body).read("$.data.inviteCode", String.class);

        Trip trip = tripRepository.findById(tripId).orElseThrow();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.WAITING);
        assertThat(trip.getHeadCount()).isEqualTo(2);
        assertThat(itineraryDayRepository.findByTripIdOrderByDayIndex(tripId)).isEmpty();
        assertThat(tripGroupRepository.findByInviteCode(inviteCode)).isPresent();
    }

    @Test
    void submitLastMember_triggersGenerationAndItinerarySaved() throws Exception {
        Map<String, Object> createRequest = Map.ofEntries(
                Map.entry("title", "도쿄 같이가기"),
                Map.entry("arrivalDate", LocalDate.of(2024, 9, 1)),
                Map.entry("arrivalTime", LocalTime.of(10, 0)),
                Map.entry("departureDate", LocalDate.of(2024, 9, 3)),
                Map.entry("departureTime", LocalTime.of(20, 0)),
                Map.entry("travelCity", "도쿄"),
                Map.entry("totalBudget", 150000),
                Map.entry("travelTheme", List.of("맛집탐방")),
                Map.entry("wantedPlace", List.of("leader-place")),
                Map.entry("travelMode", "GROUP"),
                Map.entry("headCount", 2)
        );

        MvcResult createResult = mockMvc.perform(post("/trips")
                        .with(user("leader"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        Long tripId = JsonPath.parse(createBody).read("$.data.tripId", Number.class).longValue();
        String inviteCode = JsonPath.parse(createBody).read("$.data.inviteCode", String.class);

        Map<String, Object> submitRequest = Map.of(
                "travelTheme", List.of("액티비티", "힐링"),
                "wantedPlace", List.of("member-place")
        );

        mockMvc.perform(post("/groups/join/{inviteCode}/submit", inviteCode)
                        .with(user("member1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andExpect(jsonPath("$.data.submittedCount").value(2));

        Trip trip = tripRepository.findById(tripId).orElseThrow();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.GENERATING);
        assertThat(itineraryDayRepository.findByTripIdOrderByDayIndex(tripId)).isNotEmpty();
    }

    @Test
    void expiredInviteCode_rejectsJoinAndSubmit() throws Exception {
        Map<String, Object> createRequest = Map.of(
                "title", "후쿠오카 같이가기",
                "arrivalDate", LocalDate.of(2024, 10, 1),
                "arrivalTime", LocalTime.of(10, 0),
                "departureDate", LocalDate.of(2024, 10, 2),
                "departureTime", LocalTime.of(20, 0),
                "travelCity", "후쿠오카",
                "totalBudget", 100000,
                "travelTheme", List.of("맛집탐방"),
                "travelMode", "GROUP",
                "headCount", 2
        );

        MvcResult createResult = mockMvc.perform(post("/trips")
                        .with(user("leader"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String inviteCode = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data.inviteCode", String.class);
        var group = tripGroupRepository.findByInviteCode(inviteCode).orElseThrow();
        group.updateExpiresAt(LocalDateTime.now().minusMinutes(1));
        tripGroupRepository.save(group);

        mockMvc.perform(get("/groups/join/{inviteCode}", inviteCode).with(user("member1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("GROUP_002"));

        Map<String, Object> submitRequest = Map.of("travelTheme", List.of("힐링"));
        mockMvc.perform(post("/groups/join/{inviteCode}/submit", inviteCode)
                        .with(user("member1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("GROUP_002"));
    }
}
