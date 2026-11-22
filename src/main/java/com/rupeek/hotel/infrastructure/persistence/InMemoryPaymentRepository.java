package com.rupeek.hotel.infrastructure.persistence;

import com.rupeek.hotel.domain.model.Payment;
import com.rupeek.hotel.domain.repository.PaymentRepository;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.PaymentId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<PaymentId, Payment> store = new ConcurrentHashMap<>();

    @Override
    public void save(Payment payment) {
        store.put(payment.id(), payment);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(IdempotencyKey key) {
        return store.values().stream().filter(payment -> payment.idempotencyKey().equals(key)).findFirst();
    }
}
