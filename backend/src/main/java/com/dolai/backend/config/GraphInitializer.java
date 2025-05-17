package com.dolai.backend.config;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.CollectionCreateOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GraphInitializer {

    private final ArangoDB arangoDB;
    private final ArangoConfig arangoConfig;

    private static final String DB_NAME = "dolai";
    private static final String GRAPH_NAME = "dolai";

    /**
     * ArangoDB 초기화 및 그래프 생성
     * - 문서 컬렉션: utterances, speakers, meetings, keywords, topics
     * - 엣지 컬렉션: utterance_to_speaker, meeting_to_utterance, utterance_to_keyword, utterance_to_topic
     * - 그래프 이름: dolai
     */

    // 그래프 삭제 및 관련 컬렉션 삭제
    public void dropGraphAndCollections() {
        ArangoDatabase db = arangoDB.db(DB_NAME);

        // 그래프 + 컬렉션 같이 삭제
        if (db.graph(GRAPH_NAME).exists()) {
            db.graph(GRAPH_NAME).drop(true);
        }

        // 그래프 외 컬렉션 혹시 남은 것 제거
        List.of(
                "utterances", "topics", "keywords", "meetings", "speakers",
                "utterance_to_keyword", "utterance_to_topic", "meeting_to_utterance", "utterance_to_speaker"
        ).forEach(colName -> {
            if (db.collection(colName).exists()) {
                db.collection(colName).drop();
            }
        });
    }

    @PostConstruct
    public void init() {

        // 0. 기존 dolai DB 제거
        //dropGraphAndCollections(); // TODO: 주의! 실제 운영 DB에서 사용 시 주의

	// 0. DB 존재 여부 확인 → 없으면 생성
	if (!arangoDB.getDatabases().contains(DB_NAME)) {
            arangoDB.createDatabase(DB_NAME); 
    	}

        ArangoDatabase db = arangoDB.db(arangoConfig.getDatabase());

        // 1. 문서 컬렉션 생성
        List.of("utterances", "speakers", "meetings", "keywords", "topics").forEach(name -> {
            if (!db.collection(name).exists()) {
                db.createCollection(name, new CollectionCreateOptions().type(CollectionType.DOCUMENT));
            }
        });

        // 2. 엣지 컬렉션 생성
        List.of("utterance_to_speaker", "meeting_to_utterance", "utterance_to_keyword", "utterance_to_topic").forEach(name -> {
            if (!db.collection(name).exists()) {
                db.createCollection(name, new CollectionCreateOptions().type(CollectionType.EDGES));
            }
        });

        // 3. 그래프 등록
        if (!db.graph(DB_NAME).exists()) {
            db.createGraph("dolai", List.of(
                    new EdgeDefinition().collection("utterance_to_speaker").from("utterances").to("speakers"),
                    new EdgeDefinition().collection("meeting_to_utterance").from("meetings").to("utterances"),
                    new EdgeDefinition().collection("utterance_to_keyword").from("utterances").to("keywords"),
                    new EdgeDefinition().collection("utterance_to_topic").from("utterances").to("topics")
            ));
        }
    }
}
