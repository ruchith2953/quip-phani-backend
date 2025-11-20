package com.quip.coa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.coa.helper.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;

@Service
public class ComponentExtractJson {
	@Autowired
	ActivityTracking activityTracking;
	private final ObjectMapper mapper = new ObjectMapper();

	Utility utility = new Utility();

	public JsonNode componentExtractJson(String userName, String apiUrl, String clientName) throws Exception {
		String auth = "Authorization: Basic cXVpcC1zZXJ2aWNlLXVzZXI6cXVpcFNlcnZpY2VVc2Vy=\n";
		HttpResponse<?> cmp = utility.utilityMethod(apiUrl, auth);
		if(cmp.statusCode() == 200)
			activityTracking.addActivity(userName, apiUrl, "aem_data_extract", clientName, "component");
		String cmp1 = (String) cmp.body();
		return mapper.readTree(cmp1);
	}
}