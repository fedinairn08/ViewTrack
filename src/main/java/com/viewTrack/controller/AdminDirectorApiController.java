package com.viewTrack.controller;

import com.viewTrack.data.entity.Director;
import com.viewTrack.service.DirectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/directors")
@RequiredArgsConstructor
public class AdminDirectorApiController {

    private final DirectorService directorService;

    @PostMapping
    public ResponseEntity<Director> addDirector(@RequestParam String fullName,
                                               @RequestParam(required = false) String birthDate,
                                               @RequestParam(required = false) String deathDate,
                                               @RequestParam(required = false) MultipartFile photo) {
        Director director = directorService.createDirector(fullName, birthDate, deathDate, photo);
        return ResponseEntity.ok(director);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDirector(@PathVariable Long id) {
        directorService.deleteDirector(id);
        return ResponseEntity.noContent().build();
    }
}
