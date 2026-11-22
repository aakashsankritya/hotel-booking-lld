package com.rupeek.hotel.application;

import com.rupeek.hotel.application.booking.BookCommand;
import com.rupeek.hotel.application.booking.BookingService;
import com.rupeek.hotel.application.cancellation.CancellationService;
import com.rupeek.hotel.application.common.KeyedLocks;
import com.rupeek.hotel.application.config.BookingProperties;
import com.rupeek.hotel.application.config.DomainConfig;
import com.rupeek.hotel.application.inventory.InventoryService;
import com.rupeek.hotel.application.payment.PaymentService;
import com.rupeek.hotel.application.property.OnboardPropertyCommand;
import com.rupeek.hotel.application.property.OnboardPropertyCommand.RoomTypeSpec;
import com.rupeek.hotel.application.property.PropertyService;
import com.rupeek.hotel.domain.model.Owner;
import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.model.RoomInventory;
import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.payment.PaymentProcessorRegistry;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.pricing.NightlyPricingStrategy;
import com.rupeek.hotel.domain.search.PropertySearch;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.CardDetails;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.GuestDetails;
import com.rupeek.hotel.domain.value.InventoryKey;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.StarRating;
import com.rupeek.hotel.domain.value.UpiDetails;
import com.rupeek.hotel.infrastructure.payment.CardPaymentProcessor;
import com.rupeek.hotel.infrastructure.payment.MockPaymentGateway;
import com.rupeek.hotel.infrastructure.payment.UpiPaymentProcessor;
import com.rupeek.hotel.infrastructure.payment.WalletPaymentProcessor;
import com.rupeek.hotel.infrastructure.persistence.InMemoryBookingRepository;
import com.rupeek.hotel.infrastructure.persistence.InMemoryInventoryRepository;
import com.rupeek.hotel.infrastructure.persistence.InMemoryOwnerRepository;
import com.rupeek.hotel.infrastructure.persistence.InMemoryPaymentRepository;
import com.rupeek.hotel.infrastructure.persistence.InMemoryPropertyRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Wires every service by hand with in-memory repositories (no Spring). The clock starts on 2026-12-01 06:00 UTC.
 * One property in Goa with one room type: capacity 2, price 3000 INR per night.
 */
public final class Fixture {

    public static final Duration HOLD_TTL = Duration.ofMinutes(10);
    public static final Money NIGHT_PRICE = Money.of("3000", "INR");

    public final MutableClock clock = new MutableClock(Instant.parse("2026-12-01T06:00:00Z"), ZoneOffset.UTC);
    public final InMemoryOwnerRepository owners = new InMemoryOwnerRepository();
    public final InMemoryPropertyRepository properties = new InMemoryPropertyRepository();
    public final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    public final InMemoryPaymentRepository payments = new InMemoryPaymentRepository();
    public final InMemoryInventoryRepository inventories = new InMemoryInventoryRepository();
    public final CountingGateway gateway = new CountingGateway(new MockPaymentGateway());

    public final InventoryService inventory = new InventoryService(inventories);
    public final PropertyService propertyService = new PropertyService(owners, properties);
    public final BookingService bookingService;
    public final PaymentService paymentService;
    public final CancellationService cancellationService;
    public final PropertySearch search;

    public final Owner owner;
    public final Property property;
    public final RoomType room;

    public Fixture(CancellationPolicyType policyType, int units) {
        BookingProperties settings = new BookingProperties(HOLD_TTL, Duration.ofSeconds(30), ZoneOffset.UTC);
        DomainConfig config = new DomainConfig();
        KeyedLocks<BookingId> bookingLocks = config.bookingLocks();
        NightlyPricingStrategy pricing = new NightlyPricingStrategy();
        PaymentProcessorRegistry processors = new PaymentProcessorRegistry(List.of(
                new CardPaymentProcessor(gateway, clock), new UpiPaymentProcessor(gateway),
                new WalletPaymentProcessor(gateway)));

        bookingService = new BookingService(properties, bookings, inventory, pricing, bookingLocks, clock, settings);
        paymentService = new PaymentService(bookings, payments, inventory, processors, bookingLocks, clock);
        cancellationService = new CancellationService(bookings, payments, properties, inventory,
                config.cancellationPolicyResolver(settings), processors, bookingLocks, clock);
        search = config.propertySearch(properties, inventories, pricing);

        owner = propertyService.registerOwner("Asha", "asha@example.com");
        property = propertyService.onboard(new OnboardPropertyCommand(owner.id(), "Sea View",
                new Location("Goa", "Baga"), new StarRating(4), Set.of(Amenity.WIFI), policyType,
                List.of(new RoomTypeSpec("Deluxe", 2, units, NIGHT_PRICE))));
        room = property.roomTypes().get(0);
    }

    public static DateRange stay(int fromDay, int toDay) {
        return new DateRange(LocalDate.of(2026, 12, fromDay), LocalDate.of(2026, 12, toDay));
    }

    public BookCommand bookCommand(DateRange stay) {
        return new BookCommand(property.id(), room.id(), new GuestDetails("Ravi", "ravi@example.com"), stay,
                new GuestCount(2));
    }

    public static CardDetails goodCard() {
        return new CardDetails("4242", YearMonth.of(2030, 1), "Ravi");
    }

    public static CardDetails decliningCard() {
        return new CardDetails("0000", YearMonth.of(2030, 1), "Ravi");
    }

    public static UpiDetails upi() {
        return new UpiDetails("ravi@okbank");
    }

    /** Units held on one night. A night nobody touched counts as 0. */
    public int held(LocalDate night) {
        return inventories.find(new InventoryKey(room.id(), night)).map(RoomInventory::heldUnits).orElse(0);
    }
}
