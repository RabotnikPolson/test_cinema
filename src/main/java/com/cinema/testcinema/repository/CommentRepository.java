package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByMovieIdAndParentIdIsNull(Long movieId, Pageable pageable);

    /**
     * Получить корневые комментарии, отсортированные по «популярности» (количеству реакций).
     *
     * Важно:
     * - Считаем популярность только по реакциям на сам корневой комментарий (как в YouTube).
     * - Для стабильности добавляем вторичную сортировку по дате создания (новые выше при равенстве).
     */
    @Query(
            value = "select c " +
                    "from Comment c " +
                    "left join CommentReaction r on r.comment = c " +
                    "where c.movieId = :movieId and c.parentId is null " +
                    "group by c " +
                    "order by count(r.id) desc, c.createdAt desc",
            countQuery = "select count(c) from Comment c where c.movieId = :movieId and c.parentId is null"
    )
    Page<Comment> findRootsByMovieIdOrderByPopularity(@Param("movieId") Long movieId, Pageable pageable);

    List<Comment> findByParentIdIn(Collection<Long> parentIds);

    Page<Comment> findByParentId(Long parentId, Pageable pageable);

    long countByMovieId(Long movieId);

    long countByMovieIdAndParentIdIsNull(Long movieId);

    long countByParentId(Long parentId);
}
