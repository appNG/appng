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
package org.appng.api.support;

import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.Section;
import org.appng.xml.platform.SelectionType;

/**
 * Provides factory-methods for creating {@link org.appng.xml.platform.Selection}s.
 * 
 * @author Matthias Müller
 */
public class SelectionFactory extends OptionFactory<SelectionFactory.Selection> {

	protected Selection getOwner(String id, String title) {
		return new Selection(id, title);
	}

	public class Selection extends org.appng.xml.platform.Selection implements OptionOwner {

		private OptionOwner optionOwner;

		public Selection(String id, String titleId) {
			setId(id);
			setTitle(new Label());
			getTitle().setId(titleId);
			optionOwner = new OptionOwnerBase(getOptions());
		}

		public void addOption(Option option) {
			optionOwner.addOption(option);
		}

		public Option addOption(String name, String value, boolean selected) {
			return optionOwner.addOption(name, value, selected);
		}

	}

	/**
	 * Creates a {@link Selection} of type {@link SelectionType#TEXT} with only one {@link Option}. This option uses the
	 * given id as its name and the given value.
	 * 
	 * @param id
	 *              the id for the {@link Selection} and also the name of the single {@link Option}
	 * @param title
	 *              the title for the {@link Section}
	 * @param value
	 *              the {@link Option}'s value
	 * 
	 * @return a {@link Selection} of the given type with one {@link Option}
	 */
	public Selection getTextSelection(String id, String title, String value) {
		return getSimpleSelection(id, title, value, SelectionType.TEXT);
	}

	/**
	 * Creates a {@link Selection} of type {@link SelectionType#DATE} with only one {@link Option}. This option uses the
	 * given id as its name and the given value.
	 * 
	 * @param id
	 *                   the id for the {@link Selection} and also the name of the single {@link Option}
	 * @param title
	 *                   the title for the {@link Section}
	 * @param value
	 *                   the {@link Option}'s value
	 * @param dateFormat
	 *                   the date format pattern to be used
	 * 
	 * @return a {@link Selection} of the given type with one {@link Option}
	 */
	public Selection getDateSelection(String id, String title, String value, String dateFormat) {
		Selection selection = getSimpleSelection(id, title, value, SelectionType.DATE);
		selection.setFormat(dateFormat);
		return selection;
	}

	/**
	 * Creates a {@link Selection} of type {@link SelectionType#DATE} with only one {@link Option}. This option uses the
	 * given id as its name and the given value which is being formatted. {@link FastDateFormat#getPattern()} is used
	 * for {@link Selection#setFormat(String)}.
	 * 
	 * @param id
	 *                   the id for the {@link Selection} and also the name of the single {@link Option}
	 * @param title
	 *                   the title for the {@link Section}
	 * @param value
	 *                   the {@link Option}'s value as a {@link Date}
	 * @param dateFormat
	 * 
	 * @return a {@link Selection} of type {@link SelectionType#DATE} with one {@link Option}
	 */
	public Selection getDateSelection(String id, String title, Date value, FastDateFormat dateFormat) {
		return getDateSelection(id, title, value == null ? null : dateFormat.format(value), dateFormat.getPattern());
	}

	/**
	 * Creates a {@link Selection} of the given {@link SelectionType} with only one {@link Option}. This option uses the
	 * given id as its name and the given value.
	 * 
	 * @param id
	 *              the id for the {@link Selection} and also the name of the single {@link Option}
	 * @param title
	 *              the title for the {@link Section}
	 * @param value
	 *              the {@link Option}'s value
	 * 
	 * @return a {@link Selection} of the given {@link SelectionType} with one {@link Option}
	 */
	public Selection getSimpleSelection(String id, String title, String value, SelectionType type) {
		Selection selection = new Selection(id, title);
		Option opt = new Option();
		if (null == value) {
			opt.setValue("");
		} else {
			opt.setValue(value);
		}
		opt.setName(id);
		selection.addOption(opt);
		selection.setType(type);
		return selection;
	}

}
