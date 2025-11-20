package com.quip.coa.utilities;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.quip.coa.dbhelper.MongoClientSingleton;
import org.bson.Document;
import org.springframework.stereotype.Service;

@Service
public class MongoUtility {

    // Get mongo client
    public MongoClient getMongoClient() {
        return MongoClientSingleton.getClient();
    }

    // Get database
    public MongoDatabase getDatabase(String databaseName) {
        return getMongoClient().getDatabase(databaseName);
    }

    // Get collection
    public MongoCollection<Document> getCollection(String databaseName, String collectionName) {
        return getDatabase(databaseName).getCollection(collectionName);
    }
}