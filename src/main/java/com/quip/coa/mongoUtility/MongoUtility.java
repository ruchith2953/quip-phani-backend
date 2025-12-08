package com.quip.coa.mongoUtility;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.quip.coa.dbhelper.MongoClientSingleton;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    // Insert a single document
    public void insertDocument(String databaseName, String collectionName,Document document) {
        getCollection(databaseName, collectionName).insertOne(document);
    }

    // Insert a single document
    public InsertOneResult insertOneDocument(String databaseName, String collectionName, Document document) {
       return getCollection(databaseName, collectionName).insertOne(document);
    }

    // Fetch all documents from the collection
    public FindIterable<Document> getDocuments(String databaseName, String collectionName) {
        return getCollection(databaseName, collectionName).find();
    }

    // Fetch all documents from the collection and return as list
    public List<Document> getDocsList(String databaseName, String collectionName) {
        return getDocuments(databaseName, collectionName).into(new ArrayList<>());
    }

    // Fetch the first document from the collection
    public Document getFirstDocument(String databaseName, String collectionName) {
        return getDocuments(databaseName, collectionName).first();
    }

    // Fetch all documents from the collection based on query
    public FindIterable<Document> getMatchedDocsByQuery(String databaseName, String collectionName, Document query) {
        return getCollection(databaseName, collectionName).find(query);
    }

    // Fetch document from the collection based on query
    public Document getDocByQuery(String databaseName, String collectionName, Document query) {
        return getMatchedDocsByQuery(databaseName, collectionName, query).first();
    }

    // Fetch document from the collection based on query & projection
    public Document getDocByQueryAndProjection(String databaseName, String collectionName, Document query, Bson projection) {
        return getMatchedDocsByQuery(databaseName, collectionName, query).projection(projection).first();
    }

    // Fetch document from the collection based on query, projection & sort
    public Document getDocByQueryProjectionAndSort(String databaseName, String collectionName, Document query, Document projection, Document sort) {
        return getMatchedDocsByQuery(databaseName, collectionName, query).projection(projection).sort(sort).first();
    }

    // Update document based on query and update operation
    public void updateDocument(String databaseName, String collectionName, Document query, Document updateDocument) {
        getCollection(databaseName, collectionName).updateOne(query, new Document("$set", updateDocument));
    }

    // Delete document based on query
    public void deleteDocument(String databaseName, String collectionName, Document query) {
        getCollection(databaseName, collectionName).deleteOne(query);
    }

    // Insert Many documents into a collection
    public void insertManyDocuments(List<Document> documents, String databaseName, String collectionName) {
        getCollection(databaseName, collectionName).insertMany(documents);
    }

    // Delete Many documents based on a query
    public void deleteManyDocuments(String databaseName, String collectionName, Document query) {
        getCollection(databaseName, collectionName).deleteMany(query);
    }

    //Get all docs by replacing _id with id by converting value form ObjectID to string
    public List<Document> getAllDocsWithID(String database, String collectionName, Document query) {
        List<Document> documentList = getMatchedDocsByQuery(database, collectionName, query).into(new ArrayList<>());

        for (Document document : documentList) {
            String objectId = document.getObjectId("_id").toHexString();
            document.remove("_id");
            document.put("id", objectId);
        }

        return documentList;
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

    /** Delete one document */
    public DeleteResult deleteOne(String dbName, String collName, Bson filter) {
        return getCollection(dbName, collName).deleteOne(filter);
    }

    /** Delete many documents */
    public DeleteResult deleteMany(String dbName, String collName, Bson filter) {
        return getCollection(dbName, collName).deleteMany(filter);
    }

    /** Find one document (returns null if not found) */
    public Document findOne(String dbName, String collName, Bson filter) {
        return getCollection(dbName, collName).find(filter).first();
    }

    /** Find one document (returns null if not found) */
    public Document findFirst(String dbName, String collName) {
        return getCollection(dbName, collName).find().first();
    }
    /** Find one document by _id (String or ObjectId) */
    public Document findById(String dbName, String collName, String id) {
        if (ObjectId.isValid(id)) {
            return findOne(dbName, collName, Filters.eq("_id", new ObjectId(id)));
        }
        return findOne(dbName, collName, Filters.eq("_id", id));
    }
    /** Find the first document with filter & sort */
    public Document findFirstWithFilterSort(String dbName, String collName, Bson filter, Bson sort) {
        return getCollection(dbName, collName).find(filter).sort(sort).first();
    }

    /** Find the first document in a collection with sort */
    public Document findFirstSorted(String dbName, String collName, Bson sort) {
        return getCollection(dbName, collName).find().sort(sort).first();
    }

    /** Update one document */
    public UpdateResult updateOne(String dbName, String collName, Bson filter, Bson update) {
        return getCollection(dbName, collName).updateOne(filter, update);
    }

}
