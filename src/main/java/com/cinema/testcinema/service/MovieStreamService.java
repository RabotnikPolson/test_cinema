package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.MovieStreamDto;
import com.cinema.testcinema.model.MovieStream;
import com.cinema.testcinema.repository.MovieStreamRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MovieStreamService {

    private final MovieStreamRepository movieStreamRepository;

    public MovieStreamService(MovieStreamRepository movieStreamRepository) {
        this.movieStreamRepository = movieStreamRepository;
    }

    public List<MovieStreamDto> findAll() {
        return movieStreamRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<MovieStream> findEntityByImdbId(String imdbId) {
        return movieStreamRepository.findByImdbId(imdbId);
    }

    public Optional<MovieStreamDto> findByImdbId(String imdbId) {
        return movieStreamRepository.findByImdbId(imdbId)
                .map(this::toDto);
    }

    public MovieStreamDto createOrUpdate(MovieStreamDto dto) {
        MovieStream entity = movieStreamRepository
                .findByImdbId(dto.getImdbId())
                .orElseGet(() -> new MovieStream(dto.getImdbId(), dto.getStreamPath()));

        entity.setStreamPath(dto.getStreamPath());
        MovieStream saved = movieStreamRepository.save(entity);
        return toDto(saved);
    }

    public void deleteByImdbId(String imdbId) {
        movieStreamRepository.deleteByImdbId(imdbId);
    }

    private MovieStreamDto toDto(MovieStream entity) {
        return new MovieStreamDto(
                entity.getId(),
                entity.getImdbId(),
                entity.getStreamPath()
        );
    }
}
