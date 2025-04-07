package graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import graph.entity.SpeakerNode;

public interface SpeakerNodeRepository extends ArangoRepository<SpeakerNode, String> {
    SpeakerNode findByName(String name); // 이름 기반 검색 추가 가능
}
