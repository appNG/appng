/*
 * Copyright 2011-2022 the original author or authors.
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.BeanOption;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.Datasources;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.Events;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BeanOptionsGenerator {

	private String sourceFolder = "application-home/conf";
	private String packageName = "org.appng.api.business.options";
	private String outFolder = "target/generated-sources/appng/";

	/**
	 * @param args
	 *             args[0] - the sourcefolder to search for XML files containing actions and datasources<br/>
	 *             args[1] - the packageName to use<br/>
	 *             args[2] - the output-folder for the generated classes
	 * 
	 * @throws IOException
	 *                       if the file can not be found or the target class can not be written
	 * @throws JAXBException
	 *                       if a file can not unmarshalled
	 */
	public static void main(String[] args) throws IOException, JAXBException {
		new BeanOptionsGenerator(args[0], args[1], args[2]).create();
	}

	public void create() throws JAXBException, IOException {
		Map</* BeanName */String, Map</* OptionName */ String, /* OptionAttributes */ Set<String>>> optionPerBean = new HashMap<>();
		Map<String, Map<String, Set<String>>> collectBeanOptions = collectBeanOptions(optionPerBean,
				new File(sourceFolder));
		for (Entry<String, Map<String, Set<String>>> bean : collectBeanOptions.entrySet()) {

			if (!bean.getValue().isEmpty()) {

				StringBuilder source = new StringBuilder("package ").append(packageName).append(";\r\n\r\n");

				source.append("import org.appng.api.Options;\r\n");
				source.append("import lombok.AccessLevel;\r\n");
				source.append("import lombok.AllArgsConstructor;\r\n\r\n");

				String className = StringUtils.capitalize(bean.getKey()) + "Options";
				source.append("public class " + className + " {\r\n\r\n");

				source.append("\tpublic @AllArgsConstructor(access = AccessLevel.PRIVATE) static class Option {\r\n");
				source.append("\t\tprivate final org.appng.api.Option inner;\r\n");
				source.append("\t\tprivate final String name;\r\n");
				source.append("\t\tpublic @Override String toString() { return inner.getString(name);}\r\n");
				source.append("\t\tpublic Boolean toBoolean() { return inner.getBoolean(name);}\r\n");
				source.append("\t\tpublic Integer toInt() { return inner.getInteger(name);}\r\n");
				source.append("\t\tpublic Long toLong() { return Long.valueOf(inner.getString(name));}\r\n");
				source.append("\t\tpublic Double toDouble() { return Double.valueOf(inner.getString(name));}\r\n");
				source.append(
						"\t\tpublic <E extends Enum<E>> E toEnum(Class<E> type) { return inner.getEnum(name, type);}\r\n");
				source.append("\t}\r\n\r\n");

				// 1. create static class for each option
				for (String option : new TreeSet<>(bean.getValue().keySet())) {
					Set<String> attributes = bean.getValue().get(option);
					if (!attributes.isEmpty()) {
						source.append("\tpublic @AllArgsConstructor(access = AccessLevel.PRIVATE) static class ");
						source.append(option).append(" {");
						for (String attribute : new TreeSet<>(attributes)) {
							source.append(" public final Option ").append(attribute).append(";");
						}
						source.append("}\r\n");
					}
				}
				source.append("\r\n");

				// 2. create a field for each option
				for (String option : new TreeSet<>(bean.getValue().keySet())) {
					source.append("\tpublic final ").append(option).append(" ").append(option).append(";\r\n");
				}
				source.append("\r\n");

				// 3. create constructor
				source.append("\tprivate ").append(className).append("(Options options) {\r\n");
				for (String option : new TreeSet<>(bean.getValue().keySet())) {
					Set<String> values = new TreeSet<>(bean.getValue().get(option));
					source.append("\t\t").append(option).append(" =  new ").append(option).append("(");
					int idx = 0;
					int size = values.size();
					for (String attr : values) {
						source.append("new Option(options.getOption(\"").append(option).append("\"),\"").append(attr)
								.append("\")");
						if (++idx < size) {
							source.append(", ");
						}
					}
					source.append(");\r\n");
				}
				source.append("\t}\r\n\r\n");

				source.append("\tpublic static " + className + " of(Options options) { return new " + className
						+ "(options); }\r\n");

				source.append("}\r\n");
				System.err.println(source);
				File java = new File(outFolder + "/" + packageName.replace('.', '/'), className + ".java");
				java.getParentFile().mkdirs();
				Path outPath = java.toPath();
				Files.write(outPath, source.toString().getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	public Map<String, Map<String, Set<String>>> collectBeanOptions(Map<String, Map<String, Set<String>>> optionPerBean,
			File folder) throws JAXBException {
		File[] files = folder.listFiles();
		if (null != files) {
			for (File file : files) {
				if (file.isFile()) {
					Object unmarshalled = MarshallService.getMarshallService().unmarshall(file);
					if (unmarshalled instanceof Datasource) {
						collectBean(optionPerBean, Datasource.class.cast(unmarshalled).getBean());
					} else if (unmarshalled instanceof Datasources) {
						Datasources.class.cast(unmarshalled).getDatasourceList()
								.forEach(d -> collectBean(optionPerBean, d.getBean()));
					} else if (unmarshalled instanceof Action) {
						collectBean(optionPerBean, Action.class.cast(unmarshalled).getBean());
					} else if (unmarshalled instanceof Event) {
						Event.class.cast(unmarshalled).getActions()
								.forEach(a -> collectBean(optionPerBean, a.getBean()));
					} else if (unmarshalled instanceof Events) {
						Events.class.cast(unmarshalled).getEventList()
								.forEach(e -> e.getActions().forEach(a -> collectBean(optionPerBean, a.getBean())));
					}
				} else {
					collectBeanOptions(optionPerBean, file);
				}
			}
		}
		return optionPerBean;
	}

	public void collectBean(Map<String, Map<String, Set<String>>> optionPerBean, Bean bean) {
		String beanId = bean.getId();
		if (StringUtils.isNotBlank(beanId)) {
			Map<String, Set<String>> options;
			if (optionPerBean.containsKey(beanId)) {
				options = optionPerBean.get(beanId);
			} else {
				options = new HashMap<>();
				optionPerBean.put(beanId, options);
			}
			List<BeanOption> beanOptions = bean.getOptions();
			Set<String> optionNames;
			for (BeanOption opt : beanOptions) {
				if (options.containsKey(opt.getName())) {
					optionNames = options.get(opt.getName());
				} else {
					optionNames = new TreeSet<>();
					options.put(opt.getName(), optionNames);
				}
				for (QName qname : opt.getOtherAttributes().keySet()) {
					String optionName = qname.getLocalPart();
					optionNames.add(optionName);
				}
			}
		}
	}

}
