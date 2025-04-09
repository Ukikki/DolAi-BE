package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.dolai.backend.graph.edge.UtteranceToTopicEdge;
import org.springframework.stereotype.Repository;

@Repository
public interface UtteranceToTopicEdgeRepository extends ArangoRepository<UtteranceToTopicEdge, String> {
}
