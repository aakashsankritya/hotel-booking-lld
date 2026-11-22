package com.rupeek.hotel.api;

import com.rupeek.hotel.api.dto.owner.OwnerResponse;
import com.rupeek.hotel.api.dto.owner.RegisterOwnerRequest;
import com.rupeek.hotel.application.property.PropertyService;
import com.rupeek.hotel.domain.value.OwnerId;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/owners")
public class OwnerController {

    private final PropertyService properties;

    public OwnerController(PropertyService properties) {
        this.properties = properties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // documents 201; ResponseEntity.created(...) still sets Location
    public ResponseEntity<OwnerResponse> register(@RequestBody @Valid RegisterOwnerRequest request) {
        OwnerResponse owner = OwnerResponse.from(properties.registerOwner(request.name(), request.email()));
        return ResponseEntity.created(URI.create("/owners/" + owner.id())).body(owner);
    }

    @GetMapping("/{id}")
    public OwnerResponse get(@PathVariable String id) {
        return OwnerResponse.from(properties.getOwner(OwnerId.of(id)));
    }
}
