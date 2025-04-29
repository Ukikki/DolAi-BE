package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;

import java.util.Optional;

public interface KeywordNodeRepository extends ArangoRepository<KeywordNode, String> {
    Optional<KeywordNode> findByName(String name);
}