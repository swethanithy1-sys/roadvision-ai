package com.roadvision.storage;

/**
 * Storage abstraction. {@link LocalFileStorageServiceImpl} writes to disk today; an
 * S3/Blob-backed implementation can be swapped in later via a Spring profile without
 * changing callers — they only ever deal in the relative path this returns.
 */
public interface FileStorageService {

    /**
     * @return the relative path (e.g. "reports/&lt;uuid&gt;.jpg") the file was stored under,
     * to be persisted and later resolved against the public /uploads/** URL.
     */
    String store(byte[] content, String originalFilename, String subDirectory);
}
