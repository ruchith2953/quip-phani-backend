package com.quip.coa.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.InsertOneResult;
import com.quip.coa.utilities.Constants;
import com.quip.coa.utilities.MongoUtility;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DomainService {

    @Autowired
   private MongoUtility mongoUtility;

    public String postDomain(Map<String, String> domainData) {
        MongoCollection<Document> collection= mongoUtility.getCollection(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION);
        Document document=new Document(domainData).append("createdAt", Instant.now());
        InsertOneResult insertOne= collection.insertOne(document);
        return Objects.requireNonNull(insertOne.getInsertedId()).asObjectId().getValue().toHexString();
    }

    public List<Document> getAllDomains() {
        Bson projection = Projections.exclude("_id");
        return mongoUtility.findAll(Constants.AUTHOR_DOMAIN_DB,Constants.AUTHOR_DOMAIN_URLS_COLLECTION,projection);
    }

}

