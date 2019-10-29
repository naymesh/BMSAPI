package org.ibp.api.java.impl.middleware.design.type;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.generationcp.middleware.domain.dms.ExperimentDesignType;
import org.generationcp.middleware.domain.dms.InsertionMannerItem;
import org.generationcp.middleware.domain.etl.MeasurementVariable;
import org.generationcp.middleware.domain.gms.SystemDefinedEntryType;
import org.generationcp.middleware.domain.oms.TermId;
import org.generationcp.middleware.service.api.study.StudyGermplasmDto;
import org.ibp.api.java.design.type.ExperimentDesignTypeService;
import org.ibp.api.java.impl.middleware.design.generator.ExperimentDesignGenerator;
import org.ibp.api.java.impl.middleware.design.util.ExperimentalDesignUtil;
import org.ibp.api.java.impl.middleware.design.validator.ExperimentDesignTypeValidator;
import org.ibp.api.rest.dataset.ObservationUnitData;
import org.ibp.api.rest.dataset.ObservationUnitRow;
import org.ibp.api.rest.design.ExperimentDesignInput;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class EntryListOrderDesignTypeServiceImpl implements ExperimentDesignTypeService {

	protected static final List<Integer> DESIGN_FACTOR_VARIABLES = Arrays.asList(TermId.PLOT_NO.getId());

	protected static final List<Integer> EXPERIMENT_DESIGN_VARIABLES =
		Arrays.asList(TermId.EXPERIMENT_DESIGN_FACTOR.getId());

	protected static final List<Integer> EXPERIMENT_DESIGN_VARIABLES_WITH_CHECK_PLAN =
		Arrays.asList(TermId.EXPERIMENT_DESIGN_FACTOR.getId(), TermId.CHECK_START.getId(), TermId.CHECK_INTERVAL.getId(),
			TermId.CHECK_PLAN.getId());

	@Resource
	private ExperimentDesignTypeValidator experimentDesignTypeValidator;

	@Resource
	private ExperimentDesignGenerator experimentDesignGenerator;

	@Override
	public List<ObservationUnitRow> generateDesign(final int studyId, final ExperimentDesignInput experimentDesignInput,
		final String programUUID, final List<StudyGermplasmDto> studyGermplasmDtoList) {

		this.experimentDesignTypeValidator.validateEntryListOrderDesign(experimentDesignInput, studyGermplasmDtoList);

		final List<StudyGermplasmDto> checkList = new LinkedList<>();

		final List<StudyGermplasmDto> testEntryList = new LinkedList<>();

		this.loadChecksAndTestEntries(studyGermplasmDtoList, checkList, testEntryList);

		final Integer startingPosition = experimentDesignInput.getCheckStartingPosition();

		final Integer spacing = experimentDesignInput.getCheckSpacing();

		final Integer insertionManner = experimentDesignInput.getCheckInsertionManner();

		final List<StudyGermplasmDto> mergedGermplasmList =
			this.mergeTestAndCheckEntries(testEntryList, checkList, startingPosition, spacing, insertionManner);

		final int numberOfTrials = experimentDesignInput.getNoOfEnvironments();

		final List<MeasurementVariable> measurementVariables = this.getMeasurementVariables(studyId, experimentDesignInput, programUUID);
		final List<ObservationUnitRow> observationUnitRows = new ArrayList<>();
		for (int instanceNumber = 1; instanceNumber <= numberOfTrials; instanceNumber++) {

			int plotNumber = experimentDesignInput.getStartingPlotNo();

			for (final StudyGermplasmDto germplasm : mergedGermplasmList) {
				final ObservationUnitRow observationUnitRow =
					this.createObservationUnitRow(instanceNumber, germplasm, plotNumber++, measurementVariables);
				observationUnitRows.add(observationUnitRow);
			}
		}
		return observationUnitRows;
	}

	@Override
	public Boolean requiresLicenseCheck() {
		return Boolean.FALSE;
	}

	@Override
	public Integer getDesignTypeId() {
		return ExperimentDesignType.ENTRY_LIST_ORDER.getId();
	}

	@Override
	public List<MeasurementVariable> getMeasurementVariables(final int studyId, final ExperimentDesignInput experimentDesignInput,
		final String programUUID) {
		return this.experimentDesignGenerator
			.constructMeasurementVariables(studyId, programUUID, DESIGN_FACTOR_VARIABLES,
				experimentDesignInput.getCheckSpacing() != null ? EXPERIMENT_DESIGN_VARIABLES_WITH_CHECK_PLAN :
					EXPERIMENT_DESIGN_VARIABLES, experimentDesignInput);
	}

	ObservationUnitRow createObservationUnitRow(final int instanceNumber, final StudyGermplasmDto germplasm,
		final int plotNumber, final List<MeasurementVariable> measurementVariables) {
		final ObservationUnitRow row = new ObservationUnitRow();
		row.setTrialInstance(instanceNumber);

		final Map<String, ObservationUnitData> observationUnitDataMap = new HashMap<>();
		ObservationUnitData observationUnitData;

		observationUnitData = new ObservationUnitData(TermId.TRIAL_INSTANCE_FACTOR.getId(), String.valueOf(instanceNumber));
		observationUnitDataMap.put(String.valueOf(observationUnitData.getVariableId()), observationUnitData);
		for (final MeasurementVariable var : measurementVariables) {
			final Integer termId = var.getTermId();
			if (termId == TermId.ENTRY_NO.getId()) {
				observationUnitData = new ObservationUnitData(termId, String.valueOf(germplasm.getEntryNumber()));
			} else if (termId == TermId.SOURCE.getId() || termId == TermId.GERMPLASM_SOURCE.getId()) {
				observationUnitData = new ObservationUnitData(termId, germplasm.getSeedSource() != null ? germplasm.getSeedSource() : StringUtils.EMPTY);
			} else if (termId == TermId.GROUPGID.getId()) {
				observationUnitData = new ObservationUnitData(termId,
					germplasm.getGroupId() != null ? germplasm.getGroupId().toString() : StringUtils.EMPTY);
			} else if (termId == TermId.STOCKID.getId()) {
				observationUnitData = new ObservationUnitData(termId, germplasm.getStockIds() != null ? germplasm.getStockIds() : StringUtils.EMPTY);
			} else if (termId == TermId.CROSS.getId()) {
				observationUnitData = new ObservationUnitData(termId, germplasm.getCross());
			} else if (termId == TermId.DESIG.getId()) {
				observationUnitData = new ObservationUnitData(termId, germplasm.getDesignation());
			} else if (termId == TermId.GID.getId()) {
				observationUnitData = new ObservationUnitData(termId, String.valueOf(germplasm.getGermplasmId()));
			} else if (termId == TermId.ENTRY_CODE.getId()) {
				observationUnitData = new ObservationUnitData(termId, germplasm.getEntryCode());
			} else if (termId == TermId.PLOT_NO.getId()) {
				observationUnitData = new ObservationUnitData(termId, Integer.toString(plotNumber));
			} else if (termId == TermId.ENTRY_TYPE.getId()) {
				observationUnitData = new ObservationUnitData(termId, Integer.toString(germplasm.getCheckType()));
			} else {
				// meaning non factor
				observationUnitData = new ObservationUnitData(termId, StringUtils.EMPTY);
			}
			observationUnitDataMap.put(String.valueOf(observationUnitData.getVariableId()), observationUnitData);
		}

		row.setVariables(observationUnitDataMap);
		row.setEnvironmentVariables(new HashMap<>());
		return row;
	}

	private void loadChecksAndTestEntries(final List<StudyGermplasmDto> studyGermplasmDtoList, final List<StudyGermplasmDto> checkList,
		final List<StudyGermplasmDto> testEntryList) {

		for (final StudyGermplasmDto studyGermplasmDto : studyGermplasmDtoList) {
			if (studyGermplasmDto.getCheckType().equals(SystemDefinedEntryType.TEST_ENTRY.getEntryTypeCategoricalId())) {
				testEntryList.add(studyGermplasmDto);
			} else {
				checkList.add(studyGermplasmDto);
			}
		}
	}

	private boolean isThereSomethingToMerge(final List<StudyGermplasmDto> entriesList, final List<StudyGermplasmDto> checkList,
		final Integer startEntry, final Integer interval) {
		Boolean isThereSomethingToMerge = Boolean.TRUE;
		if (checkList == null || checkList.isEmpty()) {
			isThereSomethingToMerge = Boolean.FALSE;
		} else if (entriesList == null || entriesList.isEmpty()) {
			isThereSomethingToMerge = Boolean.FALSE;
		} else if (startEntry < 1 || startEntry > entriesList.size() || interval < 1) {
			isThereSomethingToMerge = Boolean.FALSE;
		}
		return isThereSomethingToMerge;
	}

	private List<StudyGermplasmDto> generateChecksToInsert(final List<StudyGermplasmDto> checkList, final Integer checkIndex,
		final Integer insertionManner) {
		final List<StudyGermplasmDto> newList = new ArrayList<>();
		if (insertionManner.equals(InsertionMannerItem.INSERT_ALL_CHECKS.getId())) {
			for (final StudyGermplasmDto checkGermplasm : checkList) {
				newList.add(SerializationUtils.clone(checkGermplasm));
			}
		} else {
			final int checkListIndex = checkIndex % checkList.size();
			final StudyGermplasmDto checkGermplasm = checkList.get(checkListIndex);
			newList.add(SerializationUtils.clone(checkGermplasm));
		}
		return newList;
	}

	private List<StudyGermplasmDto> mergeTestAndCheckEntries(final List<StudyGermplasmDto> testEntryList,
		final List<StudyGermplasmDto> checkList, final Integer startingIndex, final Integer spacing, final Integer insertionManner) {

		if (!this.isThereSomethingToMerge(testEntryList, checkList, startingIndex, spacing)) {
			return testEntryList;
		}

		final List<StudyGermplasmDto> newList = new ArrayList<>();

		int primaryEntry = 1;
		boolean isStarted = Boolean.FALSE;
		boolean shouldInsert = Boolean.FALSE;
		int checkIndex = 0;
		int intervalEntry = 0;
		for (final StudyGermplasmDto primaryGermplasm : testEntryList) {
			if (primaryEntry == startingIndex || intervalEntry == spacing) {
				isStarted = Boolean.TRUE;
				shouldInsert = Boolean.TRUE;
				intervalEntry = 0;
			}

			if (isStarted) {
				intervalEntry++;
			}

			if (shouldInsert) {
				shouldInsert = Boolean.FALSE;
				final List<StudyGermplasmDto> checks = this.generateChecksToInsert(checkList, checkIndex, insertionManner);
				checkIndex++;
				newList.addAll(checks);
			}
			final StudyGermplasmDto primaryNewGermplasm = SerializationUtils.clone(primaryGermplasm);
			primaryNewGermplasm.setCheckType(SystemDefinedEntryType.TEST_ENTRY.getEntryTypeCategoricalId());

			newList.add(primaryNewGermplasm);

			primaryEntry++;
		}

		return newList;
	}

}