package com.fluts.app.web;

import com.fluts.app.security.Actor;
import com.fluts.app.security.CurrentActor;
import com.fluts.app.security.Roles;

import com.fluts.persistence.MerchantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/me}: who is logged in and for which merchant, so the UI can show it and hide what they may not do. */
@RestController
@Tag(name = "Session")
public class MeController {

    private final CurrentActor currentActor;
    private final MerchantRepository merchants;

    public MeController(final CurrentActor currentActor, final MerchantRepository merchants) {
        this.currentActor = currentActor;
        this.merchants = merchants;
    }

    @GetMapping(path = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "The logged-in user, their roles and their merchant")
    public MeResponse me(@Parameter(hidden = true) final Authentication authentication) {
        final Actor actor = currentActor.of(authentication);
        return new MeResponse(actor.username(), Roles.ALL.stream()
                .filter(role -> authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(("ROLE_" + role)::equals))
                .toList(), merchantOf(actor));
    }

    /** A merchant the token names but the database does not know is shown by its code. */
    private @Nullable MerchantResponse merchantOf(final Actor actor) {
        final String code = actor.merchantCode();
        if (code == null) {
            return null;
        }
        return merchants.findById(code)
                .map(MerchantResponse::from)
                .orElseGet(() -> new MerchantResponse(code, code));
    }
}
