package com.roadvision.storage;

import com.roadvision.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Uploads report photos to a Supabase Storage bucket instead of local disk — needed on hosts
 * like Render's free tier with no persistent disk, where a local file would vanish on redeploy.
 * The bucket must be public; uploads go through Supabase's Storage REST API using the project's
 * secret key (full access, bypasses RLS — see Supabase's new API key system).
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseStorageServiceImpl implements FileStorageService {

    private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif"
    );

    private final RestClient supabaseRestClient;
    private final String secretKey;
    private final String bucket;
    private final String publicBaseUrl;

    public SupabaseStorageServiceImpl(
            RestClient supabaseRestClient,
            @Value("${app.supabase.secret-key}") String secretKey,
            @Value("${app.storage.supabase.bucket}") String bucket,
            @Value("${app.supabase.project-url}") String projectUrl
    ) {
        this.supabaseRestClient = supabaseRestClient;
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.publicBaseUrl = projectUrl + "/storage/v1/object/public/" + bucket;
    }

    @Override
    public String store(byte[] content, String originalFilename, String subDirectory) {
        if (content == null || content.length == 0) {
            throw new BadRequestException("An image file is required");
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension != null ? "." + extension : "");
        String objectPath = subDirectory + "/" + storedFilename;

        supabaseRestClient.post()
                .uri("/object/{bucket}/{path}", bucket, objectPath)
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
                .contentType(resolveContentType(extension))
                .body(content)
                .retrieve()
                .toBodilessEntity();

        return publicBaseUrl + "/" + objectPath;
    }

    private MediaType resolveContentType(String extension) {
        String key = extension != null ? extension.toLowerCase(Locale.ROOT) : "";
        return MediaType.parseMediaType(CONTENT_TYPES_BY_EXTENSION.getOrDefault(key, MediaType.APPLICATION_OCTET_STREAM_VALUE));
    }
}
