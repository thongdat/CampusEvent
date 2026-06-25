package com.example.repository;

import com.example.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailAndPassword(String email, String password);
    long countByStatus(Boolean status);
    long countByRole_Id(Long roleId);
    long countByRole_Name(String roleName);
    List<User> findByRole_NameAndStatus(String roleName, Boolean status);

    @Query(
            value = "select u from User u left join u.role r left join Student s on s.user = u " +
                    "where (:q is null or lower(u.fullName) like lower(concat('%', :q, '%')) " +
                    "or lower(u.email) like lower(concat('%', :q, '%')) " +
                    "or lower(r.name) like lower(concat('%', :q, '%')) " +
                    "or lower(coalesce(u.major, s.major, '')) like lower(concat('%', :q, '%')) " +
                    "or lower(coalesce(s.studentCode, '')) like lower(concat('%', :q, '%'))) " +
                    "and (:role is null or r.name = :role)",
            countQuery = "select count(u) from User u left join u.role r left join Student s on s.user = u " +
                    "where (:q is null or lower(u.fullName) like lower(concat('%', :q, '%')) " +
                    "or lower(u.email) like lower(concat('%', :q, '%')) " +
                    "or lower(r.name) like lower(concat('%', :q, '%')) " +
                    "or lower(coalesce(u.major, s.major, '')) like lower(concat('%', :q, '%')) " +
                    "or lower(coalesce(s.studentCode, '')) like lower(concat('%', :q, '%'))) " +
                    "and (:role is null or r.name = :role)"
    )
    Page<User> searchUsers(@Param("q") String q, @Param("role") String role, Pageable pageable);

    @Query(
            value = "select u from User u left join u.role r left join Student s on s.user = u " +
                    "where (:q is null or lower(u.fullName) like lower(concat('%', :q, '%')) " +
                    "or lower(u.email) like lower(concat('%', :q, '%')) " +
                    "or lower(r.name) like lower(concat('%', :q, '%')) " +
                    "or lower(coalesce(u.major, s.major, '')) like lower(concat('%', :q, '%')) " +
                    "or lower(coalesce(s.studentCode, '')) like lower(concat('%', :q, '%'))) " +
                    "and (:role is null or r.name = :role) " +
                    "and coalesce(u.major, s.major, '') in :majors",
            countQuery = "select count(u) from User u left join u.role r left join Student s on s.user = u " +
                    "where (:q is null or lower(u.fullName) like lower(concat('%', :q, '%')) " +
                    "or lower(u.email) like lower(concat('%', :q, '%')) " +
                    "or lower(r.name) like lower(concat('%', :q, '%')) " +
                    "or lower(coalesce(u.major, s.major, '')) like lower(concat('%', :q, '%')) " +
                    "or lower(coalesce(s.studentCode, '')) like lower(concat('%', :q, '%'))) " +
                    "and (:role is null or r.name = :role) " +
                    "and coalesce(u.major, s.major, '') in :majors"
    )
    Page<User> searchUsersByMajors(
            @Param("q") String q,
            @Param("role") String role,
            @Param("majors") Collection<String> majors,
            Pageable pageable);
}
