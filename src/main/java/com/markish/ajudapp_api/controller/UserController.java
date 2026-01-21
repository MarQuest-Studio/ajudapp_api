package com.markish.ajudapp_api.controller;

import com.markish.ajudapp_api.model.UserResponse;
import com.markish.ajudapp_api.model.UserSettingsUpdateRequest;
import com.markish.ajudapp_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;


    @Operation(summary = "Finds or creates user", description = "Returns information about the user who called it if it existed, or creates if it didn't")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request body"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public UserResponse me() {
        return userService.getOrCreateCurrentUser();
    }

    @Operation(summary = "Updates user settings", description = "Updates some user settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request body"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/settings")
    public UserResponse update(@RequestBody UserSettingsUpdateRequest request) {
        return userService.updateSettings(request);
    }

    @Operation(summary = "Delete user", description = "Anonymizes a user and deletes its profile picture")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe() {
        userService.anonymizeCurrentUser();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload profile picture", description = "Uploads a given profile picture for the user who called it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping(path = "/me/upload-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadProfilePicture(
            @Parameter(description = "Profile picture image", required = true)
            @RequestPart MultipartFile file
    ) {
        return userService.uploadProfilePicture(file);
    }
}
