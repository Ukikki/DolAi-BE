package graph.repository;
import com.arangodb.springframework.repository.ArangoRepository;
import graph.edge.SpokenBy;

public interface SpokenByRepository extends ArangoRepository<SpokenBy, String> {
}
