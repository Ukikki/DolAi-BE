package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.dolai.backend.graph.edge.UtteranceToKeywordEdge;
import org.springframework.stereotype.Repository;

@Repository
public interface UtteranceToKeywordEdgeRepository extends ArangoRepository<UtteranceToKeywordEdge, String> {
}
