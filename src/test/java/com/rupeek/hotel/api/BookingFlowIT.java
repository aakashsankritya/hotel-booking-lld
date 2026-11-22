package com.rupeek.hotel.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The whole guest journey with real beans: search, book, pay, replay, cancel, search again. */
@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowIT {

    private static final String CITY = "FlowCity";

    @Autowired
    private MockMvcTester mvc;

    @Test
    void searchBookPayReplayCancelAndSearchAgain() throws Exception {
        ApiFlow api = new ApiFlow(mvc);
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(2);

        // an owner puts a property with one room on the platform
        String ownerId = api.registerOwner();
        MvcTestResult onboarded = api.onboard(ownerId, CITY, 1);
        assertThat(onboarded).hasStatus(201);
        String propertyId = ApiFlow.read(onboarded, "$.id");
        String roomTypeId = ApiFlow.read(onboarded, "$.roomTypes[0].id");

        // one property so far; a second one makes the owner multi-property
        assertThat(api.getOwner(ownerId)).bodyJson().extractingPath("$.multiProperty").isEqualTo(false);
        assertThat(api.onboard(ownerId, CITY, 1)).hasStatus(201);
        MvcTestResult owner = api.getOwner(ownerId);
        assertThat(owner).hasStatus(200);
        assertThat(ApiFlow.<List<String>>read(owner, "$.propertyIds")).hasSize(2).contains(propertyId);
        assertThat(owner).bodyJson().extractingPath("$.multiProperty").isEqualTo(true);

        // a guest finds it
        assertThat(api.searchPropertyIds(CITY, checkIn, checkOut)).contains(propertyId);

        // book: the room is held, so it is not searchable any more
        MvcTestResult booked = api.book(propertyId, roomTypeId, checkIn, checkOut);
        assertThat(booked).hasStatus(201);
        String bookingId = ApiFlow.read(booked, "$.id");
        assertThat(booked).bodyJson().extractingPath("$.status").isEqualTo("PENDING_PAYMENT");
        assertThat(api.searchPropertyIds(CITY, checkIn, checkOut)).doesNotContain(propertyId);

        // pay by card: the booking is confirmed
        MvcTestResult paid = api.payByCard(bookingId, "flow-key-1");
        assertThat(paid).hasStatus(200);
        assertThat(paid).bodyJson().extractingPath("$.status").isEqualTo("SUCCEEDED");
        assertThat(paid).bodyJson().extractingPath("$.bookingStatus").isEqualTo("CONFIRMED");
        String paymentId = ApiFlow.read(paid, "$.paymentId");

        // the same key again returns the same payment
        MvcTestResult replay = api.payByCard(bookingId, "flow-key-1");
        assertThat(replay).hasStatus(200);
        assertThat(ApiFlow.<String>read(replay, "$.paymentId")).isEqualTo(paymentId);
        assertThat(api.get(bookingId)).bodyJson().extractingPath("$.status").isEqualTo("CONFIRMED");

        // cancel 30 days ahead under MODERATE: full refund of 2 nights x 3000
        MvcTestResult cancelled = api.cancel(bookingId);
        assertThat(cancelled).hasStatus(200);
        assertThat(cancelled).bodyJson().extractingPath("$.booking.status").isEqualTo("CANCELLED");
        assertThat(cancelled).bodyJson().extractingPath("$.refund.status").isEqualTo("SUCCEEDED");
        Number refunded = ApiFlow.read(cancelled, "$.refund.amount.amount");
        assertThat(refunded.doubleValue()).isEqualTo(6000.0);

        // the room is free again
        assertThat(api.searchPropertyIds(CITY, checkIn, checkOut)).contains(propertyId);
    }
}
