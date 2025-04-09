package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.dolai.backend.graph.entity.TopicNode;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicNodeRepository extends ArangoRepository<TopicNode, String> {
    Optional<TopicNode> findById(String id);

    TopicNode save(TopicNode topic);

    Optional<TopicNode> findByName(String topicName);
}
