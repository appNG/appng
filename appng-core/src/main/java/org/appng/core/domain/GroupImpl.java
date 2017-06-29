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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.appng.api.ValidationMessages;
import org.appng.api.model.Group;
import org.appng.api.model.Role;
import org.appng.api.model.Subject;

/**
 * 
 * Default {@link Group}-implementation
 * 
 * @author Matthias Müller
 * 
 */
@Entity
@Table(name = "authgroup")
public class GroupImpl implements Group {

	private Integer id;
	private String name;
	private String description;
	private Date version;
	private boolean defaultAdmin;
	private Set<Subject> subjects = new HashSet<Subject>();
	private Set<Role> applicationRoles = new HashSet<Role>();

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
	@Column(unique = true)
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

	@ManyToMany(mappedBy = "groups", targetEntity = SubjectImpl.class)
	public Set<Subject> getSubjects() {
		return subjects;
	}

	public void setSubjects(Set<Subject> subjects) {
		this.subjects = subjects;
	}

	@Version
	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	public void setRoles(Set<Role> applicationRoles) {
		this.applicationRoles = applicationRoles;
	}

	@ManyToMany(targetEntity = RoleImpl.class)
	@JoinTable(name = "authgroup_role", joinColumns = @JoinColumn(name = "authgroup_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	public Set<Role> getRoles() {
		return applicationRoles;
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
		return "Group#" + getId() + "_" + getName();
	}

	@Column(name = "default_admin")
	public boolean isDefaultAdmin() {
		return defaultAdmin;
	}

	public void setDefaultAdmin(boolean defaultAdmin) {
		this.defaultAdmin = defaultAdmin;
	}

}
