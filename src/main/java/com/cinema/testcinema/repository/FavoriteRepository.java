package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Movie;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Movie, Long> {

    // language=PostgreSQL
    @Query(value = "select m.* from movies m join user_favorites uf on uf.movie_id = m.id where uf.user_id = :userId", nativeQuery = true)
    List<Movie> findFavoritesByUserId(@Param("userId") Long userId);

    // language=PostgreSQL
    @Modifying
    @Transactional
    @Query(value = "insert into public.user_favorites(user_id, movie_id) values(:userId, :movieId) on conflict (user_id, movie_id) do nothing", nativeQuery = true)
    void insertIfNotExists(@Param("userId") Long userId, @Param("movieId") Long movieId);

    // language=PostgreSQL
    @Modifying
    @Transactional
    @Query(value = "delete from public.user_favorites where user_id = :userId and movie_id = :movieId", nativeQuery = true)
    void deleteLink(@Param("userId") Long userId, @Param("movieId") Long movieId);

}
