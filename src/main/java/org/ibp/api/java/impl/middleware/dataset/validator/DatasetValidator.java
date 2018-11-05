
package org.ibp.api.java.impl.middleware.dataset.validator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.generationcp.middleware.domain.dms.DMSVariableType;
import org.generationcp.middleware.domain.dms.DataSet;
import org.generationcp.middleware.domain.dms.DataSetType;
import org.generationcp.middleware.domain.dms.StandardVariable;
import org.generationcp.middleware.domain.dms.VariableTypeList;
import org.generationcp.middleware.domain.ontology.VariableType;
import org.generationcp.middleware.manager.api.OntologyDataManager;
import org.generationcp.middleware.manager.api.StudyDataManager;
import org.ibp.api.domain.dataset.DatasetVariable;
import org.ibp.api.exception.ApiRequestValidationException;
import org.ibp.api.exception.NotSupportedException;
import org.ibp.api.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

@Component
public class DatasetValidator {
	
	private static final List<VariableType> VALID_VARIABLE_TYPES = 
			Arrays.asList(VariableType.TRAIT, VariableType.SELECTION_METHOD);

	@Autowired
	private StudyDataManager studyDataManager;

	@Autowired
	private OntologyDataManager ontologyDataManager;

	private BindingResult errors;

	public void validateDataset(final Integer studyId, final Integer datasetId, final Boolean shouldBeSubobservationDataset) {

		final DataSet dataSet = this.studyDataManager.getDataSet(datasetId);
		this.validateDataset(dataSet, shouldBeSubobservationDataset);
	}

	private void validateDataset(final DataSet dataSet, final Boolean shouldBeSubobservationDataset) {
		errors = new MapBindingResult(new HashMap<String, String>(), Integer.class.getName());
		
		if (dataSet == null) {
			this.errors.reject("dataset.does.not.exist", "");
			throw new ResourceNotFoundException(this.errors.getAllErrors().get(0));
		}

		// TODO add validation here that dataset is valid dataset of study

		if (shouldBeSubobservationDataset && !DataSetType.isSubObservationDatasetType(dataSet.getDataSetType())) {
			this.errors.reject("dataset.type.not.subobservation", "");
			throw new NotSupportedException(this.errors.getAllErrors().get(0));
		}
	}

	public StandardVariable validateDatasetVariable(final Integer studyId, final Integer datasetId,
			final Boolean shouldBeSubobservationDataset, final DatasetVariable datasetVariable, final Boolean shouldAlreadyBeDatasetVariable) {
		errors = new MapBindingResult(new HashMap<String, String>(), Integer.class.getName());
		
		final DataSet dataSet = this.studyDataManager.getDataSet(datasetId);
		this.validateDataset(dataSet, shouldBeSubobservationDataset);

		// Validate if variable exists and of supported variable type
		final VariableType variableType = this.validateVariableType(datasetVariable.getVariableTypeId());
		final Integer variableId = datasetVariable.getVariableId();
		final StandardVariable standardVariable = this.ontologyDataManager.getStandardVariable(variableId, dataSet.getProgramUUID());
		this.validateVariable(standardVariable, variableType, variableId);

		this.validateIfAlreadyDatasetVariable(variableId, shouldAlreadyBeDatasetVariable, dataSet);

		return standardVariable;
	}
	
	public void validateExistingDatasetVariables(final Integer studyId, final Integer datasetId,
			final Boolean shouldBeSubobservationDataset, final List<Integer> variableIds) {
		errors = new MapBindingResult(new HashMap<String, String>(), Integer.class.getName());
		
		final DataSet dataSet = this.studyDataManager.getDataSet(datasetId);
		this.validateDataset(dataSet, shouldBeSubobservationDataset);

		for (final Integer variableId : variableIds) {			
			final StandardVariable standardVariable = this.ontologyDataManager.getStandardVariable(variableId, dataSet.getProgramUUID());
			this.validateVariableExists(standardVariable, variableId);
			this.validateIfAlreadyDatasetVariable(variableId, true, dataSet);
		}

	}

	void validateIfAlreadyDatasetVariable(final Integer variableId, final Boolean shouldAlreadyBeDatasetVariable,
			final DataSet dataSet) {
		final VariableTypeList variableList = dataSet.getVariableTypes();
		final VariableTypeList variates = variableList.getVariates();
		if (variates == null && shouldAlreadyBeDatasetVariable) {
			this.errors.reject("variable.not.dataset.variable", new Integer[] {variableId}, "");
			throw new ApiRequestValidationException(this.errors.getAllErrors());
		}

		boolean isDatasetVariable = false;
		for (final DMSVariableType datasetVariable : variates.getVariableTypes()) {
			if (variableId.equals(datasetVariable.getId())) {
				isDatasetVariable = true;
				final VariableType variableType = datasetVariable.getVariableType();
				if (!shouldAlreadyBeDatasetVariable) {
					this.errors.reject("variable.already.dataset.variable", new Object[] {String.valueOf(variableId)}, "");
					throw new ApiRequestValidationException(this.errors.getAllErrors());
				} else if (!VALID_VARIABLE_TYPES.contains(variableType)) {
					this.errors.reject("dataset.variable.cannot.be.deleted", new Object[] {String.valueOf(variableId), variableType.getName()}, "");
					throw new NotSupportedException(this.errors.getAllErrors().get(0));
				}
			}
		}

		if (!isDatasetVariable && shouldAlreadyBeDatasetVariable) {
			this.errors.reject("variable.not.dataset.variable", new Object[] {String.valueOf(variableId)}, "");
			throw new ApiRequestValidationException(this.errors.getAllErrors());
		}
	}

	VariableType validateVariableType(final Integer variableTypeId) {
		final VariableType variableType = VariableType.getById(variableTypeId);
		if (variableType == null) {
			this.errors.reject("variable.type.does.not.exist", "");
			throw new ResourceNotFoundException(this.errors.getAllErrors().get(0));
		}
		
		if (!VALID_VARIABLE_TYPES.contains(variableType)) {
			this.errors.reject("variable.type.not.supported", "");
			throw new NotSupportedException(this.errors.getAllErrors().get(0));
		}
		
		return variableType;
		
	}
	
	void validateVariable(final StandardVariable variable, final VariableType variableType, final Integer variableId) {
		validateVariableExists(variable, variableId);
		
		// Check if variable is configured to be given variable type
		if (!variable.getVariableTypes().contains(variableType)) {
			this.errors.reject("variable.not.of.given.variable.type", "");
			throw new ApiRequestValidationException(this.errors.getAllErrors());
		}
	}

	// TODO remove this, unreachable code as MiddlewareQueryException is thrown for invalid variable ID
	private void validateVariableExists(final StandardVariable variable, final Integer variableId) {
		if (variable == null) {
			this.errors.reject("variable.does.not.exist", new Integer[] {variableId}, "");
			throw new ResourceNotFoundException(this.errors.getAllErrors().get(0));
		}
	}
	

}