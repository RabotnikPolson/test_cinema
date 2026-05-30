package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.movie.TrendingMovieDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieClickRepository;
import com.cinema.testcinema.repository.TrendingClickProjection;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrendingService {

    private static final Logger log = LoggerFactory.getLogger(TrendingService.class);

    private final KinopoiskClient kinopoiskClient;
    private final KinopoiskSyncService syncService;
    private final MovieClickRepository movieClickRepository;

    public TrendingService(KinopoiskClient kinopoiskClient,
                           KinopoiskSyncService syncService,
                           MovieClickRepository movieClickRepository) {
        this.kinopoiskClient = kinopoiskClient;
        this.syncService = syncService;
        this.movieClickRepository = movieClickRepository;
    }

    /**
     * Топ-10 из Kinopoisk TOP_POPULAR_MOVIES — IDs для Hero-баннера.
     * Кэшируем только ID чтобы не класть JPA-сущности в Redis.
     * TTL = 24ч (дефолт), авто-импортирует фильмы которых нет в БД.
     */
    @Cacheable(value = "hero_movies", key = "'top10'")
    public List<Long> getHeroMovieIds() {
        log.info("[HERO] Загружаем TOP_POPULAR_MOVIES из Кинопоиска...");
        List<Long> ids = new ArrayList<>();
        try {
            JsonNode data = kinopoiskClient.fetchTopPopularMovies(1);
            if (data == null || !data.has("items")) return ids;
            for (JsonNode item : data.get("items")) {
                if (ids.size() >= 10) break;
                String kinopoiskId = item.path("kinopoiskId").asText("");
                if (kinopoiskId.isEmpty()) continue;
                try {
                    Movie movie = syncService.fetchAndSave(kinopoiskId);
                    ids.add(movie.getId());
                } catch (Exception e) {
                    log.warn("[HERO] Ошибка импорта {}: {}", kinopoiskId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[HERO] Ошибка загрузки TOP_POPULAR_MOVIES", e);
        }
        log.info("[HERO] Готово, фильмов в баннере: {}", ids.size());
        return ids;
    }

    @Cacheable(value = "trending", key = "'daily'")
    public List<TrendingMovieDto> getTrendingMovies() {
        log.info("[TRENDING] Вычисляем тренды. Прогрев кэша...");
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Movie> movieMap = new HashMap<>();

        // 1. Внешние тренды: Kinopoisk Top 20 (только фильмы, исключая РФ)
        int rank = 0;
        int page = 1;
        while (rank < 20 && page <= 5) { // Ограничим 5 страницами, чтобы не спамить API
            try {
                JsonNode topPopular = kinopoiskClient.fetchTopPopular(page);
                if (topPopular != null && topPopular.has("items")) {
                    JsonNode items = topPopular.get("items");
                    if (items.isEmpty()) break;
                    
                    for (JsonNode item : items) {
                        if (rank >= 20) break; // набрали топ-20

                        String kinopoiskId = item.path("kinopoiskId").asText("");
                        if (kinopoiskId.isEmpty()) continue;
                        
                        // Пропускаем сериалы (если type известен заранее)
                        String type = item.path("type").asText("");
                        if (!type.isEmpty() && type.contains("serial") || type.contains("TV_SHOW") || type.contains("MINI_SERIES") || type.contains("TV_SERIES")) {
                            continue;
                        }

                        // Пропускаем российские (если страны известны)
                        boolean hasRussia = false;
                        if (item.has("countries")) {
                            for (JsonNode countryNode : item.get("countries")) {
                                String cName = countryNode.path("country").asText("");
                                if (cName.contains("Россия") || cName.contains("Russia")) {
                                    hasRussia = true;
                                    break;
                                }
                            }
                        }
                        if (hasRussia) continue;

                        try {
                            Movie movie = syncService.fetchAndSave(kinopoiskId);
                            // Повторная проверка уже на сохраненной модели
                            if (movie.isSerial() || (movie.getCountry() != null && movie.getCountry().contains("Россия"))) {
                                continue;
                            }
                            
                            double score = 20.0 - rank;
                            scores.put(movie.getId(), score);
                            movieMap.put(movie.getId(), movie);
                            rank++;
                        } catch (Exception e) {
                            log.warn("[TRENDING] Ошибка синхронизации фильма {} из топа: {}", kinopoiskId, e.getMessage());
                        }
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                log.error("[TRENDING] Не удалось получить топы с Kinopoisk на странице {}", page, e);
                break;
            }
            page++;
        }

        // 2. Внутренние тренды: Клики за последние 10 дней
        Instant lastWeek = Instant.now().minus(10, ChronoUnit.DAYS);
        List<TrendingClickProjection> internalTrends = movieClickRepository.findTopTrendingMovies(lastWeek, PageRequest.of(0, 20));

        for (TrendingClickProjection projection : internalTrends) {
            Movie movie = projection.getMovie();
            Long clicks = projection.getClicks();
            
            movieMap.putIfAbsent(movie.getId(), movie);
            
            // Начисляем баллы за клики. Пусть 1 клик = 1 балл
            double currentScore = scores.getOrDefault(movie.getId(), 0.0);
            scores.put(movie.getId(), currentScore + clicks);
        }

        // 3. Агрегация, бусты и сортировка
        List<TrendingMovieDto> result = new ArrayList<>();
        
        for (Map.Entry<Long, Movie> entry : movieMap.entrySet()) {
            Movie movie = entry.getValue();
            double finalScore = scores.getOrDefault(movie.getId(), 0.0);

            // Буст для казахстанских фильмов (x1.4)
            if (movie.isDomestic()) {
                finalScore *= 1.4;
            }

            result.add(new TrendingMovieDto(
                    movie.getId(),
                    movie.getTitle(),
                    movie.getPosterUrl(),
                    Math.round(finalScore * 100.0) / 100.0, // округление
                    movie.isDomestic()
            ));
        }

        // Сортировка по убыванию score
        result.sort((a, b) -> Double.compare(b.score(), a.score()));

        log.info("[TRENDING] Тренды успешно обновлены. Записей: {}", result.size());
        return result;
    }
}
