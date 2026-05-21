package com.example.repository;

import com.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
