package com.rupeek.hotel.domain.search;

import com.rupeek.hotel.domain.pricing.PricingStrategy;
import com.rupeek.hotel.domain.repository.PropertyRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Finds properties that match the criteria.
 * Every filter must accept a room type for it to be offered. A property with no accepted room type is dropped.
 * Results have no fixed order.
 */
public final class PropertySearch {

    private final PropertyRepository properties;
    private final List<SearchFilter> filters;
    private final PricingStrategy pricing;

    public PropertySearch(PropertyRepository properties, List<SearchFilter> filters, PricingStrategy pricing) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.filters = List.copyOf(filters);
        this.pricing = Objects.requireNonNull(pricing, "pricing");
    }

    public List<PropertySearchResult> search(SearchCriteria criteria) {
        return properties.findByCity(criteria.city()).stream()
                .flatMap(property -> property.roomTypes().stream().map(room -> new SearchCandidate(property, room)))
                .filter(candidate -> filters.stream().allMatch(filter -> filter.accepts(candidate, criteria)))
                .collect(Collectors.groupingBy(
                        SearchCandidate::property,
                        LinkedHashMap::new,
                        Collectors.mapping(candidate -> offer(candidate, criteria), Collectors.toList())))
                .entrySet().stream()
                .map(entry -> new PropertySearchResult(entry.getKey(), entry.getValue()))
                .toList();
    }

    private RoomOffer offer(SearchCandidate candidate, SearchCriteria criteria) {
        return new RoomOffer(candidate.roomType(), pricing.priceFor(candidate.roomType(), criteria.stay()));
    }
}
