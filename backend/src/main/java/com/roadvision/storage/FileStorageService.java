package com.roadvision.storage;

/**
 * Storage abstraction. {@link LocalFileStorageServiceImpl} (default) writes to local disk;
 * {@link SupabaseStorageServiceImpl} (opt-in) uploads to Supabase Storage instead — useful on
 * hosts with no persistent disk (e.g. Render's free tier). Callers never construct URLs
 * themselves — {@link #store} always returns the file's final publicly-resolvable URL.
 */
public interface FileStorageService {

    /**
     * @return the publicly resolvable URL of the stored file — root-relative (resolved
     * against the backend's own origin) for local storage, or a fully-qualified absolute
     * URL for externally-hosted storage.
     */
    String store(byte[] content, String originalFilename, String subDirectory);
}
