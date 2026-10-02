package com.fluts.app.security;

import java.util.List;

/** The app's realm roles in Keycloak. */
public final class Roles {

    /** May read the scenarios of their own merchant. */
    public static final String VIEWER = "viewer";

    /** May also create, change, delete, import and evaluate (which stores) scenarios of their own merchant. */
    public static final String EDITOR = "editor";

    /** May read the scenarios of every merchant; changes nothing. */
    public static final String ADMIN = "admin";

    public static final List<String> ALL = List.of(ADMIN, EDITOR, VIEWER);

    private Roles() {
    }
}
