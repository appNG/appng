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
package org.appng.api.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.appng.api.model.Identifiable;
import org.appng.api.model.NameProvider;
import org.appng.api.model.Named;
import org.appng.api.support.OptionOwner.HitCounter;
import org.appng.api.support.OptionOwner.Selector;
import org.appng.xml.platform.Option;

abstract class OptionFactory<T extends OptionOwner> {

	protected abstract T getOwner(String id, String titleId);

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Named} elements. For the {@link Option}'s id,
	 * {@link Named#getId()} is used. If no {@link NameProvider} is given, {@link Named#getName()()} is used for the
	 * {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElements
	 *            the selected elements
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <N extends Named<?>> T fromNamed(String id, String titleId, Iterable<? extends N> allElements,
			Collection<? extends N> selectedElements, NameProvider<N> nameProvider) {
		T owner = getOwner(id, titleId);
		addNamedOptions(allElements, selectedElements, owner, nameProvider);
		return owner;
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Named} elements. For the {@link Option}'s id,
	 * {@link Named#getId()} is used. If no {@link NameProvider} is given, {@link Named#getName()()} is used for the
	 * {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElement
	 *            the selected element
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <N extends Named<?>> T fromNamed(String id, String titleId, Iterable<? extends N> allElements,
			N selectedElement, NameProvider<N> nameProvider) {
		T owner = getOwner(id, titleId);
		addNamedOptions(allElements, Arrays.asList(selectedElement), owner, nameProvider);
		return owner;
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Named} elements. For the {@link Option}'s id,
	 * {@link Named#getId()} is used. {@link Named#getName()()} is used for the {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElements
	 *            the selected elements
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <N extends Named<?>> T fromNamed(String id, String titleId, Iterable<? extends N> allElements,
			Collection<? extends N> selectedElements) {
		return fromNamed(id, titleId, allElements, selectedElements, null);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Named} elements. For the {@link Option}'s id,
	 * {@link Named#getId()} is used. {@link Named#getName()()} is used for the {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElement
	 *            the selected element
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <N extends Named<?>> T fromNamed(String id, String titleId, Iterable<? extends N> allElements,
			N selectedElement) {
		return fromNamed(id, titleId, allElements, Arrays.asList(selectedElement), null);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Named} elements. For the {@link Option}'s id,
	 * {@link Named#getId()} is used. {@link Named#getName()()} is used for the {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selector
	 *            a {@link org.appng.api.support.OptionOwner.Selector}
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <N extends Named<?>> T fromNamed(String id, String titleId, Iterable<? extends N> allElements,
			Selector selector) {
		return fromNamed(id, titleId, allElements, selector, null);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Named} elements. For the {@link Option}'s id,
	 * {@link Named#getId()} is used. If no {@link NameProvider} is given, {@link Named#getName()()} is used for the
	 * {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selector
	 *            a {@link org.appng.api.support.OptionOwner.Selector}
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <N extends Named<?>> T fromNamed(String id, String titleId, Iterable<? extends N> allElements,
			Selector selector, NameProvider<N> nameProvider) {
		T owner = getOwner(id, titleId);
		addNamedOptions(allElements, new ArrayList<N>(), owner, nameProvider);
		applySelector(owner, selector);
		return owner;
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Identifiable} elements. For the
	 * {@link Option}'s id, {@link Identifiable#getId()} is used, for the value {@link #toString()}.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElements
	 *            the selected elements
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <I extends Identifiable<?>> T fromIdentifiable(String id, String titleId, Iterable<? extends I> allElements,
			Collection<? extends I> selectedElements) {
		T owner = getOwner(id, titleId);
		addIdentifiableOptions(allElements, selectedElements, owner, null);
		return owner;
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Identifiable} elements. For the
	 * {@link Option}'s id, {@link Identifiable#getId()} is used, for the value {@link #toString()}.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElement
	 *            the selected element
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <I extends Identifiable<?>> T fromIdentifiable(String id, String titleId, Iterable<? extends I> allElements,
			I selectedElement) {
		return fromIdentifiable(id, titleId, allElements, selectedElement, null);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Identifiable} elements. For the
	 * {@link Option}'s id, {@link Identifiable#getId()} is used. If no {@link NameProvider} is given,
	 * {@link #toString()} is used for the {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElement
	 *            the selected element
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <I extends Identifiable<?>> T fromIdentifiable(String id, String titleId, Iterable<? extends I> allElements,
			I selectedElement, NameProvider<I> nameProvider) {
		return fromIdentifiable(id, titleId, allElements, Arrays.asList(selectedElement), nameProvider);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Identifiable} elements. For the
	 * {@link Option}'s id, {@link Identifiable#getId()} is used. If no {@link NameProvider} is given,
	 * {@link #toString()} is used for the {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElements
	 *            the selected elements
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <I extends Identifiable<?>> T fromIdentifiable(String id, String titleId, Iterable<? extends I> allElements,
			Collection<? extends I> selectedElements, NameProvider<I> nameProvider) {
		T owner = getOwner(id, titleId);
		addIdentifiableOptions(allElements, selectedElements, owner, nameProvider);
		return owner;
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Identifiable} elements. For the
	 * {@link Option}'s id, {@link Identifiable#getId()} is used, for the value {@link #toString()}.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selector
	 *            a {@link org.appng.api.support.OptionOwner.Selector}
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <I extends Identifiable<?>> T fromIdentifiable(String id, String titleId, Iterable<? extends I> allElements,
			Selector selector) {
		return fromIdentifiable(id, titleId, allElements, selector, null);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Identifiable} elements. For the
	 * {@link Option}'s id, {@link Identifiable#getId()} is used. If no {@link NameProvider} is given,
	 * {@link #toString()} is used for the {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selector
	 *            a {@link org.appng.api.support.OptionOwner.Selector}
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <I extends Identifiable<?>> T fromIdentifiable(String id, String titleId, Iterable<? extends I> allElements,
			Selector selector, NameProvider<I> nameProvider) {
		T owner = getOwner(id, titleId);
		addIdentifiableOptions(allElements, new ArrayList<I>(), owner, nameProvider);
		applySelector(owner, selector);
		return owner;
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Enum} elements. For both, the {@link Option}
	 * 's id and value, {@link Enum#name()} is used.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElements
	 *            the selected elements
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <E extends Enum<E>> T fromEnum(String id, String titleId, E[] allElements, Collection<E> selectedElements) {
		return fromEnum(id, titleId, allElements, selectedElements, null);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Enum} elements. For both, the {@link Option}
	 * 's id and value, {@link Enum#name()} is used.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElement
	 *            the selected element
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <E extends Enum<E>> T fromEnum(String id, String titleId, E[] allElements, E selectedElement) {
		return fromEnum(id, titleId, allElements, selectedElement, null);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Enum} elements. For the {@link Option}'s id,
	 * {@link Enum#name()} is used. If no {@link NameProvider} is given, {@link Enum#name()} is also used for the
	 * {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElements
	 *            the selected elements
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <E extends Enum<E>> T fromEnum(String id, String titleId, E[] allElements, Collection<E> selectedElements,
			NameProvider<E> nameProvider) {
		T owner = getOwner(id, titleId);
		addEnumOptions(allElements, selectedElements, owner, nameProvider);
		return owner;
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Enum} elements. For the {@link Option}'s id,
	 * {@link Enum#name()} is used. If no {@link NameProvider} is given, {@link Enum#name()} is also used for the
	 * {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElement
	 *            the selected element
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <E extends Enum<E>> T fromEnum(String id, String titleId, E[] allElements, E selectedElement,
			NameProvider<E> nameProvider) {
		return fromEnum(id, titleId, allElements, Arrays.asList(selectedElement), nameProvider);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Object}s. For both, the {@link Option}'s id
	 * and value, {@link Object#toString()} is used.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selector
	 *            a {@link org.appng.api.support.OptionOwner.Selector}
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <S> T fromObjects(String id, String titleId, S[] allElements, Selector selector) {
		return fromObjects(id, titleId, allElements, selector, null);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Object}s. For the {@link Option}'s id,
	 * {@link Object#toString()} is used. If no {@link NameProvider} is given, {@link Object#toString()} is also used
	 * for the {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selector
	 *            a {@link org.appng.api.support.OptionOwner.Selector}
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <S> T fromObjects(String id, String titleId, S[] allElements, Selector selector,
			NameProvider<S> nameProvider) {
		T owner = getOwner(id, titleId);
		addOptions(allElements, new ArrayList<S>(), owner, nameProvider);
		applySelector(owner, selector);
		return owner;
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Object}s. For both, the {@link Option}'s id
	 * and value, {@link Object#toString()} is used.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElements
	 *            the selected elements
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <S> T fromObjects(String id, String titleId, S[] allElements, S... selectedElements) {
		return fromObjects(id, titleId, allElements, null, selectedElements);
	}

	/**
	 * Builds and returns a new {@link OptionOwner} using the given {@link Object}s. For the {@link Option}'s id,
	 * {@link Object#toString()} is used. If no {@link NameProvider} is given, {@link Object#toString()} is also used
	 * for the {@link Option}'s value.
	 * 
	 * @param id
	 *            the id of the {@link OptionOwner}
	 * @param titleId
	 *            the title-id of the {@link OptionOwner} (see {@link org.appng.xml.platform.Label#getId()}).
	 * @param allElements
	 *            the elements to create {@link Option}s from
	 * @param selectedElements
	 *            the selected elements
	 * @param nameProvider
	 *            a {@link NameProvider} (optional)
	 * @return a new {@link OptionOwner} (actually a {@link org.appng.xml.platform.Selection} or a
	 *         {@link org.appng.xml.platform.OptionGroup})
	 */
	public <S> T fromObjects(String id, String titleId, S[] allElements, NameProvider<S> nameProvider,
			S... selectedElements) {
		T owner = getOwner(id, titleId);
		addOptions(allElements, Arrays.asList(selectedElements), owner, nameProvider);
		return owner;
	}

	/**
	 * Counts and sets the hits for the options based on their value.
	 * 
	 * @param options
	 *            some options
	 * @param counter
	 *            a {@link HitCounter}
	 * 
	 * @see Option#getValue()
	 * @see Option#getHits()
	 */
	public void countHits(Iterable<Option> options, HitCounter<String> counter) {
		for (Option option : options) {
			option.setHits(counter.count(option.getValue()));
		}
	}

	/**
	 * Counts and sets the hits for the owner's options based on their value.
	 * 
	 * @param optionOwner
	 *            an {@link OptionOwner}
	 * @param counter
	 *            a {@link HitCounter}
	 * 
	 * @see Option#getValue()
	 * @see Option#getHits()
	 */
	public void countHits(OptionOwner optionOwner, HitCounter<String> counter) {
		countHits(optionOwner.getOptions(), counter);
	}

	private <I extends Identifiable<?>> void addIdentifiableOptions(Iterable<? extends I> allElements,
			Collection<? extends I> selectedElements, OptionOwner owner, NameProvider<I> nameProvider) {
		for (I element : allElements) {
			String name = (null == nameProvider) ? element.toString() : nameProvider.getName(element);
			boolean selected = selectedElements.contains(element);
			owner.addOption(name, element.getId().toString(), selected);
		}
	}

	private <N extends Named<?>> void addNamedOptions(Iterable<? extends N> allElements,
			Collection<? extends N> selectedElements, OptionOwner owner, NameProvider<N> nameProvider) {
		for (N element : allElements) {
			boolean selected = selectedElements.contains(element);
			String name = nameProvider == null ? element.getName() : nameProvider.getName(element);
			owner.addOption(name, element.getId().toString(), selected);
		}
	}

	private <E extends Enum<E>> void addEnumOptions(E[] allElements, Collection<E> selectedElements, OptionOwner owner,
			NameProvider<E> nameProvider) {
		for (E element : allElements) {
			boolean selected = selectedElements.contains(element);
			String name = (null == nameProvider) ? element.name() : nameProvider.getName(element);
			owner.addOption(name, element.name(), selected);
		}
	}

	private <S> void addOptions(S[] allElements, Collection<S> selectedElements, OptionOwner owner,
			NameProvider<S> nameProvider) {
		for (S element : allElements) {
			boolean selected = selectedElements.contains(element);
			String name = (null == nameProvider) ? element.toString() : nameProvider.getName(element);
			owner.addOption(name, element.toString(), selected);
		}
	}

	private void applySelector(OptionOwner owner, Selector selector) {
		for (Option option : owner.getOptions()) {
			selector.select(option);
			option.setHits(selector.count(option.getValue()));
		}
	}

}
