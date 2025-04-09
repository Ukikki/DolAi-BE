package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.dolai.backend.graph.entity.UtteranceNode;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtteranceNodeRepository extends ArangoRepository<UtteranceNode, String> {
    Optional<UtteranceNode> findById(String id);

    UtteranceNode save(UtteranceNode utterance);
}
