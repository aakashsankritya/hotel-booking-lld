package com.rupeek.hotel.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description for the generated OpenAPI document. Endpoints are read from the controllers. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI hotelBookingApi() {
        return new OpenAPI().info(new Info()
                .title("Hotel Booking API")
                .version("1.0")
                .description("Property onboarding, search, booking with a 10-minute hold, payment and cancellation."));
    }
}
