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
package org.appng.api.support.environment;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.servlet.http.HttpSession;

import org.appng.api.Scope;
import org.appng.api.model.Site;
import org.appng.api.support.SiteAwareObjectInputStream;

/**
 * A wrapper used for wrapping {@link Scope#SESSION}-scoped attributes. Keeps track of the {@link Site}'s name. This is
 * needed because the right {@link ClassLoader} has to be used when deserializing objects that have been stored inside the
 * {@link HttpSession}.
 * 
 * @author Matthias Müller
 *
 */
class AttributeWrapper implements Serializable {

	private Object value;
	private String siteName;

	public AttributeWrapper() {

	}

	public AttributeWrapper(String siteName, Object value) throws IllegalArgumentException {
		if (null == value) {
			throw new IllegalArgumentException("value can not be null");
		}
		this.siteName = siteName;
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public String getSiteName() {
		return siteName;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(siteName);
		out.writeObject(value);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		try {
			siteName = (String) in.readObject();
			if (in instanceof SiteAwareObjectInputStream) {
				((SiteAwareObjectInputStream) in).setSite(siteName);
			}
			value = in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			throw e;
		}
	}

	@Override
	public String toString() {
		return value.toString();
	}

}