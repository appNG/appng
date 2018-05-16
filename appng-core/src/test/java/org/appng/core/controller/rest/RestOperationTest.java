package org.appng.core.controller.rest;

import java.util.Locale;
import java.util.TimeZone;

import org.appng.api.ApplicationConfigProvider;
import org.appng.api.Environment;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.support.ApplicationRequest;
import org.appng.core.model.ApplicationProvider;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RestOperationTest {

	static {
		WritingJsonValidator.writeJson = false;
	}

	@Mock
	Environment environment;
	@Mock
	Site site;
	@Mock
	ApplicationProvider application;
	@Mock
	Subject subject;
	@Mock
	ApplicationRequest request;
	@Mock
	MessageSource messageSource;
	@Mock
	ApplicationConfigProvider appconfig;
	@Mock
	Properties siteProps;
	@Mock
	org.appng.forms.Request formsRequest;
	MockHttpServletResponse servletResponse = new MockHttpServletResponse();
	MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(environment.getSubject()).thenReturn(subject);
		Mockito.when(environment.getLocale()).thenReturn(Locale.GERMANY);
		Mockito.when(environment.getTimeZone()).thenReturn(TimeZone.getDefault());
		Mockito.when(site.getProperties()).thenReturn(siteProps);
		Mockito.when(site.getName()).thenReturn("site");
		Mockito.when(siteProps.getString(SiteProperties.MANAGER_PATH)).thenReturn("/manager");
		Mockito.when(siteProps.getString(SiteProperties.SERVICE_PATH)).thenReturn("/service");
		Mockito.when(application.getName()).thenReturn("application");
		Mockito.when(application.getApplicationConfig()).thenReturn(appconfig);
	}
}
