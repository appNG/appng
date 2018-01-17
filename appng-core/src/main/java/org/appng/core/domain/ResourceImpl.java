/*
 * Copyright 2011-2018 the original author or authors.
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

import java.io.Closeable;
import java.io.File;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.apache.commons.codec.digest.DigestUtils;
import org.appng.api.model.Application;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;

/**
 * 
 * Default {@link Resource}-implementation
 * 
 * @author Matthias Müller
 * 
 */
@Entity
@Table(name = "resource")
@EntityListeners(PlatformEventListener.class)
public class ResourceImpl implements Resource, Auditable<Integer>, Closeable {

	private Integer id;
	private String name;
	private String description;
	private Date version;
	private Application application;
	private ResourceType type;
	private byte[] bytes;
	private File cachedFile;
	private String checkSum;

	public ResourceImpl() {

	}

	public ResourceImpl(Application application, Resource resource) {
		setApplication(application);
		setBytes(resource.getBytes());
		setName(resource.getName());
		setResourceType(resource.getResourceType());
		setDescription(resource.getDescription());
		calculateChecksum();
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

	@ManyToOne(targetEntity = ApplicationImpl.class)
	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	public ResourceType getResourceType() {
		return type;
	}

	public void setResourceType(ResourceType type) {
		this.type = type;
	}

	@Lob
	@NotNull
	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	@Column(name = "checksum")
	public String getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(String checkSum) {
		this.checkSum = checkSum;
	}

	@Transient
	public File getCachedFile() {
		return cachedFile;
	}

	public void setCachedFile(File cachedFile) {
		this.cachedFile = cachedFile;
	}

	@Transient
	public int getSize() {
		return null == bytes ? 0 : bytes.length;
	}

	public void calculateChecksum() {
		if (getSize() > 0) {
			setCheckSum(DigestUtils.sha256Hex(getBytes()));
		}
	}

	public void close() {
		this.cachedFile = null;
	}

}
