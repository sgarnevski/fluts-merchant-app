package com.fluts.app.web;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The logged-in user.
 *
 * @param username Keycloak user name
 * @param roles    the app roles of the user ({@code admin}, {@code editor}, {@code viewer})
 * @param merchant the user's merchant; absent for users without one, e.g. admins
 */
public record MeResponse(String username, List<String> roles, @Nullable MerchantResponse merchant) {
}
