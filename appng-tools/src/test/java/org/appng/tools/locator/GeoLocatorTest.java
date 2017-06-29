/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appng.tools.locator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class GeoLocatorTest {

	@Test
	public void test() {
		Coordinate location = new GMapGeoLocator() {
			@Override
			public Coordinate locate(String zip, String city, String street, String country) {
				return new Coordinate(50.12d, 8.63d);
			}
		}.locate("60486", "Frankfurt", "Solmstrasse 41", "Deutschland");
		Assert.assertEquals(50.12d, location.getLatitude(), 0.1d);
		Assert.assertEquals(8.63d, location.getLongitude(), 0.1d);
	}

	@Test
	public void testSigned() throws MalformedURLException, GeneralSecurityException, IOException {
		Properties props = new Properties();
		props.put(GMapGeoLocator.GOOGLE_CLIENT_ID, "clientID");
		props.put(GMapGeoLocator.GOOGLE_SIGNING_KEY, "vNIXE0xscrmjlyV-12Nj_BvUPaw=");
		GMapGeoLocator locator = new GMapGeoLocator();
		locator.configure(props);
		String originalUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=New+York&client=clientID";
		URL url = locator.buildUrl(originalUrl);
		Assert.assertEquals(originalUrl + "&signature=chaRF2hTJKOScPr-RQCEhZbSzIE", url.toString());
	}
}
