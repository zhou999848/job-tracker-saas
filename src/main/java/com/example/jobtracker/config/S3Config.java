// src/main/java/.../config/S3Config.java
package com.example.jobtracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
@Profile("!test") // 测试环境完全不装配 S3Config
@Configuration
public class S3Config {

    // S3Config.java
    @Bean
    public S3Client s3Client(
            @Value("${file.s3.endpoint}") String endpoint,
            @Value("${file.s3.region}") String region,
            @Value("${file.s3.accessKey}") String ak,
            @Value("${file.s3.secretKey}") String sk,
            @Value("${file.s3.pathStyle:true}") boolean pathStyle
    ) {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk)))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyle)   // ✅ 强制路径风格
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${file.s3.endpoint}") String endpoint,
            @Value("${file.s3.region}") String region,
            @Value("${file.s3.accessKey}") String ak,
            @Value("${file.s3.secretKey}") String sk,
            @Value("${file.s3.pathStyle:true}") boolean pathStyle
    ) {
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk)))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyle)   // ✅ Presigner 同样需要
                        .build())
                .build();
    }

}
