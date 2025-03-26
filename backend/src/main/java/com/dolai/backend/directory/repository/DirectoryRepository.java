package com.dolai.backend.directory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dolai.backend.directory.model.Directory;

@Repository
public interface DirectoryRepository extends JpaRepository<Directory, Long> {

}
