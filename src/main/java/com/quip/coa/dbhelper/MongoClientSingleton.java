package com.quip.coa.dbhelper;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.quip.coa.utilities.Constants;

public final class MongoClientSingleton {

	private MongoClientSingleton() {}

	private static class Holder {
		private static final MongoClient INSTANCE = createMongoClient();
	}

	private static MongoClient createMongoClient() {
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(Constants.MONGO_CONNECTION_URI))
				.build();

		return MongoClients.create(settings);
	}

	public static MongoClient getClient() {
		return Holder.INSTANCE;
	}
}
