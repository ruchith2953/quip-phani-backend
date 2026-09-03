package com.quip.phani.dbhelper;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.quip.phani.utilities.Constants;

public class MongoClientSingleton {
	private static MongoClient clientSync;

	private MongoClientSingleton() {
		MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(new ConnectionString(Constants.MONGO_CONNECTION_URI)).build();
		clientSync = MongoClients.create(settings);
	}

	public static synchronized MongoClient getClient() {
		if(clientSync == null) {
			new MongoClientSingleton();
		}
		return clientSync;
	}

}
