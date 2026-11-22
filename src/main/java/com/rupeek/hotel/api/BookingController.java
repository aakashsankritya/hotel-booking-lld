package com.rupeek.hotel.api;

import com.rupeek.hotel.api.dto.booking.BookRequest;
import com.rupeek.hotel.api.dto.booking.BookingResponse;
import com.rupeek.hotel.api.dto.cancellation.CancellationResponse;
import com.rupeek.hotel.api.dto.payment.PaymentRequest;
import com.rupeek.hotel.api.dto.payment.PaymentResponse;
import com.rupeek.hotel.application.booking.BookingService;
import com.rupeek.hotel.application.cancellation.CancellationService;
import com.rupeek.hotel.application.payment.PaymentService;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookings;
    private final PaymentService payments;
    private final CancellationService cancellations;

    public BookingController(BookingService bookings, PaymentService payments, CancellationService cancellations) {
        this.bookings = bookings;
        this.payments = payments;
        this.cancellations = cancellations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // documents 201; ResponseEntity.created(...) still sets Location
    public ResponseEntity<BookingResponse> book(@RequestBody @Valid BookRequest request) {
        BookingResponse booking = BookingResponse.from(bookings.book(request.toCommand()));
        return ResponseEntity.created(URI.create("/bookings/" + booking.id())).body(booking);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable String id) {
        return BookingResponse.from(bookings.get(BookingId.of(id)));
    }

    /** A declined payment is still a 200. The request worked. The result is in {@code status}. */
    @PostMapping("/{id}/payments")
    public PaymentResponse pay(@PathVariable String id, @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid PaymentRequest request) {
        return PaymentResponse.from(payments.pay(BookingId.of(id), request.toInstrument(),
                new IdempotencyKey(idempotencyKey)));
    }

    @PostMapping("/{id}/cancel")
    public CancellationResponse cancel(@PathVariable String id) {
        return CancellationResponse.from(cancellations.cancel(BookingId.of(id)));
    }
}
