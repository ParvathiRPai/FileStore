package com.example.pava.filestore.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class S3StorageService implements IStorageService {

    private final String bucketName = "pava-hw";


    @Autowired
    public S3StorageService() {
    }

    @Override
    public void store(MultipartFile file, String username) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                throw new StorageException(
                        "Cannot store file outside current directory "
                                + filename);
            }

            putInS3Bucket(bucketName, username + "/" + filename, file);
        } catch (Exception e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    private String putInS3Bucket(String bucketName, String filename, MultipartFile multipartFile) {
        Region region = Region.US_WEST_2;
        S3Client s3Client = S3Client.builder().region(region).build();

        try (InputStream inputStream = multipartFile.getInputStream()) {
            //Put a file into the bucket
            PutObjectResponse response = s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(filename)
                            .build(),
                    RequestBody.fromInputStream(inputStream, multipartFile.getSize()));

            return response.eTag();
        } catch (S3Exception | IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "";
    }

    @Override
    public Stream<String> loadAll(String username) {
        var s3Objects = getAllInS3Bucket(bucketName, username + "/");
        var paths = s3Objects
                .stream()
                .map(s3Object -> s3Object.key())
                .collect(Collectors.toList());

        return paths.stream();
    }

    private List<S3Object> getAllInS3Bucket(String bucketName, String prefix) {
        try {
            Region region = Region.US_WEST_2;
            S3Client s3Client = S3Client.builder().region(region).build();

            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .prefix(prefix)
                    .bucket(bucketName)
                    .build();

            ListObjectsResponse res = s3Client.listObjects(listObjects);
            List<S3Object> objects = res.contents();

            return objects;
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
            return new ArrayList<S3Object>();
        }
    }

    @Override
    public ResponseInputStream<GetObjectResponse> loadAsResource(String filename) {
        Region region = Region.US_WEST_2;
        S3Client s3Client = S3Client.builder().region(region).build();
        var getObjectRequest = GetObjectRequest
                .builder()
                .bucket(bucketName)
                .key(filename)
                .build();
        var response = s3Client.getObject(getObjectRequest,
                ResponseTransformer.toInputStream());
        return response;
    }

    @Override
    public DeleteObjectResponse deleteFile(String filename) {
        Region region = Region.US_WEST_2;
        S3Client s3Client = S3Client.builder().region(region).build();
        var getObjectRequest = GetObjectRequest
                .builder()
                .bucket(bucketName)
                .key(filename)
                .build();

        var deleteObjectRequest = DeleteObjectRequest
                .builder()
                .bucket(bucketName)
                .key(filename)
                .build();
        var response = s3Client.deleteObject(deleteObjectRequest);
        return response;
    }
}