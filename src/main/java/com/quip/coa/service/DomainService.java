package com.quip.coa.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.quip.coa.utilities.Constants;
import com.quip.coa.utilities.MongoUtility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DomainService {

    @Autowired
    private MongoUtility mongoUtility;

    public String postDomain(Map<String, String> domainData) {
        MongoCollection<Document> collection = mongoUtility.getCollection(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION);
        Document document = new Document(domainData).append("createdAt", Instant.now());
        InsertOneResult insertOne = collection.insertOne(document);
        return Objects.requireNonNull(insertOne.getInsertedId()).asObjectId().getValue().toHexString();
    }

    public List<Document> getAllDomains() {
        List<Document> domains = mongoUtility.findAll(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION);
        for (Document document : domains) {
            String id = document.get("_id").toString();
            document.append("id", id);
            document.remove("_id");
            document.put("domainName", document.get("domainName"));
            document.remove("createdAt");
        }
        return domains;
    }

    public DeleteResult deleteDomain(String id) {
        MongoCollection<Document> collection = mongoUtility.getCollection(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION);
        return collection.deleteOne(new Document("_id", new ObjectId(id)));
    }
}

