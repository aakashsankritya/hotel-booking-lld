package com.rupeek.hotel.domain.search;

/**
 * One rule of a search. To add a rule, add a class. {@link PropertySearch} does not change.
 */
@FunctionalInterface
public interface SearchFilter {

    boolean accepts(SearchCandidate candidate, SearchCriteria criteria);
}
