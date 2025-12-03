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

	public JsonNode componentExtractJson(String userEmail, String domainUrl, String domainName) {

		Map<String, String> headers = new HashMap<>();
		String credentials = environment.getProperty("AEM_PAGE_USERNAME")+ ":" + environment.getProperty("AEM_PAGE_PASSWORD");
		String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
		headers.put(Constants.AUTHORIZATION_HEADER, "Basic " + encodedAuth);
		headers.put(Constants.CONTENT_TYPE, Constants.ACCEPT_JSON);

		domainUrl=String.format(Constants.CONTENT_EXTRACT_BASE_URL,domainUrl,Constants.CONTENT_EXTRACT_MAPPING);

		// Execute GET request
		JsonNode responseJson = httpUtilities.httpGetResponse(domainUrl, headers);
		if (responseJson == null) {
			throw new RuntimeException("Failed to fetch component JSON from API: " + domainUrl);
		}

		// Track activity
		activityTracking.addActivity(userEmail, domainUrl, Constants.ACTIVITY_TYPE, domainName, Constants.COMPONENT);

		return responseJson;
	}}