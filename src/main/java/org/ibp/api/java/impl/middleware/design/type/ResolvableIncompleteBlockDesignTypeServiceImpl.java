package org.ibp.api.java.impl.middleware.design.type;

import org.apache.commons.lang3.StringUtils;
import org.generationcp.middleware.domain.dms.ExperimentDesignType;
import org.generationcp.middleware.domain.dms.StandardVariable;
import org.generationcp.middleware.domain.etl.MeasurementVariable;
import org.generationcp.middleware.domain.oms.TermId;
import org.generationcp.middleware.manager.api.OntologyDataManager;
import org.generationcp.middleware.service.api.study.StudyGermplasmDto;
import org.ibp.api.domain.design.MainDesign;
import org.ibp.api.java.design.type.ExperimentDesignTypeService;
import org.ibp.api.java.impl.middleware.design.generator.ExperimentDesignGenerator;
import org.ibp.api.java.impl.middleware.design.util.ExperimentalDesignUtil;
import org.ibp.api.java.impl.middleware.design.validator.ExperimentDesignTypeValidator;
import org.ibp.api.rest.dataset.ObservationUnitRow;
import org.ibp.api.rest.design.ExperimentDesignInput;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ResolvableIncompleteBlockDesignTypeServiceImpl implements ExperimentDesignTypeService {

	protected static final List<Integer> DESIGN_FACTOR_VARIABLES =
		Arrays.asList(TermId.REP_NO.getId(), TermId.PLOT_NO.getId(), TermId.ENTRY_NO.getId(), TermId.BLOCK_NO.getId());

	protected static final List<Integer> EXPERIMENT_DESIGN_VARIABLES_LATINIZED =
		Arrays.asList(TermId.EXPERIMENT_DESIGN_FACTOR.getId(), TermId.NUMBER_OF_REPLICATES.getId(), TermId.BLOCK_SIZE.getId(),
			TermId.NO_OF_CBLKS_LATINIZE.getId(), TermId.REPLICATIONS_MAP.getId(), TermId.NO_OF_REPS_IN_COLS.getId());

	protected static final List<Integer> EXPERIMENT_DESIGN_VARIABLES =
		Arrays.asList(TermId.EXPERIMENT_DESIGN_FACTOR.getId(), TermId.NUMBER_OF_REPLICATES.getId(), TermId.BLOCK_SIZE.getId());

	@Resource
	private ExperimentDesignTypeValidator experimentDesignTypeValidator;

	@Resource
	private ExperimentDesignGenerator experimentDesignGenerator;

	@Resource
	private OntologyDataManager ontologyDataManager;

	@Override
	public List<ObservationUnitRow> generateDesign(final int studyId, final ExperimentDesignInput experimentDesignInput,
		final String programUUID, final List<StudyGermplasmDto> studyGermplasmDtoList) {

		this.experimentDesignTypeValidator.validateResolvableIncompleteBlockDesign(experimentDesignInput, studyGermplasmDtoList);

		final int nTreatments = studyGermplasmDtoList.size();
		final Integer blockSize = experimentDesignInput.getBlockSize();
		final Integer replicates = experimentDesignInput.getReplicationsCount();
		final int numberOfTrials = experimentDesignInput.getNoOfEnvironments();

		final Map<Integer, StandardVariable> standardVariablesMap =
			this.ontologyDataManager.getStandardVariables(DESIGN_FACTOR_VARIABLES, programUUID).stream()
				.collect(Collectors.toMap(StandardVariable::getId, standardVariable -> standardVariable));

		final String entryNumberName = standardVariablesMap.get(TermId.ENTRY_NO.getId()).getName();
		final String replicateNumberName = standardVariablesMap.get(TermId.REP_NO.getId()).getName();
		final String plotNumberName = standardVariablesMap.get(TermId.PLOT_NO.getId()).getName();
		final String blockNumberName = standardVariablesMap.get(TermId.BLOCK_NO.getId()).getName();

		ExperimentalDesignUtil.setReplatinGroups(experimentDesignInput);

		final Integer plotNo = experimentDesignInput.getStartingPlotNo() == null? 1 : experimentDesignInput.getStartingPlotNo();

		final MainDesign mainDesign = this.experimentDesignGenerator
			.createResolvableIncompleteBlockDesign(blockSize, nTreatments, replicates, entryNumberName,
				replicateNumberName, blockNumberName, plotNumberName, plotNo,
				experimentDesignInput.getNblatin(),
				experimentDesignInput.getReplatinGroups(), "", experimentDesignInput.getUseLatenized());

		final List<MeasurementVariable> measurementVariables = this.getMeasurementVariables(studyId, experimentDesignInput, programUUID);
		return this.experimentDesignGenerator
			.generateExperimentDesignMeasurements(numberOfTrials, measurementVariables, studyGermplasmDtoList, mainDesign, entryNumberName,
				null,
				new HashMap<>());
	}

	@Override
	public Boolean requiresLicenseCheck() {
		return Boolean.TRUE;
	}

	@Override
	public Integer getDesignTypeId() {
		return ExperimentDesignType.RESOLVABLE_INCOMPLETE_BLOCK.getId();
	}

	@Override
	public List<MeasurementVariable> getMeasurementVariables(final int studyId, final ExperimentDesignInput experimentDesignInput,
		final String programUUID) {
		return this.experimentDesignGenerator
			.constructMeasurementVariables(studyId, programUUID, DESIGN_FACTOR_VARIABLES,
				(experimentDesignInput.getUseLatenized() != null && experimentDesignInput.getUseLatenized()) ?
					EXPERIMENT_DESIGN_VARIABLES_LATINIZED : EXPERIMENT_DESIGN_VARIABLES, experimentDesignInput);
	}
}