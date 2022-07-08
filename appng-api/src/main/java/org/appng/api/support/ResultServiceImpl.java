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
package org.appng.api.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.api.FieldProcessor;
import org.appng.api.FieldWrapper;
import org.appng.api.ResultService;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Default {@link ResultService} implementation
 * 
 * @author Matthias Müller
 */
public class ResultServiceImpl extends AdapterBase implements ResultService {

	public ResultServiceImpl(ExpressionEvaluator expressionEvaluator) {
		this.expressionEvaluator = expressionEvaluator;
	}

	public ResultServiceImpl() {

	}

	private <T> void addItem(Resultset resultset, T item, FieldProcessor fp) {
		Result result = getResult(fp, item);
		resultset.getResults().add(result);
	}

	public final Result getResult(final FieldProcessor fp, Object object) {
		expressionEvaluator.setVariable(CURRENT, object);
		final Result r = new Result();
		setResultSelector(fp, r);
		DatafieldOwner dataFieldOwner = getDataFieldOwner(r);
		for (FieldDef fieldDef : fp.getFields()) {
			Linkpanel linkpanel = fp.getLinkPanel(fieldDef.getName());
			FieldWrapper fieldWrapper = new FieldWrapper(fieldDef, new BeanWrapperImpl(object));
			fieldWrapper.backupFields();
			fieldWrapper.setLinkpanel(linkpanel);
			fieldConverter.addField(dataFieldOwner, fieldWrapper);
			fieldWrapper.restoreFields();
		}
		return r;
	}

	private void setResultSelector(final FieldProcessor fp, final Result r) {
		String resultSelector = fp.getMetaData().getResultSelector();
		if (null != resultSelector) {
			boolean selected = expressionEvaluator.evaluate(resultSelector);
			if (selected) {
				r.setSelected(true);
			}
		}
	}

	public Resultset getResultset(FieldProcessor fp, Collection<?> items) {
		int pageSize = (0 == items.size() ? 10 : items.size());
		return getResultset(fp,
				new PageImpl<Object>(new ArrayList<>(items), PageRequest.of(0, pageSize), items.size()));
	}

	public final Resultset getResultset(FieldProcessor fp, Page<?> page) {
		Resultset resultset = new Resultset();
		resultset.setChunk(page.getNumber());
		resultset.setChunkname(fp.getReference());
		resultset.setChunksize(page.getSize());

		resultset.setFirstchunk(0);
		resultset.setLastchunk(page.getTotalPages() - 1);
		resultset.setPreviouschunk(page.hasPrevious() ? page.getNumber() - 1 : 0);
		resultset.setNextchunk(page.hasNext() ? page.getNumber() + 1 : 0);
		resultset.setHits((int) page.getTotalElements());

		for (Object item : page.getContent()) {
			addItem(resultset, item, fp);
		}
		return resultset;
	}

	private DatafieldOwner getDataFieldOwner(final Result r) {
		return new DatafieldOwner() {
			public final List<Datafield> getFields() {
				return r.getFields();
			}

			public List<Linkpanel> getLinkpanels() {
				return r.getLinkpanel();
			}

		};
	}

}
