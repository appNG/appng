/*
 * Copyright 2011-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.appng.api.model.Identifier;
import org.appng.core.model.RepositoryUtils;
import org.appng.xml.application.TemplateType;

/**
 * The persistent representation of an appNG template
 * 
 * @author Matthias Müller
 *
 */
@Entity
@Table(name = "template")
@EntityListeners(PlatformEventListener.class)
public class Template implements Identifier, Auditable<Integer> {

	private Integer id;
	private String name;
	private String appNGVersion;
	private String description;
	private String displayName;
	private String longDescription;
	private String timestamp;
	private String packageVersion;
	private TemplateType type;
	private Date version;
	private List<TemplateResource> resources;

	public Template() {
	}

	public Template(org.appng.xml.application.Template template) {
		setName(template.getName());
		setDisplayName(template.getDisplayName());
		setAppNGVersion(template.getAppngVersion());
		setDescription(template.getDescription());
		setLongDescription(template.getLongDescription());
		setPackageVersion(template.getVersion());
		setType(template.getType());
		setTimestamp(template.getTimestamp());
		setResources(new ArrayList<TemplateResource>());
	}

	public void update(Template template) {
		setName(template.getName());
		setDisplayName(template.getDisplayName());
		setAppNGVersion(template.getAppNGVersion());
		setDescription(template.getDescription());
		setLongDescription(template.getLongDescription());
		setPackageVersion(template.getPackageVersion());
		setType(template.getType());
		setTimestamp(template.getTimestamp());
		for (TemplateResource resource : template.getResources()) {
			getResources().add(resource);
			resource.setTemplate(this);
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "appng_version")
	public String getAppNGVersion() {
		return appNGVersion;
	}

	public void setAppNGVersion(String appNGVersion) {
		this.appNGVersion = appNGVersion;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	@Column(name = "display_name")
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Column(name = "long_desc")
	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	@Column(name = "template_version")
	public String getPackageVersion() {
		return packageVersion;
	}

	public void setPackageVersion(String packageVersion) {
		this.packageVersion = packageVersion;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "tpl_type")
	public TemplateType getType() {
		return type;
	}

	public void setType(TemplateType type) {
		this.type = type;
	}

	@Transient
	public boolean isInstalled() {
		return true;
	}

	@Transient
	public boolean isSnapshot() {
		return getPackageVersion().endsWith(RepositoryUtils.SNAPSHOT);
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "template")
	public List<TemplateResource> getResources() {
		return resources;
	}

	public void setResources(List<TemplateResource> resources) {
		this.resources = resources;
	}

	@Version
	@Temporal(TemporalType.TIMESTAMP)
	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCode(this);
	}

	@Override
	public boolean equals(Object o) {
		return ObjectUtils.equals(this, o);
	}

}
