package com.quip.coa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AEMDataConsumer {

	@Autowired
	private ComponentExtractJson componentExtractJson;

	@Autowired
	private AemService aemService;

	private final ObjectMapper mapper = new ObjectMapper();

	public JsonNode processAEMData(String domainUrl, String userEmail, String domainName) throws JsonProcessingException {
		JsonNode aemDataJson = componentExtractJson.componentExtractJson(userEmail, domainUrl,domainName);

		int count = aemService.extractAndStore(aemDataJson,domainName);

		return aemDataJson;
	}
}