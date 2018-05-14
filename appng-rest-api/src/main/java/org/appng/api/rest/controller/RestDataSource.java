/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.api.rest.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.JAXBException;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.rest.model.Datasource;
import org.appng.api.rest.model.Element;
import org.appng.api.rest.model.ErrorModel;
import org.appng.api.rest.model.Field;
import org.appng.api.rest.model.FieldType;
import org.appng.api.rest.model.FieldValue;
import org.appng.api.rest.model.Filter;
import org.appng.api.rest.model.Link;
import org.appng.api.rest.model.OptionType;
import org.appng.api.rest.model.Page;
import org.appng.api.rest.model.Sort.OrderEnum;
import org.appng.api.rest.model.User;
import org.appng.api.support.ApplicationRequest;
import org.appng.core.model.ApplicationProvider;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.PanelLocation;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.Sort;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class RestDataSource extends RestOperation {

	private Request request;

	@Autowired
	public RestDataSource(Request request) {
		this.request = request;
	}

	@GetMapping(path = "/datasource/{id}")
	public ResponseEntity<Datasource> getDataSource(@PathVariable(name = "id") String dataSourceId, Site site,
			Application application, Environment environment, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws ProcessingException, JAXBException,
			InvalidConfigurationException, org.appng.api.ProcessingException {
		ApplicationProvider applicationProvider = (ApplicationProvider) application;

		DatasourceRef datasourceRef = new DatasourceRef();
		datasourceRef.setId(dataSourceId);
		org.appng.xml.platform.Datasource processedDataSource = applicationProvider.processDataSource(
				httpServletResponse, false, (ApplicationRequest) request, dataSourceId,
				MarshallService.getMarshallService());

		if (httpServletResponse.getStatus() != HttpStatus.OK.value()) {
			return new ResponseEntity<>(HttpStatus.valueOf(httpServletResponse.getStatus()));
		}

		Datasource datasource = new Datasource();
		datasource.setId(dataSourceId);
		User user = getUser(environment);
		datasource.setUser(user);
		datasource.setParameters(getParameters(processedDataSource.getConfig().getParams()));

		processedDataSource.getConfig().getLinkpanel().stream()
				.filter(lp -> !lp.getLocation().equals(PanelLocation.INLINE)).forEach(lp -> {
					lp.getLinks().forEach(l -> {
						datasource.addLinksItem(getLink(l));
					});
				});

		processedDataSource.getConfig().getMetaData().getFields().forEach(f -> {
			Field field = new Field();
			if (!org.appng.xml.platform.FieldType.LINKPANEL.equals(f.getType())) {
				field.setName(f.getName());
				field.setLabel(f.getLabel().getValue());
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
				datasource.addFieldsItem(field);
			}
		});

		List<SelectionGroup> selectionGroups = processedDataSource.getData().getSelectionGroups();
		if (null != selectionGroups) {
			selectionGroups.forEach(sg -> {
				sg.getSelections().forEach(s -> {
					Filter filter = new Filter();
					filter.setLabel(s.getTitle().getValue());
					filter.setName(s.getId());
					filter.setType(OptionType.valueOf(s.getType().name()));
					filter.setMultiple(filter.getType().equals(OptionType.CHECKBOX)
							|| filter.getType().equals(OptionType.SELECT_MULTIPLE));
					s.getOptions().forEach(o -> {
						filter.addOptionsItem(getOption(o));
					});
					datasource.addFiltersItem(filter);
				});
			});
		}

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
				page.addElementsItem(getElement(r));
			});
			datasource.setPage(page);
		} else {
			datasource.setElement(getElement(processedDataSource.getData().getResult()));
		}

		Messages messages = environment.getAttribute(Scope.SESSION, Session.Environment.MESSAGES);
		datasource.setMessages(getMessages(messages));

		postProcessDataSource(datasource, site, applicationProvider, environment);
		return new ResponseEntity<Datasource>(datasource, HttpStatus.OK);
	}

	protected Element getElement(Result r) {
		Element element = new Element();
		element.setSelected(Boolean.TRUE.equals(r.isSelected()));
		r.getFields().forEach(f -> {
			FieldValue fv = new FieldValue();
			fv.setName(f.getName());
			fv.setValue(f.getValue());
			element.addFieldsItem(fv);
		});
		r.getLinkpanel().forEach(lp -> {
			lp.getLinks().forEach(l -> {
				element.addLinksItem(getLink(l));
			});
		});
		return element;
	}

	protected Link getLink(org.appng.xml.platform.Link l) {
		Link link = new Link();
		link.setName(l.getLabel().getValue());
		link.setTarget(l.getTarget());
		link.setIcon(l.getIcon().getContent());
		link.setDefault(Boolean.TRUE.toString().equalsIgnoreCase(l.getDefault()));
		return link;
	}

	@ExceptionHandler
	public ResponseEntity<ErrorModel> handleError(Exception e, HttpServletResponse response) {
		return super.handleError(e, response);
	}

	protected void postProcessDataSource(Datasource datasource, Site site, Application application,
			Environment environment) {
		// optionally implemented by subclass
	}

	Logger getLogger() {
		return log;
	}
}
