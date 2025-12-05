package com.quip.coa.dbhelper;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;


public class MongoClientSingleton {
	private static MongoClient clientSync;
	//@Value("${spring.data.mongodb.host}")
	private String hosts = "http://100.24.248.226";
	//@Value("${spring.data.mongodb.username}")
	private String username="admin";
	//@Value("${spring.data.mongodb.password}")
	private String password="password";
	//@Value("${spring.data.mongodb.database}")
	private String db="admin";
	private MongoClientSingleton() {

		/*
		 * List<String> hostArr = Arrays.asList(hosts.split(",")); List<ServerAddress>
		 * seeds = new ArrayList<>(); for(String host : hostArr) { seeds.add(new
		 * ServerAddress(host)); }
		 */
		//MongoCredential mongoCredential = MongoCredential.createCredential(username, db, password.toCharArray());
		//MongoClientSettings settings = MongoClientSettings.builder().credential(mongoCredential).readPreference(ReadPreference.secondaryPreferred()).build();
		MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(new ConnectionString("mongodb://admin:admin@34.194.175.59:27017/?authSource=admin")).build();

		clientSync = MongoClients.create(settings);
	}

	public static synchronized MongoClient getClient() {
		if(clientSync == null) {
			new MongoClientSingleton();
		}
		return clientSync;
	}

}
