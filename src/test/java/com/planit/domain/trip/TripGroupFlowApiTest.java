package com.planit.domain.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.entity.TripGroupMember;
import com.planit.domain.trip.entity.TripStatus;
import com.planit.domain.trip.repository.TripGroupMemberRepository;
import com.planit.domain.trip.repository.TripGroupRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.trip.service.ItineraryEnqueueService;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private TripGroupMemberRepository tripGroupMemberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ItineraryEnqueueService itineraryEnqueueService;

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .loginId("leader")
                .password("hashed")
                .nickname("leader1")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        userRepository.save(User.builder()
                .loginId("member1")
                .password("hashed")
                .nickname("member01")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void submitMemberBeforeLastSubmission_keepsWaitingAndSyncsSubmittedPreferences() throws Exception {
        // Given
        MvcResult createResult = mockMvc.perform(post("/trips")
                        .with(user("leader"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupTripCreateRequest("오사카 같이가기", 3))))
                .andExpect(status().isCreated())
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        Long tripId = JsonPath.parse(createBody).read("$.data.tripId", Number.class).longValue();
        String inviteCode = JsonPath.parse(createBody).read("$.data.inviteCode", String.class);
        Long groupId = tripGroupRepository.findByInviteCode(inviteCode).orElseThrow().getId();
        Long memberId = userRepository.findByLoginIdAndDeletedFalse("member1").orElseThrow().getId();

        Map<String, Object> submitRequest = Map.of(
                "travelTheme", List.of("힐링", "액티비티"),
                "wantedPlace", List.of("member-place", "leader-place")
        );

        // When
        mockMvc.perform(post("/groups/join/{inviteCode}/submit", inviteCode)
                        .with(user("member1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tripId").value(tripId))
                .andExpect(jsonPath("$.data.inviteCode").value(inviteCode))
                .andExpect(jsonPath("$.data.headCount").value(3))
                .andExpect(jsonPath("$.data.submittedCount").value(2))
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        // Then
        Trip trip = tripRepository.findById(tripId).orElseThrow();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.WAITING);
        assertThat(trip.getHeadCount()).isEqualTo(3);

        TripGroupMember member = tripGroupMemberRepository.findByGroupIdAndUserId(groupId, memberId).orElseThrow();
        assertThat(member.isSubmitted()).isTrue();
        assertThat(member.getThemesJson()).isEqualTo("[\"힐링\",\"액티비티\"]");
        assertThat(member.getWantedPlacesJson()).isEqualTo("[\"member-place\",\"leader-place\"]");
        assertThat(member.getSubmittedAt()).isNotNull();

        assertThat(selectTripThemes(tripId)).containsExactlyInAnyOrder("맛집탐방", "힐링", "액티비티");
        assertThat(selectWantedPlaces(tripId)).containsExactlyInAnyOrder("leader-place", "member-place");
        verifyNoInteractions(itineraryEnqueueService);
    }

    @Test
    void submitGroupInput_withExpiredInviteCode_returns400AndLeavesGroupStateUntouched() throws Exception {
        // Given
        MvcResult createResult = mockMvc.perform(post("/trips")
                        .with(user("leader"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupTripCreateRequest("후쿠오카 같이가기", 3))))
                .andExpect(status().isCreated())
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        Long tripId = JsonPath.parse(createBody).read("$.data.tripId", Number.class).longValue();
        String inviteCode = JsonPath.parse(createBody).read("$.data.inviteCode", String.class);
        var group = tripGroupRepository.findByInviteCode(inviteCode).orElseThrow();
        group.updateExpiresAt(LocalDateTime.now().minusMinutes(1));
        tripGroupRepository.save(group);

        Map<String, Object> submitRequest = Map.of(
                "travelTheme", List.of("힐링"),
                "wantedPlace", List.of("member-place")
        );

        // When
        mockMvc.perform(post("/groups/join/{inviteCode}/submit", inviteCode)
                        .with(user("member1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("GROUP_002"))
                .andExpect(jsonPath("$.error.code").value("GROUP_002"))
                .andExpect(jsonPath("$.error.message").value("초대코드가 만료되었습니다"));

        // Then
        Long groupId = group.getId();
        Long memberId = userRepository.findByLoginIdAndDeletedFalse("member1").orElseThrow().getId();

        assertThat(tripGroupMemberRepository.findByGroupIdAndUserId(groupId, memberId)).isEmpty();
        assertThat(tripGroupMemberRepository.countByGroupIdAndSubmittedTrue(groupId)).isEqualTo(1);
        assertThat(selectTripThemes(tripId)).containsExactly("맛집탐방");
        assertThat(selectWantedPlaces(tripId)).containsExactly("leader-place");
        assertThat(tripRepository.findById(tripId).orElseThrow().getStatus()).isEqualTo(TripStatus.WAITING);
        verifyNoInteractions(itineraryEnqueueService);
    }

    @AfterEach
    void verifyNoEnqueueSideEffects() {
        verifyNoInteractions(itineraryEnqueueService);
    }

    private Map<String, Object> groupTripCreateRequest(String title, int headCount) {
        return Map.ofEntries(
                Map.entry("title", title),
                Map.entry("arrivalDate", "2024-08-15"),
                Map.entry("arrivalTime", "09:00"),
                Map.entry("departureDate", "2024-08-17"),
                Map.entry("departureTime", "18:00"),
                Map.entry("travelCity", "부산"),
                Map.entry("totalBudget", 120000),
                Map.entry("travelTheme", List.of("맛집탐방")),
                Map.entry("wantedPlace", List.of("leader-place")),
                Map.entry("travelMode", "GROUP"),
                Map.entry("headCount", headCount)
        );
    }

    private List<String> selectTripThemes(Long tripId) {
        return jdbcTemplate.queryForList(
                "select name from travel_themes where travel_id = ?",
                String.class,
                tripId
        );
    }

    private List<String> selectWantedPlaces(Long tripId) {
        return jdbcTemplate.queryForList(
                "select google_map_id from wanted_places where travel_id = ?",
                String.class,
                tripId
        );
    }
}
