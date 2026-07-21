package com.example.repository;

import com.example.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByNameIgnoreCase(String name);

    List<Room> findByActiveTrueOrderByNameAsc();

    @Query("select r from Room r where "
            + "(:activeOnly = false or r.active = true) and "
            + "(:q is null or :q = '' or lower(r.name) like lower(concat('%', :q, '%')) "
            + " or lower(coalesce(r.description, '')) like lower(concat('%', :q, '%')))")
    Page<Room> search(@Param("q") String q, @Param("activeOnly") boolean activeOnly, Pageable pageable);
}
