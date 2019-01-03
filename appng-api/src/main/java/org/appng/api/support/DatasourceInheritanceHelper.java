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
package org.appng.api.support;

import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.BeanOption;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a helper class providing some static methods to process the inheritance of datasource definitions found in a
 * resource. If a datasource id contains the inheritance separator '::' appNG clones the ancestor datasource definition
 * and overrides or adds:
 * <ul>
 * <li>datasource id - override</li>
 * <li>title - override</li>
 * <li>parameter - add</li>
 * <li>bind class - override</li>
 * <li>fields - add</li>
 * <li>bean id - override</li>
 * <li>bean options - add</li>
 * <li>linkpanel - override</li>
 * <li>config permissions - override</li>
 * <li>config labels - add</li>
 * </ul>
 * 
 * The simplest usage of this inheritance mechanism can be used to create a clone of an existing datasource definition
 * just with another datasource id to be able to place the datasource a second time with other parameters on the same
 * page.
 * 
 * @author Claus Stümke, aiticon GmbH, 2016
 *
 */
public class DatasourceInheritanceHelper {

	private static String INHERITANCE_SEPARATOR = "::";

	private static final Logger log = LoggerFactory.getLogger(ApplicationConfigProviderImpl.class);

	/**
	 * Returns {@code true} if the id contains exactly one inheritance separator string "::"
	 * 
	 * @param datasourceId
	 * @return {@code true} if the id contains an inheritance separator string
	 */
	public static boolean isInheriting(String datasourceId) {
		return (StringUtils.countMatches(datasourceId, "::") == 1) && (!StringUtils.startsWith(datasourceId, "::"))
				&& (!StringUtils.endsWith(datasourceId, "::"));
	}

	/**
	 * returns the part of the datasource id of a datatsource for inheritance which indicates the final id of the
	 * descendant
	 * 
	 * @param datasourceId
	 * @return the descendant's id
	 */
	public static String getDescendantId(String datasourceId) {
		return datasourceId.split(INHERITANCE_SEPARATOR)[0];
	}

	/**
	 * returns the part of the datasource id of a datatsource for inheritance which indicates the id of the ancestor
	 * datasource
	 * 
	 * @param datasourceId
	 * @return the ancestor's id
	 */
	public static String getAncestorId(String datasourceId) {
		return datasourceId.split(INHERITANCE_SEPARATOR)[1];
	}

	/**
	 * clones the ancestor datasource and adds or overrides id, title, parameter, fields, bean options, bean id, bind
	 * class and linkpanel. Returns the new datasource.
	 * 
	 * @param descendantDefinition
	 * @param ancestor
	 * @return the cloned {@link Datasource}
	 */
	public static Datasource inherit(Datasource descendantDefinition, Datasource ancestor,
			MarshallService marshallService) {
		log.info("process inheritance for " + descendantDefinition.getId());

		String marshalledAncestor;
		Datasource descendant = null;
		try {
			marshalledAncestor = marshallService.marshallNonRoot(ancestor);
			descendant = (Datasource) marshallService.unmarshall(marshalledAncestor);
		} catch (JAXBException e) {
			log.error("error while marshalling/unmarshalling datasource" + ancestor.getId(), e);
			return null;
		}

		DataConfig descendantDefConfig = descendantDefinition.getConfig();
		DataConfig descendantConfig = descendant.getConfig();

		// ID
		descendant.setId(getDescendantId(descendantDefinition.getId()));

		// title
		if (null != descendantDefConfig.getTitle()) {
			if (StringUtils.isNotBlank(descendantDefConfig.getTitle().getId())
					|| StringUtils.isNotBlank(descendantDefConfig.getTitle().getValue())) {
				log.info("override title");
				descendantConfig.setTitle(descendantDefConfig.getTitle());
			}
		}

		// description
		if (null != descendantDefConfig.getDescription()) {
			descendantConfig.setDescription(descendantDefConfig.getDescription());
			log.info("override description");
		}
		// labels are merged
		if (null != descendantDefConfig.getLabels()) {
			List<Label> inheritedLabels = descendantConfig.getLabels().getLabels();
			for (Label l : descendantDefConfig.getLabels().getLabels()) {
				Label orig = findLabel(inheritedLabels, l.getId());
				if (null != orig) {
					inheritedLabels.remove(orig);
					log.info("remove label with id " + orig.getId());
				}
				inheritedLabels.add(l);
				log.info("add label with id " + l.getId());
			}
		}

		// permissions are not merged.
		if (null != descendantDefConfig.getPermissions()) {
			descendantConfig.setPermissions(descendantDefConfig.getPermissions());
			log.info("override permissions");
		}

		// parameter
		if (null != descendantDefConfig.getParams()) {
			List<Param> inheritedParams = descendantConfig.getParams().getParam();
			for (Param p : descendantDefConfig.getParams().getParam()) {
				Param origP = getParam(inheritedParams, p.getName());
				if (null != origP) {
					inheritedParams.remove(origP);
					log.info("remove original parameter " + p.getName());
				}
				inheritedParams.add(p);
				log.info("add parameter " + p.getName());
			}
		}

		// bind class
		descendantConfig.getMetaData().setBindClass(descendantDefConfig.getMetaData().getBindClass());

		// binding
		if (StringUtils.isNotEmpty(descendantDefConfig.getMetaData().getBinding())) {
			descendantConfig.getMetaData().setBinding(descendantDefConfig.getMetaData().getBinding());
		}

		// fields
		List<FieldDef> inheritedFields = descendantConfig.getMetaData().getFields();
		List<FieldDef> fields = descendantDefConfig.getMetaData().getFields();
		for (FieldDef field : fields) {
			FieldDef origF = getField(inheritedFields, field.getName());
			if (null != origF) {
				int index = inheritedFields.indexOf(origF);
				inheritedFields.remove(origF);
				inheritedFields.add(index, field);
				log.info("override field " + field.getName() + " at position " + index);
			} else {
				// place it before "action" field if there is a action field. If not,
				// put it in front of the field list
				FieldDef linkpanelField = getFirstFieldOfType(inheritedFields, FieldType.LINKPANEL);
				if (null == linkpanelField) {
					inheritedFields.add(0, field);
					log.info("add new field " + field.getName() + " at position 0");
				} else {
					int index = inheritedFields.indexOf(linkpanelField);
					inheritedFields.add(index, field);
					log.info("add new field " + field.getName() + " before linkpanel field at position " + index);
				}
			}
		}

		// bean-id
		if (StringUtils.isNotBlank(descendantDefinition.getBean().getId())) {
			descendant.getBean().setId(descendantDefinition.getBean().getId());
			log.info("override bean id " + descendantDefinition.getBean().getId());

			// it does not make sense to keep the old bean options if the bean id has been overwritten
			descendant.getBean().getOptions().clear();
		}

		// bean options
		List<BeanOption> inheritedOptions = descendant.getBean().getOptions();
		for (BeanOption b : descendantDefinition.getBean().getOptions()) {
			BeanOption origB = getBeanOption(inheritedOptions, b.getName());
			if (null != origB) {
				inheritedOptions.remove(origB);
				log.info("remove original bean option " + origB.getName());
			}
			inheritedOptions.add(b);
			log.info("add bean option " + b.getName());
		}

		// linkpanel
		List<Linkpanel> inheritedLinkPanels = descendantConfig.getLinkpanel();
		for (Linkpanel lp : descendantDefConfig.getLinkpanel()) {
			Linkpanel origLp = getLinkpanel(inheritedLinkPanels, lp.getId());
			if (null != origLp) {
				// may we could an attribute to allow adding or overwriting links within a link panel
				int index = inheritedLinkPanels.indexOf(origLp);
				inheritedLinkPanels.remove(origLp);
				inheritedLinkPanels.add(index, lp);
				log.info("override linkpanel with id " + origLp.getId());
			} else {
				// add the complete panel if it does not exist
				inheritedLinkPanels.add(lp);
				log.info("add linkpanel with id " + lp.getId());
			}
		}
		return descendant;
	}

	private static Label findLabel(List<Label> inheritedLabels, String id) {
		for (Label l : inheritedLabels) {
			if (l.getId().equals(id)) {
				return l;
			}
		}
		return null;
	}

	private static Linkpanel getLinkpanel(List<Linkpanel> inheritedLinkPanels, String id) {
		for (Linkpanel lp : inheritedLinkPanels) {
			if (lp.getId().equals(id)) {
				return lp;
			}
		}
		return null;
	}

	private static BeanOption getBeanOption(List<BeanOption> inheritedOptions, String name) {
		for (BeanOption o : inheritedOptions) {
			if (o.getName().equals(name)) {
				return o;
			}
		}
		return null;
	}

	private static FieldDef getField(List<FieldDef> inheritedFields, String name) {
		for (FieldDef f : inheritedFields) {
			if (f.getName().equals(name)) {
				return f;
			}
		}
		return null;
	}

	private static FieldDef getFirstFieldOfType(List<FieldDef> inheritedFields, FieldType type) {
		for (FieldDef f : inheritedFields) {
			if (f.getType().equals(type)) {
				return f;
			}
		}
		return null;
	}

	private static Param getParam(List<Param> params, String name) {
		for (Param p : params) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

}
