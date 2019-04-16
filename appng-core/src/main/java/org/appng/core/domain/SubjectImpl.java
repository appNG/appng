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
package org.appng.core.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ValidationMessages;
import org.appng.api.model.Application;
import org.appng.api.model.Authorizable;
import org.appng.api.model.Group;
import org.appng.api.model.Role;
import org.appng.api.model.Subject;
import org.appng.api.model.UserType;

/**
 * 
 * Default {@link Subject}-implementation
 * 
 * @author Matthias Müller
 * 
 */
@Entity
@Table(name = "subject")
@EntityListeners(PlatformEventListener.class)
public class SubjectImpl implements Subject, Auditable<Integer> {

	private Integer id;
	private String name;
	private String description;
	private String realname;
	private String email;
	private String language;
	private String timeZone;
	private String digest;
	private String salt;
	private Date version;
	private List<Group> groups = new ArrayList<>();
	private UserType userType;
	private String typeName;
	private boolean isAuthenticated;

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Pattern(regexp = ValidationPatterns.USERNAME_PATTERN, message = ValidationPatterns.USERNAME_MSSG)
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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Version
	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Size(min = 2, max = 3, message = ValidationMessages.VALIDATION_STRING_MIN_MAX)
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Size(max = ValidationPatterns.LENGTH_64, message = ValidationMessages.VALIDATION_STRING_MAX)
	public String getRealname() {
		return realname;
	}

	public void setRealname(String username) {
		this.realname = username;
	}

	@Pattern(regexp = ValidationPatterns.EMAIL_PATTERN, message = ValidationMessages.VALIDATION_EMAIL)
	@Column(name = "email")
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		if (StringUtils.isNotBlank(email)) {
			this.email = email;
		}
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}

	@ManyToMany(targetEntity = GroupImpl.class)
	@JoinTable(joinColumns = { @JoinColumn(name = "subject_Id") }, inverseJoinColumns = {
			@JoinColumn(name = "group_id") })
	public List<Group> getGroups() {
		return groups;
	}

	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	public UserType getUserType() {
		return userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}

	@Transient
	public boolean isAuthenticated() {
		return isAuthenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.isAuthenticated = authenticated;
	}

	@Transient
	public boolean hasApplication(Application application) {
		for (Group g : groups) {
			for (Role role : g.getRoles()) {
				if (application.getRoles().contains(role)) {
					return true;
				}
			}
		}
		return false;
	}

	@Transient
	public List<Role> getApplicationroles(Application application) {
		List<Role> applicationRoles = new ArrayList<>();
		for (Group g : groups) {
			for (Role role : g.getRoles()) {
				if (application.getRoles().contains(role)) {
					applicationRoles.add(role);
				}
			}
		}
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

	public boolean isAuthorized(Authorizable<?> authorizable) {
		if (null == authorizable) {
			return false;
		}
		for (Group group : getGroups()) {
			for (Role applicationRole : group.getRoles()) {
				if (authorizable.getRoleIds().contains(applicationRole.getId())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "Subject#" + getId() + "_" + getName();
	}

	@Column(name = "timezone")
	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	@Transient
	public String getAuthName() {
		return getName();
	}

	@Transient
	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
}
