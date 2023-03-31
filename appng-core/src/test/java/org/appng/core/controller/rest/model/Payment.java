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
package org.appng.core.controller.rest.model;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import com.google.common.collect.Lists;

public class Payment {

	private String id;
	private Date validFrom;
	private Date validTo;
	private Double amount;
	private Integer increments;
	private Integer decrements;
	private String receiptNo;
	private String changeReason;
	private boolean editable;
	@Valid
	private List<PaymentItem> items = Lists.newArrayList();
	private List<Date> dueDates = Lists.newArrayList();
	private String dueDatesAsString;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public Integer getIncrements() {
		return increments;
	}

	public void setIncrements(Integer increments) {
		this.increments = increments;
	}

	public Integer getDecrements() {
		return decrements;
	}

	public void setDecrements(Integer decrements) {
		this.decrements = decrements;
	}

	public String getReceiptNo() {
		return receiptNo;
	}

	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	public String getChangeReason() {
		return changeReason;
	}

	public void setChangeReason(String changeReason) {
		this.changeReason = changeReason;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public List<PaymentItem> getItems() {
		return items;
	}

	public void setItems(List<PaymentItem> items) {
		this.items = items;
	}

	public List<Date> getDueDates() {
		return dueDates;
	}

	public void setDueDates(List<Date> dueDates) {
		this.dueDates = dueDates;
	}

	public String getDueDatesAsString() {
		return dueDatesAsString;
	}

	public void setDueDatesAsString(String dueDatesAsString) {
		this.dueDatesAsString = dueDatesAsString;
	}

}