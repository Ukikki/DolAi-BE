package com.dolai.backend.directory.repository;

import com.dolai.backend.directory.model.DirectoryUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectoryUserRepository extends JpaRepository<DirectoryUser, Long> {

}
