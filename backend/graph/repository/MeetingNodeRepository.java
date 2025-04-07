package graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import graph.entity.MeetingNode;

public interface MeetingNodeRepository extends ArangoRepository<MeetingNode, String> {
    // 필요 시 커스텀 쿼리 추가 가능
}
