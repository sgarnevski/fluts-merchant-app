package com.fluts.trader;

import java.util.List;

/** Port for delivering files. */
public interface FileUploader {

    /**
     * Delivers the files; a file becomes visible under its name only once it is complete.
     *
     * @throws RuntimeException if the delivery fails (server not reachable, directory missing, ...)
     */
    void upload(List<GeneratedFile> files);
}
