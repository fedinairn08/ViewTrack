package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Director;
import com.viewTrack.data.repository.DirectorRepository;
import com.viewTrack.service.DirectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorServiceImpl implements DirectorService {

    private final DirectorRepository directorRepository;

    @Override
    public List<Director> findAll() {
        return directorRepository.findAll();
    }
}
