package graph.repository;
import com.arangodb.springframework.repository.ArangoRepository;
import graph.edge.ParticipatedIn;

public interface ParticipatedInRepository extends ArangoRepository<ParticipatedIn, String> {
}

