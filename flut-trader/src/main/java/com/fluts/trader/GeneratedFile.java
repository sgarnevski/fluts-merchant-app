package com.fluts.trader;

/**
 * A scenario file to deliver.
 *
 * @param name    file name, {@code .txt} (original format) or {@code .json} (normalized format)
 * @param content the file's text
 * @param signature the merchant's signature of the content, delivered as {@code <name>.sig}
 */
public record GeneratedFile(String name, String content, String signature) {

    public static final String SIGNATURE_SUFFIX = ".sig";

    public String signatureName() {
        return name + SIGNATURE_SUFFIX;
    }
}
