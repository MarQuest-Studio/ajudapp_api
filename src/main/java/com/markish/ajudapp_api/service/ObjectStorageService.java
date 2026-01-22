package com.markish.ajudapp_api.service;

import com.markish.ajudapp_api.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ObjectStorageService {
    String upload(String key, MultipartFile file) throws IOException;
    void delete(String key);
    String getProfileUrl(User user);
}
