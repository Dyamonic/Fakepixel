package club.abelix.commons.language.download;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class S3LocalizationDownloadService implements LocalizationDownloadService {

    private static final String KEY_FORMAT = "localization/%s/%s/localization.zip";
    private static final String DEFAULT_BRANCH_NAME = Optional.ofNullable(System.getenv("BRANCH"))
            .orElse("main");
    private static final String NAMESPACE = Optional.ofNullable(System.getenv("NAMESPACE"))
            .orElseThrow(() -> new RuntimeException("NAMESPACE environment variable not set"));
    private static final String BUCKET_NAME = System.getenv("BUCKET_NAME");
    private static final String S3_ENDPOINT = System.getenv("AWS_ENDPOINT_URL");
    private static final String S3_REGION = System.getenv("AWS_REGION");

    S3Client s3Client;

    public static S3LocalizationDownloadService create() {
        val s3ClientBuilder = S3Client.builder()
                .endpointOverride(URI.create(S3_ENDPOINT))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(S3_REGION));

        try {
            val s3Client = s3ClientBuilder.build();

            log.info("Successfully created S3 client for localization download service");
            log.info("Using bucket: {}, namespace: {}, branch: {}", BUCKET_NAME, NAMESPACE, DEFAULT_BRANCH_NAME);

            return new S3LocalizationDownloadService(s3Client);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create S3 client", exception);
        }
    }

    @Override
    public InputStream download() {
        String branch = DEFAULT_BRANCH_NAME;
        String key = KEY_FORMAT.formatted(NAMESPACE, branch);

        if (notExists(key)) {
            log.info("Key not found for branch '{}', fallback to 'main'", branch);

            branch = "main";
            key = KEY_FORMAT.formatted(NAMESPACE, branch);

            if (notExists(key)) {
                throw new IllegalStateException("Localization ZIP not found for branches: "
                        + DEFAULT_BRANCH_NAME + ", main");
            }
        }

        log.info("Downloading from s3://{}/{}", BUCKET_NAME, key);

        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build());
    }

    private boolean notExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build());
            return false;
        } catch (NoSuchKeyException e) {
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return true;
            throw e;
        }
    }
}
