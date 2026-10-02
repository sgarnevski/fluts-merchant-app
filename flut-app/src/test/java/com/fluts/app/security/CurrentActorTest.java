package com.fluts.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CurrentActorTest {

    private final CurrentActor currentActor = new CurrentActor();

    @Test
    void aPrincipalWithoutClaimsHasNoMerchant() {
        final Actor actor = currentActor.of(new UsernamePasswordAuthenticationToken("local", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + Roles.EDITOR))));

        assertThat(actor).isEqualTo(new Actor("local", null, false));
        assertThatThrownBy(actor::scope).isInstanceOf(NoMerchantException.class)
                .hasMessage("User 'local' is not assigned to a merchant");
    }

    @Test
    void anAdminSeesEveryMerchant() {
        final Actor actor = currentActor.of(new UsernamePasswordAuthenticationToken("root", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + Roles.ADMIN))));

        assertThat(actor.admin()).isTrue();
        assertThat(actor.scope()).isEqualTo(ScenarioScope.ALL);
        assertThat(actor.scope().merchant()).isEmpty();
    }
}
