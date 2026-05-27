package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Movie;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom repository for filtering and paginating movies.  This repository performs a two‑step
 * query to avoid N+1 issues when fetching many‑to‑many relations.  First it selects a page
 * of movie IDs matching the provided filters, then fetches the corresponding Movie entities
 * along with their genres using a join fetch.  When no paging parameters are provided
 * (page and size are null) the full result set is returned.
 */
@Repository
public class MovieFilterRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Search movies according to optional filters.  All parameters are nullable and
     * ignored when null.  Paging is applied only when both page and size are provided.
     * Sorting is always by ascending movie id to maintain a stable order.
     *
     * @param q       substring to search in movie titles (case insensitive)
     * @param genreId filter by genre id
     * @param yearFrom inclusive lower bound for the movie year
     * @param yearTo   inclusive upper bound for the movie year
     * @param page     zero‑based page index; applied only when size is also provided
     * @param size     number of items per page; applied only when page is also provided
     * @return list of movies matching the provided criteria
     */
    public List<Movie> searchMovies(String q,
                                    Long genreId,
                                    Long yearFrom,
                                    Long yearTo,
                                    Integer page,
                                    Integer size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // First stage: select matching movie IDs with filters and paging
        CriteriaQuery<Long> idQuery = cb.createQuery(Long.class);
        Root<Movie> movieRoot = idQuery.from(Movie.class);
        idQuery.select(movieRoot.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(movieRoot.get("title")), pattern),
                cb.like(cb.lower(movieRoot.get("nameOriginal")), pattern),
                cb.like(cb.lower(movieRoot.get("nameEn")), pattern)
            ));
        }
        if (genreId != null) {
            // inner join on genres when filtering by genre id
            Join<Object, Object> genreJoin = movieRoot.join("genres", JoinType.INNER);
            predicates.add(cb.equal(genreJoin.get("id"), genreId));
        }
        if (yearFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(movieRoot.get("year"), yearFrom));
        }
        if (yearTo != null) {
            predicates.add(cb.lessThanOrEqualTo(movieRoot.get("year"), yearTo));
        }
        if (!predicates.isEmpty()) {
            idQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        idQuery.orderBy(cb.asc(movieRoot.get("id")));
        TypedQuery<Long> typedIdQuery = entityManager.createQuery(idQuery).setHint("org.hibernate.readOnly", true);
        if (page != null && size != null) {
            typedIdQuery.setFirstResult(page * size);
            typedIdQuery.setMaxResults(size);
        }
        List<Long> ids = typedIdQuery.getResultList();
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Second stage: fetch movies with genres using join fetch to avoid N+1
        CriteriaQuery<Movie> movieQuery = cb.createQuery(Movie.class);
        Root<Movie> fetchRoot = movieQuery.from(Movie.class);
        // join fetch genres on the second query
        fetchRoot.fetch("genres", JoinType.LEFT);
        movieQuery.select(fetchRoot).distinct(true);
        movieQuery.where(fetchRoot.get("id").in(ids));
        movieQuery.orderBy(cb.asc(fetchRoot.get("id")));
        TypedQuery<Movie> typedMovieQuery = entityManager.createQuery(movieQuery);
        return typedMovieQuery.getResultList();
    }
}
