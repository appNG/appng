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
package org.appng.core.model;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.XPathProcessor;
import org.appng.api.model.Properties;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.templating.ThymeleafReplaceInterceptor;
import org.appng.core.templating.ThymeleafTemplateEngine;
import org.appng.xml.MarshallService.AppNGSchema;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.GetParams;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Labels;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.NavigationItem;
import org.appng.xml.platform.PageReference;
import org.appng.xml.platform.PagesReference;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.appng.xml.platform.Rule;
import org.appng.xml.platform.Section;
import org.appng.xml.platform.Sectionelement;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.Session;
import org.appng.xml.platform.SessionParams;
import org.appng.xml.platform.Sort;
import org.appng.xml.platform.Subject;
import org.appng.xml.platform.Template;
import org.appng.xml.platform.UrlParams;
import org.appng.xml.platform.Validation;
import org.appng.xml.platform.ValidationRule;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.thymeleaf.cache.StandardCacheManager;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.AbstractLinkBuilder;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThymeleafProcessor extends AbstractRequestProcessor {

	private List<Template> templates;
	private DocumentBuilderFactory dbf;

	public ThymeleafProcessor(@Autowired DocumentBuilderFactory dbf) {
		templates = new ArrayList<>();
		this.dbf = dbf;
	}

	public String processWithTemplate(Site applicationSite) throws InvalidConfigurationException {
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String charsetName = platformProperties.getString(Platform.Property.ENCODING);
		Charset charset = Charset.forName(charsetName);
		Boolean devMode = platformProperties.getBoolean(Platform.Property.DEV_MODE);
		this.contentType = HttpHeaders.getContentType(HttpHeaders.CONTENT_TYPE_TEXT_HTML, charsetName);
		String templatePrefix = platformProperties.getString(Platform.Property.TEMPLATE_PREFIX);

		org.appng.xml.platform.Platform platform = processPlatform(applicationSite);
		if (isRedirect()) {
			LOGGER.debug("request is beeing redirected");
			return "redirect";
		}
		platform.setVersion(env.getAttributeAsString(Scope.PLATFORM, Platform.Environment.APPNG_VERSION));

		ApplicationProvider applicationProvider = getApplicationProvider(applicationSite);
		ConfigurableApplicationContext context = applicationProvider.getContext();
		List<ThymeleafReplaceInterceptor> interceptors = null;

		if (null != context) {
			interceptors = new ArrayList<>(context.getBeansOfType(ThymeleafReplaceInterceptor.class).values());
			Collections.sort(interceptors, new Comparator<ThymeleafReplaceInterceptor>() {
				public int compare(ThymeleafReplaceInterceptor o1, ThymeleafReplaceInterceptor o2) {
					return Integer.compare(o1.getPriority(), o2.getPriority());
				}
			});
		}

		// SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		ThymeleafTemplateEngine templateEngine = new ThymeleafTemplateEngine(interceptors);
		StandardCacheManager cacheManager = new StandardCacheManager();
		cacheManager.setExpressionCacheInitialSize(500);
		cacheManager.setExpressionCacheMaxSize(1000);
		templateEngine.setCacheManager(cacheManager);
		if (null != interceptors) {
			for (ThymeleafReplaceInterceptor interceptor : interceptors) {
				// An interceptor can define some template resource to be added to the template
				// resolver
				if (null != interceptor.getAdditionalTemplateResourceNames()) {
					for (String resource : interceptor.getAdditionalTemplateResourceNames()) {
						Template template = new Template();
						template.setPath(resource);
						templates.add(template);
					}
				}

			}
		}

		if (!templates.isEmpty()) {
			// assumes that custom template files are located in the 'tpl'-folder!
			Set<String> patterns = new HashSet<>();
			CacheProvider cacheProvider = new CacheProvider(platformProperties);
			File platformCache = cacheProvider.getPlatformCache(applicationSite, applicationProvider);
			File tplFolder = new File(platformCache, ResourceType.TPL.getFolder());
			for (Template tpl : templates) {
				String tplPath = FilenameUtils.normalize(new File(tplFolder, tpl.getPath()).getAbsolutePath());
				File tplFile = new File(tplPath);
				if (tplFile.exists()) {
					patterns.add(tplFile.getName());
					LOGGER.debug("added template file {}", tplFile);
				} else {
					LOGGER.debug("template file {} does not exist!", tplFile);
				}
			}
			if (!patterns.isEmpty()) {
				FileTemplateResolver appTplResolver = new FileTemplateResolver();
				appTplResolver.setName("Template Resolver for " + applicationProvider.getName());
				appTplResolver.setResolvablePatterns(patterns);
				appTplResolver.setPrefix(tplFolder.getPath() + File.separator);
				appTplResolver.setTemplateMode(TemplateMode.HTML);
				appTplResolver.setCharacterEncoding(charset.name());
				appTplResolver.setCacheable(!devMode);
				appTplResolver.setOrder(0);
				templateEngine.addTemplateResolver(appTplResolver);
			}

			AbstractLinkBuilder appLinkBuilder = new AbstractLinkBuilder() {
				public String buildLink(IExpressionContext context, String base, Map<String, Object> parameters) {
					String resourcePath = FilenameUtils.normalize(new File(tplFolder, base).getAbsolutePath());
					if (new File(resourcePath).exists()) {
						return templatePrefix + "_" + applicationProvider.getName() + "/" + base;
					}
					return null;
				}
			};
			appLinkBuilder.setName("Link Builder for " + applicationProvider.getName());
			appLinkBuilder.setOrder(0);
			templateEngine.addLinkBuilder(appLinkBuilder);
		}

		FileTemplateResolver globalTemplateResolver = new FileTemplateResolver();
		globalTemplateResolver.setName("Global Template Resolver");
		globalTemplateResolver.setResolvablePatterns(Collections.singleton("*"));
		globalTemplateResolver.setPrefix(templatePath + "/" + ResourceType.RESOURCE.getFolder() + "/html/");
		globalTemplateResolver.setTemplateMode(TemplateMode.HTML);
		globalTemplateResolver.setCharacterEncoding(charset.name());
		globalTemplateResolver.setCacheable(!devMode);
		globalTemplateResolver.setOrder(1);
		templateEngine.addTemplateResolver(globalTemplateResolver);
		if (null != context) {
			MessageSource ms = context.getBean(MessageSource.class);
			templateEngine.setTemplateEngineMessageSource(ms);
		}
		AbstractLinkBuilder globalLinkBuilder = new AbstractLinkBuilder() {

			public String buildLink(IExpressionContext context, String base, Map<String, Object> parameters) {
				return templatePrefix + "/" + ResourceType.RESOURCE.getFolder() + base;
			}
		};
		globalLinkBuilder.setName("Global Link Builder");
		globalLinkBuilder.setOrder(1);
		templateEngine.addLinkBuilder(globalLinkBuilder);

		XPathProcessor xpath = null;
		try {
			Document doc = dbf.newDocumentBuilder().newDocument();
			AppNGSchema.PLATFORM.getContext().createMarshaller().marshal(platform, doc);
			xpath = new XPathProcessor(doc);
			xpath.setNamespace("appng", AppNGSchema.PLATFORM.getNamespace());
		} catch (Exception e) {
			throw new InvalidConfigurationException(applicationProvider.getName(), e.getMessage());
		}

		Context ctx = new Context(Locale.ENGLISH);
		ctx.setVariable("platform", platform);
		ctx.setVariable("appNG", new AppNG(platform, xpath));

		String result = templateEngine.process("platform.html", ctx);
		this.contentLength = result.getBytes(charset).length;
		return result;
	}

	Logger logger() {
		return LOGGER;
	}

	/**
	 * This is a helper class to make it easier for the thymeleaf template to interact with appNG's
	 * {@link org.appng.xml.platform.Platform} object.
	 * 
	 * @author Matthias Müller
	 */
	public static class AppNG {

		private org.appng.xml.platform.Platform platform;
		private Map<String, Map<String, Action>> actions = new HashMap<>();
		private Map<String, Datasource> datasources = new HashMap<>();
		private Map<String, PageReference> pages_ = new HashMap<>();
		private String siteName;
		private String applicationName;
		private XPathProcessor xpath;

		public AppNG(org.appng.xml.platform.Platform platform, XPathProcessor xpath) {
			this.platform = platform;
			this.xpath = xpath;
			parse();
		}

		private void parse() {
			List<NavigationItem> siteNavigation = getSiteNavigation();
			for (NavigationItem site : siteNavigation) {
				if (Boolean.TRUE.equals(site.isSelected())) {
					siteName = site.getLabel();
					for (NavigationItem app : site.getItem()) {
						if (Boolean.TRUE.equals(app.isSelected())) {
							applicationName = app.getLabel();
						}
					}
				}
			}

			PagesReference pages = platform.getContent().getApplication().getPages();
			if (null != pages) {
				for (PageReference page : pages.getPage()) {
					pages_.put(page.getId(), page);
					for (Section section : page.getStructure().getSection()) {
						for (Sectionelement element : section.getElement()) {
							setSectionTitle(section, element);
							Action action = element.getAction();
							if (null != action) {
								if (!actions.containsKey(action.getEventId())) {
									actions.put(action.getEventId(), new HashMap<String, Action>());
								}
								actions.get(action.getEventId()).put(action.getId(), action);
							}
							Datasource datasource = element.getDatasource();
							if (null != datasource) {
								if (!datasources.containsKey(datasource.getId())) {
									datasources.put(datasource.getId(), datasource);
								}
							}
						}
					}
				}
			}
		}

		public boolean hasSort(DataConfig dataConfig) {
			List<FieldDef> fields = dataConfig.getMetaData().getFields();
			for (FieldDef fieldDef : fields) {
				Sort sort = fieldDef.getSort();
				if (null != sort && null != sort.getOrder() && null != sort.getPrio()) {
					return true;
				}
			}
			return false;
		}

		public List<ValidationRule> rules(FieldDef field) {
			List<ValidationRule> result = new ArrayList<>();
			Validation validation = field.getValidation();
			if (null != validation) {
				addRule(result, validation.getDigits());
				addRule(result, validation.getFileUpload());
				addRule(result, validation.getFuture());
				addRule(result, validation.getMax());
				addRule(result, validation.getMin());
				addRule(result, validation.getNotNull());
				addRule(result, validation.getPast());
				addRule(result, validation.getPattern());
				addRule(result, validation.getSize());
				addRule(result, validation.getType());
				List<Rule> rules = validation.getRules();
				if (null != rules) {
					rules.forEach(r -> addRule(result, r));
				}
			}
			return result;
		}

		private void addRule(List<ValidationRule> rules, ValidationRule r) {
			if (null != r) {
				rules.add(r);
			}
		}

		private void setSectionTitle(Section section, Sectionelement element) {
			if (null == section.getTitle() && !Boolean.TRUE.toString().equalsIgnoreCase(element.getPassive())) {
				DataConfig config = null;
				String id;
				if (null == element.getAction()) {
					id = element.getDatasource().getId();
					config = element.getDatasource().getConfig();
				} else {
					id = element.getAction().getId();
					config = element.getAction().getConfig();
				}
				if (null != config.getTitle()) {
					Label title = new Label();
					title.setId(id);
					title.setValue(config.getTitle().getValue());
					section.setTitle(title);
				}
			}
		}

		public Subject getSubject() {
			return platform.getSubject();
		}

		public List<NavigationItem> getSiteNavigation() {
			List<NavigationItem> items = new ArrayList<>();
			for (NavigationItem item : platform.getNavigation().getItem()) {
				if (org.appng.xml.platform.ItemType.SITE.equals(item.getType())) {
					items.add(item);
				}
			}
			return items;
		}

		public String getSiteName() {
			return siteName;
		}

		public String getApplicationName() {
			return applicationName;
		}

		public Action action(String eventId, String id) {
			return actions.containsKey(eventId) ? actions.get(eventId).get(id) : null;
		}

		public boolean hasErrors(FieldDef field) {
			return hasErrors(field.getMessages());
		}

		public boolean hasErrors(Messages messages) {
			if (null != messages) {
				for (Message m : messages.getMessageList()) {
					if (MessageType.ERROR.equals(m.getClazz())) {
						return true;
					}
				}
			}
			return false;
		}

		public boolean hasAction(String eventId, String id) {
			return actions.containsKey(eventId) ? actions.get(eventId).containsKey(id) : false;
		}

		public Datasource datasource(String id) {
			return datasources.get(id);
		}

		public boolean hasDatasource(String id) {
			return datasources.containsKey(id);
		}

		public String param(Datasource datasource, String name) {
			Params params = datasource.getConfig().getParams();
			return param(params, name);
		}

		public String param(Action action, String name) {
			Params params = action.getConfig().getParams();
			return param(params, name);
		}

		public String param(Params params, String name) {
			for (Param param : params.getParam()) {
				if (param.getName().equals(name)) {
					return param.getValue();
				}
			}
			return null;
		}

		public FieldDef actionField(String eventId, String id, String name) {
			return field(action(eventId, id).getConfig(), name);
		}

		public String label(Config config, String id) {
			if (null != config) {
				String label = label(config.getLabels(), id);
				return label.startsWith("???_") ? label(platform.getConfig().getLabels(), id) : label;
			}
			return label(platform.getConfig().getLabels(), id);
		}

		private String label(Labels labels, String id) {
			if (null != labels) {
				for (Label l : labels.getLabels()) {
					if (l.getId().equals(id)) {
						return l.getValue();
					}
				}
			}
			return "???_" + id;
		}

		public Linkpanel linkpanel(DataConfig config, String name) {
			for (Linkpanel linkpanel : config.getLinkpanel()) {
				if (linkpanel.getId().equals(name)) {
					return linkpanel;
				}
			}
			return null;
		}

		public Linkpanel linkpanel(Result result, String name) {
			for (Linkpanel linkpanel : result.getLinkpanel()) {
				if (linkpanel.getId().equals(name)) {
					return linkpanel;
				}
			}
			return null;
		}

		public List<Integer> pages(Resultset resultset) {
			List<Integer> pages = new ArrayList<Integer>();
			for (int i = 0; i <= resultset.getLastchunk(); i++) {
				pages.add(i);
			}
			return pages;
		}

		public FieldDef field(Action action, String name) {
			return field(action.getConfig(), name);
		}

		public FieldDef dataSourceField(String id, String name) {
			return field(datasource(id).getConfig(), name);
		}

		public FieldDef field(Datasource datasource, String name) {
			return field(datasource.getConfig(), name);
		}

		public FieldDef field(DataConfig config, String name) {
			return field(config.getMetaData(), name);
		}

		public boolean isFiltered(String pageId, Datasource datasource) {
			for (SelectionGroup selectionGroup : datasource.getData().getSelectionGroups()) {
				for (Selection selection : selectionGroup.getSelections()) {
					if (StringUtils.isNotBlank(getParam(pageId, selection.getId()))) {
						return true;
					}
				}
			}
			return false;
		}

		public FieldDef field(MetaData metaData, String name) {
			for (FieldDef fieldDef : metaData.getFields()) {
				if (fieldDef.getName().equals(name)) {
					return fieldDef;
				}
			}
			return null;
		}

		public FieldDef childField(FieldDef parent, String name) {
			for (FieldDef fieldDef : parent.getFields()) {
				if (fieldDef.getName().equals(name)) {
					return fieldDef;
				}
			}
			return null;
		}

		public Datafield data(String eventId, String id, String name) {
			return data(action(eventId, id).getData().getResult(), name);
		}

		public Datafield data(Action action, String name) {
			return data(action.getData().getResult(), name);
		}

		public Result result(Datasource datasource, int index) {
			return datasource.getData().getResultset().getResults().get(index);
		}

		public Datafield data(Result result, String name) {
			List<Datafield> fields = result.getFields();
			for (Datafield datafield : fields) {
				if (datafield.getName().equals(name)) {
					return datafield;
				}
			}
			return null;
		}

		public Datafield childData(Datafield parent, String name) {
			for (Datafield datafield : parent.getFields()) {
				if (datafield.getName().equals(name)) {
					return datafield;
				}
			}
			return null;
		}

		public Selection selection(Data data, String name) {
			for (Selection selection : data.getSelections()) {
				if (selection.getId().equals(name)) {
					return selection;
				}
			}
			return null;
		}

		public PageReference page(String id) {
			return pages_.get(id);
		}

		public String getParam(String pageId, String name) {
			PageReference page = page(pageId);
			if (null != page) {
				GetParams getParams = page.getConfig().getUrlSchema().getGetParams();
				if (null != getParams) {
					List<Param> paramList = getParams.getParamList();
					for (Param param : paramList) {
						if (param.getName().equals(name)) {
							return param.getValue();
						}
					}
				}
			}
			return null;
		}

		public String urlParam(String pageId, String name) {
			PageReference page = page(pageId);
			if (null != page) {
				UrlParams urlParams = page.getConfig().getUrlSchema().getUrlParams();
				if (null != urlParams) {
					List<Param> paramList = urlParams.getParamList();
					for (Param param : paramList) {
						if (param.getName().equals(name)) {
							return param.getValue();
						}
					}
				}
			}
			return null;
		}

		public Link defaultLink(Linkpanel panel) {
			if (null != panel) {
				for (Link l : panel.getLinks()) {
					if (Boolean.parseBoolean(l.getDefault())) {
						return l;
					}
				}
			}
			return null;
		}

		public String sessionParam(String name) {
			Session session = platform.getContent().getApplication().getConfig().getSession();
			if (null != session) {
				SessionParams sessionParams = session.getSessionParams();
				if (null != sessionParams) {
					for (Param param : sessionParams.getSessionParam()) {
						if (param.getName().equals(name)) {
							return param.getValue();
						}
					}
				}
			}
			return null;
		}

		// XPATH delegates
		public Node xNode(String xpathExpression) {
			Node n = xpath.getNode(xpathExpression);
			return n;
		}

		public Node xElement(String xpathExpression) {
			Element e = xpath.getElement(xpathExpression);
			return e;
		}

		public Node xNode(Node node, String xpathExpression) {
			Node n = xpath.getNode(node, xpathExpression);
			return n;
		}

		public Node xElement(Node node, String xpathExpression) {
			Element e = xpath.getElement(node, xpathExpression);
			return e;
		}

	}

	protected void addTemplates(List<Template> templates) {
		this.templates.addAll(templates);
	}

}
