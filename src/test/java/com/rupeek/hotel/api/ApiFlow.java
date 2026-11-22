package com.rupeek.hotel.api;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Calls the real API through MockMvc. The two integration tests share it. */
final class ApiFlow {

    private final MockMvcTester mvc;

    ApiFlow(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    static <T> T read(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    MvcTestResult post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    MvcTestResult postWithKey(String uri, String body, String idempotencyKey) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", idempotencyKey)
                .content(body).exchange();
    }

    String registerOwner() throws Exception {
        return read(post("/owners", "{\"name\": \"Asha\", \"email\": \"asha@example.com\"}"), "$.id");
    }

    /** Onboards a MODERATE property with one room type (capacity 2, 3000 INR a night). Returns the response. */
    MvcTestResult onboard(String ownerId, String city, int units) {
        return post("/owners/" + ownerId + "/properties", """
                {"name": "Sea View", "city": "%s", "locality": "Baga", "stars": 4, "amenities": ["WIFI"],
                 "cancellationPolicy": "MODERATE",
                 "roomTypes": [{"name": "Deluxe", "capacity": 2, "units": %d,
                                "pricePerNight": {"amount": 3000.00, "currency": "INR"}}]}
                """.formatted(city, units));
    }

    MvcTestResult book(String propertyId, String roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return post("/bookings", """
                {"propertyId": "%s", "roomTypeId": "%s", "guestName": "Ravi", "guestEmail": "ravi@example.com",
                 "checkIn": "%s", "checkOut": "%s", "guests": 2}
                """.formatted(propertyId, roomTypeId, checkIn, checkOut));
    }

    MvcTestResult payByCard(String bookingId, String key) {
        return postWithKey("/bookings/" + bookingId + "/payments", """
                {"method": "CARD", "card": {"last4": "4242", "expiry": "%s", "holderName": "Ravi"}}
                """.formatted(YearMonth.now().plusYears(2)), key);
    }

    MvcTestResult cancel(String bookingId) {
        return mvc.post().uri("/bookings/{id}/cancel", bookingId).exchange();
    }

    MvcTestResult getOwner(String ownerId) {
        return mvc.get().uri("/owners/{id}", ownerId).exchange();
    }

    MvcTestResult get(String bookingId) {
        return mvc.get().uri("/bookings/{id}", bookingId).exchange();
    }

    List<String> searchPropertyIds(String city, LocalDate checkIn, LocalDate checkOut) throws Exception {
        MvcTestResult result = mvc.get().uri("/properties/search?city={city}&checkIn={in}&checkOut={out}&guests=2",
                city, checkIn, checkOut).exchange();
        return read(result, "$[*].propertyId");
    }
}
