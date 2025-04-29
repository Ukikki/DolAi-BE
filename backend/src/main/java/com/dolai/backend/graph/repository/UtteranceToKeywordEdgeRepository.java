package com.dolai.backend.graph.repository;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import org.springframework.data.repository.query.Param;

public interface UtteranceToKeywordEdgeRepository extends ArangoRepository<UtteranceToKeywordEdge, String> {
    @Query("FOR edge IN utteranceToKeywordEdges FILTER edge._from == @fromId AND edge._to == @toId RETURN COUNT(edge) > 0")
    boolean existsByFromIdAndToId(@Param("fromId") String fromId, @Param("toId") String toId);
}