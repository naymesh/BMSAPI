
package org.ibp.api.java.impl.middleware.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.generationcp.middleware.domain.oms.CvId;
import org.generationcp.middleware.domain.ontology.DataType;
import org.generationcp.middleware.domain.oms.OntologyVariableInfo;
import org.generationcp.middleware.domain.ontology.OntologyVariableSummary;
import org.generationcp.middleware.domain.ontology.VariableType;
import org.generationcp.middleware.domain.ontology.Scale;
import org.generationcp.middleware.domain.ontology.Variable;
import org.generationcp.middleware.exceptions.MiddlewareException;
import org.generationcp.middleware.manager.ontology.api.OntologyScaleDataManager;
import org.generationcp.middleware.manager.ontology.api.OntologyVariableDataManager;
import org.generationcp.middleware.util.StringUtil;
import org.ibp.api.domain.common.GenericResponse;
import org.ibp.api.domain.ontology.VariableDetails;
import org.ibp.api.domain.ontology.VariableSummary;
import org.ibp.api.domain.program.ProgramSummary;
import org.ibp.api.exception.ApiRequestValidationException;
import org.ibp.api.exception.ApiRuntimeException;
import org.ibp.api.java.impl.middleware.ServiceBaseImpl;
import org.ibp.api.java.impl.middleware.common.validator.ProgramValidator;
import org.ibp.api.java.impl.middleware.ontology.validator.VariableValidator;
import org.ibp.api.java.ontology.VariableService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import com.google.common.base.Strings;

/**
 * Validate data of API Services and pass data to middleware services
 */

@Service
public class VariableServiceImpl extends ServiceBaseImpl implements VariableService {

	@Autowired
	private OntologyVariableDataManager ontologyVariableDataManager;

	@Autowired
	private VariableValidator variableValidator;

	@Autowired
	private ProgramValidator programValidator;

	@Autowired
	private OntologyScaleDataManager ontologyScaleDataManager;

	@Override
	public List<VariableSummary> getAllVariablesByFilter(String cropName, String programId, String propertyId, Boolean favourite) {

		BindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(), "Variable");

		ProgramSummary program = new ProgramSummary();
		program.setCropType(cropName);
		program.setUniqueID(programId);

		this.programValidator.validate(program, bindingResult);

		if (bindingResult.hasErrors()) {
			throw new ApiRequestValidationException(bindingResult.getAllErrors());
		}

		if (!Strings.isNullOrEmpty(propertyId)) {
			this.validateId(propertyId, "Variable");
		}

		try {
			List<OntologyVariableSummary> variableSummaries =
					this.ontologyVariableDataManager.getWithFilter(programId, favourite, null, StringUtil.parseInt(propertyId, null), null);
			List<VariableSummary> variableSummaryList = new ArrayList<>();

			ModelMapper mapper = OntologyMapper.getInstance();

			for (OntologyVariableSummary variable : variableSummaries) {
				VariableSummary variableSummary = mapper.map(variable, VariableSummary.class);
				variableSummaryList.add(variableSummary);
			}
			return variableSummaryList;
		} catch (MiddlewareException e) {
			throw new ApiRuntimeException("Error!", e);
		}
	}

	@Override
	public VariableDetails getVariableById(String cropName, String programId, String variableId) {

		this.validateId(variableId, "Variable");
		BindingResult errors = new MapBindingResult(new HashMap<String, String>(), "Variable");

		ProgramSummary program = new ProgramSummary();
		program.setCropType(cropName);
		program.setUniqueID(programId);

		this.programValidator.validate(program, errors);

		if (errors.hasErrors()) {
			throw new ApiRequestValidationException(errors.getAllErrors());
		}

		TermRequest term = new TermRequest(variableId, "variable", CvId.VARIABLES.getId());
		this.termValidator.validate(term, errors);

		if (errors.hasErrors()) {
			throw new ApiRequestValidationException(errors.getAllErrors());
		}

		try {
			Integer id = StringUtil.parseInt(variableId, null);

			Variable ontologyVariable = this.ontologyVariableDataManager.getVariable(programId, id);

			if (ontologyVariable == null) {
				return null;
			}

			boolean deletable = true;
			if (this.termDataManager.isTermReferred(id)) {
				deletable = false;
			}

			ModelMapper mapper = OntologyMapper.getInstance();
			VariableDetails response = mapper.map(ontologyVariable, VariableDetails.class);

			if (!deletable) {
				response.getMetadata().addEditableField("description");
			} else {
				response.getMetadata().addEditableField("name");
				response.getMetadata().addEditableField("description");
				response.getMetadata().addEditableField("alias");
				response.getMetadata().addEditableField("cropOntologyId");
				response.getMetadata().addEditableField("variableTypeIds");
				response.getMetadata().addEditableField("propertySummary");
				response.getMetadata().addEditableField("methodSummary");
				response.getMetadata().addEditableField("scale");
				response.getMetadata().addEditableField("expectedRange");
			}
			response.getMetadata().setDeletable(deletable);
			return response;
		} catch (MiddlewareException e) {
			throw new ApiRuntimeException("Error!", e);
		}
	}

	@Override
	public GenericResponse addVariable(String cropName, String programId, VariableSummary variable) {

		variable.setId(null);
		variable.setProgramUuid(programId);

		ProgramSummary program = new ProgramSummary();
		program.setCropType(cropName);
		program.setUniqueID(programId);

		try {

			BindingResult errors = new MapBindingResult(new HashMap<String, String>(), "Variable");
			this.programValidator.validate(program, errors);
			if (errors.hasErrors()) {
				throw new ApiRequestValidationException(errors.getAllErrors());
			}

			this.variableValidator.validate(variable, errors);
			if (errors.hasErrors()) {
				throw new ApiRequestValidationException(errors.getAllErrors());
			}

			formatVariableSummary(variable);

			Integer methodId = StringUtil.parseInt(variable.getMethodSummary().getId(), null);
			Integer propertyId = StringUtil.parseInt(variable.getPropertySummary().getId(), null);
			Integer scaleId = StringUtil.parseInt(variable.getScaleSummary().getId(), null);

			OntologyVariableInfo variableInfo = new OntologyVariableInfo();
			variableInfo.setName(variable.getName());
			variableInfo.setDescription(variable.getDescription());
			variableInfo.setMethodId(methodId);
			variableInfo.setPropertyId(propertyId);
			variableInfo.setScaleId(scaleId);
			variableInfo.setProgramUuid(variable.getProgramUuid());

			if (!Strings.isNullOrEmpty(variable.getExpectedRange().getMin())) {
				variableInfo.setMinValue(variable.getExpectedRange().getMin());
			}

			if (!Strings.isNullOrEmpty(variable.getExpectedRange().getMax())) {
				variableInfo.setMaxValue(variable.getExpectedRange().getMax());
			}

			for (org.ibp.api.domain.ontology.VariableType variableType : variable.getVariableTypes()) {
				variableInfo.addVariableType(VariableType.getById(variableType.getId()));
			}

			this.ontologyVariableDataManager.addVariable(variableInfo);
			return new GenericResponse(String.valueOf(variableInfo.getId()));
		} catch (MiddlewareException e) {
			throw new ApiRuntimeException("Error!", e);
		}
	}

	@Override
	public void updateVariable(String cropName, String programId, String variableId, VariableSummary variable) {

		variable.setId(variableId);
		variable.setProgramUuid(programId);

		ProgramSummary program = new ProgramSummary();
		program.setCropType(cropName);
		program.setUniqueID(programId);

		try {

			BindingResult errors = new MapBindingResult(new HashMap<String, String>(), "Variable");

			this.programValidator.validate(program, errors);

			if (errors.hasErrors()) {
				throw new ApiRequestValidationException(errors.getAllErrors());
			}

			this.validateId(variableId, "Variable");
			TermRequest term = new TermRequest(variableId, "variable", CvId.VARIABLES.getId());
			this.termValidator.validate(term, errors);

			if (errors.hasErrors()) {
				throw new ApiRequestValidationException(errors.getAllErrors());
			}

			this.variableValidator.validate(variable, errors);
			if (errors.hasErrors()) {
				throw new ApiRequestValidationException(errors.getAllErrors());
			}

			formatVariableSummary(variable);

			Integer id = StringUtil.parseInt(variable.getId(), null);

			Integer methodId = StringUtil.parseInt(variable.getMethodSummary().getId(), null);
			Integer propertyId = StringUtil.parseInt(variable.getPropertySummary().getId(), null);
			Integer scaleId = StringUtil.parseInt(variable.getScaleSummary().getId(), null);

			OntologyVariableInfo variableInfo = new OntologyVariableInfo();
			variableInfo.setId(id);
			variableInfo.setProgramUuid(variable.getProgramUuid());
			variableInfo.setName(variable.getName());
			variableInfo.setAlias(variable.getAlias());
			variableInfo.setDescription(variable.getDescription());
			variableInfo.setMethodId(methodId);
			variableInfo.setPropertyId(propertyId);
			variableInfo.setScaleId(scaleId);
			variableInfo.setIsFavorite(variable.isFavourite());
			variableInfo.setProgramUuid(variable.getProgramUuid());

			if (!Strings.isNullOrEmpty(variable.getExpectedRange().getMin())) {
				variableInfo.setMinValue(variable.getExpectedRange().getMin());
			}

			if (!Strings.isNullOrEmpty(variable.getExpectedRange().getMax())) {
				variableInfo.setMaxValue(variable.getExpectedRange().getMax());
			}

			for (org.ibp.api.domain.ontology.VariableType variableType : variable.getVariableTypes()) {
				variableInfo.addVariableType(VariableType.getById(variableType.getId()));
			}

			this.ontologyVariableDataManager.updateVariable(variableInfo);
		} catch (MiddlewareException e) {
			throw new ApiRuntimeException("Error!", e);
		}
	}

	@Override
	public void deleteVariable(String id) {

		// Note: Validate Id for valid format and check if variable exists or not
		this.validateId(id, "Variable");
		BindingResult errors = new MapBindingResult(new HashMap<String, String>(), "Variable");

		// Note: Check if variable is deletable or not by checking its usage in variable
		this.termDeletableValidator.validate(new TermRequest(String.valueOf(id), "Variable", CvId.VARIABLES.getId()), errors);
		if (errors.hasErrors()) {
			throw new ApiRequestValidationException(errors.getAllErrors());
		}

		try {
			this.ontologyVariableDataManager.deleteVariable(StringUtil.parseInt(id, null));
		} catch (MiddlewareException e) {
			throw new ApiRuntimeException("Error!", e);
		}
	}

	protected void formatVariableSummary(VariableSummary variableSummary){

		Integer scaleId = StringUtil.parseInt(variableSummary.getScaleSummary().getId(), null);

		//Should discard unwanted parameters. We do not want expected min/max values if associated data type is not numeric
		if(scaleId != null){
			try {
				Scale scale = ontologyScaleDataManager.getScaleById(scaleId);

				if(scale != null && !Objects.equals(scale.getDataType().getId(), DataType.NUMERIC_VARIABLE.getId())){
					variableSummary.setExpectedMin(null);
					variableSummary.setExpectedMax(null);
				}

			} catch (MiddlewareException e) {
				throw new ApiRuntimeException("Error!", e);
			}
		}
	}

}
