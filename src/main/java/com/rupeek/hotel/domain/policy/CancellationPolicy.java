package com.rupeek.hotel.domain.policy;

import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.RefundQuote;
import java.time.Instant;

/** Decides how much money a guest gets back when a booking is cancelled. */
@FunctionalInterface
public interface CancellationPolicy {

    RefundQuote quote(Money amountPaid, DateRange stay, Instant cancelledAt);
}
