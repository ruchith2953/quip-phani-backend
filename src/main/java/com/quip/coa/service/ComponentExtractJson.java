package com.quip.coa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.quip.coa.utilities.Constants;
import com.quip.coa.utilities.HttpUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@Service
public class ComponentExtractJson {
	@Autowired
	private Environment environment;

	@Autowired
	ActivityTracking activityTracking;

	@Autowired
	private HttpUtilities httpUtilities;

	public JsonNode componentExtractJson(String userName, String apiUrl, String clientName) {

		Map<String, String> headers = new HashMap<>();
		String credentials = environment.getProperty("AEM_PAGE_USERNAME")+ ":" + environment.getProperty("AEM_PAGE_PASSWORD");
		String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
		headers.put(Constants.AUTHORIZATION_HEADER, "Basic " + encodedAuth);
		headers.put(Constants.CONTENT_EXTRACT_HEADER_FIELD, Constants.CONTENT_EXTRACT_HEADER_VALUE);
		headers.put(Constants.CONTENT_TYPE, Constants.ACCEPT_JSON);

		// Execute GET request
		JsonNode responseJson = httpUtilities.httpGetResponse(apiUrl, headers);
		if (responseJson == null) {
			throw new RuntimeException("Failed to fetch component JSON from API: " + apiUrl);
		}

		// Track activity
		activityTracking.addActivity(userName, apiUrl, Constants.ACTIVITY_TYPE, clientName, Constants.COMPONENT);

		return responseJson;
	}}