package com.viewTrack.controller;

import com.viewTrack.data.entity.User;
import com.viewTrack.service.UserService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final AuthUtils authUtils;

    @GetMapping("/profile")
    public String showProfile(Model model) {
        User currentUser = authUtils.getUserEntity();
        model.addAttribute("user", currentUser);

        model.addAttribute("watchedCount", userService.getWatchedCount(currentUser.getId()));
        model.addAttribute("toWatchCount", userService.getToWatchCount(currentUser.getId()));
        model.addAttribute("ratingsCount", userService.getRatingsCount(currentUser.getId()));

        return "profile";
    }
}
