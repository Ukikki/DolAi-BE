package com.dolai.backend.document.repository;

import com.dolai.backend.document.model.DocumentPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentPlacementRepository extends JpaRepository<DocumentPlacement, Long> {
}
