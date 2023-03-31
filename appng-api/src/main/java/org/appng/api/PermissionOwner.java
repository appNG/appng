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
package org.appng.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkable;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.OutputFormat;
import org.appng.xml.platform.OutputType;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.Permission;
import org.appng.xml.platform.Permissions;

/**
 * This is a container for a set of {@link Permission}s, making it easier for a {@link PermissionProcessor} to check
 * those.
 * 
 * @author Matthias Müller
 * 
 * @see PermissionProcessor
 */
public final class PermissionOwner {

	private String name;

	private Collection<Permission> permissions;

	/**
	 * Creates a new PermissionOwner from {@link PageDefinition}.
	 * 
	 * @param page
	 *             the {@link PageDefinition}
	 */
	public PermissionOwner(PageDefinition page) {
		this("page:" + page.getId(), page.getConfig());
	}

	/**
	 * Creates a new PermissionOwner from an {@link Event}.
	 * 
	 * @param event
	 *              the {@link Event}
	 */
	public PermissionOwner(Event event) {
		this("event:" + event.getId(), event.getConfig());
	}

	/**
	 * Creates a new PermissionOwner from an {@link Action}.
	 * 
	 * @param action
	 *               the {@link Action}
	 */
	public PermissionOwner(Action action) {
		this("action:" + action.getId(), action.getConfig());
	}

	/**
	 * Creates a new PermissionOwner from {@link ActionRef}.
	 * 
	 * @param actionRef
	 *                  the {@link ActionRef}
	 */
	public PermissionOwner(ActionRef actionRef) {
		this("action-reference:" + actionRef.getId(), actionRef.getPermissions());
	}

	/**
	 * Creates a new PermissionOwner from a {@link Datasource}.
	 * 
	 * @param datasource
	 *                   the {@link Datasource}
	 */
	public PermissionOwner(Datasource datasource) {
		this("datasource:" + datasource.getId(), datasource.getConfig());
	}

	/**
	 * Creates a new PermissionOwner from an {@link DatasourceRef}.
	 * 
	 * @param datasourceRef
	 *                      the {@link DatasourceRef}
	 */
	public PermissionOwner(DatasourceRef datasourceRef) {
		this("datasource-reference:" + datasourceRef.getId(), datasourceRef.getPermissions());
	}

	/**
	 * Creates a new PermissionOwner from an {@link OutputType}.
	 * 
	 * @param outputType
	 *                   the {@link OutputType}
	 */
	public PermissionOwner(OutputType outputType) {
		this("outputType:" + outputType.getId(), outputType.getPermissions());
	}

	/**
	 * Creates a new PermissionOwner from an {@link OutputFormat}.
	 * 
	 * @param outputFormat
	 *                     the {@link OutputFormat}
	 */
	public PermissionOwner(OutputFormat outputFormat) {
		this("outputFormat:" + outputFormat.getId(), outputFormat.getPermissions());
	}

	/**
	 * Creates a new PermissionOwner from a {@link Linkable}.
	 * 
	 * @param linkable
	 *             the {@link PermissionOwner}
	 */
	public PermissionOwner(Linkable linkable) {
		this("link:" + linkable.getLabel().getValue(), linkable.getPermissions());
	}

	/**
	 * Creates a new PermissionOwner from a {@link Linkpanel}.
	 * 
	 * @param linkpanel
	 *                  the {@link Linkpanel}
	 */
	public PermissionOwner(Linkpanel linkpanel) {
		this("linkpanel:" + linkpanel.getId(), linkpanel.getPermissions());
	}

	/**
	 * Creates a new PermissionOwner from a {@link Config}.
	 * 
	 * @param config
	 *               the {@link Config}
	 */
	public PermissionOwner(Config config) {
		this("config", config.getPermissions());
	}

	private PermissionOwner(String name, Permissions permissions) {
		this.name = name;
		setPermissions(permissions);
	}

	private PermissionOwner(String name, Config config) {
		this.name = name;
		if (null == config) {
			setPermissions(null);
		} else {
			setPermissions(config.getPermissions());
		}
	}

	private void setPermissions(Permissions permissions) {
		if (null == permissions) {
			this.permissions = Collections.unmodifiableList(new ArrayList<>());
		} else {
			this.permissions = Collections.unmodifiableList(permissions.getPermissionList());
		}
	}

	/**
	 * Returns the name of this {@link PermissionOwner}
	 * 
	 * @return the name of this {@link PermissionOwner}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns all {@link Permission}s owned by this {@link PermissionOwner}.
	 * 
	 * @return the {@link Permission}s
	 */
	public Collection<Permission> getPermissions() {
		return permissions;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getName());
		if (null != getPermissions()) {
			sb.append(" permissions = ");
			for (Permission p : getPermissions()) {
				sb.append(p.getRef() + ", ");
			}
		}
		return sb.toString();
	}
}
