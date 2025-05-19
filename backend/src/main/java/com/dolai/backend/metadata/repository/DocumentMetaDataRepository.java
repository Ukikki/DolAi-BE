package com.dolai.backend.metadata.repository;

import com.dolai.backend.metadata.model.DocumentMetaData;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMetaDataRepository extends JpaRepository<DocumentMetaData, Long> {
    Optional<DocumentMetaData> findByDocumentId(Long documentId);
    @Modifying
    @Query("DELETE FROM DocumentMetaData d WHERE d.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}