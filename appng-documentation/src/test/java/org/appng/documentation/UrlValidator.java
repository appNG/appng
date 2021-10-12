/*
 * Copyright 2011-2021 the original author or authors.
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
package org.appng.documentation;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class UrlValidator {

	public static void main(String[] args) throws IOException {
		File in = new File("target/generated-docs/html/developerguide.html");
		Document doc = Jsoup.parse(in, StandardCharsets.UTF_8.name());
		for (Element element : doc.getElementsByTag("a")) {
			String href = element.attr("href");
			if (href.startsWith("http") && !href.contains("localhost")) {
				try {
					HttpURLConnection con = (HttpURLConnection) new URL(href).openConnection();
					con.setRequestMethod("HEAD");
					con.setRequestProperty("User-Agent", "Wget");
					con.connect();
					int responseCode = con.getResponseCode();
					if (200 != responseCode) {
						System.err.println(responseCode + " :  " + href);
					}
				} catch (Exception e) {
					System.err.println(e.getMessage() + " " + href);
				}

				String target = element.attr("target");
				if (!"_blank".equals(target)) {
					System.err.println("External link '" + href + "' misses target=\"_blank\"");
				}
			} else if (href.startsWith("#")) {
				Element elementById = doc.getElementById(href.substring(1));
				if (null == elementById) {
					System.err.println(href + " not found!");
				}
			}
		}

	}
}
