package com.rupeek.hotel.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Errors from the real application have the RFC 9457 shape plus the {@code error} property. */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorShapeIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void soldOutBookingReturnsAProblemWithAllFields() throws Exception {
        ApiFlow api = new ApiFlow(mvc);
        LocalDate checkIn = LocalDate.now().plusDays(40);
        LocalDate checkOut = checkIn.plusDays(1);
        MvcTestResult onboarded = api.onboard(api.registerOwner(), "ShapeCity", 1);
        String propertyId = ApiFlow.read(onboarded, "$.id");
        String roomTypeId = ApiFlow.read(onboarded, "$.roomTypes[0].id");
        assertThat(api.book(propertyId, roomTypeId, checkIn, checkOut)).hasStatus(201);

        MvcTestResult second = api.book(propertyId, roomTypeId, checkIn, checkOut);

        assertThat(second).hasStatus(409);
        // "type" is left out on purpose: RFC 9457 says a missing type means "about:blank"
        assertThat(second).bodyJson().extractingPath("$.title").isEqualTo("Conflict");
        assertThat(second).bodyJson().extractingPath("$.status").isEqualTo(409);
        assertThat(second).bodyJson().extractingPath("$.detail").isNotNull();
        assertThat(second).bodyJson().extractingPath("$.error").isEqualTo("InsufficientInventoryException");
    }

    @Test
    void unknownOwnerReturns404WithTheErrorName() {
        MvcTestResult result = new ApiFlow(mvc).getOwner(UUID.randomUUID().toString());

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("EntityNotFoundException");
    }

    @Test
    void ownerIdThatIsNotAUuidReturns400() {
        assertThat(new ApiFlow(mvc).getOwner("not-a-uuid")).hasStatus(400);
    }

    @Test
    void validationErrorsAlsoHaveTheErrorProperty() {
        MvcTestResult result = new ApiFlow(mvc).post("/owners", "{\"name\": \"\"}");

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
        assertThat(result).bodyJson().extractingPath("$.error").isNotNull();
    }
}
