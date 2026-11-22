package com.rupeek.hotel.application.config;

import com.rupeek.hotel.application.common.KeyedLocks;
import com.rupeek.hotel.domain.payment.PaymentProcessor;
import com.rupeek.hotel.domain.payment.PaymentProcessorRegistry;
import com.rupeek.hotel.domain.policy.CancellationPolicy;
import com.rupeek.hotel.domain.policy.CancellationPolicyResolver;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.policy.TieredCancellationPolicy;
import com.rupeek.hotel.domain.pricing.NightlyPricingStrategy;
import com.rupeek.hotel.domain.pricing.PricingStrategy;
import com.rupeek.hotel.domain.repository.InventoryRepository;
import com.rupeek.hotel.domain.repository.PropertyRepository;
import com.rupeek.hotel.domain.search.PropertySearch;
import com.rupeek.hotel.domain.search.filter.AmenityFilter;
import com.rupeek.hotel.domain.search.filter.AvailabilityFilter;
import com.rupeek.hotel.domain.search.filter.CapacityFilter;
import com.rupeek.hotel.domain.search.filter.LocationFilter;
import com.rupeek.hotel.domain.search.filter.PriceFilter;
import com.rupeek.hotel.domain.search.filter.StarRatingFilter;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.CancellationTier;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the domain objects, which do not use Spring themselves. */
@Configuration
@EnableConfigurationProperties(BookingProperties.class)
public class DomainConfig {

    @Bean
    public Clock clock(BookingProperties settings) {
        return Clock.system(settings.zone());
    }

    @Bean
    public PricingStrategy pricingStrategy() {
        return new NightlyPricingStrategy();
    }

    /** The one booking lock. Booking, payment and cancellation must share it. */
    @Bean
    public KeyedLocks<BookingId> bookingLocks() {
        return new KeyedLocks<>();
    }

    /** The refund table of the README. */
    @Bean
    public CancellationPolicyResolver cancellationPolicyResolver(BookingProperties settings) {
        Map<CancellationPolicyType, CancellationPolicy> policies = new EnumMap<>(CancellationPolicyType.class);
        policies.put(CancellationPolicyType.FLEXIBLE, new TieredCancellationPolicy(
                List.of(new CancellationTier(Duration.ofHours(24), 100)), settings.zone()));
        policies.put(CancellationPolicyType.MODERATE, new TieredCancellationPolicy(
                List.of(new CancellationTier(Duration.ofHours(72), 100),
                        new CancellationTier(Duration.ofHours(24), 50)), settings.zone()));
        policies.put(CancellationPolicyType.NON_REFUNDABLE, new TieredCancellationPolicy(List.of(), settings.zone()));
        return new CancellationPolicyResolver(policies);
    }

    /** The filter order is the list order. Availability is last because it reads inventory. */
    @Bean
    public PropertySearch propertySearch(PropertyRepository properties, InventoryRepository inventories,
            PricingStrategy pricing) {
        return new PropertySearch(properties, List.of(
                new LocationFilter(), new CapacityFilter(), new StarRatingFilter(), new AmenityFilter(),
                new PriceFilter(), new AvailabilityFilter(inventories)), pricing);
    }

    @Bean
    public PaymentProcessorRegistry paymentProcessorRegistry(List<PaymentProcessor> processors) {
        return new PaymentProcessorRegistry(processors);
    }
}
