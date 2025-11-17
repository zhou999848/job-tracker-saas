// src/test/java/.../container/MinioTCBase.java
package com.example.jobtracker.container;

import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MinioTCBase {

    protected static final String ACCESS_KEY = "minioadmin";
    protected static final String SECRET_KEY = "minioadmin";
    protected static final String BUCKET = "jobtracker";

    protected static final GenericContainer<?> MINIO = new GenericContainer<>(
            DockerImageName.parse("minio/minio:latest"))
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server /data --console-address :9001")
            .withExposedPorts(9000, 9001);

    static {
        MINIO.start();
        // 创建 bucket（用 AWS SDK v2）
        String endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            try {
                s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
            } catch (Exception ignore) {}
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        String endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        r.add("file.s3.endpoint", () -> endpoint);
        r.add("file.s3.region", () -> "us-east-1");
        r.add("file.s3.accessKey", () -> ACCESS_KEY);
        r.add("file.s3.secretKey", () -> SECRET_KEY);
        r.add("file.s3.bucket", () -> BUCKET);
        r.add("file.s3.pathStyle", () -> true);
    }
}
