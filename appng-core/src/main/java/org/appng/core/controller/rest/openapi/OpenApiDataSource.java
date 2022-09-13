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
package org.appng.core.controller.rest.openapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.ApplicationRequest;
import org.appng.core.model.ApplicationProvider;
import org.appng.openapi.model.ActionLink;
import org.appng.openapi.model.Datasource;
import org.appng.openapi.model.Field;
import org.appng.openapi.model.FieldType;
import org.appng.openapi.model.FieldValue;
import org.appng.openapi.model.Filter;
import org.appng.openapi.model.Icon;
import org.appng.openapi.model.Item;
import org.appng.openapi.model.Link;
import org.appng.openapi.model.Link.TypeEnum;
import org.appng.openapi.model.OptionType;
import org.appng.openapi.model.Options;
import org.appng.openapi.model.Page;
import org.appng.openapi.model.PageSize;
import org.appng.openapi.model.Parameter;
import org.appng.openapi.model.Sort.OrderEnum;
import org.appng.openapi.model.User;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Linkmode;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.PanelLocation;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.Sort;
import org.appng.xml.platform.Validation;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
abstract class OpenApiDataSource extends OpenApiOperation {

	public OpenApiDataSource(Site site, Application application, Request request, MessageSource messageSource)
			throws JAXBException {
		super(site, application, request, messageSource);
	}

	@GetMapping(path = {
			// without parameters
			"/openapi/datasource/{id}",
			// with matrix-style parameters
			"/openapi/datasource/{id}/**" })
	public ResponseEntity<Datasource> getDataSource(
	// @formatter:off
		@PathVariable(name = "id") String dataSourceId,		
		Environment environment,
		HttpServletRequest httpServletRequest, 
		HttpServletResponse httpServletResponse
	// @formatter:on
	) throws ProcessingException, JAXBException, InvalidConfigurationException, org.appng.api.ProcessingException {
		ApplicationProvider applicationProvider = (ApplicationProvider) application;

		org.appng.xml.platform.Datasource originalDatasource = applicationProvider.getApplicationConfig()
				.getDatasource(dataSourceId);
		if (null == originalDatasource) {
			LOGGER.debug("Datasource {} not found on application {} of site {}", dataSourceId, application.getName(),
					site.getName());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		applyPathParameters(httpServletRequest, originalDatasource.getConfig(), request);

		org.appng.xml.platform.Datasource processedDataSource = applicationProvider.processDataSource(
				httpServletResponse, false, (ApplicationRequest) request, dataSourceId, marshallService);

		if (null == processedDataSource) {
			LOGGER.debug("Datasource {} not found on application {} of site {}", dataSourceId, application.getName(),
					site.getName());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (httpServletResponse.getStatus() != HttpStatus.OK.value()) {
			LOGGER.debug("Datasource {} on application {} of site {} returned status {}", processedDataSource.getId(),
					application.getName(), site.getName(), httpServletResponse.getStatus());
			return new ResponseEntity<>(HttpStatus.valueOf(httpServletResponse.getStatus()));
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Processed datasource: {}", marshallService.marshallNonRoot(processedDataSource));
		}

		Datasource datasource = transformDataSource(environment, httpServletResponse, applicationProvider,
				processedDataSource);
		return new ResponseEntity<Datasource>(datasource, HttpStatus.OK);
	}

	protected Datasource transformDataSource(Environment environment, HttpServletResponse httpServletResponse,
			ApplicationProvider applicationProvider, org.appng.xml.platform.Datasource processedDataSource) {
		MetaData metaData = processedDataSource.getConfig().getMetaData();
		addValidationRules(metaData);

		Datasource datasource = new Datasource();
		String id = processedDataSource.getId();
		datasource.setId(id);
		User user = getUser(environment);
		Label title = processedDataSource.getConfig().getTitle();
		if (null != title) {
			datasource.setTitle(title.getValue());
		}
		datasource.setUser(user);
		datasource.setParameters(getParameters(processedDataSource.getConfig().getParams(), false));
		datasource.setPermissions(getPermissions(processedDataSource.getConfig().getPermissions()));

		StringBuilder self = getSelf("/datasource/" + id);
		boolean hasQueryParams = appendParams(processedDataSource.getConfig().getParams(), self);
		datasource.setSelf(self.toString());

		processedDataSource.getConfig().getLinkpanel().stream()
				.filter(lp -> !lp.getLocation().equals(PanelLocation.INLINE)).forEach(lp -> {
					lp.getLinks().forEach(l -> {
						datasource.addLinksItem(getLink(l));
					});
				});

		processedDataSource.getConfig().getMetaData().getFields().forEach(f -> {
			if (!org.appng.xml.platform.FieldType.LINKPANEL.equals(f.getType())) {
				datasource.addFieldsItem(getField(self.toString(), id, f, hasQueryParams));
			}
		});

		Data data = processedDataSource.getData();
		if (null != data) {
			String filterResetLink = addFilters(processedDataSource, datasource, hasQueryParams);
			datasource.setFilterResetPath(self.toString() + filterResetLink);

			Resultset resultset = data.getResultset();
			if (null != resultset) {
				Page page = new Page();
				page.setNumber(resultset.getChunk());
				page.setIsFirst(page.getNumber() == resultset.getFirstchunk());
				page.setIsLast(page.getNumber() == resultset.getLastchunk());
				page.setSize(resultset.getChunksize());
				page.setTotalItems(resultset.getHits());
				page.setTotalPages(resultset.getLastchunk() + 1);
				page.setFirst(getPageLink(hasQueryParams, self.toString(), id, page.getSize(), 0));

				if (!Boolean.TRUE.equals(page.getIsFirst())) {
					page.setPrevious(
							getPageLink(hasQueryParams, self.toString(), id, page.getSize(), page.getNumber() - 1));
				}
				if (!Boolean.TRUE.equals(page.getIsLast())) {
					page.setNext(
							getPageLink(hasQueryParams, self.toString(), id, page.getSize(), page.getNumber() + 1));
				}

				page.setLast(
						getPageLink(hasQueryParams, self.toString(), id, page.getSize(), resultset.getLastchunk()));
				resultset.getResults().forEach(r -> {
					datasource.addItemsItem(getItem(null, r, metaData, getBindClass(metaData)));
				});

				int[] pageSizes = { 5, 10, 25, 50 };
				for (int size : pageSizes) {
					PageSize pageSize = new PageSize();
					pageSize.setSize(size);
					pageSize.setPath(getPageLink(hasQueryParams, self.toString(), id, size));
					page.addPageSizesItem(pageSize);
				}

				datasource.setPage(page);
			} else {
				datasource.setItem(getItem(data.getSelections(), data.getResult(), metaData, getBindClass(metaData)));
			}
		}

		return datasource;
	}

	protected String getPageLink(boolean hasQueryParams, String self, String id, int pageSize, int page) {
		return getPageLink(hasQueryParams, self, id, pageSize) + encode(";") + encode("page:" + page);
	}

	protected String getPageLink(boolean hasQueryParams, String self, String id, int pageSize) {
		return self.toString() + ";" + getSortParam(id) + "=" + encode("pageSize:" + pageSize);
	}

	private String encode(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			// will not happen
		}
		return value;
	}

	private String addFilters(org.appng.xml.platform.Datasource processedDataSource, Datasource datasource,
			boolean hasQueryParams) {
		StringBuilder filterResetLink = new StringBuilder(hasQueryParams ? "" : ";");
		List<SelectionGroup> selectionGroups = processedDataSource.getData().getSelectionGroups();
		if (null != selectionGroups) {
			selectionGroups.forEach(sg -> {
				AtomicBoolean istFirst = new AtomicBoolean(!hasQueryParams);
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
					filterResetLink.append((istFirst.getAndSet(false) ? "" : ";") + s.getId() + "=");
				});
			});
		}
		return filterResetLink.toString();
	}

	protected Field getField(String self, String dataSourceId, FieldDef f, boolean hasQueryParams) {
		Field field = new Field();
		field.setName(f.getName());
		if (null != f.getLabel()) {
			field.setLabel(f.getLabel().getValue());
		}
		field.setFieldType(FieldType.valueOf(f.getType().name()));
		field.setFormat(f.getFormat());
		Sort s = f.getSort();
		if (s != null) {
			org.appng.openapi.model.Sort sort = new org.appng.openapi.model.Sort();
			if (null != s.getPrio()) {
				sort.setPrio(s.getPrio());
			}
			if (null != s.getOrder()) {
				sort.setOrder(OrderEnum.fromValue(s.getOrder().name().toLowerCase()));
			}

			String sortParam = self + (hasQueryParams ? ";" : "") + getSortParam(dataSourceId) + "=";
			sort.setPathDesc(sortParam + encode(f.getBinding() + ":desc"));
			sort.setPathAsc(sortParam + encode(f.getBinding() + ":asc"));
			field.setSort(sort);
		}
		List<FieldDef> childFields = f.getFields();
		if (null != childFields) {
			field.setFields(childFields.stream().map(fieldDef -> getField(self, dataSourceId, fieldDef, hasQueryParams))
					.collect(Collectors.toMap(Field::getName, Function.identity())));
		}
		Validation validation = f.getValidation();
		if (null != validation) {
			validation.getRules().stream().forEach(r -> field.addRulesItem(getRule(r)));
		}
		return field;
	}

	public String getSortParam(String dataSourceId) {
		return "sort" + StringUtils.capitalize(dataSourceId);
	}

	protected Item getItem(List<Selection> selections, Result r, MetaData metaData, Class<?> bindClass) {
		Item item = new Item();
		item.setFields(new HashMap<>());
		item.setSelected(Boolean.TRUE.equals(r.isSelected()));
		r.getFields().forEach(f -> {

			Optional<FieldDef> fieldDef = metaData.getFields().stream().filter(mf -> mf.getName().equals(f.getName()))
					.findFirst();
			FieldValue fieldValue = getFieldValue(f, fieldDef, bindClass);
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
						Map<String, FieldValue> fieldMap = new LinkedHashMap<>();
						for (FieldValue value : collectedValues) {
							if (null != value.getName()) {
								fieldMap.put(value.getName(), value);
							}
						}
					}
				}
			}
			item.getFields().put(fieldValue.getName(), fieldValue);
		});
		r.getLinkpanel().forEach(lp -> {
			lp.getLinks().forEach(l -> {
				item.addLinksItem(getLink(l));
			});
		});
		return item;
	}

	private List<FieldValue> collectSelectedOptions(List<Option> options) {
		return options.stream().filter(o -> Boolean.TRUE.equals(o.isSelected())).map(o -> {
			FieldValue childValue = new FieldValue();
			childValue.setValue(o.getValue());
			return childValue;
		}).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	protected FieldValue getFieldValue(Datafield data, Optional<FieldDef> fieldDef, Class<?> bindClass) {
		if (fieldDef.isPresent()) {
			FieldValue fv = getFieldValue(data, fieldDef.get(),
					BeanUtils.findPropertyType(fieldDef.get().getBinding(), bindClass));

			List<Datafield> childDataFields = data.getFields();
			if (!childDataFields.isEmpty()) {
				final AtomicInteger i = new AtomicInteger(0);

				if (org.appng.xml.platform.FieldType.LIST_OBJECT.equals(fieldDef.get().getType())) {
					List<Map<String, FieldValue>> objectEntries = new ArrayList<>();
					for (Datafield childData : childDataFields) {
						Map<String, FieldValue> object = new LinkedHashMap<>();
						Optional<FieldDef> childField = getChildField(fieldDef.get(), data, i.get(), childData);
						FieldValue attribute = getFieldValue(childData, childField, bindClass);
						if (null != attribute.getName()) {
							Map<String, FieldValue> attributes = (Map<String, FieldValue>) attribute.getValue();
							for (Entry<String, FieldValue> childEntry : attributes.entrySet()) {
								object.put(childEntry.getKey(), childEntry.getValue());
							}
						}
						i.incrementAndGet();
						objectEntries.add(object);
					}
					fv.setValue(objectEntries);
				} else {
					Map<String, FieldValue> object = new LinkedHashMap<>();
					for (Datafield childData : childDataFields) {
						Optional<FieldDef> childField = getChildField(fieldDef.get(), data, i.get(), childData);
						FieldValue attribute = getFieldValue(childData, childField, bindClass);
						if (null != attribute.getName()) {
							object.put(attribute.getName(), attribute);
						}
						i.incrementAndGet();
					}
					fv.setValue(object);
				}
			} else {
				// simple type, nothing more to do
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
		data.getIcons().forEach(i -> {
			Icon icon = new Icon();
			icon.setName(i.getContent());
			icon.setLabel(i.getLabel());
			fv.addIconsItem(icon);
		});
		fv.setValue(getObjectValue(data, field, type, Collections.emptyList()));
		if (null != fv.getValue() && !org.appng.xml.platform.FieldType.DATE.equals(field.getType())
				&& StringUtils.isNotBlank(field.getFormat())) {
			fv.setFormattedValue(data.getValue());
		}
		return fv;
	}

	protected Link getLink(org.appng.xml.platform.Linkable l) {
		Link link = null;

		if (l instanceof org.appng.xml.platform.Link) {
			link = new Link();
			org.appng.xml.platform.Link ll = (org.appng.xml.platform.Link) l;
			link.setId(ll.getId());
			link.setType(LINK_MAPPING.get(ll.getMode()));
			if (Linkmode.INTERN.equals(ll.getMode())) {
				String managerPath = site.getProperties().getString(SiteProperties.MANAGER_PATH);
				String completePath = String.format("%s/%s/%s%s", managerPath, site.getName(), application.getName(),
						ll.getTarget());
				link.setTarget(completePath);
			} else {
				link.setTarget(ll.getTarget());
			}
		} else if (l instanceof org.appng.xml.platform.OpenapiAction) {
			ActionLink action = new ActionLink();
			org.appng.xml.platform.OpenapiAction al = (org.appng.xml.platform.OpenapiAction) l;
			action.setId(al.getId());

			Params params = ((org.appng.xml.platform.OpenapiAction) l).getParams();
			if (null != params) {
				List<Parameter> parameters = new ArrayList<>();
				params.getParam().forEach(p -> {
					Parameter param = new Parameter();
					param.setName(p.getName());
					param.setValue(p.getValue());
					parameters.add(param);
				});
				action.setParameters(parameters);
			}

			action.setTarget(al.getTarget());
			action.setEventId(al.getEventId());
			action.setInteractive(al.isInteractive());
			action.setType(TypeEnum.ACTION);
			link = action;
		}

		link.setLabel(l.getLabel().getValue());
		link.setIcon(l.getIcon().getContent());
		link.setDefault(Boolean.TRUE.toString().equalsIgnoreCase(l.getDefault()));
		if (null != l.getConfirmation()) {
			link.setConfirmation(l.getConfirmation().getValue());
		}
		return link;
	}

	protected static final Map<Linkmode, Link.TypeEnum> LINK_MAPPING = new HashMap<Linkmode, Link.TypeEnum>();
	static {
		LINK_MAPPING.put(Linkmode.EXTERN, Link.TypeEnum.EXTERN);
		LINK_MAPPING.put(Linkmode.INTERN, Link.TypeEnum.PAGE);
		LINK_MAPPING.put(Linkmode.WEBSERVICE, Link.TypeEnum.INTERN);
	}

	Logger getLogger() {
		return LOGGER;
	}
}
