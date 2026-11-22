package com.rupeek.hotel.domain.search.filter;

import com.rupeek.hotel.domain.search.SearchCandidate;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.search.SearchFilter;

public final class AmenityFilter implements SearchFilter {

    @Override
    public boolean accepts(SearchCandidate candidate, SearchCriteria criteria) {
        return candidate.property().hasAllAmenities(criteria.amenities());
    }
}
