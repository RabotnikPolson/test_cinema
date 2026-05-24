package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.movie.BulkImportItemResult;
import com.cinema.testcinema.dto.movie.BulkImportRequest;
import com.cinema.testcinema.dto.movie.BulkImportResponse;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BulkImportService {

    private static final Logger log = LoggerFactory.getLogger(BulkImportService.class);

    private final KinopoiskSyncService kinopoiskSyncService;
    private final MovieRepository movieRepository;

    public BulkImportService(KinopoiskSyncService kinopoiskSyncService, MovieRepository movieRepository) {
        this.kinopoiskSyncService = kinopoiskSyncService;
        this.movieRepository = movieRepository;
    }

    public BulkImportResponse bulkImport(List<String> kinopoiskIds) {
        int total = kinopoiskIds.size();
        int success = 0;
        int failed = 0;
        List<BulkImportItemResult> results = new ArrayList<>();

        for (String kpId : kinopoiskIds) {
            Optional<Movie> existingMovie = movieRepository.findByKinopoiskId(kpId);
            if (existingMovie.isPresent()) {
                Movie m = existingMovie.get();
                results.add(new BulkImportItemResult(
                        kpId, "already_exists", m.getTitle(), m.getId(), null
                ));
                success++; // Consider already exists as a successful result for the batch
            } else {
                try {
                    Movie savedMovie = kinopoiskSyncService.fetchAndSave(kpId);
                    results.add(new BulkImportItemResult(
                            kpId, "success", savedMovie.getTitle(), savedMovie.getId(), null
                    ));
                    success++;
                } catch (Exception e) {
                    log.error("Failed to import kinopoiskId={}", kpId, e);
                    results.add(new BulkImportItemResult(
                            kpId, "error", null, null, e.getMessage()
                    ));
                    failed++;
                }
            }

            // Rate limiter: 100ms pause
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Bulk import interrupted");
                break;
            }
        }

        return new BulkImportResponse(total, success, failed, results);
    }
}
