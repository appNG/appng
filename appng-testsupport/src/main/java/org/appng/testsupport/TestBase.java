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
package org.appng.testsupport;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FilenameUtils;
import org.appng.api.ActionProvider;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.FileUpload.Unit;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ParameterSupport;
import org.appng.api.Path;
import org.appng.api.Platform;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.VHostMode;
import org.appng.api.model.Application;
import org.appng.api.model.ApplicationSubject;
import org.appng.api.model.FeatureProvider;
import org.appng.api.model.Group;
import org.appng.api.model.Permission;
import org.appng.api.model.Property;
import org.appng.api.model.Resource;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.appng.api.model.SimpleProperty;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.search.Consumer;
import org.appng.api.search.DocumentEvent;
import org.appng.api.search.DocumentProducer;
import org.appng.api.support.ApplicationConfigProviderImpl;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.ApplicationResourceHolder;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.api.support.DollarParameterSupport;
import org.appng.api.support.DummyPermissionProcessor;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.OptionImpl;
import org.appng.api.support.OptionsImpl;
import org.appng.api.support.PropertyHolder;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.forms.FormUpload;
import org.appng.forms.impl.FormUploadBean;
import org.appng.forms.impl.RequestBean;
import org.appng.testsupport.persistence.ConnectionHelper;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.appng.tools.image.ImageProcessor;
import org.appng.xml.BaseObject;
import org.appng.xml.MarshallService;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.PermissionRef;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.custommonkey.xmlunit.DifferenceListener;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Base class for integration-testing an {@link Application}.<br />
 * 
 * 
 * Example Usage (w/o JPA):
 * 
 * <pre>
 * &#064;org.springframework.test.context.ContextConfiguration(initializers = MyTest.class)
 * public class MyTest extends TestBase {
 * 
 * 	public MyTest() {
 * 		super(&quot;myapplication&quot;, &quot;application-home&quot;);
 * 	}
 * 
 * }
 * </pre>
 * 
 * Example Usage (with JPA):
 * 
 * <pre>
 * &#064;org.springframework.test.context.ContextConfiguration(locations = {
 * 		TestBase.TESTCONTEXT_JPA }, initializers = MyTest.class)
 * public class MyTest extends TestBase {
 * 
 * 	public MyTest() {
 * 		super(&quot;myapplication&quot;, &quot;application-home&quot;);
 * 		setEntityPackage(&quot;org.myapplication.domain&quot;);
 * 		setRepositoryBase(&quot;org.myapplication.repository&quot;);
 * 	}
 * 
 * }
 * </pre>
 * 
 * Testing {@link ActionProvider}s and {@link DataProvider}s is this simple:
 * 
 * <pre>
 * &#064;org.junit.Test
 * public void testShowPersons() throws ProcessingException, IOException {
 * 	addParameter(&quot;sortPersons&quot;, &quot;name:desc&quot;);
 * 	initParameters();
 * 	CallableDataSource datasource = getDataSource(&quot;persons&quot;).getCallableDataSource();
 * 	datasource.perform(&quot;personPage&quot;);
 * 	validate(datasource.getDatasource());
 * }
 * 
 * &#064;org.junit.Test
 * public void testCreatePerson() throws ProcessingException, IOException {
 * 	CallableAction callableAction = getAction(&quot;personEvent&quot;, &quot;create&quot;).withParam(FORM_ACTION, &quot;create&quot;)
 * 			.getCallableAction(new Person(&quot;John&quot;, &quot;Doe&quot;));
 * 	FieldProcessor fieldProcessor = callableAction.perform();
 * 	validate(callableAction.getAction(), &quot;-action&quot;);
 * 	validate(fieldProcessor.getMessages(), &quot;-messages&quot;);
 * }
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@ContextConfiguration(locations = { TestBase.BEANS_PATH, TestBase.TESTCONTEXT })
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class TestBase implements ApplicationContextInitializer<GenericApplicationContext> {

	public static final String TESTCONTEXT = "classpath:org/appng/testsupport/application-testcontext.xml";

	public static final String TESTCONTEXT_CORE = "classpath:org/appng/testsupport/application-testcontext-core.xml";

	public static final String TESTCONTEXT_JPA = "classpath:org/appng/testsupport/application-testcontext-jpa.xml";

	public static final String BEANS_PATH = "file:application-home/beans.xml";

	protected static final String APPLICATION_HOME = "application-home";

	protected static final String FORM_ACTION = "form_action";

	private static final String SITE_MANAGER_PATH = "/manager";

	private static final String SITE_SERVICE_PATH = "/service";

	@Autowired
	protected ServletContext servletContext;

	protected HttpSession session;

	@Autowired
	protected MockHttpServletRequest servletRequest;

	protected MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	@Autowired
	protected ConfigurableApplicationContext context;

	@Autowired
	protected MessageSource messageSource;

	@Autowired
	protected ConversionService conversionService;

	@Autowired
	protected MarshallService marshallService;

	@Autowired
	@Qualifier("applicationMarshallService")
	private MarshallService applicationMarshallService;

	protected Environment environment;

	protected ApplicationRequest request;

	protected Application application;

	@Mock
	protected Site site;

	@Mock
	protected Subject subject;

	@Mock
	protected Path path;

	private String applicationName;

	private String applicationLocation;

	private String entityPackage;

	private String repositoryBase;

	private boolean useFullClassname = false;

	private Map<String, String> parameters = new HashMap<String, String>();

	public TestBase() {
		this("application", APPLICATION_HOME);
	}

	public TestBase(String name) {
		this(name, APPLICATION_HOME);
	}

	protected void subjectWithRole(String roleName) {
		Group group = Mockito.mock(Group.class);
		Mockito.when(group.getRoles()).thenReturn(new HashSet<Role>());
		for (Role role : application.getRoles()) {
			if (role.getName().equals(roleName)) {
				group.getRoles().add(role);
			}
		}
		List<Group> groups = new ArrayList<Group>();
		groups.add(group);
		Mockito.when(subject.getGroups()).thenReturn(groups);
	}

	public TestBase(String applicationName, String applicationLocation) {
		Locale.setDefault(Locale.ENGLISH);
		this.applicationName = applicationName;
		this.applicationLocation = applicationLocation;
	}

	public void initialize(GenericApplicationContext applicationContext) {
		Properties properties = getProperties();
		PropertySourcesPlaceholderConfigurer placeholderConfigurer = new PropertySourcesPlaceholderConfigurer();
		placeholderConfigurer.setProperties(properties);
		applicationContext.addBeanFactoryPostProcessor(placeholderConfigurer);

		File dictFolder = new File(applicationLocation + "/dictionary").getAbsoluteFile();
		final List<String> baseNames = new ArrayList<String>();
		if (dictFolder.exists() && dictFolder.list() != null) {

			for (String file : dictFolder.list()) {
				if (FilenameUtils.getExtension(file).equalsIgnoreCase("properties")) {
					String name = FilenameUtils.getBaseName(file).replaceAll("_(.)*", "");
					if (!baseNames.contains(name)) {
						baseNames.add(name);
					}
				}
			}
			try {
				URL dictUrl = dictFolder.toURI().toURL();
				URLClassLoader classLoader = new URLClassLoader(new URL[] { dictUrl }, getClass().getClassLoader());
				applicationContext.setClassLoader(classLoader);
			} catch (MalformedURLException e) {
				// impossible!
			}
		}

		applicationContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
				baseNames.add("messages-core");
				ResourceBundleMessageSource bean = beanFactory.getBean(ResourceBundleMessageSource.class);
				bean.setBasenames(baseNames.toArray(new String[baseNames.size()]));
			}
		});

	}

	protected Properties getProperties() {
		Properties properties = new Properties();
		properties.put("entityPackage", entityPackage);
		properties.put("hsqlPort", ConnectionHelper.getHsqlPort());
		properties.put("repositoryBase", repositoryBase);
		return properties;
	}

	/**
	 * Adds a parameter to the {@link org.appng.api.Request} used in the testcase.<br/>
	 * After all parameters have been added, {@link #initParameters()} (or {@link #initParameters(boolean)}) has to be
	 * called.<br/>
	 * <strong>Not to confiuse with {@link DataSourceCall#withParam(String, String)} and
	 * {@link ActionCall#withParam(String, String)}!</strong>
	 * 
	 * @param name
	 *            the name of the parameter
	 * @param value
	 *            the value of the parameter
	 */
	protected void addParameter(String name, String value) {
		parameters.put(name, value);
	}

	protected FormUpload getFormUpload(String resourceName) throws URISyntaxException {
		URL resource = getClass().getClassLoader().getResource(resourceName);
		File file = new File(resource.toURI());
		String extension = FilenameUtils.getExtension(resourceName);
		long maxSize = 10 * Unit.MB.getFactor();
		return new FormUploadBean(file, file.getName(), extension, Arrays.asList(extension), maxSize);
	}

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		initEnvironment();
		application = new SimpleApplication(applicationName, context, getApplicationSubjects());
		initRequest();
		((SimpleApplication) application).init(getProperties(), request.getApplicationConfig().getApplicationInfo());
	}

	protected List<ApplicationSubject> getApplicationSubjects() {
		return new ArrayList<ApplicationSubject>();
	}

	protected void initRequest() throws InvalidConfigurationException, JAXBException {
		File targetFolder = new File("target/temp");
		File applicationFolder = new File(applicationLocation);
		Resources applicationResources = new ApplicationResourceHolder(application, applicationMarshallService,
				applicationFolder, targetFolder);
		ApplicationConfigProvider applicationConfigProvider = new ApplicationConfigProviderImpl(marshallService,
				applicationName, applicationResources, false);
		request = (ApplicationRequest) context.getBean(Request.class);
		request.setPermissionProcessor(new DummyPermissionProcessor(subject, site, application));
		request.setApplicationConfig(applicationConfigProvider);
	}

	/**
	 * Initializes the {@link org.appng.api.Request} for the testcase. Before, some paramters can be added using
	 * {@link #addParameter(String, String)}
	 */
	protected void initParameters() {
		initParameters(false);
	}

	protected void initParameters(boolean isPost) {
		servletRequest.setParameters(parameters);
		init(isPost);
	}

	protected void init(boolean isPost) {
		if (isPost) {
			servletRequest.setMethod("POST");
		}
		RequestBean requestBean = new RequestBean();
		requestBean.process(servletRequest);
		request.setWrappedRequest(requestBean);
	}

	protected void initEnvironment() {
		ConcurrentHashMap<String, Object> platformEnv = new ConcurrentHashMap<String, Object>();
		List<Property> platformProperties = getPlatformProperties("platform.");
		platformEnv.put(Platform.Environment.PLATFORM_CONFIG, new PropertyHolder("platform.", platformProperties));

		Mockito.when(site.getName()).thenReturn("localhost");
		Mockito.when(site.getDomain()).thenReturn("localhost");
		Mockito.when(site.getHost()).thenReturn("localhost");
		Mockito.when(site.getSiteClassLoader()).thenReturn(new URLClassLoader(new URL[0]));
		Map<String, Site> sites = new HashMap<String, Site>();
		sites.put("localhost", site);
		platformEnv.put(Platform.Environment.SITES, sites);

		List<Property> siteProperties = getSiteProperties("platform.site.localhost.");

		Mockito.when(site.getProperties()).thenReturn(new PropertyHolder("platform.site.localhost.", siteProperties));

		this.session = new MockHttpSession(servletContext);
		servletContext.setAttribute(Scope.PLATFORM.name(), platformEnv);

		environment = context.getBean("environment", Environment.class);
		if (!((DefaultEnvironment) environment).isInitialized()) {
			environment.init(servletContext, session, servletRequest, servletResponse, site.getHost());
		}
		environment.setAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO, path);
		Mockito.when(path.getServicePath()).thenReturn(SITE_SERVICE_PATH);
		Mockito.when(path.getGuiPath()).thenReturn(SITE_MANAGER_PATH);
	}

	protected List<Property> getSiteProperties(String prefix) {
		List<Property> siteProperties = new ArrayList<Property>();
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.SERVICE_PATH, SITE_SERVICE_PATH));
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.MANAGER_PATH, SITE_MANAGER_PATH));
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.DEFAULT_PAGE_SIZE, "25"));
		return siteProperties;
	}

	protected List<Property> getPlatformProperties(String prefix) {
		List<Property> platformProperties = new ArrayList<Property>();
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.VHOST_MODE, VHostMode.NAME_BASED.name()));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.LOCALE, "en"));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.TIME_ZONE, "Europe/Berlin"));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.PLATFORM_ROOT_PATH, "target/ROOT"));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.CACHE_FOLDER, "cache"));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.APPLICATION_CACHE_FOLDER, "application"));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.PLATFORM_CACHE_FOLDER, "platform"));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.UPLOAD_DIR, "/target/uploads"));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.MAX_UPLOAD_SIZE, "10485760"));
		platformProperties.add(new SimpleProperty(prefix + Platform.Property.XSS_PROTECT, "false"));
		return platformProperties;
	}

	public void validate(BaseObject object) throws IOException {
		String controlFile = getControlFileName(null);
		WritingXmlValidator.validateXml(object, controlFile);
	}

	public void validate(BaseObject object, DifferenceListener differenceListener) throws IOException {
		String controlFile = getControlFileName(null);
		WritingXmlValidator.validateXml(object, controlFile, differenceListener);
	}

	public void validate(BaseObject object, String suffix, DifferenceListener differenceListener) throws IOException {
		String controlFile = getControlFileName(suffix);
		WritingXmlValidator.validateXml(object, controlFile, differenceListener);
	}

	public void validate(BaseObject object, String suffix) throws IOException {
		String controlFile = getControlFileName(suffix);
		WritingXmlValidator.validateXml(object, controlFile);
	}

	private String getControlFileName(String suffix) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement stackTraceElement = stackTrace[3];
		String className = stackTraceElement.getClassName();
		if (!useFullClassname) {
			className = className.substring(className.lastIndexOf('.') + 1);
		}
		String controlFile = "xml" + File.separator + className + "-" + stackTraceElement.getMethodName()
				+ (suffix == null ? "" : suffix) + ".xml";
		return controlFile;
	}

	public class CallableTestAction extends CallableAction {
		private Object form;

		CallableTestAction(Site site, Application application, ApplicationRequest applicationRequest,
				ActionRef actionRef, Object form) throws ProcessingException {
			super(site, application, applicationRequest, actionRef);
			this.form = form;
		}

		@Override
		protected Object getBindObject(FieldProcessor fieldProcessor) throws BusinessException {
			if (null != form) {
				BeanWrapper original = new BeanWrapperImpl(form);
				BeanWrapper copy = new BeanWrapperImpl(form.getClass());
				for (FieldDef fieldDef : fieldProcessor.getMetaData().getFields()) {
					copy.setPropertyValue(fieldDef.getBinding(), original.getPropertyValue(fieldDef.getBinding()));
				}
				return copy.getWrappedInstance();
			}
			return null;
		}

		private Action initialize() throws ProcessingException {
			retrieveData(false);
			return getAction();
		}
	}

	protected String getEntityPackage() {
		return entityPackage;
	}

	protected void setEntityPackage(String entityPackage) {
		this.entityPackage = entityPackage;
	}

	protected String getRepositoryBase() {
		return repositoryBase;
	}

	protected void setRepositoryBase(String repositoryBase) {
		this.repositoryBase = repositoryBase;
	}

	protected boolean isUseFullClassname() {
		return useFullClassname;
	}

	protected void setUseFullClassname(boolean useFullClassname) {
		this.useFullClassname = useFullClassname;
	}

	class ParametrizedCall {
		private Map<String, String> configParams = new HashMap<String, String>();
		private Params params;

		ParametrizedCall(Params params) {
			this.params = params;
		}

		Params getParams() {
			for (String key : configParams.keySet()) {
				Param param = new Param();
				param.setName(key);
				param.setValue(configParams.get(key));
				params.getParam().add(param);
			}
			return params;
		}

		ParameterSupport getParameterSupport() {
			return new DollarParameterSupport(configParams);
		}

		ParametrizedCall clearParams() {
			configParams.clear();
			getParams().getParam().clear();
			return this;
		}

		ParametrizedCall withParam(String name, String value) {
			configParams.put(name, value);
			return this;
		}
	}

	/**
	 * Returns a {@link DataSourceCall} that wraps a {@link DatasourceRef}, using the given id for a {@link Datasource}
	 * 
	 * @param id
	 *            the id of the {@link Datasource}
	 * @return the {@link DataSourceCall}
	 */
	protected DataSourceCall getDataSource(String id) {
		return new DataSourceCall(id);
	}

	/**
	 * Returns an {@link ActionCall} that wraps an {@link ActionRef}, using the given id for an {@link Action}.
	 * 
	 * @param eventId
	 *            the id of the {@link Event}
	 * @param id
	 *            the id of the {@link Action}
	 * @return the {@link ActionCall}
	 */
	protected ActionCall getAction(String eventId, String id) {
		return new ActionCall(eventId, id);
	}

	/**
	 * Returns a {@link HandlerMethodArgumentResolver} that can resolve a(n)
	 * <ul>
	 * <li>{@link Environment}
	 * <li>{@link Site}
	 * <li>{@link Application}
	 * </ul>
	 * Useful when testing a {@link Controller}/ {@link RestController} with {@link MockMvc}:
	 * 
	 * <pre>
	 * MockMvc mockMvc = MockMvcBuilders.standaloneSetup(context.getBean(MyRestController.class))
	 * 		.setCustomArgumentResolvers(getHandlerMethodArgumentResolver()).build();
	 * </pre>
	 * 
	 * @return the resolver
	 */
	public HandlerMethodArgumentResolver getHandlerMethodArgumentResolver() {
		return new HandlerMethodArgumentResolver() {

			public boolean supportsParameter(MethodParameter parameter) {
				return isSite(parameter) || isEnvironment(parameter) || isApplication(parameter);
			}

			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
					NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
				return isSite(parameter) ? site
						: (isEnvironment(parameter) ? environment : (isApplication(parameter) ? application : null));
			}

			private boolean isParameterType(MethodParameter parameter, Class<?> type) {
				return parameter.getParameterType().equals(type);
			}

			private boolean isEnvironment(MethodParameter parameter) {
				return isParameterType(parameter, Environment.class);
			}

			protected boolean isSite(MethodParameter parameter) {
				return isParameterType(parameter, Site.class);
			}

			private boolean isApplication(MethodParameter parameter) {
				return isParameterType(parameter, Application.class);
			}
		};
	}

	/**
	 * A wrapper for a {@link DatasourceRef}, allowing to add {@link Param}eters and to retrieve the actual
	 * {@link CallableDataSource}.
	 */
	protected class DataSourceCall extends DatasourceRef {

		private ParametrizedCall parametrizedCall;

		DataSourceCall(String id) {
			setId(id);
			setParams(new Params());
			this.parametrizedCall = new ParametrizedCall(super.getParams());
		}

		@Override
		public Params getParams() {
			return parametrizedCall.getParams();
		}

		ParameterSupport getParameterSupport() {
			return parametrizedCall.getParameterSupport();
		}

		public DataSourceCall clearParams() {
			parametrizedCall.clearParams();
			return this;
		}

		/**
		 * Adds a {@link Param} to the wrapped {@link DatasourceRef}.
		 * 
		 * @param name
		 *            the name of the {@link Param}
		 * @param value
		 *            the value
		 * @return
		 */
		public DataSourceCall withParam(String name, String value) {
			parametrizedCall.withParam(name, value);
			return this;
		}

		/**
		 * Returns the {@link CallableDataSource}.
		 * 
		 * @return the {@link CallableDataSource}
		 * @throws ProcessingException
		 *             if an error occurs while assembling the CallableDataSource
		 */
		public CallableDataSource getCallableDataSource() throws ProcessingException {
			return new CallableDataSource(site, application, request, getParameterSupport(), this);
		}
	}

	/**
	 * A wrapper for an {@link ActionRef}, allowing to add {@link Param}eters and to retrieve the actual
	 * {@link CallableAction}.
	 */
	protected class ActionCall extends ActionRef {

		private ParametrizedCall parametrizedCall;

		ActionCall(String eventId, String id) {
			setEventId(eventId);
			setId(id);
			setParams(new Params());
			this.parametrizedCall = new ParametrizedCall(super.getParams());
		}

		@Override
		public Params getParams() {
			return parametrizedCall.getParams();
		}

		ParameterSupport getParameterSupport() {
			return parametrizedCall.getParameterSupport();
		}

		public ActionCall clearParams() {
			parametrizedCall.clearParams();
			return this;
		}

		/**
		 * Adds a {@link Param} to the wrapped {@link ActionRef}.
		 * 
		 * @param name
		 *            the name of the {@link Param}
		 * @param value
		 *            the value
		 * @return
		 */
		public ActionCall withParam(String name, String value) {
			parametrizedCall.withParam(name, value);
			return this;
		}

		/**
		 * Returns the {@link CallableAction}.
		 * 
		 * @param form
		 *            an instance of the bind-object used by the {@link Action}. The type must be compatible with
		 *            {@link MetaData#getBindClass()} of the {@link Datasource} that is used by the {@link Action}.
		 * @return the {@link CallableAction}
		 * @throws ProcessingException
		 *             if an error occurs while assembling the CallableAction
		 */
		public CallableAction getCallableAction(Object form) throws ProcessingException {
			return new CallableTestAction(site, application, request, this, form);
		}

		/**
		 * Returns the {@link Action} in it's initial state, meaning the action is initialized with the original data
		 * coming from {@link DataProvider}, but not performed.
		 * 
		 * @return the {@link Action}
		 * @throws ProcessingException
		 *             if an error occurs while assembling the Action
		 */
		public Action initialize() throws ProcessingException {
			return new CallableTestAction(site, application, request, this, null).initialize();
		}

		/**
		 * Returns the initial form for the action, i.e. of the underlying {@link Datasource}, if any. This is done by
		 * directly calling
		 * {@link DataProvider#getData(Site, Application, Environment, org.appng.api.Options, Request, FieldProcessor)}
		 * and then returning the result of {@link DataContainer#getItem()}.
		 * 
		 * @return the initial form, may be {@code null}
		 * @throws ProcessingException
		 *             if an error occurs while retrieving the data
		 */
		@SuppressWarnings("unchecked")
		public <T> T getForm() throws ProcessingException {
			CallableTestAction callableTestAction = new CallableTestAction(site, application, request, this, null);
			DatasourceRef datasourceRef = callableTestAction.getAction().getDatasource();
			if (null != datasourceRef) {
				DataSourceCall dataSourceCall = new DataSourceCall(datasourceRef.getId());
				Map<String, String> parameters = getParameterSupport().getParameters();
				datasourceRef.getParams().getParam()
						.forEach(p -> dataSourceCall.withParam(p.getName(), parameters.get(p.getName())));

				Datasource datasource = dataSourceCall.getCallableDataSource().getDatasource();

				OptionsImpl options = new OptionsImpl();
				datasource.getBean().getOptions().forEach(o -> {
					OptionImpl opt = new OptionImpl(o.getName());
					o.getOtherAttributes().entrySet()
							.forEach(e -> opt.addAttribute(e.getKey().getLocalPart(), e.getValue()));
					options.addOption(opt);
				});

				MetaData metaData = datasource.getConfig().getMetaData();
				FieldProcessorImpl fp = new FieldProcessorImpl(id, metaData);
				fp.addLinkPanels(datasource.getConfig().getLinkpanel());

				DataProvider dataProvider = application.getBean(datasource.getBean().getId(), DataProvider.class);
				DataContainer dataContainer = dataProvider.getData(site, application, environment, options, request,
						fp);
				return (T) dataContainer.getItem();
			}
			return null;
		}
	}

	protected class SimpleApplication implements Application {

		private String name;
		private ConfigurableApplicationContext context;
		private Map<String, Permission> permissionMap = new HashMap<String, Permission>();
		private Set<Role> roleSet;
		private org.appng.api.model.Properties properties;
		private List<ApplicationSubject> applicationSubjects;
		private SimpleFeatureProvider featureProvider;
		private ApplicationInfo applicationInfo;

		protected SimpleApplication(String name, ConfigurableApplicationContext context) {
			this(name, context, new ArrayList<ApplicationSubject>());
		}

		protected SimpleApplication(String name, ConfigurableApplicationContext context,
				List<ApplicationSubject> applicationSubjects) {
			this.name = name;
			this.context = context;
			this.applicationSubjects = applicationSubjects;
		}

		protected void init(Properties overrides, ApplicationInfo applicationInfo) {
			this.applicationInfo = applicationInfo;
			for (org.appng.xml.application.Permission p : applicationInfo.getPermissions().getPermission()) {
				Permission simplePermission = new SimplePermission(p);
				permissionMap.put(p.getId(), simplePermission);
			}

			roleSet = new HashSet<Role>();
			int roleId = 1;
			for (org.appng.xml.application.Role r : applicationInfo.getRoles().getRole()) {
				SimpleRole role = new SimpleRole(r, roleId++);
				List<PermissionRef> permission = r.getPermission();
				for (PermissionRef permissionRef : permission) {
					Permission e = permissionMap.get(permissionRef.getId());
					role.getPermissions().add(e);
				}
				roleSet.add(role);
			}
			Set<Property> props = new HashSet<Property>();
			for (org.appng.xml.application.Property prop : applicationInfo.getProperties().getProperty()) {
				String propName = prop.getId();
				String value = overrides.containsKey(propName) ? overrides.getProperty(propName) : prop.getValue();
				SimpleProperty property;
				if (Boolean.TRUE.equals(prop.isClob())) {
					property = new SimpleProperty(propName, null);
					property.setClob(value);
				} else {
					property = new SimpleProperty(propName, value);
				}
				property.setDescription(prop.getDescription());
				props.add(property);
			}
			properties = new PropertyHolder("", props);
			featureProvider = new SimpleFeatureProvider(properties);
		}

		public String getName() {
			return name;
		}

		public String getDisplayName() {
			return applicationInfo.getDisplayName();
		}

		public String getPackageVersion() {
			return applicationInfo.getVersion();
		}

		public String getTimestamp() {
			return applicationInfo.getTimestamp();
		}

		public String getLongDescription() {
			return applicationInfo.getLongDescription();
		}

		public String getAppNGVersion() {
			return applicationInfo.getAppngVersion();
		}

		public boolean isInstalled() {
			return true;
		}

		public boolean isSnapshot() {
			return getPackageVersion().endsWith("-SNAPSHOT");
		}

		public String getDescription() {
			return applicationInfo.getDescription();
		}

		public Integer getId() {
			return null;
		}

		public Date getVersion() {
			return null;
		}

		public Set<Permission> getPermissions() {
			return new HashSet<Permission>(permissionMap.values());
		}

		public Set<Role> getRoles() {
			return roleSet;
		}

		public Resources getResources() {
			return request.getApplicationConfig().getResources();
		}

		public Set<Resource> getResourceSet() {
			return getResources().getResources();
		}

		public org.appng.api.model.Properties getProperties() {
			return properties;
		}

		public <T> T getBean(String name, Class<T> clazz) {
			return context.getBean(name, clazz);
		}

		public String[] getBeanNames(Class<?> clazz) {
			return context.getBeanNamesForType(clazz);
		}

		public <T> T getBean(Class<T> clazz) {
			return context.getBean(clazz);
		}

		public Object getBean(String beanName) {
			return context.getBean(beanName);
		}

		public boolean containsBean(String beanName) {
			return context.containsBean(name);
		}

		public boolean isFileBased() {
			return true;
		}

		public boolean isPrivileged() {
			return false;
		}

		@Deprecated
		public boolean isCoreApplication() {
			return isPrivileged();
		}

		public boolean isHidden() {
			return false;
		}

		public String getMessage(Locale locale, String key, Object... args) {
			return messageSource.getMessage(key, args, locale);
		}

		public String getSessionParamKey(Site site) {
			return site.getName() + "." + getName();
		}

		public Map<String, String> getSessionParams(Site site, Environment environment) {
			String sessionParamKey = getSessionParamKey(site);
			Map<String, String> sessionsParams = environment.getAttribute(Scope.SESSION, sessionParamKey);
			if (null == sessionsParams) {
				sessionsParams = new HashMap<String, String>();
				environment.setAttribute(Scope.SESSION, sessionParamKey, sessionsParams);
			}
			return sessionsParams;
		}

		public SimpleFeatureProvider getFeatureProvider() {
			return featureProvider;
		}

		public List<ApplicationSubject> getApplicationSubjects() {
			return applicationSubjects;
		}

	}

	protected class SimpleFeatureProvider implements FeatureProvider {

		private int documentCount = 0;
		private org.appng.api.model.Properties properties;

		SimpleFeatureProvider(org.appng.api.model.Properties properties) {
			this.properties = properties;
		}

		public Consumer<DocumentEvent, DocumentProducer> getIndexer() {
			if (properties.getString("featureIndexing", "false").equalsIgnoreCase("true")) {
				return new Consumer<DocumentEvent, DocumentProducer>() {

					@Override
					public void put(DocumentProducer element) throws InterruptedException {
						super.put(element);
						documentCount++;
					}

					@Override
					public boolean put(DocumentProducer element, long timeoutMillis) throws InterruptedException {
						documentCount++;
						return super.put(element, timeoutMillis);
					}

					@Override
					public void putWithTimeout(DocumentProducer element, long timeoutMillis)
							throws InterruptedException, TimeoutException {
						super.putWithTimeout(element, timeoutMillis);
						documentCount++;
					}

				};
			}
			return null;
		}

		public Integer getDocumentCount() {
			return documentCount;
		}

		public ImageProcessor getImageProcessor(File sourceFile, String targetFile) {
			if (properties.getString("featureImageProcessing", "false").equalsIgnoreCase("true")) {
				return new ImageProcessor(sourceFile, new File(getImageCache(), targetFile));
			}
			return null;
		}

		public File getImageCache() {
			if (properties.getString("featureImageProcessing", "false").equalsIgnoreCase("true")) {
				return new File("target/imageCache");
			}
			return null;
		}

	}

	class SimpleRole implements Role {

		private org.appng.xml.application.Role role;
		private Set<Permission> permissions = new HashSet<Permission>();
		private Integer id;

		public SimpleRole(org.appng.xml.application.Role role, Integer id) {
			this.role = role;
			this.id = id;
		}

		public String getName() {
			return role.getName();
		}

		public String getDescription() {
			return role.getDescription();
		}

		public Integer getId() {
			return id;
		}

		public Application getApplication() {
			return application;
		}

		public Set<Permission> getPermissions() {
			return permissions;
		}

	}

	class SimplePermission implements Permission {

		private org.appng.xml.application.Permission permission;

		SimplePermission(org.appng.xml.application.Permission permission) {
			this.permission = permission;
		}

		public String getName() {
			return permission.getId();
		}

		public String getDescription() {
			return permission.getValue();
		}

		public Integer getId() {
			return null;
		}

		public Application getApplication() {
			return application;
		}

	}
}
