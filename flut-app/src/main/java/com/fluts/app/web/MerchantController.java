package com.fluts.app.web;

import com.fluts.persistence.MerchantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/merchants}: the registered merchants, e.g. for the admin's filter. */
@RestController
@Tag(name = "Merchants")
public class MerchantController {

    private final MerchantRepository merchants;

    public MerchantController(final MerchantRepository merchants) {
        this.merchants = merchants;
    }

    @GetMapping(path = "/api/merchants", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "All merchants, by name")
    public List<MerchantResponse> list() {
        return merchants.findAll(Sort.by("name")).stream().map(MerchantResponse::from).toList();
    }
}
