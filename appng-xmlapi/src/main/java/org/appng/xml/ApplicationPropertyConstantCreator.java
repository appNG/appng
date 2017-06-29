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
package org.appng.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.Property;

public class ApplicationPropertyConstantCreator {

	/**
	 * Generates a .java file containing constants for all the {@link org.appng.xml.application.Properties} defined in
	 * the given {@code application.xml}.
	 * 
	 * @param args
	 *            args[0] - the path to {@code application.xml} (required)<br/>
	 *            args[1] - the fully qualified name of the target class to generate (required)<br/>
	 *            args[2] - the output-folder for the generated class (required)<br/>
	 *            args[3] - a prefix for the name of the generated constants (optional)<br/>
	 * @throws IOException
	 *             if the file can not be found or the target class can not be written
	 * @throws JAXBException
	 *             if the file can not unmarshalled
	 * @throws IllegalArgumentException
	 *             if one of the parameters is missing
	 */
	public static void main(String[] args) throws IOException, JAXBException {

		if (args.length < 3) {
			throw new IllegalArgumentException("need 3 params (filePath, targetClass, outFolder)");
		}

		String filePath = args[0];
		String targetClass = args[1];
		String outfolder = args[2];
		String prefix = args.length == 4 ? args[3] : "";

		if (!targetClass.matches("([a-zA-Z]+[0-9]*)+(\\.[a-zA-Z]+[0-9]*)*")) {
			throw new IllegalArgumentException("not a valid classname: " + targetClass);
		}

		File file = new File(filePath);
		Properties props = new Properties();
		props.load(new FileInputStream(file));

		ApplicationInfo application = MarshallService.getApplicationMarshallService().unmarshall(file,
				ApplicationInfo.class);
		List<Property> properties = application.getProperties().getProperty();
		Collections.sort(properties, new Comparator<Property>() {
			public int compare(Property o1, Property o2) {
				return o1.getId().compareToIgnoreCase(o2.getId());
			}
		});
		String separator = System.getProperty("line.separator");
		int pckg = targetClass.lastIndexOf(".");
		StringBuilder sb = new StringBuilder();
		sb.append("package " + targetClass.substring(0, pckg) + ";" + separator);
		sb.append(separator);
		sb.append("/** Property constants for " + application.getName() + " " + application.getVersion() + " */");
		sb.append(separator);
		sb.append("public class " + targetClass.substring(pckg + 1) + " {" + separator);
		sb.append(separator);

		for (Property property : properties) {
			if (StringUtils.isNotBlank(property.getDescription())) {
				sb.append("\t/** " + property.getDescription() + " */" + separator);
			}
			sb.append("\tpublic static final String ");
			sb.append(prefix);
			String[] tokens = StringUtils.splitByCharacterTypeCamelCase(property.getId());
			for (int i = 0; i < tokens.length; i++) {
				String s = tokens[i];
				if (!"_".equals(s)) {
					if (i > 0) {
						sb.append("_");
					}
					sb.append(s.toUpperCase());
				}
			}
			sb.append(" = \"" + property.getId() + "\";" + separator);
		}
		sb.append(separator);
		sb.append("}");
		String fileName = targetClass.replaceAll("\\.", "/") + ".java";
		File outFile = new File(new File(outfolder).getAbsoluteFile(), fileName);
		outFile.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(outFile);
		fos.write(sb.toString().getBytes());
		fos.close();
	}
}
