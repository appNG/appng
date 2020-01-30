/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.appngizer.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.model.AuthSubject.PasswordChangePolicy;
import org.appng.api.model.UserType;
import org.appng.appngizer.model.Group;
import org.appng.appngizer.model.Groups;
import org.appng.appngizer.model.Subject;
import org.appng.appngizer.model.Subjects;
import org.appng.appngizer.model.Utils;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.security.BCryptPasswordHandler;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SubjectController extends ControllerBase {

	@GetMapping(value = "/subject")
	public ResponseEntity<Subjects> listSubjects() {
		List<Subject> subjects = new ArrayList<>();
		for (org.appng.api.model.Subject g : getCoreService().getSubjects()) {
			subjects.add(Subject.fromDomain(g, false));
		}
		Subjects entity = new Subjects(subjects);
		entity.applyUriComponents(getUriBuilder());
		return ok(entity);
	}

	@GetMapping(value = "/subject/{name}")
	public ResponseEntity<Subject> getSubject(@PathVariable("name") String name) {
		SubjectImpl subject = getCoreService().getSubjectByName(name, true);
		if (null == subject) {
			return notFound();
		}
		Subject fromDomain = addGroups(subject);
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	public Subject addGroups(SubjectImpl subject) {
		Subject fromDomain = Subject.fromDomain(subject, true);
		Groups groups = new Groups();
		fromDomain.setGroups(groups);
		List<org.appng.api.model.Group> subjectGroups = subject.getGroups();
		for (org.appng.api.model.Group group : subjectGroups) {
			Group g = Group.fromDomain(group);
			g.applyUriComponents(getUriBuilder());
			fromDomain.getGroups().getGroup().add(g);
		}
		return fromDomain;
	}

	@PostMapping(value = "/subject")
	public ResponseEntity<Subject> createSubject(@RequestBody org.appng.appngizer.model.xml.Subject subject)
			throws BusinessException {
		SubjectImpl currentSubject = getCoreService().getSubjectByName(subject.getName(), false);
		if (null != currentSubject) {
			return conflict();
		}
		SubjectImpl newSubject = Subject.toDomain(subject);
		setOptionalFieldsForDomain(subject, newSubject, subject.getDigest());

		getCoreService().createSubject(newSubject);
		assignGroups(subject.getName(), subject);
		return created(getSubject(subject.getName()).getBody());
	}

	@PutMapping(value = "/subject/{name}")
	public ResponseEntity<Subject> updateSubject(@PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Subject subject) throws BusinessException {
		SubjectImpl subjectByName = getCoreService().getSubjectByName(name, true);
		if (null == subjectByName) {
			return notFound();
		}
		subjectByName.setRealname(subject.getRealName());
		subjectByName.setEmail(subject.getEmail());
		subjectByName.setDescription(subject.getDescription());
		subjectByName.setTimeZone(subject.getTimeZone());
		subjectByName.setLanguage(subject.getLanguage());
		subjectByName.setUserType(UserType.valueOf(subject.getType().name()));
		setOptionalFieldsForDomain(subject, subjectByName, subject.getDigest());
		getCoreService().updateSubject(subjectByName);
		assignGroups(name, subject);

		SubjectImpl updatedSubject = getCoreService().getSubjectByName(name, true);
		Subject fromDomain = addGroups(updatedSubject);
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	void setOptionalFieldsForDomain(org.appng.appngizer.model.xml.Subject subject, SubjectImpl subjectByName,
			String digest) {
		if (null != subject.isLocked()) {
			Date lockedSince = null;
			if (subject.isLocked()) {
				lockedSince = null == subject.getLockedSince() ? new Date() : Utils.getDate(subject.getLockedSince());
				subjectByName.setLockedSince(new Date());
			}
			subjectByName.setLockedSince(lockedSince);
		}

		if (null != subject.getPasswordChangePolicy()) {
			PasswordChangePolicy passwordChangePolicy = org.appng.api.model.AuthSubject.PasswordChangePolicy
					.valueOf(subject.getPasswordChangePolicy().name());
			subjectByName.setPasswordChangePolicy(passwordChangePolicy);
		}
		if (StringUtils.isNotBlank(digest)) {
			if (digest.startsWith(BCryptPasswordHandler.getPrefix())) {
				subjectByName.setDigest(digest);
			} else {
				new BCryptPasswordHandler(subjectByName).savePassword(digest);
			}
		}
	}

	public void assignGroups(String name, org.appng.appngizer.model.xml.Subject subject) throws BusinessException {
		List<String> groupNames = new ArrayList<>();
		if (null != subject.getGroups() && null != subject.getGroups().getGroup()) {
			for (org.appng.appngizer.model.xml.Group group : subject.getGroups().getGroup()) {
				groupNames.add(group.getName());
			}
		}
		getCoreService().addGroupsToSubject(name, groupNames, true);
	}

	@DeleteMapping(value = "/subject/{name}")
	public ResponseEntity<Void> deleteSubject(@PathVariable("name") String name) {
		SubjectImpl currentSubject = getCoreService().getSubjectByName(name, false);
		if (null == currentSubject) {
			return notFound();
		} else {
			HttpHeaders headers = new HttpHeaders();
			headers.setLocation(getUriBuilder().path("/subject").build().toUri());
			getCoreService().deleteSubject(currentSubject);
			return noContent(headers);
		}
	}

	Logger logger() {
		return LOGGER;
	}
}
