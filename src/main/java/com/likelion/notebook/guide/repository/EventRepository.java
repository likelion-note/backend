package com.likelion.notebook.guide.repository;

import com.likelion.notebook.guide.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCohortOrderByEventDateAsc(Integer cohort);

    boolean existsByCohortAndType(Integer cohort, Event.EventType type);
}
