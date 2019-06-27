package org.ibp.api.java.impl.middleware.derived;

import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;
import org.generationcp.middleware.domain.etl.MeasurementVariable;
import org.generationcp.middleware.domain.ontology.FormulaDto;
import org.generationcp.middleware.domain.ontology.FormulaVariable;
import org.generationcp.middleware.domain.ontology.VariableType;
import org.generationcp.middleware.service.api.derived_variables.DerivedVariableService;
import org.generationcp.middleware.service.api.derived_variables.FormulaService;
import org.ibp.api.exception.ApiRequestValidationException;
import org.ibp.api.java.dataset.DatasetService;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DerivedVariableValidator {

	public static final String STUDY_EXECUTE_CALCULATION_INVALID_REQUEST = "study.execute.calculation.invalid.request";
	public static final String STUDY_EXECUTE_CALCULATION_FORMULA_NOT_FOUND = "study.execute.calculation.formula.not.found";
	public static final String STUDY_EXECUTE_CALCULATION_MISSING_VARIABLES = "study.execute.calculation.missing.variables";
	@Resource
	private FormulaService formulaService;

	@Resource
	private DatasetService datasetService;

	@Resource
	private DerivedVariableService middlewareDerivedVariableService;

	public void validate(final Integer variableId, final List<Integer> geoLocationIds) {

		final BindingResult errors = new MapBindingResult(new HashMap<String, String>(), Integer.class.getName());

		if (geoLocationIds == null || variableId == null) {
			errors.reject(STUDY_EXECUTE_CALCULATION_INVALID_REQUEST);
			throw new ApiRequestValidationException(errors.getAllErrors());
		}

		final Optional<FormulaDto> formulaOptional = this.formulaService.getByTargetId(variableId);
		if (!formulaOptional.isPresent()) {
			errors.reject(STUDY_EXECUTE_CALCULATION_FORMULA_NOT_FOUND);
			throw new ApiRequestValidationException(errors.getAllErrors());
		}

	}

	public void verifyInputVariablesArePresentInStudy(final Integer variableId, final Integer studyId) {

		final BindingResult errors = new MapBindingResult(new HashMap<String, String>(), Integer.class.getName());

		final Optional<FormulaDto> formulaOptional = this.formulaService.getByTargetId(variableId);
		if (formulaOptional.isPresent()) {

			// Verify that Environment Detail, Study Condition and Trait variables are present in study
			final Set<Integer> variableIds =
				this.middlewareDerivedVariableService.createVariableIdMeasurementVariableMap(studyId).keySet();
			final Set<String> inputMissingVariables = new HashSet<>();
			for (final FormulaVariable formulaVariable : formulaOptional.get().getInputs()) {
				if (!variableIds.contains(formulaVariable.getId())) {
					inputMissingVariables.add(formulaVariable.getName());
				}
			}
			if (!inputMissingVariables.isEmpty()) {
				errors.reject(
					STUDY_EXECUTE_CALCULATION_MISSING_VARIABLES,
					new String[] {StringUtils.join(inputMissingVariables.toArray(), ", ")}, "");
				throw new ApiRequestValidationException(errors.getAllErrors());
			}

		}

	}

}
