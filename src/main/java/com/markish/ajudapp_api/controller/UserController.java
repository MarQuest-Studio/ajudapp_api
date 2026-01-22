package com.markish.ajudapp_api.controller;

import com.markish.ajudapp_api.model.UserResponse;
import com.markish.ajudapp_api.model.UserSettingsUpdateRequest;
import com.markish.ajudapp_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;


    @Operation(summary = "Finds or creates user", description = "Returns information about the user who called it if it existed, or creates if it didn't")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "400", description = "Bad request body")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/me")
    public UserResponse me() {
        return userService.getOrCreateCurrentUser();
    }

    @Operation(summary = "Updates user settings", description = "Updates some user settings")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "400", description = "Bad request body")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/settings")
    public UserResponse update(@RequestBody UserSettingsUpdateRequest request) {
        return userService.updateSettings(request);
    }

    @Operation(summary = "Delete user", description = "Anonymizes a user and deletes its profile picture")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "User not deleted, error deleting profile picture")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe() {
        userService.anonymizeCurrentUser();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload profile picture", description = "Uploads a given profile picture for the user who called it")
    @ApiResponse(responseCode = "200", description = "Uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Upload failed due to unknown error")
    @PostMapping(path = "/me/upload-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadProfilePicture(
            @Parameter(description = "Profile picture image", required = true)
            @RequestPart MultipartFile file
    ) throws IOException {
        return userService.uploadProfilePicture(file);
    }
}
