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

// Two-step query: selects IDs first, then join-fetches entities with genres to avoid N+1.
@Repository
public class MovieFilterRepository {

    @PersistenceContext
    private EntityManager entityManager;

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
        fetchRoot.fetch("genres", JoinType.LEFT);
        movieQuery.select(fetchRoot).distinct(true);
        movieQuery.where(fetchRoot.get("id").in(ids));
        movieQuery.orderBy(cb.asc(fetchRoot.get("id")));
        TypedQuery<Movie> typedMovieQuery = entityManager.createQuery(movieQuery);
        return typedMovieQuery.getResultList();
    }
}
