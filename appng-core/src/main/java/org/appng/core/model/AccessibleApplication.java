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
package org.appng.core.model;

import java.util.Date;
import java.util.Set;

import org.appng.api.model.Application;
import org.appng.api.model.FeatureProvider;
import org.appng.api.model.Permission;
import org.appng.api.model.Properties;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Interface extending {@link Application}, adding the ability to modify the fields with appropriate setters.
 * 
 * @author Matthias Müller
 */
public interface AccessibleApplication extends Application {

	void setId(Integer id);

	void setName(String name);

	void setDescription(String description);

	void setFileBased(boolean fileBased);

	void setVersion(Date version);

	void setPermissions(Set<Permission> permissions);

	void setRoles(Set<Role> roles);

	Resources getResources();

	void setResources(Resources resources);

	void setProperties(Properties properties);

	void setContext(ConfigurableApplicationContext applicationContext);

	ConfigurableApplicationContext getContext();

	void setPrivileged(boolean isPrivileged);

	void setFeatureProvider(FeatureProvider featureProvider);

	void closeContext();

}
