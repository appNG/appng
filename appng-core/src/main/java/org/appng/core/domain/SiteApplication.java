/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.core.domain;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.appng.api.model.Application;
import org.appng.api.model.Site;

/**
 * A {@link SiteApplication} represents a {@link Application} which is assigned to a {@link Site}, optionally using a
 * {@link DatabaseConnection}.
 * 
 * @author Matthias Müller
 */
@Entity
@Table(name = "site_application")
public class SiteApplication {

	private Site site;
	private Application application;
	private DatabaseConnection databaseConnection;
	private SiteApplicationPK siteApplicationId;
	private boolean markedForDeletion;
	private boolean active;
	private boolean reloadRequired;
	private Set<Site> grantedSites;

	public SiteApplication() {

	}

	public SiteApplication(Site site, Application application) {
		setSiteApplicationId(new SiteApplicationPK(site.getId(), application.getId()));
		setSite(site);
		setApplication(application);
	}

	@ManyToOne(targetEntity = SiteImpl.class)
	@JoinColumn(name = "site_id", referencedColumnName = "id", insertable = false, updatable = false)
	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	@ManyToOne(targetEntity = ApplicationImpl.class)
	@JoinColumn(name = "application_id", referencedColumnName = "id", insertable = false, updatable = false)
	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	@ManyToOne
	@JoinColumn(name = "connection_id")
	public DatabaseConnection getDatabaseConnection() {
		return databaseConnection;
	}

	public void setDatabaseConnection(DatabaseConnection databaseConnection) {
		this.databaseConnection = databaseConnection;
	}

	@ManyToMany(targetEntity = SiteImpl.class)
	@JoinTable(name = "sites_granted", joinColumns = {
			@JoinColumn(name = "application_id", referencedColumnName = "application_id"),
			@JoinColumn(name = "site_id", referencedColumnName = "site_id") }, inverseJoinColumns = @JoinColumn(name = "granted_site_id", referencedColumnName = "id"))
	public Set<Site> getGrantedSites() {
		return grantedSites;
	}

	public void setGrantedSites(Set<Site> grantedSites) {
		this.grantedSites = grantedSites;
	}

	@EmbeddedId
	public SiteApplicationPK getSiteApplicationId() {
		return siteApplicationId;
	}

	public void setSiteApplicationId(SiteApplicationPK siteApplicationId) {
		this.siteApplicationId = siteApplicationId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Column(name = "reload_required")
	public boolean isReloadRequired() {
		return reloadRequired;
	}

	public void setReloadRequired(boolean reloadRequired) {
		this.reloadRequired = reloadRequired;
	}

	@Column(name = "deletion_mark")
	public boolean isMarkedForDeletion() {
		return markedForDeletion;
	}

	public void setMarkedForDeletion(boolean markedForDeletion) {
		this.markedForDeletion = markedForDeletion;
	}
}
