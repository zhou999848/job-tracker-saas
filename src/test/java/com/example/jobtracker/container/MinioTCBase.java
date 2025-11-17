
package com.example.jobtracker.container;

import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MinioTCBase {

    protected static final String ACCESS_KEY = "minioadmin";
    protected static final String SECRET_KEY = "minioadmin";
    protected static final String BUCKET = "jobtracker";

    @Container   // ⭐⭐ 加上这一行
    protected static final GenericContainer<?> MINIO = new GenericContainer<>(
            DockerImageName.parse("minio/minio:latest"))
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server /data --console-address :9001")
            .withExposedPorts(9000, 9001);  // ⭐⭐ 删除 static { MINIO.start() }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("file.s3.endpoint", () ->
                "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        r.add("file.s3.region", () -> "us-east-1");
        r.add("file.s3.accessKey", () -> ACCESS_KEY);
        r.add("file.s3.secretKey", () -> SECRET_KEY);
        r.add("file.s3.bucket", () -> BUCKET);
        r.add("file.s3.pathStyle", () -> true);
    }
}

