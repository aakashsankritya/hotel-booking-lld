package com.rupeek.hotel.domain.search.filter;

import com.rupeek.hotel.domain.search.SearchCandidate;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.search.SearchFilter;

public final class CapacityFilter implements SearchFilter {

    @Override
    public boolean accepts(SearchCandidate candidate, SearchCriteria criteria) {
        return candidate.roomType().canHost(criteria.guests());
    }
}
