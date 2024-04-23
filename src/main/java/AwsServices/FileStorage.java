package AwsServices;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;

/**
 * Singleton class that handles all S3 operations.
 */
public class FileStorage {
    private static FileStorage instance;
    private final AmazonS3 s3;

    private FileStorage() {
        s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
    }

    /**
     * Gets the instance of the FileStorage.
     * @return the instance.
     */
    public static FileStorage getInstance() {
        if (instance == null) {
            instance = new FileStorage();
        }

        return instance;
    }

    /**
     * Uploads a file to the S3 bucket.
     * @param encodedString the encoded representation of the file.
     * @param fileExtension the file extension.
     * @param email the user's email.
     * @param album the photo album.
     * @param uuid the photo id.
     * @return the key to retrieve the file from S3.
     */
    public String uploadFile(String encodedString, String fileExtension, String email, String album, String uuid) {
        String fileKey = buildFileKey(email, album, uuid, fileExtension);

        System.out.println("---file key:" + fileKey);
        byte[] bytes = Base64.getDecoder().decode(encodedString);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.setContentType(getContentType(fileExtension));

        PutObjectRequest putObjectRequest = new PutObjectRequest(Constants.BUCKET_NAME, fileKey, new ByteArrayInputStream(bytes), metadata);
        s3.putObject(putObjectRequest);

        return fileKey;
    }

    /**
     * Downloads a file from the S3 bucket.
     * @param fileKey The name of the file.
     * @return A base64 string representation of the file.
     * @throws IOException If an I/O exception occurs when reading bytes.
     */
    public String downloadFile(String fileKey) throws IOException {
        if (fileKey == null || !fileExists(fileKey)) {
            return "";
        }

        S3Object file = s3.getObject(Constants.BUCKET_NAME, fileKey);
        byte[] bytes = file.getObjectContent().readAllBytes();

        String extension = fileKey.substring(fileKey.lastIndexOf('.'));
        String mimeType = "data:" + getContentType(extension) + ";base64,";

        return mimeType + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Relocates a file from one location to another.
     * @param oldFileKey the file key being updated.
     * @param email the user's email.
     * @param album the new album.
     * @param id the photo id.
     * @return the new file key.
     */
    public String moveFile(String oldFileKey, String email, String album, String id) {
        String extension = oldFileKey.substring(oldFileKey.lastIndexOf('.'));
        String newFileKey = buildFileKey(email, album, id, extension);

        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(Constants.BUCKET_NAME, oldFileKey, Constants.BUCKET_NAME, newFileKey);
        s3.copyObject(copyObjectRequest);

        deleteFile(oldFileKey);

        return newFileKey;
    }

    public void deleteFile(String fileKey) throws SdkClientException, AmazonServiceException {
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(Constants.BUCKET_NAME, fileKey);

        s3.deleteObject(deleteObjectRequest);
    }

    public String createPresignedUrl(String fileKey) {
        if (fileKey.isEmpty()) {
            return fileKey;
        }

        try (S3Presigner presigner = S3Presigner.create()) {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(Constants.BUCKET_NAME)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofDays(7))
                    .getObjectRequest(request)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(presignRequest);

            return presignedGetObjectRequest.url().toExternalForm();
        }
    }

    private String buildFileKey(String email, String album, String uuid, String fileExtension) {
        album = album.replace("%20", " ");
        return email + "/" + album + "/" + uuid + fileExtension;
    }

    private boolean fileExists(String fileKey) {
        return s3.doesObjectExist(Constants.BUCKET_NAME, fileKey);
    }

    private String getContentType(String fileExtension) {
        return switch (fileExtension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".apng" -> "image/apng";
            case ".avif" -> "image/avif";
            case ".gif" -> "image/gif";
            case ".svg" -> "image/svg+xml";
            case ".mp3" -> "audio/mpeg";
            case ".m4a" -> "audio/mp4";
            case ".wav" -> "audio/wav";
            case ".aac" -> "audio/aac";
            case ".oga" -> "audio/ogg";
            default -> throw new IllegalArgumentException("Unsupported file extension: " + fileExtension);
        };
    }
}
