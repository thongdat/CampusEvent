package com.example.config;

import com.example.model.Event;
import com.example.repository.EventRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class EventDataBackfill implements ApplicationRunner {

    private final EventRepository eventRepository;

    public EventDataBackfill(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Event> changed = new ArrayList<>();
        for (Event event : eventRepository.findAll()) {
            boolean touched = false;
            if (isBlank(event.getImageUrl())) {
                event.setImageUrl(defaultImageFor(event));
                touched = true;
            }
            if (event.getBudget() == null) {
                event.setBudget(BigDecimal.ZERO);
                touched = true;
            }
            if (touched) {
                changed.add(event);
            }
        }
        if (!changed.isEmpty()) {
            eventRepository.saveAll(changed);
        }
    }

    private String defaultImageFor(Event event) {
        String title = event.getTitle() == null ? "" : event.getTitle();
        String department = event.getDepartment() == null || event.getDepartment().getName() == null
                ? ""
                : event.getDepartment().getName();
        String signal = normalize(title + " " + department);
        if (signal.contains("marketing") || signal.contains("kinh te") || signal.contains("business")) {
            return "https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("security") || signal.contains("an toan") || signal.contains("ctf")) {
            return "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("ai") || signal.contains("tri tue") || signal.contains("data")) {
            return "https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("design") || signal.contains("ux") || signal.contains("thiet ke")) {
            return "https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=900&q=80";
        }
        return "https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
