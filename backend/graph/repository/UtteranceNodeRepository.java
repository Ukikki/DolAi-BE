package graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import graph.entity.UtteranceNode;

public interface UtteranceNodeRepository extends ArangoRepository<UtteranceNode, String> {
}
