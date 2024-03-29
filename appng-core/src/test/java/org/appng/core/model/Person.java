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
package org.appng.core.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;

import org.appng.api.model.Named;
import org.appng.forms.FormUpload;
import org.appng.tools.locator.Coordinate;

public class Person implements Named<Integer> {

	private Integer id;

	private String firstname;

	private String name;

	private Date birthDate;

	private Float size;

	private Double savings;

	private Long age;

	private Boolean rightHanded;

	private Person mother;

	private Person father;

	private String description;

	private List<Integer> integerList = new ArrayList<>();

	private List<String> strings = new ArrayList<>();

	private List<Person> offsprings = new ArrayList<>();

	private FormUpload picture;

	private List<FormUpload> morePictures = new ArrayList<>();

	private Date version;

	private Coordinate coordinate = new Coordinate();

	public static enum Force {
		BRIGHTSIDE, DARKSIDE;
	}

	public Person(Integer id, String firstname, String name) {
		this.id = id;
		this.firstname = firstname;
		this.name = name;
	}

	public Person() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@NotNull
	@Size(min = 1, max = 5)
	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	@NotNull
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Past
	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public Float getSize() {
		return size;
	}

	public void setSize(Float size) {
		this.size = size;
	}

	public Boolean getRightHanded() {
		return rightHanded;
	}

	public void setRightHanded(Boolean rightHanded) {
		this.rightHanded = rightHanded;
	}

	public Person getMother() {
		return mother;
	}

	public void setMother(Person mother) {
		this.mother = mother;
	}

	public Person getFather() {
		return father;
	}

	public void setFather(Person father) {
		this.father = father;
	}

	public Double getSavings() {
		return savings;
	}

	public void setSavings(Double savings) {
		this.savings = savings;
	}

	@Min(value = 0)
	@Max(value = 100)
	public Long getAge() {
		return age;
	}

	public void setAge(Long age) {
		this.age = age;
	}

	public List<Integer> getIntegerList() {
		return integerList;
	}

	public void setIntegerList(List<Integer> integerList) {
		this.integerList = integerList;
	}

	public List<Person> getOffsprings() {
		return offsprings;
	}

	public void setOffsprings(List<Person> offsprings) {
		this.offsprings = offsprings;
	}

	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public FormUpload getPicture() {
		return picture;
	}

	public void setPicture(FormUpload picture) {
		this.picture = picture;
	}

	public List<FormUpload> getMorePictures() {
		return morePictures;
	}

	public void setMorePictures(List<FormUpload> morePictures) {
		this.morePictures = morePictures;
	}

	public List<String> getStrings() {
		return strings;
	}

	public void setStrings(List<String> strings) {
		this.strings = strings;
	}

	@Override
	public String toString() {
		return "#" + getId() + " " + getFirstname() + " " + getName();
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

}
