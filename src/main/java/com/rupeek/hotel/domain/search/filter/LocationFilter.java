package com.rupeek.hotel.domain.search.filter;

import com.rupeek.hotel.domain.search.SearchCandidate;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.search.SearchFilter;

public final class LocationFilter implements SearchFilter {

    @Override
    public boolean accepts(SearchCandidate candidate, SearchCriteria criteria) {
        var location = candidate.property().location();
        return location.matchesCity(criteria.city()) && location.matchesLocality(criteria.locality());
    }
}
