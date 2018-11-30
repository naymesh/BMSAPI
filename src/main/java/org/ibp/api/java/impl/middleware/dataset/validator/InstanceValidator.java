package org.ibp.api.java.impl.middleware.dataset.validator;

import org.generationcp.middleware.manager.api.StudyDataManager;
import org.ibp.api.exception.ApiRequestValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.Set;

@Component
public class InstanceValidator {

	@Autowired
	private StudyDataManager studyDataManager;

	private BindingResult errors;

	public void validate(final Integer datasetId, final Set<Integer> instanceIds) {

		errors = new MapBindingResult(new HashMap<String, String>(), Integer.class.getName());

		if (!studyDataManager.areAllInstancesExistInDataset(datasetId, instanceIds)) {
			errors.reject("dataset.invalid.instances", "");
			throw new ApiRequestValidationException(this.errors.getAllErrors());
		}
	}

}
