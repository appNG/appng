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
package org.appng.core.controller.filter;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.appng.core.controller.filter.RedirectFilter.CachedUrlRewriter;
import org.appng.core.controller.filter.RedirectFilter.RedirectRule;
import org.appng.core.controller.filter.RedirectFilter.UrlRewriteConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JspExtensionFilterTest {

	private static final String JSP = ".jsp";
	private static final String domain = "http://www.appng.org";
	private JspExtensionFilter filter = new JspExtensionFilter();
	private String sourcePath = "URLRewriteFilterTest";
	private CachedUrlRewriter cachedUrlRewriter;

	@Before
	public void setup() throws Exception {
		String confPath = "conf/urlrewrite.xml";
		InputStream confIs = getClass().getClassLoader().getResourceAsStream(confPath);
		URL confUrl = getClass().getClassLoader().getResource(confPath);
		UrlRewriteConfig conf = new UrlRewriteConfig(confIs, "urlrewrite.xml", confUrl);
		RedirectFilter redirectFilter = new RedirectFilter();
		cachedUrlRewriter = redirectFilter.new CachedUrlRewriter(conf, domain, ".jsp");
	}

	@Test
	public void testRedirectRules() {
		List<RedirectRule> redirectRules = getRedirectRules();
		Assert.assertEquals(3, redirectRules.size());
		verifyRule(redirectRules.get(0), "/en/page.jsp", "/de/seite");
		verifyRule(redirectRules.get(1), "/en/index.jsp", "/de/index");
		verifyRule(redirectRules.get(2), "/fr/index.jsp", "/fr/accueil");
	}

	private void verifyRule(RedirectRule redirectRule, String pattern, String target) {
		Assert.assertEquals(pattern, redirectRule.getPattern());
		Assert.assertEquals(target, redirectRule.getTarget());
	}

	@Test
	public void testReplace() {
		Assert.assertEquals("<a href=\"/de/index\">", doReplace("<a href=\"/de/index.jsp\">"));
		Assert.assertEquals("<a href=\"/de/seite\">", doReplace("<a href=\"/en/page.jsp\">"));
		Assert.assertEquals("url='/de/seite'", doReplace("url='/en/page.jsp'"));
		Assert.assertEquals("url='/de/foo' '/en/bar'", doReplace("url='/de/foo.jsp' '/en/bar.jsp'"));
		Assert.assertEquals("/app", doReplace("/app"));
	}

	@Test
	public void testStripJsp() {
		assertUnchanged("<a href=\"/de/index\">");
		assertUnchanged("<a href=\"/de/seite\">");
		assertUnchanged("/de/foobar.jsp");
		Assert.assertEquals("<a href=\"/de/foo\">", doReplace("<a href=\"/de/foo.jsp\">"));
		Assert.assertEquals("'/en/bar'", doReplace("'/en/bar.jsp'"));
		Assert.assertEquals("/de/index", doReplace("/en/index.jsp"));
	}

	@Test
	public void testReplaceDomain() {
		assertUnchanged("<a href=\"" + domain + "/de/seite\">");
		assertUnchanged("<a href=\"" + domain + "/de/foo\">");
		assertUnchanged("http://www.example.com/foobar.jsp");
		assertUnchanged("'http://www.example.com/foobar.jsp'");
		Assert.assertEquals("url='" + domain + "/de/seite'", doReplace("url='" + domain + "/en/page.jsp'"));
		Assert.assertEquals("url='" + domain + "/de/foo'", doReplace("url='" + domain + "/de/foo.jsp'"));
		Assert.assertEquals(domain + "/de/foo", doReplace(domain + "/de/foo.jsp"));
		Assert.assertEquals("<a href=\"" + domain + "/de/seite\">",
				doReplace("<a href=\"" + domain + "/en/page.jsp\">"));
		assertUnchanged("http://www.appng.org/app");
	}

	private void assertUnchanged(String string) {
		Assert.assertEquals(string, doReplace(string));
	}

	private List<RedirectRule> getRedirectRules() {
		return cachedUrlRewriter.getRedirectRules();
	}

	private String doReplace(String content) {
		return filter.doReplace(getRedirectRules(), sourcePath, domain, JSP, content);
	}

}
