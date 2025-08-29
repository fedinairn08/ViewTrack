package com.viewTrack.service;

import com.viewTrack.data.entity.Director;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DirectorService {
    List<Director> findAll();
    
    List<Director> getDirectors(String sort, String search);
    
    Director createDirector(String fullName, String birthDate, String deathDate, MultipartFile photo);
    
    void deleteDirector(Long id);
}
