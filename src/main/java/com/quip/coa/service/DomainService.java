package com.quip.coa.service;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.quip.coa.mongoUtility.MongoUtility;
import com.quip.coa.utilities.Constants;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DomainService {

    @Autowired
    private MongoUtility mongoUtility;

    public String postDomain(String domainName, String domainUrl) {
        Document exisistingDocument=mongoUtility.getDocumentByQuery(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION,new Document("domainUrl", domainUrl));

        if (exisistingDocument!=null) return null;

        Map<String, String> domainData = new HashMap<>();
        domainData.put("domainName", domainName);
        domainData.put("domainUrl", domainUrl);

        Document document = new Document(domainData);

        InsertOneResult insertOne = mongoUtility.insertOneDocument(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION,document);
        return Objects.requireNonNull(insertOne.getInsertedId()).asObjectId().getValue().toHexString();
    }

    public List<Document> getAllDomains() {
        List<Document> domains = mongoUtility.getDocumentsList(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION);
        for (Document document : domains) {
            String id = document.get("_id").toString();
            document.append("id", id);
            document.remove("_id");
        }
        return domains;
    }

    public DeleteResult deleteDomain(String id) {
        return mongoUtility.deleteDocument(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION, new Document("_id", new ObjectId(id)));
    }

    public Map<String, String> updateDomain(String id, String domainName, String domainUrl) {
        Map<String, String> response = new HashMap<>();
        Document existing=mongoUtility.getDocumentByQuery(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION,new Document("_id", new ObjectId(id)));

        if (existing==null){
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Domain not found");
            return response;
        }

        Document updateFields = new Document();
        if (domainName != null && !domainName.trim().isEmpty()) {
            updateFields.put("domainName", domainName);
        }

        if (domainUrl != null && !domainUrl.trim().isEmpty()) {
            updateFields.put("domainUrl", domainUrl);
        }
        updateFields.put("updatedAt", Instant.now().toString());

        UpdateResult result = mongoUtility.updateDocument(Constants.AUTHOR_DOMAIN_DB, Constants.AUTHOR_DOMAIN_URLS_COLLECTION,new Document("_id",new ObjectId(id)),updateFields);
        if (result.getModifiedCount() == 0) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "No changes were applied");
            return response;
        }

        response.put(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS);
        response.put(Constants.FIELD_MESSAGE, "Domain updated successfully");
        response.put("domainId", id);

        return response;
    }
}

