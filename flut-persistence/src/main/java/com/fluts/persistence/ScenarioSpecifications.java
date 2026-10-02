package com.fluts.persistence;

import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/** Filters for listing scenarios; a {@code null} filter value matches everything (e.g. {@code merchantCode} null = all merchants). */
public final class ScenarioSpecifications {

    private ScenarioSpecifications() {
    }

    public static Specification<ScenarioEntity> matching(final @Nullable String merchantCode,
            final @Nullable ScenarioInputType inputType, final @Nullable String nameContains) {
        return Specification.allOf(belongsTo(merchantCode), hasInputType(inputType), nameContains(nameContains));
    }

    private static Specification<ScenarioEntity> belongsTo(final @Nullable String merchantCode) {
        return (root, query, builder) -> merchantCode == null ? null : builder.equal(root.get("merchantCode"), merchantCode);
    }

    private static Specification<ScenarioEntity> hasInputType(final @Nullable ScenarioInputType inputType) {
        return (root, query, builder) -> inputType == null ? null : builder.equal(root.get("inputType"), inputType);
    }

    private static Specification<ScenarioEntity> nameContains(final @Nullable String text) {
        return (root, query, builder) -> text == null || text.isBlank()
                ? null
                : builder.like(builder.lower(root.get("name")), "%" + escape(text.strip().toLowerCase(Locale.ROOT)) + "%", '\\');
    }

    private static String escape(final String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
