package com.rupeek.hotel.domain.search.filter;

import com.rupeek.hotel.domain.search.SearchCandidate;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.search.SearchFilter;

/** Checks the base price per night against the price range of the search. */
public final class PriceFilter implements SearchFilter {

    @Override
    public boolean accepts(SearchCandidate candidate, SearchCriteria criteria) {
        return criteria.priceRange().contains(candidate.roomType().basePricePerNight());
    }
}
