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
package org.appng.tools.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

public class PropertyConstantCreator {

	/**
	 * Generates a .java file containing constants for all the keys defined in the given property-file. For the
	 * constant's name, dots (.) and Dashes (-) are replaced with an underscore (_). Also, for a camel-case an
	 * underscore is being added.<br/>
	 * Usage:<br/>
	 * 
	 * <pre>
	 * PropertyConstantCreator
	 * 		.main(new String[] { "path/to/file.properties", "org.appng.example.Constants", "target/constants" })
	 * </pre>
	 * 
	 * @param args
	 *            args[0]* - the path to the property file to use<br/>
	 *            args[1]* - the fully qualified name of the target class to generate<br/>
	 *            args[2]* - the output-folder for the generated class<br/>
	 *            args[3] - the charset used to read the properties file, defaults to
	 *            {@code System.getProperty("file.encoding")}
	 * @throws IOException
	 *             if the property file can not be found or the target class can not be written
	 * @throws IllegalArgumentException
	 *             if one of the parameters is missing
	 */
	public static void main(String[] args) throws IOException {

		if (args.length < 3) {
			throw new IllegalArgumentException(
					"at least 3 parameters needed: filePath* targetClass* outFolder* [charset]");
		}

		String filePath = args[0];
		String targetClass = args[1];
		String outfolder = args[2];
		String charSet = StandardCharsets.UTF_8.name();
		if (args.length == 4) {
			charSet = args[3];
		}

		if (!targetClass.matches("([a-zA-Z]+[0-9]*)+(\\.[a-zA-Z]+[0-9]*)*")) {
			throw new IllegalArgumentException("not a valid classname: " + targetClass);
		}

		Properties props = new Properties();
		props.load(new InputStreamReader(new FileInputStream(filePath), charSet));

		int pckg = targetClass.lastIndexOf(".");
		String lineBreak = System.lineSeparator();
		StringBuilder sb = new StringBuilder();
		if (pckg > 0) {
			sb.append("package " + targetClass.substring(0, pckg) + ";");
			sb.append(lineBreak);
			sb.append(lineBreak);
		}
		sb.append("public class " + targetClass.substring(pckg + 1) + " {");
		sb.append(lineBreak);
		sb.append(lineBreak);
		Set<Object> keySet = new TreeSet<>(props.keySet());
		for (Object object : keySet) {
			String key = (String) object;
			sb.append("\t/** " + props.getProperty(key).replace("*/", "*&#47;") + " */" + lineBreak);
			sb.append("\tpublic static final String ");
			String constantName = key.replace('-', '_').replace('.', '_');
			String[] tokens = StringUtils.splitByCharacterTypeCamelCase(constantName);
			for (int i = 0; i < tokens.length; i++) {
				String s = tokens[i];
				if (!"_".equals(s)) {
					if (i > 0) {
						sb.append("_");
					}
					sb.append(s.toUpperCase());
				}
			}
			sb.append(" = \"" + key + "\";");
			sb.append(lineBreak);
		}
		sb.append(lineBreak);
		sb.append("}");
		String fileName = targetClass.replace('.', '/') + ".java";
		File outFile = new File(new File(outfolder).getAbsoluteFile(), fileName);
		outFile.getParentFile().mkdirs();
		try (FileOutputStream fos = new FileOutputStream(outFile)) {
			fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
		}
	}
}
