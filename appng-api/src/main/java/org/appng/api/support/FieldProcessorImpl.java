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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.FieldProcessor;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

/**
 * 
 * Default {@link FieldProcessor}-implementation
 * 
 * @author Matthias Müller
 * 
 */
public final class FieldProcessorImpl implements FieldProcessor, Serializable {

	private List<Message> noticeMessages = new ArrayList<>();
	private List<Message> okMessages = new ArrayList<>();
	private List<Message> errorMessages = new ArrayList<>();
	private List<Message> invalidMessages = new ArrayList<>();
	private final String reference;
	private Map<String, FieldDef> fieldMap;
	private MetaData metaData;
	private List<Linkpanel> panels = new ArrayList<>();
	private Pageable pageable;

	public void addLinkPanels(List<Linkpanel> panels) {
		this.panels.addAll(panels);
	}

	public List<Linkpanel> getLinkPanels() {
		return panels;
	}

	public FieldProcessorImpl(String reference) {
		this(reference, new MetaData());
	}

	public FieldProcessorImpl(String reference, MetaData metaData) {
		this.reference = reference;
		this.metaData = metaData;
		this.fieldMap = new HashMap<>();
		if (null != metaData) {
			for (FieldDef fieldDef : metaData.getFields()) {
				fieldMap.put(fieldDef.getBinding(), fieldDef);
			}
		}
	}

	public List<FieldDef> getFields() {
		if (null == metaData) {
			return Collections.emptyList();
		}
		return metaData.getFields();
	}

	public FieldDef getField(String fieldBinding) {
		return fieldMap.get(fieldBinding);
	}

	public Linkpanel getLinkPanel(String fieldName) {
		for (Linkpanel linkpanel : panels) {
			if (linkpanel.getId().equals(fieldName)) {
				return linkpanel;
			}
		}
		return null;
	}

	public boolean hasField(String fieldBinding) {
		return fieldMap.containsKey(fieldBinding);
	}

	public String getReference() {
		return reference;
	}

	public MetaData getMetaData() {
		return metaData;
	}

	public boolean hasErrors() {
		if (errorMessages.isEmpty()) {
			return hasFieldErrors();
		}
		return true;
	}

	public boolean hasFieldErrors() {
		return hasFieldErrors(getFields());
	}

	private boolean hasFieldErrors(List<FieldDef> fields) {
		for (FieldDef fieldDef : fields) {
			boolean errors = hasFieldErrors(fieldDef.getFields());
			if (errors) {
				return errors;
			}
			Messages messages = fieldDef.getMessages();
			if (messages != null) {
				for (Message m : messages.getMessageList()) {
					if (MessageType.ERROR.equals(m.getClazz())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void addOkMessage(String message) {
		okMessages.add(getOkMessage(reference, message));
	}

	public void addOkMessage(FieldDef field, String message) {
		addFieldMessage(field, getOkMessage(field.getName(), message));
	}

	public void addErrorMessage(String message) {
		errorMessages.add(getErrorMessage(reference, message));
	}

	public void addErrorMessage(FieldDef field, String message) {
		addFieldMessage(field, getErrorMessage(field.getName(), message));
	}

	public void addInvalidMessage(String message) {
		invalidMessages.add(getInvalidMessage(reference, message));
	}

	public void addInvalidMessage(FieldDef field, String message) {
		addFieldMessage(field, getInvalidMessage(field.getName(), message));
	}

	public void addNoticeMessage(String message) {
		noticeMessages.add(getNoticeMessage(reference, message));
	}

	public void addNoticeMessage(FieldDef field, String message) {
		addFieldMessage(field, getNoticeMessage(field.getName(), message));
	}

	private void addFieldMessage(FieldDef field, Message mssg) {
		Messages messages = field.getMessages();
		if (field.getMessages() == null) {
			messages = new Messages();
			messages.setRef(field.getName());
			field.setMessages(messages);
		}
		messages.getMessageList().add(mssg);
	}

	private Message getMessage(String ref, MessageType mssgClass, String content) {
		Message message = new MessageInternal();
		message.setRef(ref);
		message.setClazz(mssgClass);
		message.setContent(content);
		return message;
	}

	public Messages getMessages() {
		Messages messages = new Messages();
		messages.setRef(reference);
		addMessages(messages, noticeMessages);
		addMessages(messages, okMessages);
		addMessages(messages, errorMessages);
		addMessages(messages, invalidMessages);
		return messages;
	}

	private Message getNoticeMessage(String reference, String message) {
		return getMessage(reference, MessageType.NOTICE, message);
	}

	private Message getErrorMessage(String reference, String message) {
		return getMessage(reference, MessageType.ERROR, message);
	}

	private Message getInvalidMessage(String reference, String message) {
		return getMessage(reference, MessageType.INVALID, message);
	}

	private Message getOkMessage(String reference, String message) {
		return getMessage(reference, MessageType.OK, message);
	}

	private void addMessages(Messages message, Collection<? extends org.appng.xml.platform.Message> messageList) {
		message.getMessageList().addAll(messageList);
	}

	private class MessageInternal extends Message {
		@Override
		public String toString() {
			return clazz + " " + ref + " " + content;
		}
	}

	public void clearMessages() {
		noticeMessages.clear();
		okMessages.clear();
		errorMessages.clear();
		invalidMessages.clear();
	}

	public void clearFieldMessages() {
		for (FieldDef field : getFields()) {
			clearMessages(field);
		}
	}

	private void clearMessages(FieldDef field) {
		Messages messages = field.getMessages();
		if (null != messages) {
			messages.getMessageList().clear();
		}
	}

	public void clearFieldMessages(String... fieldBindings) {
		for (String binding : fieldBindings) {
			if (hasField(binding)) {
				clearMessages(getField(binding));
			}
		}
	}

	public void setPageable(Pageable pageable) {
		this.pageable = pageable;
		if (null != pageable && null != getMetaData()) {
			String binding = getMetaData().getBinding();
			int prio = 0;
			Sort sort = pageable.getSort();
			if (null != sort) {
				for (Order order : sort) {
					FieldDef field = null;
					if (null == binding) {
						field = getField(order.getProperty());
					} else {
						field = getField(binding + "." + order.getProperty());
					}
					if (null != field && null != field.getSort()) {
						field.getSort().setPrio(prio);
						if (order.isIgnoreCase()) {
							field.getSort().setIgnoreCase(true);
						}
						field.getSort().setOrder(SortOrder.valueOf(order.getDirection().name()));
						prio++;
					}
				}
			}
		}
	}

	public Pageable getPageable() {
		return null == pageable ? null : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), getSort());
	}

	private Sort getSort() {
		Sort sort = null;
		if (!Sort.unsorted().equals(pageable.getSort())) {
			List<Order> orders = new ArrayList<>();
			List<FieldDef> activeSortFields = new ArrayList<>();
			for (Order order : pageable.getSort()) {
				String property = order.getProperty();
				boolean ignoreCase = false;
				FieldDef field = getField(property);
				if (field != null) {
					activeSortFields.add(field);
					org.appng.xml.platform.Sort fieldSort = field.getSort();
					if (null != fieldSort) {
						ignoreCase = Boolean.TRUE.equals(fieldSort.isIgnoreCase());
						if (StringUtils.isNotBlank(fieldSort.getName())) {
							property = fieldSort.getName();
						}
					}
				}
				Order fieldOrder = new Order(order.getDirection(), property);
				if (ignoreCase) {
					fieldOrder = fieldOrder.ignoreCase();
				}
				orders.add(fieldOrder);
			}
			if (null != getMetaData()) {
				for (FieldDef field : getMetaData().getFields()) {
					if (field.getSort() != null && !activeSortFields.contains(field)) {
						field.setSort(new org.appng.xml.platform.Sort());
					}
				}
			}
			sort = Sort.by(orders);
		} else {
			sort = getDefaultSort();
		}
		return sort;
	}

	class SortComparator implements Comparator<FieldDef> {
		public int compare(FieldDef f1, FieldDef f2) {
			Integer prio1 = f1.getSort().getPrio();
			Integer prio2 = f2.getSort().getPrio();
			boolean hasPrio1 = prio1 != null;
			boolean hasPrio2 = prio2 != null;
			if (!hasPrio1 && !hasPrio2) {
				return 0;
			} else if (hasPrio1 && !hasPrio2) {
				return 1;
			} else if (hasPrio2 && !hasPrio1) {
				return -1;
			}
			return prio1.compareTo(prio2);
		}
	}

	private Sort getDefaultSort() {
		Sort sort = null;
		if (null != getMetaData()) {
			List<FieldDef> sortFields = new ArrayList<>();
			for (FieldDef field : getMetaData().getFields()) {
				if (null != field.getSort()) {
					sortFields.add(field);
				}
			}
			Collections.sort(sortFields, new SortComparator());
			List<Order> orders = new ArrayList<>();
			for (FieldDef field : sortFields) {
				org.appng.xml.platform.Sort fieldSort = field.getSort();
				if (isValidSort(fieldSort)) {
					String sortName = fieldSort.getName();
					String property = null == sortName ? field.getName() : sortName;
					Direction direction = Direction.valueOf(fieldSort.getOrder().name().toUpperCase());
					Order order = new Order(direction, property);
					if (Boolean.TRUE.equals(fieldSort.isIgnoreCase())) {
						order = order.ignoreCase();
					}
					orders.add(fieldSort.getPrio(), order);
				}
				field.setSort(new org.appng.xml.platform.Sort());
			}
			if (!orders.isEmpty()) {
				sort = Sort.by(orders);
			}
		}
		return sort;
	}

	private boolean isValidSort(org.appng.xml.platform.Sort fieldSort) {
		return null != fieldSort && null != fieldSort.getPrio() && null != fieldSort.getOrder();
	}

}
