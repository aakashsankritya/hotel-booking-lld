package com.rupeek.hotel.domain.repository;

import com.rupeek.hotel.domain.model.Payment;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import java.util.Optional;

public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findByIdempotencyKey(IdempotencyKey key);
}
