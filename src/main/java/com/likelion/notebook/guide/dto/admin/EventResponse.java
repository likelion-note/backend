package com.likelion.notebook.guide.dto.admin;

import com.likelion.notebook.guide.entity.Event;
import com.likelion.notebook.guide.entity.Event.EventType;

import java.time.LocalDate;

public record EventResponse(Long id, String name, EventType type, LocalDate eventDate, boolean major) {

    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getName(), e.getType(), e.getEventDate(), e.isMajor());
    }
}
