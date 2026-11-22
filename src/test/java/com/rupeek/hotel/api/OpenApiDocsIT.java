package com.rupeek.hotel.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The generated OpenAPI document lists every endpoint, including the idempotency header. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void apiDocsListTheEndpointsAndTheIdempotencyHeader() throws Exception {
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();
        assertThat(result).hasStatusOk();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("Hotel Booking API", "/bookings", "/owners/{id}", "/properties/search", "Idempotency-Key",
                "\"name\":\"city\"", "\"201\""); // search params expanded; create endpoints documented as 201
    }
}
