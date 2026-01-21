package com.markish.ajudapp_api.service;

import com.markish.ajudapp_api.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {
    String upload(String key, MultipartFile file);
    void delete(String key);
    String getProfileUrl(User user);
}
