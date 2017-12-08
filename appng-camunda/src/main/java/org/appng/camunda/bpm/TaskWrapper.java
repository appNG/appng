package org.appng.camunda.bpm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.NotNull;
import org.appng.xml.platform.Validation;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.form.FormFieldValidationConstraint;
import org.camunda.bpm.engine.form.FormType;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.impl.form.type.BooleanFormType;
import org.camunda.bpm.engine.impl.form.type.DateFormType;
import org.camunda.bpm.engine.impl.form.type.LongFormType;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.IdentityLinkType;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.VariableMap;

import lombok.Data;
import lombok.experimental.Delegate;

/**
 * A wrapper for a {@link Task}, providing the ability of dynamically adding {@link FieldDef}initions based on a the
 * {@link TaskFormData} that is <br/>
 * This class can easily be used as the bind-class of an appNG {@link Datasource}.
 * 
 * 
 * @author Matthias Müller
 *
 */
@Data
public class TaskWrapper implements Task {

	private static final String VC_REQUIRED = "required";
	private static final String VC_READONLY = "readonly";
	private @Delegate Task task;
	private String candidateUser;
	private String candidateGroup;
	private VariableMap variables;
	private Map<String, Object> formFields = new HashMap<>();

	public TaskWrapper() {
		this(new TaskEntity(), Collections.emptyList(), null);
	}

	/**
	 * Create a new {@link TaskWrapper} from the given {@link Task}
	 * 
	 * @param task
	 *            the {@link Task} to wrap, retrieved from a {@link TaskService}
	 * @param identityLinks
	 *            the {@link IdentityLink}s for the task, usually retrieved with
	 *            {@link TaskService#getIdentityLinksForTask(String)}
	 * @param variables
	 *            the variables for this {@link Task}, usually retrieved with
	 *            {@link TaskService#getVariablesTyped(String)}
	 */
	public TaskWrapper(Task task, Collection<IdentityLink> identityLinks, VariableMap variables) {
		this.task = task;
		this.variables = variables;
		for (IdentityLink il : identityLinks) {
			if (IdentityLinkType.CANDIDATE.equals(il.getType())) {
				if (StringUtils.isNotBlank(il.getGroupId())) {
					this.candidateGroup = il.getGroupId();
				} else if (StringUtils.isNotBlank(il.getUserId())) {
					this.candidateUser = il.getUserId();
				}
			}
		}
	}

	/**
	 * Dynamically adds {@link FieldDef}initions to the given {@link FieldProcessor}, depending on the
	 * {@link FormField}s defined at the {@link TaskFormData}.
	 * 
	 * @param fp
	 *            the {@link FieldProcessor}
	 * @param taskFormData
	 *            the {@link TaskFormData} to dynamically add the fields from
	 * @param mandatoryMessage
	 *            the message in case a field id mandatory
	 */
	public void addFormFields(FieldProcessor fp, TaskFormData taskFormData, String mandatoryMessage) {
		for (FormField formField : taskFormData.getFormFields()) {
			String fieldName = getFieldName(formField);
			if (!fp.hasField(fieldName)) {
				FieldDef field = new FieldDef();
				determineFieldType(formField.getType(), field);
				field.setName(fieldName);
				field.setBinding(field.getName());
				Label label = new Label();
				label.setValue(formField.getLabel());
				field.setLabel(label);

				List<FormFieldValidationConstraint> validationConstraints = formField.getValidationConstraints();
				if (validationConstraints.size() > 0) {
					field.setValidation(new Validation());
					for (FormFieldValidationConstraint validationConstraint : validationConstraints) {
						addValidation(field, validationConstraint, mandatoryMessage);
					}
				}
				fp.getMetaData().getFields().add(field);
				formFields.put(formField.getId(), formField.getValue().getValue());
			}
		}
	}

	protected void addValidation(FieldDef field, FormFieldValidationConstraint validationConstraint,
			String mandatoryMessage) {
		if (VC_REQUIRED.equals(validationConstraint.getName())) {
			NotNull notNull = new NotNull();
			Message mssg = new Message();
			mssg.setClazz(MessageType.ERROR);
			mssg.setRef(field.getBinding());
			mssg.setContent(mandatoryMessage);
			notNull.setMessage(mssg);
			field.getValidation().setNotNull(notNull);
		}
		if (VC_READONLY.equals(validationConstraint.getName())) {
			field.setReadonly("true");
		}
	}

	protected void determineFieldType(FormType type, FieldDef field) {
		if (type instanceof BooleanFormType) {
			field.setType(FieldType.CHECKBOX);
		} else if (type instanceof LongFormType) {
			field.setType(FieldType.LONG);
		} else if (type instanceof DateFormType) {
			field.setType(FieldType.DATE);
			Object datePattern = type.getInformation("datePattern");
			field.setFormat((String) datePattern);
		} else {
			field.setType(FieldType.TEXT);
		}
	}

	/**
	 * Validates this {@link TaskWrapper}. Therefore, the {@link FormField}s returned by the {@link TaskFormData} are
	 * checked for {@link FormFieldValidationConstraint}s. If there is a {@code required} constraint, and the value
	 * returned by {@link #getFormField(String)} is {@code null} or empty for the given {@link FormField}, an
	 * error-message as append to the {@link FieldDef}inition of that field.
	 * 
	 * @param site
	 *            the current {@link Site}
	 * @param application
	 *            the current {@link Application}
	 * @param environment
	 *            the current {@link Environment}
	 * @param options
	 *            the current {@link Options}
	 * @param request
	 *            the current {@link Request}
	 * @param fp
	 *            the current {@link FieldProcessor}
	 * @param taskFormData
	 *            the current {@link TaskFormData}
	 * @param requiredMessage
	 *            the current {@link Site}
	 */
	public void validate(Site site, Application application, Environment environment, Options options, Request request,
			FieldProcessor fp, TaskFormData taskFormData, String requiredMessage) {
		for (FormField formField : taskFormData.getFormFields()) {
			List<FormFieldValidationConstraint> validationConstraints = formField.getValidationConstraints();
			for (FormFieldValidationConstraint validationConstraint : validationConstraints) {
				if (VC_REQUIRED.equals(validationConstraint.getName())) {
					Object value = getFormField(formField.getId());
					if (null == value || (value instanceof String && StringUtils.isBlank((String) value))) {
						String fieldName = getFieldName(formField);
						fp.addErrorMessage(fp.getField(fieldName), requiredMessage);
					}
				}
			}
		}
	}

	private String getFieldName(FormField formField) {
		return String.format("formFields['%s']", formField.getId());
	}

	public Object getFormField(String name) {
		return formFields.get(name);
	}

}
