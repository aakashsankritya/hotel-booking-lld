package com.rupeek.hotel.application.booking;

import com.rupeek.hotel.application.common.KeyedLocks;
import com.rupeek.hotel.application.config.BookingProperties;
import com.rupeek.hotel.application.inventory.InventoryService;
import com.rupeek.hotel.domain.exception.CapacityExceededException;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.pricing.PricingStrategy;
import com.rupeek.hotel.domain.repository.BookingRepository;
import com.rupeek.hotel.domain.repository.PropertyRepository;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.Money;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final InventoryService inventory;
    private final PricingStrategy pricing;
    private final KeyedLocks<BookingId> bookingLocks;
    private final Clock clock;
    private final BookingProperties settings;

    public BookingService(PropertyRepository properties, BookingRepository bookings, InventoryService inventory,
            PricingStrategy pricing, KeyedLocks<BookingId> bookingLocks, Clock clock, BookingProperties settings) {
        this.properties = properties;
        this.bookings = bookings;
        this.inventory = inventory;
        this.pricing = pricing;
        this.bookingLocks = bookingLocks;
        this.clock = clock;
        this.settings = settings;
    }

    /** Holds the room for {@code holdTtl} and waits for payment. */
    public Booking book(BookCommand command) {
        Clock now = Clock.fixed(clock.instant(), clock.getZone()); // one instant for the whole use case
        Property property = properties.getById(command.propertyId());
        RoomType roomType = property.roomType(command.roomTypeId());
        LocalDate today = LocalDate.now(now);
        if (command.stay().startsBefore(today)) {
            throw new DomainValidationException(
                    "check-in " + command.stay().checkIn() + " is before today " + today);
        }
        if (!roomType.canHost(command.guests())) {
            throw new CapacityExceededException(command.guests().value(), roomType.capacity());
        }
        Money amount = pricing.priceFor(roomType, command.stay());
        // Create the booking before the hold. It is the last step that can throw, so nothing needs undoing.
        Booking booking = Booking.hold(property.id(), roomType.id(), command.guest(), command.stay(),
                command.guests(), amount, now, settings.holdTtl());
        inventory.hold(roomType, command.stay());
        bookings.save(booking);
        return booking;
    }

    public Booking get(BookingId id) {
        return bookings.getById(id);
    }

    /** Expires every hold that has ended and frees its rooms. Returns the number of bookings expired. */
    public int expireHolds() {
        Clock now = Clock.fixed(clock.instant(), clock.getZone());
        int expired = 0;
        for (Booking candidate : bookings.findExpiredHolds(now.instant())) {
            if (bookingLocks.withLock(candidate.id(), () -> expire(candidate.id(), now))) {
                expired++;
            }
        }
        return expired;
    }

    /** Runs under the booking lock. The booking may have been paid since the sweep listed it, so check again. */
    private boolean expire(BookingId id, Clock now) {
        Booking booking = bookings.getById(id);
        if (booking.status() != BookingStatus.PENDING_PAYMENT || !booking.isHoldExpired(now.instant())) {
            return false;
        }
        booking.expire(now);
        inventory.release(booking.roomTypeId(), booking.stay());
        bookings.save(booking);
        return true;
    }
}
