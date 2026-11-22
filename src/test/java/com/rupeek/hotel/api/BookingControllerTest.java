package com.rupeek.hotel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rupeek.hotel.application.booking.BookCommand;
import com.rupeek.hotel.application.booking.BookingService;
import com.rupeek.hotel.application.cancellation.CancellationService;
import com.rupeek.hotel.application.payment.PaymentOutcome;
import com.rupeek.hotel.application.payment.PaymentService;
import com.rupeek.hotel.domain.exception.IdempotencyKeyConflictException;
import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.exception.InsufficientInventoryException;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.model.Payment;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.GuestDetails;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PaymentInstrument;
import com.rupeek.hotel.domain.value.PropertyId;
import com.rupeek.hotel.domain.value.RoomTypeId;
import com.rupeek.hotel.domain.value.UpiDetails;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-01T06:00:00Z"), ZoneOffset.UTC);
    private static final String BOOK_JSON = """
            {"propertyId": "%s", "roomTypeId": "%s", "guestName": "Ravi", "guestEmail": "ravi@example.com",
             "checkIn": "2026-12-10", "checkOut": "2026-12-12", "guests": 2}
            """;
    private static final String UPI_PAYMENT = """
            {"method": "UPI", "upi": {"vpa": "ravi@okbank"}}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BookingService bookings;
    @MockitoBean
    private PaymentService payments;
    @MockitoBean
    private CancellationService cancellations;

    private final Booking booking = Booking.hold(PropertyId.newId(), RoomTypeId.newId(),
            new GuestDetails("Ravi", "ravi@example.com"),
            new DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), new GuestCount(2),
            Money.of("6000", "INR"), CLOCK, Duration.ofMinutes(10));

    private MvcTestResult post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult postWithKey(String uri, String body, String idempotencyKey) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", idempotencyKey)
                .content(body).exchange();
    }

    private String bookJson() {
        return BOOK_JSON.formatted(PropertyId.newId().value(), RoomTypeId.newId().value());
    }

    @Test
    void bookReturns201WithLocationAndTheBooking() {
        when(bookings.book(any(BookCommand.class))).thenReturn(booking);

        MvcTestResult result = post("/bookings", bookJson());

        assertThat(result).hasStatus(201).headers().hasValue("Location", "/bookings/" + booking.id().value());
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("PENDING_PAYMENT");
        assertThat(result).bodyJson().extractingPath("$.nights").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.amount.currency").isEqualTo("INR");
    }

    @Test
    void bookWithoutCheckInReturns400AsAProblem() {
        String json = """
                {"propertyId": "%s", "roomTypeId": "%s", "guestName": "Ravi", "guestEmail": "ravi@example.com",
                 "checkOut": "2026-12-12", "guests": 2}
                """.formatted(PropertyId.newId().value(), RoomTypeId.newId().value());

        MvcTestResult result = post("/bookings", json);

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
        verifyNoInteractions(bookings);
    }

    @Test
    void bookWithAnIdThatIsNotAUuidReturns400() {
        String json = BOOK_JSON.formatted("not-a-uuid", RoomTypeId.newId().value());

        assertThat(post("/bookings", json)).hasStatus(400);
        verifyNoInteractions(bookings);
    }

    @Test
    void soldOutRoomReturns409WithTheErrorName() {
        when(bookings.book(any(BookCommand.class))).thenThrow(
                new InsufficientInventoryException(RoomTypeId.newId(), LocalDate.of(2026, 12, 10), 1, 0));

        MvcTestResult result = post("/bookings", bookJson());

        assertThat(result).hasStatus(409);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("InsufficientInventoryException");
    }

    @Test
    void getUnknownBookingReturns404() {
        BookingId id = BookingId.newId();
        when(bookings.get(any(BookingId.class))).thenThrow(new EntityNotFoundException("Booking", id));

        MvcTestResult result = mvc.get().uri("/bookings/{id}", id.value()).exchange();

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("EntityNotFoundException");
    }

    @Test
    void getWithAnIdThatIsNotAUuidReturns400() {
        assertThat(mvc.get().uri("/bookings/not-a-uuid").exchange()).hasStatus(400);
        verifyNoInteractions(bookings);
    }

    @Test
    void payWithoutIdempotencyKeyReturns400() {
        assertThat(post("/bookings/" + booking.id().value() + "/payments", UPI_PAYMENT)).hasStatus(400);
        verifyNoInteractions(payments);
    }

    @Test
    void upiMethodWithoutUpiDetailsReturns400() {
        MvcTestResult result = postWithKey("/bookings/" + booking.id().value() + "/payments",
                "{\"method\": \"UPI\"}", "k1");

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("DomainValidationException");
        verifyNoInteractions(payments);
    }

    @Test
    void keyUsedForAnotherBookingReturns422() {
        when(payments.pay(any(BookingId.class), any(PaymentInstrument.class), any(IdempotencyKey.class)))
                .thenThrow(new IdempotencyKeyConflictException(new IdempotencyKey("k1")));

        MvcTestResult result = postWithKey("/bookings/" + booking.id().value() + "/payments", UPI_PAYMENT, "k1");

        assertThat(result).hasStatus(422);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("IdempotencyKeyConflictException");
    }

    @Test
    void declinedPaymentIsStillA200WithStatusFailed() {
        Payment payment = Payment.initiate(booking.id(), booking.amount(), new UpiDetails("ravi@okbank"),
                new IdempotencyKey("k1"), CLOCK);
        payment.markFailed("declined");
        when(payments.pay(any(BookingId.class), any(PaymentInstrument.class), any(IdempotencyKey.class)))
                .thenReturn(new PaymentOutcome(payment, BookingStatus.PAYMENT_FAILED));

        MvcTestResult result = postWithKey("/bookings/" + booking.id().value() + "/payments", UPI_PAYMENT, "k1");

        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("FAILED");
        assertThat(result).bodyJson().extractingPath("$.failureReason").isEqualTo("declined");
        assertThat(result).bodyJson().extractingPath("$.bookingStatus").isEqualTo("PAYMENT_FAILED");
        assertThat(result).bodyJson().extractingPath("$.createdAt").isNotNull();
    }
}
