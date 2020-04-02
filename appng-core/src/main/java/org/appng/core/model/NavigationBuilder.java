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

import static org.appng.api.Scope.PLATFORM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.ParameterSupport;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.xml.platform.ItemType;
import org.appng.xml.platform.Navigation;
import org.appng.xml.platform.NavigationItem;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility-class for ehlping to build a {@link Navigation}-object, based on the current {@link Environment} and
 * {@link PathInfo}.
 * 
 * @author Matthias Müller
 */
@Slf4j
class NavigationBuilder {

	static final String CHANGE_PASSWORD = "changePassword";
	private static final String SLASH = "/";

	private PathInfo pathInfo;
	private Environment env;

	NavigationBuilder(PathInfo pathInfo, Environment environment) {
		this.pathInfo = pathInfo;
		this.env = environment;
	}

	void processNavigation(Navigation navigation, ParameterSupport parameterSupport, boolean allowChangePassword) {

		List<NavigationItem> items = navigation.getItem();
		List<NavigationItem> sites = new ArrayList<>();
		NavigationItem siteTemplate = null;

		for (NavigationItem navItem : items) {

			ItemType navType = navItem.getType();

			switch (navType) {

			// direct call of a certain application page
			case APPLICATION:
				processItem(navItem, false);
				break;
			case ANCHOR:
				replaceProperties(navItem, parameterSupport);
				processItem(navItem, false);
				break;

			// insert site list. Child elements are the site template
			case SITES:
				siteTemplate = navItem;
				sites = processSiteTemplate(navItem);
				break;
			default:
				break;
			}

		}

		if (null != siteTemplate) {
			int siteTemplateIdx = items.indexOf(siteTemplate);
			items.addAll(siteTemplateIdx, sites);
			removeItem(navigation, siteTemplate);
		}
		Optional<NavigationItem> changePassword = getNavItem(navigation, CHANGE_PASSWORD);
		if (!allowChangePassword && changePassword.isPresent()) {
			removeItem(navigation, changePassword.get());
		}
		sort(items);
		sort(sites);
	}

	void replaceProperties(NavigationItem navItem, ParameterSupport parameterSupport) {
		String site = parameterSupport.replaceParameters(navItem.getSite());
		navItem.setSite(site);

		String application = parameterSupport.replaceParameters(navItem.getApplication());
		navItem.setApplication(application);

		String page = parameterSupport.replaceParameters(navItem.getPage());
		navItem.setPage(page);

		String ref = parameterSupport.replaceParameters(navItem.getRef());
		navItem.setRef(ref);

		String actionName = parameterSupport.replaceParameters(navItem.getActionName());
		navItem.setActionName(actionName);

		String actionValue = parameterSupport.replaceParameters(navItem.getActionValue());
		navItem.setActionValue(actionValue);
	}

	private void processItem(NavigationItem navItem, boolean addSitePath) {
		String sitePath = "";
		if (null != navItem.getSite()) {
			sitePath = SLASH + navItem.getSite();
		}

		String navRef = pathInfo.getRootPath() + pathInfo.getOutputPrefix();
		if (addSitePath) {
			navRef += sitePath;
		}
		navRef += SLASH + navItem.getRef();

		boolean pathSelected = pathInfo.isPathSelected(navRef);
		if (pathSelected) {
			selectNavigationItem(navItem);
		}

	}

	private List<NavigationItem> processSiteTemplate(NavigationItem siteTemplate) {
		List<NavigationItem> siteItems = new ArrayList<>();
		org.appng.api.model.Subject subject = env.getSubject();

		Map<String, Site> sites = env.getAttribute(PLATFORM, Platform.Environment.SITES);

		for (String siteName : sites.keySet()) {

			Site navSite = sites.get(siteName);

			if (null != subject) {
				NavigationItem siteItem = new NavigationItem();
				siteItem.setType(ItemType.SITE);
				siteItem.setRef(navSite.getName());
				siteItem.setName(navSite.getHost());
				siteItem.setLabel(navSite.getName());

				processItem(siteItem, true);

				List<NavigationItem> siteNavigation = addSiteNavigation(navSite, siteTemplate);
				if (!siteNavigation.isEmpty() && !isNavigationItemHidden(siteNavigation)) {
					siteItem.getItem().addAll(siteNavigation);
					siteItems.add(siteItem);
				}
			}
		}
		return siteItems;
	}

	private boolean isNavigationItemHidden(List<NavigationItem> siteNavigation) {
		boolean hidden = true;
		for (NavigationItem navigationItem : siteNavigation) {
			if (!navigationItem.isHidden()) {
				hidden = false;
				break;
			}
		}
		return hidden;
	}

	void selectNavigationItem(NavigationItem item) {
		LOGGER.debug("selecting NavigationItem @ref='{}'", item.getRef());
		item.setSelected(true);
		String site = item.getSite();
		String application = item.getApplication();
		String page = item.getPage();
		String actionName = item.getActionName();
		String actionValue = item.getActionValue();

		if (StringUtils.isNotEmpty(application) && StringUtils.isNotEmpty(site) && StringUtils.isNotEmpty(page)) {
			pathInfo.setApplicationName(application);
			pathInfo.setPage(page);
			LOGGER.debug("NavigationItem @ref='{}' points to page {}/{}/{}", item.getRef(), site, application, page);
			if (StringUtils.isNotEmpty(actionName) && StringUtils.isNotEmpty(actionValue)) {
				LOGGER.debug("NavigationItem @ref='{}' has action {}={}", item.getRef(), actionName, actionValue);
				pathInfo.setAction(actionName, actionValue);
			}
		}
	}

	private List<NavigationItem> addSiteNavigation(Site navSite, NavigationItem siteTemplate) {

		List<NavigationItem> navItems = new ArrayList<>();
		List<NavigationItem> tplItems = siteTemplate.getItem();

		for (NavigationItem tplItem : tplItems) {

			NavigationItem navItem;

			switch (tplItem.getType()) {

			case APPLICATION:
				navItem = copyNavigationItem(tplItem, false);
				navItem.setSite(navSite.getName());
				processItem(navItem, true);
				navItems.add(navItem);
				break;

			case APPLICATIONS:
				Set<Application> applications = navSite.getApplications();
				org.appng.api.model.Subject subject = env.getSubject();
				for (Application application : applications) {
					if (subject.hasApplication(application)) {
						Boolean hidden = false;
						Boolean isHidden = application.isHidden();
						if (null != isHidden) {
							hidden = isHidden;
						}

						NavigationItem applicationItem = new NavigationItem();
						applicationItem.setType(ItemType.APPLICATION);
						applicationItem.setRef(application.getName());
						applicationItem.setLabel(application.getDisplayName());
						applicationItem.setHidden(hidden);
						applicationItem.setSite(navSite.getName());

						processItem(applicationItem, true);
						navItems.add(applicationItem);
					}
				}
				break;
			default:
				break;
			}
		}
		return sort(navItems);
	}

	private List<NavigationItem> sort(List<NavigationItem> navItems) {
		Collections.sort(navItems, new Comparator<NavigationItem>() {
			public int compare(NavigationItem n1, NavigationItem n2) {
				return n1.getLabel().compareTo(n2.getLabel());
			}
		});
		return navItems;
	}

	private NavigationItem copyNavigationItem(NavigationItem item, boolean copyRecursive) {
		NavigationItem newItem = new NavigationItem();
		newItem.setHidden(item.isHidden());
		newItem.setLabel(item.getLabel());
		newItem.setName(item.getName());
		newItem.setPage(item.getPage());
		newItem.setApplication(item.getApplication());
		newItem.setRef(item.getRef());
		newItem.setSelected(item.isSelected());
		newItem.setSite(item.getSite());
		newItem.setType(item.getType());

		if (copyRecursive) {
			for (NavigationItem childItem : item.getItem()) {
				newItem.getItem().add(copyNavigationItem(childItem, true));
			}
		}

		return newItem;
	}

	public Optional<NavigationItem> getNavItem(Navigation navigation, String actionValue) {
		return navigation.getItem().stream()
				.filter(i -> i.getType().equals(ItemType.ANCHOR) && i.getActionValue().equals(actionValue)).findAny();
	}

	public void removeItems(Navigation navigation, ItemType type) {
		new ArrayList<>(navigation.getItem()).stream().filter(i -> i.getType().equals(type))
				.forEach(i -> removeItem(navigation, i));
	}

	public boolean removeItem(Navigation navigation, NavigationItem item) {
		return null == item ? false : navigation.getItem().remove(item);
	}

}
