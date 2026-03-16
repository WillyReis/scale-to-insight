package com.scaletoinsight.analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.endpoint-override:http://localhost:4566}")
    private String endpointOverride;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpointOverride))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        System.getenv().getOrDefault("AWS_ACCESS_KEY_ID", "test"),
                                        System.getenv().getOrDefault("AWS_SECRET_ACCESS_KEY", "test")
                                )
                        )
                )
                .forcePathStyle(true)
                .build();
    }
}
