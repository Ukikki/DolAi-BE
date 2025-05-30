package com.dolai.backend.user.repository;

import com.dolai.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String sub);
    Optional<User> findByName(String name);
    List<User> findByEmailContainingIgnoreCase(String email);
    List<User> findAllByName(String name);

}