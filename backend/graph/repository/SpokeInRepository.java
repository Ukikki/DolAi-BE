package graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import graph.edge.SpokeIn;

public interface SpokeInRepository extends ArangoRepository<SpokeIn, String> {
}
