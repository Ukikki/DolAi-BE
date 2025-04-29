package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;

import java.util.Optional;

public interface TopicNodeRepository extends ArangoRepository<TopicNode, String> {
    Optional<TopicNode> findByName(String name);
}