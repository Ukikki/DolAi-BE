package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.dolai.backend.graph.entity.KeywordNode;

import java.util.Optional;

public interface KeywordNodeRepository extends ArangoRepository<KeywordNode, String> {
    Optional<KeywordNode> findByName(String name);
}