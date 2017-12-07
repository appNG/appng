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
package org.appng.tools.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

public class PropertyConstantCreator {

	/**
	 * Generates a .java file containing constants for all the keys defined in the given property-file. For the
	 * constant's name, dots (.) and Dashes (-) are replaced with an underscore (_). Also, for a camelcase an underscore
	 * is being added.
	 * 
	 * @param args
	 *            args[0] - the path to the property file to use<br/>
	 *            args[1] - the fully qualified name of the target class to generate<br/>
	 *            args[2] - the outputfolder for the generated class<br/>
	 * @throws IOException
	 *             if the property file can not be found or the target class can not be written
	 * @throws IllegalArgumentException
	 *             if one of the parameters is missing
	 */
	public static void main(String[] args) throws IOException {

		if (args.length != 3) {
			throw new IllegalArgumentException("need 3 params (filePath, targetClass, outFolder)");
		}

		String filePath = args[0];
		String targetClass = args[1];
		String outfolder = args[2];

		if (!targetClass.matches("([a-zA-Z]+[0-9]*)+(\\.[a-zA-Z]+[0-9]*)*")) {
			throw new IllegalArgumentException("not a valid classname: " + targetClass);
		}

		File file = new File(filePath);
		Properties props = new Properties();
		props.load(new FileInputStream(file));

		int pckg = targetClass.lastIndexOf(".");
		StringBuilder sb = new StringBuilder();
		sb.append("package " + targetClass.substring(0, pckg) + ";\r\n");
		sb.append("\r\n");
		sb.append("public class " + targetClass.substring(pckg + 1) + " {\r\n");
		sb.append("\r\n");
		Set<Object> keySet = props.keySet();
		SortedSet<Object> sorted = new TreeSet<Object>(keySet);
		for (Object object : sorted) {
			String key = (String) object;
			sb.append("\t/** " + props.getProperty(key).replace("*/", "*&#47;") + " */\r\n");
			sb.append("\tpublic static final String ");
			String constantName = key.replaceAll("\\.", "_").replaceAll("-", "_");
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
			sb.append(" = \"" + key + "\";\r\n");
		}
		sb.append("\r\n");
		sb.append("}");
		String fileName = targetClass.replaceAll("\\.", "/") + ".java";
		File outFile = new File(new File(outfolder).getAbsoluteFile(), fileName);
		outFile.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(outFile);
		fos.write(sb.toString().getBytes());
		fos.close();
	}
}
