package com.dolai.backend.metadata.repository;

import com.dolai.backend.metadata.model.DirectoryMetaData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DirectoryMetaDataRepository extends JpaRepository<DirectoryMetaData, Long> {
    Optional<DirectoryMetaData> findByDirectoryId(Long directoryId);
}
