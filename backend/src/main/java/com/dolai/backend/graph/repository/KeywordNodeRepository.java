package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.dolai.backend.graph.entity.KeywordNode;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeywordNodeRepository extends ArangoRepository<KeywordNode, String> {
    Optional<KeywordNode> findByName(String name);

    KeywordNode save(KeywordNode keywordNode);
}
