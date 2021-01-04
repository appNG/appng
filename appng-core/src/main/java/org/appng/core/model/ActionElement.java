/*
 * Copyright 2011-2021 the original author or authors.
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

import org.appng.api.ProcessingException;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.CallableAction;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.SectionDef;
import org.appng.xml.platform.Sectionelement;
import org.appng.xml.platform.SectionelementDef;

/**
 * A {@link Sectionelement} that contains an {@link Action}. This is mainly a delegate to a {@link CallableAction}.
 * 
 * @author Matthias Müller
 */
class ActionElement extends Sectionelement {

	private CallableAction callableAction;

	/**
	 * Creates a new {@code ActionElement}.
	 * 
	 * @param site
	 *                           the current {@link Site}
	 * @param application
	 *                           the current {@link Application}
	 * @param applicationRequest
	 *                           the current {@link ApplicationRequest}
	 * @param actionRef
	 *                           the {@link ActionRef} as given in the {@link SectionelementDef} of a
	 *                           {@link PageDefinition}.
	 * 
	 * @throws ProcessingException
	 *                             if an error occurs while assembling the {@code ActionElement}
	 */
	ActionElement(Site site, Application application, ApplicationRequest applicationRequest, ActionRef actionRef)
			throws ProcessingException {
		this.callableAction = new CallableAction(site, application, applicationRequest, actionRef);
		this.action = callableAction.getAction();
	}

	/**
	 * Delegates to {@link CallableAction#perform()} and then sets the mode, folded and passive-state for this
	 * {@code ActionElement}.
	 * 
	 * @param sectionelement
	 *                        the origin {@link SectionelementDef} of the {@code ActionElement}
	 * @param isSectionHidden
	 *                        whether the {@link SectionDef} including the action is hidden, i.e. the action will not be
	 *                        visible
	 * 
	 * @throws ProcessingException
	 *                             if an error occurred while while performing
	 */
	void perform(SectionelementDef sectionelement, boolean isSectionHidden) throws ProcessingException {
		callableAction.perform(isSectionHidden);
		if (doExecute() && !callableAction.doForward() && hasErrors()
				&& Boolean.TRUE.toString().equalsIgnoreCase(sectionelement.getFolded())) {
			setFolded(Boolean.FALSE.toString());
		}
		if (doInclude()) {
			setFolded(sectionelement.getFolded());
			setMode(sectionelement.getMode());
			setPassive(sectionelement.getPassive());
		}
	}

	/**
	 * @see CallableAction#doExecute()
	 */
	boolean doExecute() {
		return callableAction.doExecute();
	}

	/**
	 * @see CallableAction#doInclude()
	 */
	boolean doInclude() {
		return callableAction.doInclude();
	}

	/**
	 * @see CallableAction#hasErrors()
	 */
	boolean hasErrors() {
		return callableAction.hasErrors();
	}

	/**
	 * @see CallableAction#getOnSuccess()
	 */
	String getOnSuccess() {
		return callableAction.getOnSuccess();
	}

	/**
	 * Returns the {@link CallableAction} this {@code ActionElement} delegates to.
	 * 
	 * @return the {@link CallableAction}
	 */
	CallableAction getCallableAction() {
		return callableAction;
	}

}
