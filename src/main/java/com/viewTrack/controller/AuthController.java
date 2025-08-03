package com.viewTrack.controller;

import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.dto.request.JwtRefreshRequest;
import com.viewTrack.dto.response.AuthResponse;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/signIn")
    public String showSignInForm() {
        return "signIn";
    }

    @PostMapping("/signIn")
    public String signIn(@RequestParam String username,
                         @RequestParam String password,
                         Model model,
                         HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.signIn(username, password);

            Cookie cookie = new Cookie("x_api_token", authResponse.accessToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            response.addCookie(cookie);
            return "redirect:/api/movie/all";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("error", "Неверные учетные данные");
            return "/signIn";
        }
    }

    @GetMapping("/signUp")
    public String showSignUpForm() {
        return "signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String name,
                         @RequestParam String surname,
                         Model model) {
        try {
            authService.signUp(username, password, name, surname);
            return "redirect:/allFilms";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка регистрации: " + e.getMessage());
            return "/signUp";
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<BasicApiResponse<AuthResponse>> refresh(@RequestBody JwtRefreshRequest refreshRequest) {
        return ResponseEntity.ok(new BasicApiResponse<>(authService.refresh(refreshRequest.refreshToken())));
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("x_api_token", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/api/auth/signIn";
    }
}
