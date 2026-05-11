package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.SubtitleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubtitleEventRepository extends JpaRepository<SubtitleEvent, Long> {
}
