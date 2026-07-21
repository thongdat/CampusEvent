package com.example.config;

import com.example.model.Room;
import com.example.repository.RoomRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Đảm bảo có sẵn các phòng sự kiện mặc định để form chọn địa điểm không bị trống.
 */
@Component
@Order(8)
public class RoomSeeder implements ApplicationRunner {

    private static final List<RoomSeed> DEFAULT_ROOMS = List.of(
            new RoomSeed("Alpha101", 80, "Phòng học Alpha 101"),
            new RoomSeed("Hội Trường Alpha", 200, "Hội trường lớn Alpha"),
            new RoomSeed("Phòng Lab", 40, "Phòng lab thực hành"),
            new RoomSeed("Phòng Luk", 60, "Phòng Luk")
    );

    private final RoomRepository roomRepository;

    public RoomSeeder(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        for (RoomSeed seed : DEFAULT_ROOMS) {
            if (roomRepository.findByNameIgnoreCase(seed.name).isEmpty()) {
                roomRepository.save(new Room(seed.name, seed.capacity, seed.description, true, now));
                created++;
            }
        }
        if (created > 0) {
            System.out.println("[RoomSeeder] seeded " + created + " room(s).");
        }
    }

    private record RoomSeed(String name, int capacity, String description) {}
}
