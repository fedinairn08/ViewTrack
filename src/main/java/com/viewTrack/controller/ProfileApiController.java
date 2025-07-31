package com.viewTrack.controller;

import com.viewTrack.data.entity.User;
import com.viewTrack.dto.request.UpdateProfileRequest;
import com.viewTrack.service.UserService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController("/api/user")
@RequiredArgsConstructor
public class ProfileApiController {

    private final AuthUtils authUtils;

    private final UserService userService;

    // не работает
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

    // не работает
    @PostMapping("/upload-profile-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestParam("profileImage") MultipartFile file) {
        User currentUser = authUtils.getUserEntity();
        String filename = userService.uploadProfileImage(currentUser.getId(), file);

        Map<String, String> response = new HashMap<>();
        response.put("filename", filename);
        return ResponseEntity.ok(response);
    }
}
