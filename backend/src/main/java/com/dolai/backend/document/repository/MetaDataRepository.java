package com.dolai.backend.document.repository;

import com.dolai.backend.document.model.MetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface MetaDataRepository extends JpaRepository<MetaData, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM MetaData m WHERE m.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    Optional<MetaData> findByDocumentId(Long documentId);
}
