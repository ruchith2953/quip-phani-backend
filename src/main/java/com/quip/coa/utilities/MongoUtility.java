package com.quip.coa.utilities;

import com.mongodb.ObjectId;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.UpdateResult;
import com.quip.coa.dbConfig.MongoClientSingleton;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MongoUtility {

    /** Get mongo client  */
    public MongoClient getMongoClient() {
        return MongoClientSingleton.getClient();
    }

    /** Get database  */
    public MongoDatabase getDatabase(String databaseName) {
        return getMongoClient().getDatabase(databaseName);
    }

    /** Get collection */
    public MongoCollection<Document> getCollection(String databaseName, String collectionName) {
        return getDatabase(databaseName).getCollection(collectionName);
    }

    /** Insert many documents */
    public InsertManyResult insertMany(String dbName, String collName, List<Document> documents) {
        return getCollection(dbName, collName).insertMany(documents);
    }

    /** Find one document (returns null if not found) */
    public Document findOne(String dbName, String collName, Bson filter) {
        return getCollection(dbName, collName).find(filter).first();
    }

    /** Find one document by _id (String or ObjectId) */
    public Document findById(String dbName, String collName, String id) {
        if (ObjectId.isValid(id)) {
            return findOne(dbName, collName, Filters.eq("_id", new ObjectId(id)));
        }
        return findOne(dbName, collName, Filters.eq("_id", id));
    }

    /** FindAll with projection */
    public List<Document> findAll(String dbName, String collName) {
        return getCollection(dbName, collName).find().into(new ArrayList<>());
    }

    /** FindAll with projection */
    public List<Document> findAll(String dbName, String collName, Bson projection) {
        var iterable = getCollection(dbName, collName).find();
        if (projection != null) iterable = iterable.projection(projection);
        return iterable.into(new ArrayList<>());
    }

    /** Find with projection */
    public List<Document> find(String dbName, String collName, Bson filter, Bson projection) {
        return getCollection(dbName, collName)
                .find(filter)
                .projection(projection)
                .into(new ArrayList<>());
    }

    /** Count documents matching filter */
    public long count(String dbName, String collName, Bson filter) {
        return getCollection(dbName, collName).countDocuments(filter);
    }

    /** Update one document */
    public UpdateResult updateOne(String dbName, String collName, Bson filter, Bson update) {
        return getCollection(dbName, collName).updateOne(filter, update);
    }

    /** Update many documents */
    public UpdateResult updateMany(String dbName, String collName, Bson filter, Bson update) {
        return getCollection(dbName, collName).updateMany(filter, update);
    }

    /** Upsert (update or insert) */
    public UpdateResult upsertOne(String dbName, String collName, Bson filter, Document document) {
        return getCollection(dbName, collName)
                .updateOne(filter, new Document("$set", document), new UpdateOptions().upsert(true));
    }

    /** Replace entire document */
    public UpdateResult replaceOne(String dbName, String collName, Bson filter, Document replacement) {
        return getCollection(dbName, collName).replaceOne(filter, replacement);
    }

    /** Delete one document */
    public DeleteResult deleteOne(String dbName, String collName, Bson filter) {
        return getCollection(dbName, collName).deleteOne(filter);
    }

    /** Delete many documents */
    public DeleteResult deleteMany(String dbName, String collName, Bson filter) {
        return getCollection(dbName, collName).deleteMany(filter);
    }
}