package com.carpool.booking;

import com.carpool.support.Fixtures;
import com.carpool.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.carpool.group.GroupMemberRepository;
import com.carpool.ride.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingControllerTest extends IntegrationTest {

    @Autowired
    private SeatBookingRepository bookings;

    @Autowired
    private RideRepository rides;

    @Autowired
    private GroupMemberRepository members;

    private String ana;
    private String ben;
    private String cara;
    private long groupId;
    private long rideId;

    @BeforeEach
    void setUp() throws Exception {
        ana = register("ana@example.com", "Ana");
        ben = register("ben@example.com", "Ben");
        cara = register("cara@example.com", "Cara");
        JsonNode group = createGroup(ana, "School run");
        groupId = group.get("id").asLong();
        joinGroup(ben, group.get("inviteCode").asText());
        joinGroup(cara, group.get("inviteCode").asText());
        mvc.perform(postAs(ana, "/api/groups/{id}/trips", groupId).content(toJson(Fixtures.schoolRun())));
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(1))));
        rideId = read(mvc.perform(getAs(ana, "/api/groups/{id}/rides", groupId)
                .param("from", "2026-03-03").param("to", "2026-03-04")).andReturn()).get(0).get("id").asLong();
    }

    @Test
    void aSeatIsPendingUntilSomeoneDrives() throws Exception {
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId))
                .andExpect(jsonPath("$.seatsTaken").value(1));
        mvc.perform(getAs(ben, "/api/rides/{id}", rideId))
                .andExpect(jsonPath("$.myBooking").value("CONFIRMED"));
    }

    @Test
    void theSecondRiderWaitsWhenTheCarIsFull() throws Exception {
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId));

        mvc.perform(postAs(cara, "/api/rides/{id}/seat", rideId))
                .andExpect(status().isCreated());

        mvc.perform(getAs(cara, "/api/rides/{id}", rideId))
                .andExpect(jsonPath("$.myBooking").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlisted").value(1));
    }

    @Test
    void cancellingPromotesTheWaitlist() throws Exception {
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId));
        mvc.perform(postAs(cara, "/api/rides/{id}/seat", rideId));

        mvc.perform(deleteAs(ben, "/api/rides/{id}/seat", rideId)).andExpect(status().isNoContent());

        mvc.perform(getAs(cara, "/api/rides/{id}/passengers", rideId))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].displayName").value("Cara"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void askingTwiceIsAConflict() throws Exception {
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId));

        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId)).andExpect(status().isConflict());
    }

    @Test
    void theDriverCannotBookASeat() throws Exception {
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));

        mvc.perform(postAs(ana, "/api/rides/{id}/seat", rideId)).andExpect(status().isBadRequest());
    }

    @Test
    void cancellingWithoutABookingIsNotFound() throws Exception {
        mvc.perform(deleteAs(ben, "/api/rides/{id}/seat", rideId)).andExpect(status().isNotFound());
    }

    @Test
    void outsidersCannotBook() throws Exception {
        String eve = register("eve@example.com", "Eve");

        mvc.perform(postAs(eve, "/api/rides/{id}/seat", rideId)).andExpect(status().isNotFound());
    }

    @Test
    void reducingSeatsWaitlistsTheLatestRider() throws Exception {
        mvc.perform(putAs(ana, "/api/groups/{id}/members/me", groupId).content(toJson(Fixtures.car(2))));
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId));
        mvc.perform(postAs(cara, "/api/rides/{id}/seat", rideId));

        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId).content(toJson(Map.of("seats", 1))))
                .andExpect(jsonPath("$.seatsTaken").value(1))
                .andExpect(jsonPath("$.waitlisted").value(1));
    }

    @Test
    void passengersShowTheirPickupNote() throws Exception {
        Map<String, Object> riderOnly = new java.util.HashMap<>(Map.of("canDrive", false, "carSeats", 0));
        riderOnly.put("pickupNote", "Bus stop on Oak Ave");
        mvc.perform(putAs(ben, "/api/groups/{id}/members/me", groupId).content(toJson(riderOnly)));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId));

        mvc.perform(getAs(ana, "/api/rides/{id}/passengers", rideId))
                .andExpect(jsonPath("$[0].pickupNote").value("Bus stop on Oak Ave"));
    }

    @Test
    void theDatabaseRejectsASecondBookingForTheSameRider() throws Exception {
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId));
        long benId = members.findAllInGroup(groupId).stream()
                .filter(m -> m.getDisplayName().equals("Ben")).findFirst().orElseThrow().getId();

        // What a second, concurrent request would insert after passing the duplicate check.
        assertThatThrownBy(() -> bookings.saveAndFlush(new SeatBooking(rides.getReferenceById(rideId),
                members.getReferenceById(benId), clock.instant())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void askingAgainAfterCancellingGoesToTheBackOfTheQueue() throws Exception {
        mvc.perform(postAs(ana, "/api/rides/{id}/driver", rideId));
        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId));
        mvc.perform(deleteAs(ben, "/api/rides/{id}/seat", rideId));
        clock.advance(Duration.ofMinutes(1));
        mvc.perform(postAs(cara, "/api/rides/{id}/seat", rideId));
        clock.advance(Duration.ofMinutes(1));

        mvc.perform(postAs(ben, "/api/rides/{id}/seat", rideId)).andExpect(status().isCreated());

        mvc.perform(getAs(ben, "/api/rides/{id}", rideId)).andExpect(jsonPath("$.myBooking").value("WAITLISTED"));
        mvc.perform(getAs(ben, "/api/rides/{id}/passengers", rideId)).andExpect(jsonPath("$", hasSize(2)));
    }
}
