package com.resumeanalyzer.api.service;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.resumeanalyzer.api.exception.InvalidFileException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class OciStorageService {

    @Value("${app.oci.namespace}")
    private String namespace;

    @Value("${app.oci.bucket-name}")
    private String bucketName;

    @Value("${app.oci.region}")
    private String region;

    @Value("${app.oci.user-ocid}")
    private String userOcid;

    @Value("${app.oci.tenancy-ocid}")
    private String tenancyOcid;

    @Value("${app.oci.fingerprint}")
    private String fingerprint;

    @Value("${app.oci.private-key}")
    private String privateKey;

    private ObjectStorage client;

    @PostConstruct
    public void init() {
        String normalizedKey = privateKey.replace("\\n", "\n");

        SimpleAuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                .tenantId(tenancyOcid)
                .userId(userOcid)
                .fingerprint(fingerprint)
                .region(Region.fromRegionId(region))
                .privateKeySupplier(() -> new ByteArrayInputStream(
                        normalizedKey.getBytes(StandardCharsets.UTF_8)))
                .build();

        client = ObjectStorageClient.builder().build(provider);
        log.info("OCI Object Storage client initialized for bucket: {}", bucketName);
    }

    public String uploadFile(String resumeId, MultipartFile file) {
        String objectKey = "resumes/" + resumeId;
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectKey)
                    .contentLength(file.getSize())
                    .contentType("application/pdf")
                    .putObjectBody(file.getInputStream())
                    .build();

            client.putObject(request);
            log.info("Uploaded resume {} to OCI bucket: {}", resumeId, bucketName);
            return objectKey;

        } catch (IOException e) {
            throw new InvalidFileException("Failed to upload file to OCI: " + e.getMessage());
        } catch (Exception e) {
            log.error("OCI upload failed for resume: {}", resumeId, e);
            throw new InvalidFileException("Failed to upload file to OCI: " + e.getMessage());
        }
    }

    public byte[] downloadFile(String objectKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectKey)
                    .build();

            GetObjectResponse response = client.getObject(request);
            byte[] bytes = response.getInputStream().readAllBytes();
            log.info("Downloaded {} bytes from OCI for key: {}", bytes.length, objectKey);
            return bytes;

        } catch (IOException e) {
            log.error("Failed to read OCI object stream: {}", objectKey, e);
            throw new InvalidFileException("Failed to read file from OCI: " + e.getMessage());
        } catch (Exception e) {
            log.error("OCI download failed for key: {}", objectKey, e);
            throw new InvalidFileException("Resume file is no longer available for download");
        }
    }

    public void deleteFile(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectKey)
                    .build();

            client.deleteObject(request);
            log.info("Deleted OCI object: {}", objectKey);

        } catch (Exception e) {
            log.warn("Could not delete OCI object: {} — {}", objectKey, e.getMessage());
        }
    }
}
