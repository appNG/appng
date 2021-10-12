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
package org.appng.core.controller.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.MessageInterpolator;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.SiteProperties;
import org.appng.api.ValidationProvider;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.rest.model.Datasource;
import org.appng.api.rest.model.Element;
import org.appng.api.rest.model.Field;
import org.appng.api.rest.model.FieldType;
import org.appng.api.rest.model.FieldValue;
import org.appng.api.rest.model.Filter;
import org.appng.api.rest.model.Link;
import org.appng.api.rest.model.OptionType;
import org.appng.api.rest.model.Options;
import org.appng.api.rest.model.Page;
import org.appng.api.rest.model.Sort.OrderEnum;
import org.appng.api.rest.model.User;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.api.support.validation.LocalizedMessageInterpolator;
import org.appng.core.model.ApplicationProvider;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Linkmode;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.PanelLocation;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.Sort;
import org.appng.xml.platform.Validation;
import org.appng.xml.platform.ValidationGroups;
import org.appng.xml.platform.ValidationGroups.Group;
import org.slf4j.Logger;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
abstract class RestDataSourceBase extends RestOperation {

	@Autowired
	public RestDataSourceBase(Site site, Application application, Request request, MessageSource messageSource,
			boolean supportPathParameters) throws JAXBException {
		super(site, application, request, messageSource, supportPathParameters);
	}

	// @formatter:off
	@GetMapping(
		path = {
			"/datasource/{id}",
			"/datasource/{id}/{pathVar1}",
			"/datasource/{id}/{pathVar1}/{pathVar2}",
			"/datasource/{id}/{pathVar1}/{pathVar2}/{pathVar3}",
			"/datasource/{id}/{pathVar1}/{pathVar2}/{pathVar3}/{pathVar4}",
			"/datasource/{id}/{pathVar1}/{pathVar2}/{pathVar3}/{pathVar4}/{pathVar5}"
		}
	)
	// @formatter:on
	public ResponseEntity<Datasource> getDataSource(@PathVariable(name = "id") String dataSourceId,
			@PathVariable(required = false) Map<String, String> pathVariables, Environment environment,
			HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ProcessingException,
			JAXBException, InvalidConfigurationException, org.appng.api.ProcessingException {
		ApplicationProvider applicationProvider = (ApplicationProvider) application;

		if (supportPathParameters) {
			org.appng.xml.platform.Datasource originalDatasource = applicationProvider.getApplicationConfig()
					.getDatasource(dataSourceId);
			applyPathParameters(pathVariables, originalDatasource.getConfig(), request);
		}

		org.appng.xml.platform.Datasource processedDataSource = applicationProvider.processDataSource(
				httpServletResponse, false, (ApplicationRequest) request, dataSourceId, marshallService);

		if (null == processedDataSource) {
			LOGGER.debug("Datasource {} not found on application {} of site {}", dataSourceId, application.getName(),
					site.getName());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		MetaData metaData = processedDataSource.getConfig().getMetaData();
		addValidationRules(metaData);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Processed datasource: {}", marshallService.marshallNonRoot(processedDataSource));
		}
		if (httpServletResponse.getStatus() != HttpStatus.OK.value()) {
			LOGGER.debug("Datasource {} on application {} of site {} returned status {}", dataSourceId,
					application.getName(), site.getName(), httpServletResponse.getStatus());
			return new ResponseEntity<>(HttpStatus.valueOf(httpServletResponse.getStatus()));
		}

		Datasource datasource = new Datasource();
		datasource.setId(dataSourceId);
		User user = getUser(environment);
		datasource.setUser(user);
		datasource.setParameters(getParameters(processedDataSource.getConfig().getParams()));
		datasource.setPermissions(getPermissions(processedDataSource.getConfig().getPermissions()));

		processedDataSource.getConfig().getLinkpanel().stream()
				.filter(lp -> !lp.getLocation().equals(PanelLocation.INLINE)).forEach(lp -> {
					lp.getLinks().forEach(l -> {
						datasource.addLinksItem(getLink(l));
					});
				});

		processedDataSource.getConfig().getMetaData().getFields().forEach(f -> {
			if (!org.appng.xml.platform.FieldType.LINKPANEL.equals(f.getType())) {
				datasource.addFieldsItem(getField(f));
			}
		});

		addFilters(processedDataSource, datasource);

		Resultset resultset = processedDataSource.getData().getResultset();
		if (null != resultset) {
			Page page = new Page();
			page.setIsFirst(resultset.getChunk() == resultset.getFirstchunk());
			page.setIsLast(resultset.getChunk() == resultset.getLastchunk());
			page.setNumber(resultset.getChunk());
			page.setSize(resultset.getChunksize());
			page.setTotalElements(resultset.getHits());
			page.setTotalPages(resultset.getLastchunk() + 1);
			resultset.getResults().forEach(r -> {
				page.addElementsItem(getElement(null, r, metaData));
			});
			datasource.setPage(page);
		} else {
			datasource.setElement(getElement(processedDataSource.getData().getSelections(),
					processedDataSource.getData().getResult(), metaData));
		}

		Messages messages = environment.removeAttribute(Scope.SESSION, Session.Environment.MESSAGES);
		datasource.setMessages(getMessages(messages));

		postProcessDataSource(datasource, site, applicationProvider, environment);
		return new ResponseEntity<Datasource>(datasource, HttpStatus.OK);
	}

	private void addFilters(org.appng.xml.platform.Datasource processedDataSource, Datasource datasource) {
		List<SelectionGroup> selectionGroups = processedDataSource.getData().getSelectionGroups();
		if (null != selectionGroups) {
			selectionGroups.forEach(sg -> {
				sg.getSelections().forEach(s -> {
					Filter filter = new Filter();
					filter.setLabel(s.getTitle().getValue());
					filter.setName(s.getId());
					filter.setType(OptionType.valueOf(s.getType().name()));
					filter.setOptions(new Options());
					filter.getOptions().setMultiple(filter.getType().equals(OptionType.CHECKBOX)
							|| filter.getType().equals(OptionType.SELECT_MULTIPLE));
					s.getOptions().forEach(o -> {
						filter.getOptions().addEntriesItem(getOption(s.getId(), o, Collections.emptyList()));
					});
					datasource.addFiltersItem(filter);
				});
			});
		}
	}

	private void addValidationRules(MetaData metaData) {
		List<Class<?>> validationGroups = new ArrayList<>();
		if (site.getProperties().getBoolean("restDatasourceAddValidationRules", true)) {
			try {
				ValidationGroups validation = metaData.getValidation();
				if (null != validation) {
					for (Group g : validation.getGroups()) {
						Class<?> group = site.getSiteClassLoader().loadClass(g.getClazz());
						validationGroups.add(group);
					}
				}
				Locale locale = request.getLocale();
				MessageInterpolator messageInterpolator = new LocalizedMessageInterpolator(locale, messageSource);
				ValidationProvider validationProvider = new DefaultValidationProvider(messageInterpolator,
						messageSource, locale, true);
				validationProvider.addValidationMetaData(metaData, site.getSiteClassLoader(),
						validationGroups.toArray(new Class[0]));
			} catch (ClassNotFoundException e) {
				getLogger().error("error retrieving validation group", e);
			}
		}
	}

	protected Field getField(FieldDef f) {
		Field field = new Field();
		field.setName(f.getName());
		if (null != f.getLabel()) {
			field.setLabel(f.getLabel().getValue());
		}
		field.setFieldType(FieldType.valueOf(f.getType().name()));
		field.setFormat(f.getFormat());
		Sort s = f.getSort();
		if (s != null) {
			org.appng.api.rest.model.Sort sort = new org.appng.api.rest.model.Sort();
			if (null != s.getPrio()) {
				sort.setPrio(s.getPrio());
			}
			if (null != s.getOrder()) {
				sort.setOrder(OrderEnum.fromValue(s.getOrder().name().toLowerCase()));
			}
			field.setSort(sort);
		}
		List<FieldDef> childFields = f.getFields();
		if (null != childFields) {
			for (FieldDef fieldDef : childFields) {
				field.addFieldsItem(getField(fieldDef));
			}
		}
		Validation validation = f.getValidation();
		if (null != validation) {
			validation.getRules().stream().forEach(r -> field.addRulesItem(getRule(r)));
		}
		return field;
	}

	protected Element getElement(List<Selection> selections, Result r, MetaData metaData) {
		Element element = new Element();
		element.setSelected(Boolean.TRUE.equals(r.isSelected()));
		r.getFields().forEach(f -> {

			Optional<FieldDef> fieldDef = metaData.getFields().stream().filter(mf -> mf.getName().equals(f.getName()))
					.findFirst();
			BeanWrapper beanWrapper = getBeanWrapper(metaData);
			FieldValue fieldValue = getFieldValue(f, fieldDef, beanWrapper);
			if (null != fieldValue && isSelectionType(fieldDef.get().getType()) && null != selections) {
				Selection selection = selections.parallelStream()
						.filter(s -> s.getId().equals(fieldDef.get().getName())).findFirst().orElse(null);
				List<FieldValue> collectedValues = new ArrayList<>();
				if (null != selection) {
					List<Option> options = selection.getOptions();
					collectedValues.addAll(collectSelectedOptions(options));
					selection.getOptionGroups().forEach(g -> {
						collectedValues.addAll(collectSelectedOptions(g.getOptions()));
					});
					if (collectedValues.size() == 1) {
						fieldValue.setValue(collectedValues.get(0).getValue());
					} else {
						fieldValue.setValues(collectedValues);
					}
				}
			}
			element.addFieldsItem(fieldValue);
		});
		r.getLinkpanel().forEach(lp -> {
			lp.getLinks().forEach(l -> {
				element.addLinksItem(getLink(l));
			});
		});
		return element;
	}

	private List<FieldValue> collectSelectedOptions(List<Option> options) {
		return options.stream().filter(o -> Boolean.TRUE.equals(o.isSelected())).map(o -> {
			FieldValue childValue = new FieldValue();
			childValue.setValue(o.getValue());
			return childValue;
		}).collect(Collectors.toList());
	}

	protected FieldValue getFieldValue(Datafield data, Optional<FieldDef> fieldDef, BeanWrapper beanWrapper) {
		if (fieldDef.isPresent()) {
			FieldValue fv = getFieldValue(data, fieldDef.get(),
					beanWrapper.getPropertyType(fieldDef.get().getBinding()));
			List<Datafield> childDataFields = data.getFields();
			if (null != childDataFields) {
				final AtomicInteger i = new AtomicInteger(0);
				for (Datafield childData : childDataFields) {
					Optional<FieldDef> childField = getChildField(fieldDef.get(), data, i.get(), childData);
					FieldValue childValue = getFieldValue(childData, childField, beanWrapper);
					fv.addValuesItem(childValue);
					i.incrementAndGet();
				}
			}
			return fv;
		}
		FieldValue fv = new FieldValue();
		fv.setName(data.getName());
		return fv;
	}

	protected FieldValue getFieldValue(Datafield data, FieldDef field, Class<?> type) {
		FieldValue fv = new FieldValue();
		fv.setName(data.getName());
		fv.setValue(getObjectValue(data, field, type, Collections.emptyList()));
		if (null != fv.getValue() && !org.appng.xml.platform.FieldType.DATE.equals(field.getType())
				&& StringUtils.isNotBlank(field.getFormat())) {
			fv.setFormattedValue(data.getValue());
		}
		return fv;
	}

	protected Link getLink(org.appng.xml.platform.Link l) {
		Link link = new Link();
		link.setLabel(l.getLabel().getValue());
		link.setId(l.getId());
		link.setIcon(l.getIcon().getContent());
		link.setDefault(Boolean.TRUE.toString().equalsIgnoreCase(l.getDefault()));
		if (null != l.getConfirmation()) {
			link.setConfirmation(l.getConfirmation().getValue());
		}
		link.setType(Link.TypeEnum.valueOf(l.getMode().name()));
		if (Linkmode.INTERN.equals(l.getMode())) {
			String managerPath = site.getProperties().getString(SiteProperties.MANAGER_PATH);
			String completePath = String.format("%s/%s/%s%s", managerPath, site.getName(), application.getName(),
					l.getTarget());
			link.setTarget(completePath);
		} else {
			link.setTarget(l.getTarget());
		}
		return link;
	}

	protected void postProcessDataSource(Datasource datasource, Site site, Application application,
			Environment environment) {
		// optionally implemented by subclass
	}

	Logger getLogger() {
		return LOGGER;
	}
}
