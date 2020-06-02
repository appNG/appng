/*
 * Copyright 2011-2020 the original author or authors.
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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.XPathProcessor;
import org.appng.api.model.Properties;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.templating.ThymeleafReplaceInterceptor;
import org.appng.core.templating.ThymeleafTemplateEngine;
import org.appng.xml.MarshallService.AppNGSchema;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ApplicationReference;
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
import org.attoparser.ParseException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.util.StopWatch;
import org.thymeleaf.cache.StandardCacheManager;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.linkbuilder.AbstractLinkBuilder;
import org.thymeleaf.linkbuilder.ILinkBuilder;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.ITemplateResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThymeleafProcessor extends AbstractRequestProcessor {

	private static final Pattern BLANK_LINES = Pattern.compile("(\\s*\\r?\\n){1,}");
	static final String PLATFORM_HTML = "platform.html";
	private List<Template> templates;
	private DocumentBuilderFactory dbf;

	public ThymeleafProcessor(@Autowired DocumentBuilderFactory dbf) {
		templates = new ArrayList<>();
		this.dbf = dbf;
	}

	public String processWithTemplate(Site applicationSite, File debugRootFolder) throws InvalidConfigurationException {
		String result;
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String charsetName = platformProperties.getString(Platform.Property.ENCODING);
		Charset charset = Charset.forName(charsetName);
		String templateName = applicationSite.getProperties().getString(SiteProperties.TEMPLATE);

		org.appng.xml.platform.Platform platform = processPlatform(applicationSite);
		if (isRedirect()) {
			LOGGER.debug("request is beeing redirected");
			return "redirect";
		}
		platform.setVersion(env.getAttributeAsString(Scope.PLATFORM, Platform.Environment.APPNG_VERSION));

		Boolean render = env.getAttribute(Scope.REQUEST, EnvironmentKeys.RENDER);
		Boolean writeDebugFiles = platformProperties.getBoolean(org.appng.api.Platform.Property.WRITE_DEBUG_FILES);

		StopWatch sw = new StopWatch("process with template " + templateName);
		String platformXML = null;
		ApplicationProvider applicationProvider = getApplicationProvider(applicationSite);
		ConfigurableApplicationContext context = applicationProvider.getContext();
		ThymeleafTemplateEngine templateEngine = prepareEngine(context);
		File debugFolder = new File(debugRootFolder, getDebugFilePrefix(new Date()));

		try {
			sw.start("build platform.xml");
			platformXML = marshallService.marshal(platform);
			sw.stop();

			sw.start("build engine");
			String templatePrefix = platformProperties.getString(Platform.Property.TEMPLATE_PREFIX);
			Boolean devMode = platformProperties.getBoolean(Platform.Property.DEV_MODE);

			if (!templates.isEmpty()) {
				CacheProvider cacheProvider = new CacheProvider(platformProperties);
				File platformCache = cacheProvider.getPlatformCache(applicationSite, applicationProvider);
				File tplFolder = new File(platformCache, ResourceType.TPL.getFolder()).getAbsoluteFile();

				Set<String> patterns = templates.parallelStream().map(t -> new File(tplFolder, t.getPath()))
						.filter(f -> f.exists()).map(f -> f.getName()).collect(Collectors.toSet());

				ITemplateResolver applicationTemplateResolver = getApplicationTemplateResolver(
						applicationProvider.getName(), charset, devMode, tplFolder, patterns);
				templateEngine.addTemplateResolver(applicationTemplateResolver);

				ILinkBuilder appLinkBuilder = getLinkBuilder(applicationProvider, templatePrefix, tplFolder);
				templateEngine.addLinkBuilder(appLinkBuilder);
			}

			ILinkBuilder globalLinkBuilder = getGlobalLinkBuilder(templatePrefix);
			templateEngine.addLinkBuilder(globalLinkBuilder);

			ITemplateResolver globalTemplateResolver = getGlobalTemplateResolver(charset, devMode);
			templateEngine.addTemplateResolver(globalTemplateResolver);

			if (null != context) {
				MessageSource ms = context.getBean(MessageSource.class);
				templateEngine.setTemplateEngineMessageSource(ms);
			}

			if (writeDebugFiles) {
				sw.stop();
				sw.start("write debug files");
				writeDebugFile(debugFolder, PLATFORM_XML, platformXML);
				writeTemplateFiles(debugFolder, templateEngine);
			}

			if (render || !applicationSite.getProperties().getBoolean(SiteProperties.ALLOW_SKIP_RENDER)) {
				sw.stop();
				sw.start("build context");
				Context ctx = getContext(platform, applicationProvider);
				sw.stop();
				sw.start("process template");
				String templateFile = PLATFORM_HTML;
				if (outputType.getTemplates().size() > 0) {
					templateFile = outputType.getTemplates().get(0).getPath();
				}
				result = templateEngine.process(templateFile, ctx);
				result = BLANK_LINES.matcher(result).replaceAll(System.lineSeparator());
				this.contentType = HttpHeaders.getContentType(HttpHeaders.CONTENT_TYPE_TEXT_HTML, charsetName);
				if (writeDebugFiles) {
					sw.stop();
					sw.start("write index.html");
					writeDebugFile(debugFolder, INDEX_HTML, result);
				}
			} else {
				result = platformXML;
				this.contentType = HttpHeaders.getContentType(HttpHeaders.CONTENT_TYPE_TEXT_XML, charsetName);
			}
		} catch (Exception e) {
			result = writeErrorPage(platformProperties, debugFolder, platformXML, templateName, e, templateEngine);
			if (writeDebugFiles) {
				writeStackTrace(debugFolder, e);
			}
		}
		sw.stop();
		if (logger().isTraceEnabled()) {
			logger().trace(sw.prettyPrint());
		} else if (logger().isDebugEnabled()) {
			logger().debug(sw.shortSummary());
		}
		this.contentLength = result.getBytes(charset).length;
		return result;
	}

	protected void writeStackTrace(File outfolder, Exception e) {
		try {
			StringWriter stackWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stackWriter));
			writeDebugFile(outfolder, STACKTRACE_TXT, stackWriter.toString());
		} catch (IOException e1) {
			logger().error("error writing stacktrace", e);
		}
	}

	private void writeDebugFile(File outfolder, String name, String content) throws IOException {
		writeDebugFile(LOGGER, outfolder, name, content);
	}

	protected void writeTemplateFiles(File outfolder, ThymeleafTemplateEngine templateEngine) throws IOException {
		Set<String> templateNames = new HashSet<>();
		for (ITemplateResolver tplRes : templateEngine.getTemplateResolvers()) {
			String prefix = ((FileTemplateResolver) tplRes).getPrefix();
			String[] fileNames = new File(prefix).list();
			if (null != fileNames) {
				for (String fileName : fileNames) {
					if (!templateNames.contains(fileName)) {
						TemplateResolution resolvedTemplate = tplRes.resolveTemplate(templateEngine.getConfiguration(),
								null, fileName, null);
						ITemplateResource templateResource = resolvedTemplate.getTemplateResource();
						try {
							String content = IOUtils.toString(templateResource.reader());
							writeDebugFile(outfolder, "template/" + fileName, content);
						} catch (IOException e) {
							logger().error("error writing template resource", e);
						}
						templateNames.add(fileName);
					}
				}
			}
		}
	}

	protected ITemplateResolver getApplicationTemplateResolver(String application, Charset charset, Boolean devMode,
			File tplFolder, Set<String> patterns) {
		FileTemplateResolver appTplResolver = new FileTemplateResolver();
		appTplResolver.setName("Template Resolver for " + application);
		appTplResolver.setResolvablePatterns(patterns);
		appTplResolver.setPrefix(tplFolder.getPath() + File.separator);
		appTplResolver.setTemplateMode(TemplateMode.HTML);
		appTplResolver.setCharacterEncoding(charset.name());
		appTplResolver.setCacheable(!devMode);
		appTplResolver.setCheckExistence(true);
		appTplResolver.setOrder(0);
		return appTplResolver;
	}

	protected Context getContext(org.appng.xml.platform.Platform platform, ApplicationProvider applicationProvider)
			throws InvalidConfigurationException {
		Context ctx = new Context(Locale.ENGLISH);
		ctx.setVariable("platform", platform);
		try {
			Document doc = dbf.newDocumentBuilder().newDocument();
			AppNGSchema.PLATFORM.getContext().createMarshaller().marshal(platform, doc);
			XPathProcessor xpath = new XPathProcessor(doc);
			xpath.setNamespace("appng", AppNGSchema.PLATFORM.getNamespace());
			ctx.setVariable("appNG", new AppNG(platform, xpath));
		} catch (Exception e) {
			throw new InvalidConfigurationException(applicationProvider.getName(), e.getMessage());
		}
		return ctx;
	}

	protected ILinkBuilder getGlobalLinkBuilder(String templatePrefix) {
		AbstractLinkBuilder globalLinkBuilder = new AbstractLinkBuilder() {

			public String buildLink(IExpressionContext context, String base, Map<String, Object> parameters) {
				return templatePrefix + "/" + ResourceType.RESOURCE.getFolder() + base;
			}
		};
		globalLinkBuilder.setName("Global Link Builder");
		globalLinkBuilder.setOrder(1);
		return globalLinkBuilder;
	}

	protected ILinkBuilder getLinkBuilder(ApplicationProvider applicationProvider, String templatePrefix,
			File tplFolder) {
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
		return appLinkBuilder;
	}

	protected ThymeleafTemplateEngine prepareEngine(ConfigurableApplicationContext context) {
		List<ThymeleafReplaceInterceptor> interceptors = null;
		if (null != context) {
			interceptors = new ArrayList<>(context.getBeansOfType(ThymeleafReplaceInterceptor.class).values());
			Collections.sort(interceptors, new Comparator<ThymeleafReplaceInterceptor>() {
				public int compare(ThymeleafReplaceInterceptor o1, ThymeleafReplaceInterceptor o2) {
					return Integer.compare(o1.getPriority(), o2.getPriority());
				}
			});
		}

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
		return templateEngine;
	}

	protected ITemplateResolver getGlobalTemplateResolver(Charset charset, Boolean devMode) {
		FileTemplateResolver globalTemplateResolver = new FileTemplateResolver();
		globalTemplateResolver.setName("Global Template Resolver");
		globalTemplateResolver.setResolvablePatterns(Collections.singleton("*"));
		globalTemplateResolver.setPrefix(templatePath + "/" + ResourceType.RESOURCE.getFolder() + "/html/");
		globalTemplateResolver.setTemplateMode(TemplateMode.HTML);
		globalTemplateResolver.setCharacterEncoding(charset.name());
		globalTemplateResolver.setCacheable(!devMode);
		globalTemplateResolver.setOrder(1);
		return globalTemplateResolver;
	}

	void writeTemplateToErrorPage(Properties platformProperties, File debugFolder, Exception e, Object executionContext,
			StringWriter errorPage) {
		if (e instanceof TemplateProcessingException) {
			try {
				Throwable current = e;
				Integer errorLine = null;
				ParseException pe = null;
				while (null != current && null == errorLine) {
					if (current instanceof TemplateProcessingException) {
						errorLine = ((TemplateProcessingException) current).getLine();
					}
					if (current instanceof ParseException) {
						pe = (ParseException) current;
					}
					current = current.getCause();
				}
				String templateName = TemplateProcessingException.class.cast(e).getTemplateName();
				File templatFile = new File(templateName);
				errorPage.append("<h3>" + templatFile.getName() + "</h3>");
				if (null != pe) {
					errorPage.append("<span class=\"error\">");
					errorPage.append(pe.getClass().getName() + ": " + pe.getMessage() + "</span><br/>");
				}
				errorPage.append("<div><pre id=\"template\">");
				String template = FileUtils.readFileToString(templatFile, StandardCharsets.UTF_8);
				String[] lines = template.split(StringUtils.LF);
				int i = 1;
				for (String line : lines) {
					errorPage.append("<span");
					if (null != errorLine && i++ == errorLine) {
						errorPage.append(" id=\"error\" class=\"error\"");
					}
					errorPage.append(">");
					errorPage.append(StringEscapeUtils.escapeHtml4(line));
					errorPage.append("</span>");
				}
			} catch (IOException e1) {
				errorPage.append("error while adding template: " + e1.getClass().getName() + "-" + e1.getMessage());

			}
			errorPage.append("</pre></div>");
			errorPage.append(StringUtils.LF);
			errorPage.append("<script>document.getElementById('error').scrollIntoView();</script>");
			return;
		}
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

			ApplicationReference application = platform.getContent().getApplication();
			if (null != application) {
				PagesReference pages = application.getPages();
				if (null != pages) {
					for (PageReference page : pages.getPage()) {
						pages_.put(page.getId(), page);
						for (Section section : page.getStructure().getSection()) {
							for (Sectionelement element : section.getElement()) {
								setSectionTitle(section, element);
								Action action = element.getAction();
								if (null != action) {
									if (!actions.containsKey(action.getEventId())) {
										actions.put(action.getEventId(), new HashMap<>());
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
			return platform.getNavigation().getItem().stream()
					.filter(n -> org.appng.xml.platform.ItemType.SITE.equals(n.getType())).collect(Collectors.toList());
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
			if (null != params) {
				return param(params.getParam(), name);
			}
			return null;
		}

		public String param(List<Param> params, String name) {
			if (null != params) {
				return params.parallelStream().filter(p -> p.getName().equals(name)).map(p -> p.getValue()).findFirst()
						.orElse(null);
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
			List<Integer> pages = new ArrayList<>();
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
					return param(getParams.getParamList(), name);
				}
			}
			return null;
		}

		public String urlParam(String pageId, String name) {
			PageReference page = page(pageId);
			if (null != page) {
				UrlParams urlParams = page.getConfig().getUrlSchema().getUrlParams();
				if (null != urlParams) {
					return param(urlParams.getParamList(), name);
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

		public Link defaultLink(List<Linkpanel> panels) {
			for (Linkpanel panel : panels) {
				Link defaultLink = defaultLink(panel);
				if (null != defaultLink) {
					return defaultLink;
				}
			}
			return null;
		}

		public String sessionParam(String name) {
			Session session = platform.getContent().getApplication().getConfig().getSession();
			if (null != session) {
				SessionParams sessionParams = session.getSessionParams();
				if (null != sessionParams) {
					return param(sessionParams.getSessionParam(), name);
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
