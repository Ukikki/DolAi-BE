package com.dolai.backend.document.repository;

import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.document.model.DocumentPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentPlacementRepository extends JpaRepository<DocumentPlacement, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentPlacement dp WHERE dp.document.id = :documentId")
    void deleteAllByDocumentId(@Param("documentId") Long documentId);

    void deleteByDirectory(Directory directory);

    List<DocumentPlacement> findAllByDirectoryIdAndUserId(Long directoryId, String userId);

}
