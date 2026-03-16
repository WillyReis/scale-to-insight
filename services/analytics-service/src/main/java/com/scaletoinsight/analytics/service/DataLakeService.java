package com.scaletoinsight.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Responsible for archiving raw events to the Data Lake (S3 / LocalStack).
 */
@Service
public class DataLakeService {

    private static final Logger log = LoggerFactory.getLogger(DataLakeService.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final S3Client s3Client;
    private final String bucketName;

    public DataLakeService(S3Client s3Client,
                           @Value("${aws.s3.datalake-bucket:sti-data-lake}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Archives a raw JSON event payload to the Data Lake.
     *
     * @param prefix    S3 prefix / folder (e.g. "raw/sales-events/")
     * @param eventId   unique event identifier used as part of the object key
     * @param payload   raw JSON string to store
     */
    public void archiveRawEvent(String prefix, String eventId, String payload) {
        try {
            String key = prefix + eventId + "_" + TS_FMT.format(LocalDateTime.now()) + ".json";
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/json")
                    .build();
            s3Client.putObject(request, RequestBody.fromString(payload));
            log.debug("Archived raw event to s3://{}/{}", bucketName, key);
        } catch (Exception e) {
            log.warn("Failed to archive event {} to Data Lake: {}", eventId, e.getMessage());
        }
    }
}
