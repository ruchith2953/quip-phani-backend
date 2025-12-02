package com.quip.coa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.quip.coa.dbConfig.MongoClientSingleton;
import com.quip.coa.model.ComponentDocument;
import com.quip.coa.utilities.Constants;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AemService {
    private static final Logger log = LoggerFactory.getLogger(AemService.class);
    private final ObjectMapper mapper;

    public AemService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private final GenericAemComponentProcessor processor = new GenericAemComponentProcessor();

    private MongoCollection<Document> getCollection(String domainName,String collectionName) {
        MongoClient client = MongoClientSingleton.getClient();
        MongoDatabase db = client.getDatabase(domainName);
        return db.getCollection(collectionName);
    }

    // -----------------------------
    // 1. Extract and save to Mongo
    // -----------------------------
    public void extractAndStore(JsonNode root, String domainName,String userEmail) throws JsonProcessingException {

        List<ComponentDocument> docs = processor.parseComponentsJson(root);

        MongoCollection<Document> collection = getCollection(domainName, Constants.COMPONENTS_COLLECTION);

        List<Document> mongoDocs = new ArrayList<>();

        for (ComponentDocument d : docs) {
            Document doc = Document.parse(mapper.writeValueAsString(d));
            mongoDocs.add(doc);
        }

        collection.insertMany(mongoDocs);

        // create metadata collection

        MongoCollection<Document> metaDataCollection = getCollection(domainName,Constants.METADATA_COLLECTION);
        Document metaData=Document.parse(root.toString());
        metaData.put("components","");
        Document metadataObject=metaData.get("metadata",Document.class);
        metadataObject.put("user_email",userEmail);
        metaDataCollection.insertOne(metaData);
    }


    public Document reStructureComponentData(ObjectNode componentData, String domainName){
        Document metaDataDocument = getCollection(domainName,Constants.METADATA_COLLECTION).find().projection(new Document("_id", 0)).first();
        if (metaDataDocument != null) {
            metaDataDocument.put("components", componentData);
        }

        return metaDataDocument;
    }




    // -----------------------------
    // 2. Reconstruct from Mongo
    // -----------------------------
    public ObjectNode reconstruct(String domainName) throws JsonProcessingException {

        MongoCollection<Document> collection = getCollection(domainName,Constants.COMPONENTS_COLLECTION);

        List<ComponentDocument> docs = new ArrayList<>();

        for (Document dbDoc : collection.find()) {
            ComponentDocument d = mapper.readValue(dbDoc.toJson(), ComponentDocument.class);
            docs.add(d);
        }

        log.info("COunt: {}", docs.size());

        return processor.reconstructComponents(docs);
    }

    // -----------------------------
    // 3. Get all component docs
    // -----------------------------
    public List<ComponentDocument> getAll(String pagePath,String domainName) throws JsonProcessingException {

        MongoCollection<Document> collection = getCollection(domainName,Constants.COMPONENTS_COLLECTION);

        List<ComponentDocument> docs = new ArrayList<>();

        for (Document dbDoc : collection.find()) {
            ComponentDocument d = mapper.readValue(dbDoc.toJson(), ComponentDocument.class);
            if (d.path.equals(pagePath)){
                docs.add(d);
            }
        }
        return docs;
    }
}
