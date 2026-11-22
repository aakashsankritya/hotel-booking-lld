package com.rupeek.hotel.api;

import com.rupeek.hotel.api.dto.property.OnboardPropertyRequest;
import com.rupeek.hotel.api.dto.property.PropertyResponse;
import com.rupeek.hotel.api.dto.property.SearchRequest;
import com.rupeek.hotel.api.dto.property.SearchResultResponse;
import com.rupeek.hotel.application.property.PropertyService;
import com.rupeek.hotel.domain.search.PropertySearch;
import com.rupeek.hotel.domain.value.OwnerId;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PropertyController {

    private final PropertyService properties;
    private final PropertySearch search;

    public PropertyController(PropertyService properties, PropertySearch search) {
        this.properties = properties;
        this.search = search;
    }

    @PostMapping("/owners/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse onboard(@PathVariable String ownerId, @RequestBody @Valid OnboardPropertyRequest request) {
        return PropertyResponse.from(properties.onboard(request.toCommand(OwnerId.of(ownerId))));
    }

    @GetMapping("/properties/search")
    public List<SearchResultResponse> search(@Valid @ModelAttribute SearchRequest request) {
        return search.search(request.toCriteria()).stream().map(SearchResultResponse::from).toList();
    }
}
