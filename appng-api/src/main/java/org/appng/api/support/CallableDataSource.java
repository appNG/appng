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
package org.appng.api.support;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.Options;
import org.appng.api.ParameterSupport;
import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.PageReference;
import org.appng.xml.platform.Permissions;
import org.appng.xml.platform.SectionelementDef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@code CallableDataSource} is responsible for preparing and performing an {@link Datasource}, based on a
 * {@link DatasourceRef}. This {@link DatasourceRef} is provided either by a {@link PageDefinition}'s
 * {@link SectionelementDef} or by an {@link Action}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class CallableDataSource {

	private ApplicationRequest applicationRequest;
	private Site site;
	private Application application;
	private DatasourceRef datasourceRef;
	private boolean include = false;
	private ElementHelper elementHelper;
	private Datasource datasource;
	private Integer defaultPageSize;

	/**
	 * Creates a new {@code CallableDataSource} and also determines the return value for {@link #doInclude()}, based on
	 * {@link Permissions} and {@link Condition}s.
	 * 
	 * @param site
	 *                           the current {@link Site}
	 * @param application
	 *                           the current {@link Application}
	 * @param applicationRequest
	 *                           the current {@link ApplicationRequest}
	 * @param parameterSupport
	 *                           the {@link ParameterSupport} holding the parameters for the {@link Datasource}
	 * @param datasourceRef
	 *                           the {@link DatasourceRef} as given in the {@link SectionelementDef} of a
	 *                           {@link PageDefinition} or by an {@link Action}.
	 * 
	 * @throws ProcessingException
	 *                             if an error occurs while assembling the {@code CallableDataSource}
	 */
	public CallableDataSource(Site site, Application application, ApplicationRequest applicationRequest,
			ParameterSupport parameterSupport, DatasourceRef datasourceRef) throws ProcessingException {
		this.site = site;
		this.application = application;
		this.applicationRequest = applicationRequest;
		this.datasourceRef = datasourceRef;
		if (null == datasourceRef.getPageSize()) {
			this.defaultPageSize = site.getProperties().getInteger(SiteProperties.DEFAULT_PAGE_SIZE);
		} else {
			this.defaultPageSize = datasourceRef.getPageSize();
		}
		PermissionProcessor permissionProcessor = applicationRequest.getPermissionProcessor();

		if (permissionProcessor.hasPermissions(new PermissionOwner(datasourceRef))) {
			ExpressionEvaluator includeEvaluator = new ExpressionEvaluator(parameterSupport.getParameters());
			this.elementHelper = new ElementHelper(site, application, includeEvaluator);
			Condition includeCondition = datasourceRef.getCondition();
			if (elementHelper.conditionMatches(includeEvaluator, includeCondition)) {
				this.datasource = applicationRequest.getApplicationConfig().getDatasource(datasourceRef.getId());
				if (null == datasource) {
					throw new ProcessingException("no such datasource: " + datasourceRef.getId(),
							new FieldProcessorImpl(datasourceRef.getId()));
				}
				if (permissionProcessor.hasPermissions(new PermissionOwner(datasource))) {
					this.include = true;
					datasource.setMode(datasourceRef.getMode());
					DataConfig config = getDatasource().getConfig();
					Map<String, String> dataSourceParameters = elementHelper.initializeParameters(
							"datasource '" + datasourceRef.getId() + "'", applicationRequest, parameterSupport,
							datasourceRef.getParams(), config.getParams());

					LOGGER.trace("parameters for datasource '{}': {}", datasourceRef.getId(), dataSourceParameters);

					elementHelper.processConfig(applicationRequest.getApplicationConfig(), applicationRequest,
							datasource.getConfig(), dataSourceParameters);
					Bean bean = getDatasource().getBean();
					if (null != bean) {
						elementHelper.initOptions(bean.getOptions());
					}
					applicationRequest.setLabels(datasource.getConfig());
					LOGGER.debug("including datasource '{}'", datasource.getId());
				} else {
					LOGGER.debug("no permission for datasource '{}'", datasource.getId());
				}
			} else {
				LOGGER.debug("include condition for datasource '{}' did not match - {}", datasourceRef.getId(),
						includeCondition.getExpression());
			}
		} else {
			LOGGER.debug("no permission(s) for datasourceRef '{}'", datasourceRef.getId());
		}
	}

	public Datasource getDatasource() {
		return datasource;
	}
	
	/**
	 * Performs this {@link CallableDataSource}, setting the {@link Bean} {@code null} afterwards.
	 * 
	  * @see #perform(String, boolean, boolean, boolean)
	 */
	public Data perform(String pageId) throws ProcessingException {
		return perform(pageId, true, false, false);
	}

	/**
	 * Performs this {@link CallableDataSource}, setting the {@link Bean} {@code null} afterwards.
	 * 
	 * @see #perform(String, boolean, boolean, boolean)
	 */
	public Data perform(String pageId, boolean addMessagesToSession) throws ProcessingException {
		return perform(pageId, true, false, addMessagesToSession);
	}
	
	/***
	 * @see #perform(String, boolean, boolean, boolean)
	 */
	public Data perform(String pageId, boolean setBeanNull, boolean addValidation) throws ProcessingException {
		return perform(pageId, setBeanNull, addValidation, false);
	}

	/**
	 * Performs the {@link CallableDataSource}.<br/>
	 * Note that the caller needs to check if the {@link Datasource} should be included by itself (by calling
	 * {@link #doInclude()}), as this method doesn't check that condition.
	 * 
	 * @param pageId
	 *                             the ID of the current page
	 * @param setBeanNull
	 *                             whether or not to set the {@link Bean} of the {@link Datasource} to {@code null}
	 *                             after performing
	 * @param addValidation
	 *                             whether or not to add validation metadata
	 * @param addMessagesToSession
	 *                             if {@link Messages} should be added to the session or kept on the {@link Datasource}
	 * 
	 * @return the {@link Data} retrieved from the {@link Datasource} by calling
	 *         {@link DataProvider#getData(Site, Application, org.appng.api.Environment, Options, org.appng.api.Request, org.appng.api.FieldProcessor)}
	 * 
	 * @throws ProcessingException
	 *                             if an error occurs while retrieving the {@code Data}
	 */
	public Data perform(String pageId, boolean setBeanNull, boolean addValidation, boolean addMessagesToSession)
			throws ProcessingException {
		Bean bean = datasource.getBean();
		if (null != bean) {
			FieldProcessorImpl fieldProcessor = null;
			try {
				String beanId = bean.getId();
				LOGGER.trace("retrieving datasource '{}'", beanId);
				DataProvider dataProvider = this.application.getBean(beanId, DataProvider.class);
				LOGGER.trace("datasource '{}' is of Type '{}'", beanId, dataProvider.getClass().getName());
				String id = datasource.getId();
				// datasource.setSource(dataProvider.getClass().getName());

				Options options = elementHelper.getOptions(bean.getOptions());

				MetaData metaData = getDatasource().getConfig().getMetaData();
				fieldProcessor = new FieldProcessorImpl(id, metaData);
				fieldProcessor.addLinkPanels(datasource.getConfig().getLinkpanel());
				SortParamSupport sortParamSupport = getSortParams(pageId, id);
				if (null != sortParamSupport) {
					String sortParam = applicationRequest.getParameter(sortParamSupport.getRequestKey());
					fieldProcessor.setPageable(sortParamSupport.getPageable(sortParam));
				} else {
					fieldProcessor.setPageable(null);
				}

				LOGGER.trace("options for datasource '{}': {}", datasourceRef.getId(), options);

				DataContainer container = dataProvider.getData(site, application, applicationRequest.getEnvironment(),
						options, applicationRequest, fieldProcessor);

				if (null == container) {
					throw new ProcessingException(dataProvider.getClass() + " returned null", fieldProcessor);
				}

				Page<?> page = container.getPage();
				if (null != page) {
					if (page.getTotalPages() > 0 && page.getNumber() >= page.getTotalPages()) {
						String requestKey = sortParamSupport.getRequestKey();
						PageRequest pageable = new PageRequest(0, page.getSize(), page.getSort());
						String sortString = sortParamSupport.getSortString(pageable);
						String servletPath = applicationRequest.getHttpServletRequest().getServletPath();
						String target = servletPath + "?" + requestKey + "=" + sortString;
						LOGGER.debug("invalid page for datasource {} requested, redirecting to {}", id, target);
						site.sendRedirect(applicationRequest.getEnvironment(), target, HttpServletResponse.SC_FOUND);
					}
				} else if (addValidation && null != container.getItem()) {
					applicationRequest.addValidationMetaData(metaData, site.getSiteClassLoader(),
							elementHelper.getValidationGroups(metaData, container.getItem()));
				}

				if (addMessagesToSession) {
					ElementHelper.addMessages(applicationRequest.getEnvironment(), fieldProcessor.getMessages());
				} else {
					getDatasource().setMessages(fieldProcessor.getMessages());
				}

				Data data = container.getWrappedData();
				getDatasource().setData(data);
				elementHelper.setSelectionTitles(data, applicationRequest);
				elementHelper.processDataContainer(applicationRequest, container, dataProvider.getClass().getName());
				if (setBeanNull) {
					getDatasource().setBean(null);
				}
			} catch (Exception e) {
				String id = String.valueOf(e.hashCode());
				if (!elementHelper.isMessageParam(e) && null != fieldProcessor) {
					fieldProcessor.clearMessages();
					String message = applicationRequest.getMessage(ElementHelper.INTERNAL_ERROR, id);
					fieldProcessor.addErrorMessage(message);
				}
				throw new ProcessingException("error retrieving datasource '" + datasource.getId() + "', ID: " + id, e,
						fieldProcessor);
			}
		} else {
			LOGGER.debug("{} is static!", getDatasource().getId());
		}
		return getDatasource().getData();
	}

	private SortParamSupport getSortParams(String pageId, String dsId) {
		if (null != pageId) {
			Environment env = applicationRequest.getEnvironment();
			Map<String, String> sessionParams = application.getSessionParams(site, env);
			return new SortParamSupport(sessionParams, pageId, dsId, defaultPageSize);
		}
		return null;
	}

	/**
	 * Checks whether or not to include this {@code CallableDataSource} on the referencing {@link PageReference} or
	 * {@link Action}.
	 * 
	 * @return {@code true} if this {@code CallableDataSource} should be included, {@code false} otherwise
	 */
	public boolean doInclude() {
		return include;
	}

	@Override
	public String toString() {
		return datasourceRef.getId();
	}

}
