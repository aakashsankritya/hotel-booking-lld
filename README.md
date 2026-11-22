# Hotel Booking System

Rupeek SDE-3 machine coding round (Question A).

## Requirements
- Java 25
- No local Gradle needed (Gradle wrapper 9.9.2 included)

## Build and run
```bash
./gradlew clean build
./gradlew bootRun
```
The service starts on `http://localhost:8080`.

## Design decisions
- **In-memory repositories keep object references.** Entities are mutable aggregates, so the services serialise changes: one lock per `BookingId` for booking and payment changes, and one lock per room type for inventory (`RoomInventory` itself is not thread-safe, by design). `Booking.version` is a placeholder for optimistic locking when a real database replaces the in-memory repositories.

## Assumptions
- Request DTOs are validated with Bean Validation (`@NotNull`, `@Valid`). A null that reaches the domain is a bug and maps to 500.

## With more time
_TBD_
