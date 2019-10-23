package org.appng.core.controller;

import javax.servlet.http.HttpServletRequest;

import org.appng.api.model.Site;
import org.springframework.http.HttpHeaders;

public class CachedResponse extends AppngCache {
	
	public CachedResponse(String id, Site site, HttpServletRequest request, int status, String contentType, byte[] data,
			HttpHeaders headers, int timeToLive) {
		super(id, site, request, status, contentType, data, headers, timeToLive);
	}

}
