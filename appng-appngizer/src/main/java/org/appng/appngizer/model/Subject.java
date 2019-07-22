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
package org.appng.appngizer.model;

import org.appng.appngizer.model.xml.UserType;
import org.appng.core.domain.SubjectImpl;

public class Subject extends org.appng.appngizer.model.xml.Subject implements UriAware {

	public static SubjectImpl toDomain(org.appng.appngizer.model.xml.Subject s) {
		SubjectImpl subjectImpl = new SubjectImpl();
		subjectImpl.setName(s.getName());
		subjectImpl.setRealname(s.getRealName());
		subjectImpl.setEmail(s.getEmail());
		subjectImpl.setDescription(s.getDescription());
		subjectImpl.setDigest(s.getDigest());
		subjectImpl.setTimeZone(s.getTimeZone());
		subjectImpl.setLanguage(s.getLanguage());
		subjectImpl.setUserType(org.appng.api.model.UserType.valueOf(s.getType().name()));
		return subjectImpl;
	}

	public static Subject fromDomain(org.appng.api.model.Subject subjectImpl, boolean setDigest) {
		Subject subject = new Subject();
		subject.setName(subjectImpl.getName());
		subject.setDescription(subjectImpl.getDescription());
		subject.setTimeZone(subjectImpl.getTimeZone());
		subject.setLanguage(subjectImpl.getLanguage());
		subject.setEmail(subjectImpl.getEmail());
		subject.setRealName(subjectImpl.getRealname());
		if(setDigest){
			subject.setDigest(subjectImpl.getDigest());
		}
		subject.setType(UserType.valueOf(subjectImpl.getUserType().name()));
		subject.setSelf("/subject/" + subject.getName());
		return subject;
	}

}