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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.appng.api.ValidationMessages;
import org.appng.api.model.Application;
import org.appng.api.model.Permission;
import org.appng.api.model.Role;

/**
 * 
 * Default {@link Role}-implementation
 * 
 * @author Matthias Müller
 * 
 */
@Entity
@Table(name = "role")
@EntityListeners(PlatformEventListener.class)
public class RoleImpl implements Role, Auditable<Integer> {

	private Integer id;
	private String name;
	private String description;
	private Date version;
	private Application application;
	private Set<Permission> permissions = new HashSet<Permission>();

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Pattern(regexp = ValidationPatterns.NAME_PATTERN, message = ValidationPatterns.NAME_MSSG)
	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Size(max = ValidationPatterns.LENGTH_8192, message = ValidationMessages.VALIDATION_STRING_MAX)
	@Column(length = ValidationPatterns.LENGTH_8192)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Version
	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	@ManyToMany(targetEntity = PermissionImpl.class, fetch = FetchType.LAZY)
	@JoinTable(name = "role_permission", joinColumns = {
			@JoinColumn(name = "role_id", referencedColumnName = "id") }, inverseJoinColumns = {
					@JoinColumn(name = "permissions_id", referencedColumnName = "id") })
	public Set<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	@ManyToOne(targetEntity = ApplicationImpl.class)
	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCode(this);
	}

	@Override
	public boolean equals(Object o) {
		return ObjectUtils.equals(this, o);
	}

	@Override
	public String toString() {
		return "Role#" + getId() + "_" + getName();
	}

}
