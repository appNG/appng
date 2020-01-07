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
package org.appng.core.controller.rest.model;

public class PaymentItem {

	private String divisionName;
	private Double amount;

	private Double amountNew;
	private String contractId;

	private Double absoluteMax;
	private Double absoluteMin;
	private Double amountMax;
	private Double amountMin;
	private Double percentMax;
	private Double percentMin;
	private Integer numberMax;
	private Integer numberMin;

	public String getDivisionName() {
		return divisionName;
	}

	public void setDivisionName(String divisionName) {
		this.divisionName = divisionName;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public Double getAmountNew() {
		return amountNew;
	}

	public void setAmountNew(Double amountNew) {
		this.amountNew = amountNew;
	}

	public String getContractId() {
		return contractId;
	}

	public void setContractId(String contractId) {
		this.contractId = contractId;
	}

	public Double getAbsoluteMax() {
		return absoluteMax;
	}

	public void setAbsoluteMax(Double absoluteMax) {
		this.absoluteMax = absoluteMax;
	}

	public Double getAbsoluteMin() {
		return absoluteMin;
	}

	public void setAbsoluteMin(Double absoluteMin) {
		this.absoluteMin = absoluteMin;
	}

	public Double getAmountMax() {
		return amountMax;
	}

	public void setAmountMax(Double amountMax) {
		this.amountMax = amountMax;
	}

	public Double getAmountMin() {
		return amountMin;
	}

	public void setAmountMin(Double amountMin) {
		this.amountMin = amountMin;
	}

	public Double getPercentMax() {
		return percentMax;
	}

	public void setPercentMax(Double percentMax) {
		this.percentMax = percentMax;
	}

	public Double getPercentMin() {
		return percentMin;
	}

	public void setPercentMin(Double percentMin) {
		this.percentMin = percentMin;
	}

	public Integer getNumberMax() {
		return numberMax;
	}

	public void setNumberMax(Integer numberMax) {
		this.numberMax = numberMax;
	}

	public Integer getNumberMin() {
		return numberMin;
	}

	public void setNumberMin(Integer numberMin) {
		this.numberMin = numberMin;
	}

}
