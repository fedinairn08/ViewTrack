package com.viewTrack.controller;

import com.viewTrack.data.entity.User;
import com.viewTrack.dto.request.ChangePasswordRequest;
import com.viewTrack.dto.request.UpdateProfileRequest;
import com.viewTrack.service.AuthService;
import com.viewTrack.service.ImageService;
import com.viewTrack.service.UserService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class ProfileApiController {

    private final AuthUtils authUtils;

    private final UserService userService;

    private final ImageService imageService;

    private final AuthService authService;

    @PostMapping("/update-profile")
    public ResponseEntity<User> updateProfile(@RequestBody UpdateProfileRequest request) {
        User currentUser = authUtils.getUserEntity();
        User updatedUser = userService.updateProfile(
                currentUser.getId(),
                request.getName(),
                request.getSurname(),
                request.getEmail()
        );
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/upload-profile-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestParam("profileImage") MultipartFile file) {
        User currentUser = authUtils.getUserEntity();
        String filename = imageService.uploadProfileImage(currentUser.getId(), file);

        Map<String, String> response = new HashMap<>();
        response.put("filename", filename);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            User currentUser = authUtils.getUserEntity();
            authService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при изменении пароля"));
        }
    }
}
