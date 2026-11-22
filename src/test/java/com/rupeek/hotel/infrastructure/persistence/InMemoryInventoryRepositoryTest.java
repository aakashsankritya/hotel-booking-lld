package com.rupeek.hotel.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.domain.model.RoomInventory;
import com.rupeek.hotel.domain.value.InventoryKey;
import com.rupeek.hotel.domain.value.RoomTypeId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class InMemoryInventoryRepositoryTest {

    private final InMemoryInventoryRepository repository = new InMemoryInventoryRepository();
    private final InventoryKey key = new InventoryKey(RoomTypeId.newId(), LocalDate.of(2026, 12, 10));

    @Test
    void findIsEmptyUntilInventoryIsOpened() {
        assertThat(repository.find(key)).isEmpty();
    }

    @Test
    void getOrOpenOpensWithTotalUnitsAndKeepsTheFirstOne() {
        RoomInventory first = repository.getOrOpen(key, 5);
        RoomInventory second = repository.getOrOpen(key, 99);

        assertThat(second).isSameAs(first);
        assertThat(first.available()).isEqualTo(5);
        assertThat(repository.find(key)).containsSame(first);
    }

    @Test
    void concurrentCallsForOneKeyGetTheSameInstance() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<RoomInventory>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(executor.submit(() -> repository.getOrOpen(key, 5)));
            }
            RoomInventory expected = futures.get(0).get();
            for (Future<RoomInventory> future : futures) {
                assertThat(future.get()).isSameAs(expected);
            }
        }
    }
}
