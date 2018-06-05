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