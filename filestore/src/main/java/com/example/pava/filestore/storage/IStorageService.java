package com.example.pava.filestore.storage;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface IStorageService {
    void store(MultipartFile file, String username);

    Stream<String> loadAll(String username);

    ResponseInputStream<GetObjectResponse> loadAsResource(String filename);

    DeleteObjectResponse deleteFile(String filename);
}
