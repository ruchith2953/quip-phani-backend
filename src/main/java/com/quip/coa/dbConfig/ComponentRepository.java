package com.quip.coa.dbConfig;

import com.quip.coa.model.ComponentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ComponentRepository extends MongoRepository<ComponentDocument, String> {

    List<ComponentDocument> findByPath(String path);

    List<ComponentDocument> findByComponentType(String type);
}
