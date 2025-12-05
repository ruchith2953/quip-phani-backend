package com.quip.coa.helper;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Utility {

	public String getClientName(String domain){
		String urlSecondPart = domain.substring(8);
		String[] urlArr = urlSecondPart.split("\\.");
		return urlArr[0];
	}

	public HttpResponse<?> utilityMethod(String apiUrl, String auth) throws Exception {
		HttpResponse<?> res = null;
		String url = "http://34.224.16.46:4502/bin/quip/v1/component-extract?siteUrl=" + apiUrl+"&mapping=content-extract";
		HttpClient httpClient = HttpClient.newHttpClient();
		List<String> headerLines = Arrays.stream(auth.split("\n")).map(String::trim).collect(Collectors.toList());
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(new URI(url)).GET();
		for (String headerLine : headerLines) {
			String[] headerParts = headerLine.split(":");
			if (headerParts.length == 2) {
				requestBuilder.header(headerParts[0].trim(), headerParts[1].trim());
			}
		}
		HttpRequest request = requestBuilder.build();
		res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		return res;
	}
	public String extractClientName(String url) {
        String client = url.substring(7);
        String[] clientArray = client.split("\\.");
        if (clientArray.length > 0) {
            return clientArray[0];
        } else {
            return "";
        }
}

	public String getPagePath(String url) {
		try {
			if (!url.startsWith("http://") && !url.startsWith("https://")) {
				url = "https://" + url;
			}

			URI uri = new URI(url);
			return uri.getPath();
		} catch (URISyntaxException e) {
			return null;
		}
	}
}