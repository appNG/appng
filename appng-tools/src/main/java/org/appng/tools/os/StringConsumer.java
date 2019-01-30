/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.tools.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.appng.tools.os.Command.StreamConsumer;

/**
 * Basic {@link StringConsumer}-implementation which reads from an {@link InputStream} line-by-line and stores the
 * result in a {@link List} of {@link String}s.
 * 
 * @author Matthias Müller
 * 
 */
public class StringConsumer implements StreamConsumer<List<String>> {

	private List<String> lines;

	public void consume(InputStream is) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			lines = new ArrayList<String>();
			String line = null;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}
	}

	public List<String> getResult() {
		return lines;
	}

}
