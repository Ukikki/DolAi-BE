package com.dolai.backend.document.repository;

import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.document.model.DocumentPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentPlacementRepository extends JpaRepository<DocumentPlacement, Long> {

    void deleteByDirectory(Directory directory);

    long countByDirectory(Directory directory); // 있으면 삭제 전 체크도 가능

}
