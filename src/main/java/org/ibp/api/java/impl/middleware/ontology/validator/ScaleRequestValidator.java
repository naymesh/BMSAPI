package org.ibp.api.java.impl.middleware.ontology.validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.generationcp.middleware.domain.oms.CvId;
import org.generationcp.middleware.domain.oms.DataType;
import org.generationcp.middleware.domain.oms.Scale;
import org.generationcp.middleware.exceptions.MiddlewareException;
import org.ibp.api.CommonUtil;
import org.ibp.api.domain.ontology.VariableCategory;
import org.ibp.api.domain.ontology.ScaleRequest;
import org.ibp.api.domain.ontology.ValidValues;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import com.google.common.base.Strings;

/**
 * Add Scale/Update Scale Validation rules for Scale request Refer:
 * http://confluence.leafnode.io/display/CD/Services+Validation 1. Name is
 * required 2. The name must be unique 3. Data type is required 4. The data type
 * ID must correspond to the ID of one of the supported data types (Numeric,
 * Categorical, Character, DateTime, Person, Location or any other special data
 * type that we add) 5. If the data type is categorical, at least one category
 * must be submitted 6. Categories are only stored if the data type is
 * categorical 7. If there are categories, all labels and values within the set
 * of categories must be unique 8. The min and max valid values are only stored
 * if the data type is numeric 9. If the data type is numeric and minimum and
 * maximum valid values are provided (they are not mandatory), they must be
 * numeric values 10. If present, the minimum valid value must be less than or
 * equal to the maximum valid value, and the maximum valid value must be greater
 * than or equal to the minimum valid value 11. The name, data type and valid
 * values cannot be changed if the scale is already in use 12. Name is no more
 * than 200 characters 13. Description is no more than 255 characters
 */
@Component
public class ScaleRequestValidator extends OntologyValidator implements
org.springframework.validation.Validator {

	static final String CATEGORIES_NAME_DUPLICATE = "scale.categories.name.duplicate";
	static final String CATEGORIES_DESCRIPTION_DUPLICATE = "scale.categories.description.duplicate";

	@Override
	public boolean supports(Class<?> aClass) {
		return ScaleRequest.class.equals(aClass);
	}

	@Override
	public void validate(Object target, Errors errors) {

		this.shouldNotNullOrEmpty("request", target, errors);

		ScaleRequest request = (ScaleRequest) target;

		if (errors.hasErrors()) {
			return;
		}

		boolean nameValidationResult = nameValidationProcessor(request, errors);

		descriptionValidationProcessor(request, errors);

		boolean dataTypeValidationResult = dataTypeValidationProcessor(request, errors);

		if(dataTypeValidationResult){
			if (Objects.equals(request.getDataTypeId(), DataType.CATEGORICAL_VARIABLE.getId())) {
				categoricalDataTypeValidationProcessor(request, errors);
			}
			if (Objects.equals(request.getDataTypeId(), DataType.NUMERIC_VARIABLE.getId())) {
				numericDataTypeValidationProcessor(request, errors);
			}
		}

		if(nameValidationResult){
			scaleShouldBeEditable(request, errors);
		}
	}

	private void validateCategoriesForUniqueness(List<VariableCategory> categories,
			DataType dataType, Errors errors) {
		if (categories != null && Objects.equals(dataType, DataType.CATEGORICAL_VARIABLE)) {
			Set<String> labels = new HashSet<>();
			Set<String> values = new HashSet<>();
			for (int i = 1; i <= categories.size(); i++) {
				VariableCategory category = categories.get(i-1);
				String name = category.getName();
				String value = category.getDescription();

				if(Strings.isNullOrEmpty(value)){
					this.addCustomError(errors, "validValues.categories[" + i + "].description",
						  OntologyValidator.CATEGORY_DESCRIPTION_IS_NECESSARY, null);
				}

				if(Strings.isNullOrEmpty(name)){
					this.addCustomError(errors, "validValues.categories[" + i + "].name",
						  OntologyValidator.CATEGORY_NAME_IS_NECESSARY, null);
				}

				if (errors.hasErrors()) {
					return;
				}

				if (labels.contains(name)) {
					this.addCustomError(errors, "validValues.categories[" + i + "].name",
							ScaleRequestValidator.CATEGORIES_NAME_DUPLICATE, null);
				} else {
					labels.add(category.getName());
				}

				if (values.contains(value)) {
					this.addCustomError(errors, "validValues.categories[" + i + "].description",
							ScaleRequestValidator.CATEGORIES_DESCRIPTION_DUPLICATE, null);
				} else {
					values.add(category.getDescription());
				}
			}
		}
	}

	private void scaleShouldBeEditable(ScaleRequest request, Errors errors) {
		if (request.getId() == null) {
			return;
		}

		try {
			Scale oldScale = this.ontologyManagerService.getScaleById(CommonUtil.tryParseSafe(request.getId()));

			// that method should exist with requestId
			if (Objects.equals(oldScale, null)) {
				this.addCustomError(errors, OntologyValidator.TERM_DOES_NOT_EXIST, new Object[] {
						"scale", request.getId().toString() });
				return;
			}

			boolean isEditable = !this.ontologyManagerService.isTermReferred(CommonUtil.tryParseSafe(request.getId()));
			if (isEditable) {
				return;
			}

			boolean isNameSame = Objects.equals(request.getName(), oldScale.getName());
			if (!isNameSame) {
				this.addCustomError(errors, "name", OntologyValidator.TERM_NOT_EDITABLE,
						new Object[] { "scale", "name" });
			}

			boolean isDataTypeSame = Objects.equals(request.getDataTypeId(),
					this.getDataTypeIdSafe(oldScale.getDataType()));
			if (!isDataTypeSame) {
				this.addCustomError(errors, "dataTypeId", OntologyValidator.TERM_NOT_EDITABLE,
						new Object[] { "scale", "dataTypeId" });
			}

			ValidValues validValues = request.getValidValues() == null ? new ValidValues()
			: request.getValidValues();
			boolean minValuesAreEqual = Objects.equals(validValues.getMin(), oldScale.getMinValue());
			boolean maxValuesAreEqual = Objects.equals(validValues.getMax(), oldScale.getMaxValue());
			List<VariableCategory> categories = validValues.getCategories() == null ? new ArrayList<VariableCategory>()
					: validValues.getCategories();
			boolean categoriesEqualSize = Objects.equals(categories.size(), oldScale
					.getCategories().size());
			boolean categoriesValuesAreSame = true;
			if (categoriesEqualSize) {
				for (VariableCategory l : categories) {
					if (oldScale.getCategories().containsKey(l.getName())
							&& Objects.equals(oldScale.getCategories().get(l.getName()),
									l.getDescription())) {
						continue;
					}
					categoriesValuesAreSame = false;
					break;
				}
			}
			if (!minValuesAreEqual || !maxValuesAreEqual || !categoriesEqualSize
					|| !categoriesValuesAreSame) {
				this.addCustomError(errors, "validValues", OntologyValidator.TERM_NOT_EDITABLE,
						new Object[] { "scale", "validValues" });
			}

		} catch (MiddlewareException e) {
			this.log.error("Error while executing scaleShouldBeEditable", e);
			this.addDefaultError(errors);
		}
	}

	private Integer getDataTypeIdSafe(DataType dataType) {
		return dataType == null ? null : dataType.getId();
	}

	private boolean nameValidationProcessor(ScaleRequest request, Errors errors){

		Integer initialCount = errors.getErrorCount();

		// 1. Name is required
		this.shouldNotNullOrEmpty("name", request.getName(), errors);

		if (errors.getErrorCount() > initialCount) {
			return false;
		}

		// 12. Name is no more than 200 characters
		this.nameShouldHaveMax200Chars("name", request.getName(), errors);

		// 2. The name must be unique
		this.checkTermUniqueness(CommonUtil.tryParseSafe(request.getId()), request.getName(), CvId.SCALES.getId(), "scale", errors);

		return errors.getErrorCount() == initialCount;
	}

	private boolean descriptionValidationProcessor(ScaleRequest request, Errors errors){

		Integer initialCount = errors.getErrorCount();

		// 13. Description is no more than 255 characters
		this.descriptionShouldHaveMax255Chars("description", request.getDescription(), errors);

		return errors.getErrorCount() == initialCount;
	}

	private boolean dataTypeValidationProcessor(ScaleRequest request, Errors errors){

		Integer initialCount = errors.getErrorCount();

		// 3. Data type is required
		this.shouldNotNullOrEmpty("dataTypeId", request.getDataTypeId(), errors);

		if (errors.getErrorCount() > initialCount) {
			return false;
		}

		// 4. The data type ID must correspond to the ID of one of the supported
		// data types (Numeric, Categorical, Character, DateTime, Person,
		// Location or any other special data type that we add)
		this.shouldHaveValidDataType("dataTypeId", request.getDataTypeId(), errors);

		return errors.getErrorCount() == initialCount;
	}

	private boolean categoricalDataTypeValidationProcessor(ScaleRequest request, Errors errors){

		Integer initialCount = errors.getErrorCount();

		DataType dataType = DataType.getById(request.getDataTypeId());

		ValidValues validValues = request.getValidValues() == null ? new ValidValues() : request.getValidValues();

		List<VariableCategory> categories = validValues.getCategories();

		// 5. If the data type is categorical, at least one category must be
		// submitted
		if (Objects.equals(dataType, DataType.CATEGORICAL_VARIABLE)) {
			if (categories.isEmpty()) {
				this.addCustomError(errors, "validValues.categories", OntologyValidator.CATEGORY_SHOULD_HAVE_AT_LEAST_ONE_ITEM, null);
			}
		}

		if (errors.getErrorCount() > initialCount) {
			return false;
		}

		// 7. If there are categories, all labels and values within the set of
		// categories must be unique
		this.validateCategoriesForUniqueness(categories, dataType, errors);

		return errors.getErrorCount() == initialCount;
	}

	private boolean numericDataTypeValidationProcessor(ScaleRequest request, Errors errors){

		Integer initialCount = errors.getErrorCount();

		DataType dataType = DataType.getById(request.getDataTypeId());

		ValidValues validValues = request.getValidValues() == null ? new ValidValues() : request.getValidValues();

		String minValue = validValues.getMin();
		String maxValue = validValues.getMax();

		// 9. If the data type is numeric and minimum and maximum valid values
		// are provided (they are not mandatory), they must be numeric values
		if (Objects.equals(dataType, DataType.NUMERIC_VARIABLE)) {
			if (minValue != null && !this.isNonNullValidNumericString(minValue)) {
				this.addCustomError(errors, "validValues.min", OntologyValidator.VALUE_SHOULD_BE_NUMERIC, null);
			}

			if (maxValue != null && !this.isNonNullValidNumericString(maxValue)) {
				this.addCustomError(errors, "validValues.max", OntologyValidator.VALUE_SHOULD_BE_NUMERIC, null);
			}
		}

		if (errors.getErrorCount() > initialCount) {
			return false;
		}

		// 10. If present, the minimum valid value must be less than or equal to
		// the maximum valid value, and the maximum valid value must be greater
		// than or equal to the minimum valid value
		if (this.isNonNullValidNumericString(minValue) && this.isNonNullValidNumericString(maxValue) && this.getIntegerValueSafe(minValue, 0) > this.getIntegerValueSafe(maxValue, 0)) {
			this.addCustomError(errors, "validValues.min", OntologyValidator.MIN_MAX_NOT_VALID, null);
		}

		return errors.getErrorCount() == initialCount;
	}
}
