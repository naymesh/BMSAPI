package org.ibp.api.java.impl.middleware.dataset;

import com.google.common.io.Files;
import org.generationcp.commons.util.FileUtils;
import org.generationcp.commons.util.ZipUtil;
import org.generationcp.middleware.domain.dms.DataSetType;
import org.generationcp.middleware.domain.dms.DatasetDTO;
import org.generationcp.middleware.domain.dms.Study;
import org.generationcp.middleware.domain.etl.MeasurementVariable;
import org.generationcp.middleware.manager.api.OntologyDataManager;
import org.generationcp.middleware.manager.api.StudyDataManager;
import org.generationcp.middleware.service.impl.study.StudyInstance;
import org.ibp.api.java.dataset.DatasetCollectionOrderService;
import org.ibp.api.java.dataset.DatasetFileGenerator;
import org.ibp.api.java.dataset.DatasetService;
import org.ibp.api.java.impl.middleware.dataset.validator.DatasetValidator;
import org.ibp.api.java.impl.middleware.dataset.validator.InstanceValidator;
import org.ibp.api.java.impl.middleware.dataset.validator.StudyValidator;
import org.ibp.api.rest.dataset.ObservationUnitRow;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractDatasetExportService {

	static final String XLS = "xls";
	static final String CSV = "csv";

	@Autowired
	private StudyValidator studyValidator;

	@Autowired
	private DatasetValidator datasetValidator;

	@Autowired
	private InstanceValidator instanceValidator;

	@Autowired
	protected DatasetService studyDatasetService;

	@Autowired
	protected DatasetCollectionOrderService datasetCollectionOrderService;

	@Autowired
	protected OntologyDataManager ontologyDataManager;

	@Resource
	protected org.generationcp.middleware.service.api.dataset.DatasetService datasetService;

	@Resource
	protected StudyDataManager studyDataManager;

	private ZipUtil zipUtil = new ZipUtil();

	protected void validate(final int studyId, final int datasetId, final Set<Integer> instanceIds) {
		this.studyValidator.validate(studyId, false);
		this.datasetValidator.validateDataset(studyId, datasetId, false);
		this.instanceValidator.validate(datasetId, instanceIds);
	}

	File generate(final int studyId, final int datasetId, final Set<Integer> instanceIds, final int collectionOrderId, final DatasetFileGenerator generator, final boolean singleFile, final String fileExtension) throws  IOException{

		final Study study = this.studyDataManager.getStudy(studyId);
		final DatasetDTO dataSet = this.datasetService.getDataset(datasetId);


		// Get all variables for the dataset
		final List<MeasurementVariable> columns = this.getColumns(study.getId(), dataSet.getDatasetId());
		final Map<Integer, StudyInstance> selectedDatasetInstancesMap = getSelectedDatasetInstancesMap(dataSet.getInstances(),
			instanceIds);
		final Map<Integer, List<ObservationUnitRow>> observationUnitRowMap =
			this.getObservationUnitRowMap(study, dataSet, selectedDatasetInstancesMap);
		final DatasetCollectionOrderServiceImpl.CollectionOrder collectionOrder = DatasetCollectionOrderServiceImpl.CollectionOrder.findById(collectionOrderId);
		final int trialDatasetId = this.studyDataManager.getDataSetsByType(study.getId(), DataSetType.SUMMARY_DATA).get(0).getId();
		this.datasetCollectionOrderService.reorder(collectionOrder, trialDatasetId, selectedDatasetInstancesMap, observationUnitRowMap);

		if(singleFile) {
			return this.generateInSingleFile(study, observationUnitRowMap, columns, generator, fileExtension);
		} else  {
			return this.generateFiles(study, dataSet, selectedDatasetInstancesMap, observationUnitRowMap, columns, generator, fileExtension);
		}

	}

	File generateInSingleFile(final Study study,
		final Map<Integer, List<ObservationUnitRow>> observationUnitRowMap, final List<MeasurementVariable> columns, final DatasetFileGenerator generator, final  String fileExtension)
		throws IOException {

		final File temporaryFolder = Files.createTempDir();
		final String sanitizedFileName = FileUtils.sanitizeFileName(String.format("%s_AllInstances." + fileExtension, study.getName()));
		final String fileNameFullPath = temporaryFolder.getAbsolutePath() + File.separator + sanitizedFileName;

		return generator.generateMultiInstanceFile (observationUnitRowMap, columns, fileNameFullPath);
	}


	File generateFiles(final Study study, final DatasetDTO dataSetDto,
		final Map<Integer, StudyInstance> selectedDatasetInstancesMap,
		final Map<Integer, List<ObservationUnitRow>> observationUnitRowMap, final List<MeasurementVariable> columns, final DatasetFileGenerator generator, final String fileExtension)
		throws IOException {
		final File temporaryFolder = Files.createTempDir();
		final List<File> files =
			this.getInstanceFiles(study, dataSetDto, selectedDatasetInstancesMap, observationUnitRowMap, columns, generator, fileExtension,
				temporaryFolder);
		return this.getReturnFile(study, files);
	}

	File getReturnFile(final Study study, final List<File> files) throws IOException {
		if (files.size() == 1) {
			return files.get(0);
		} else {
			return this.zipUtil.zipFiles(study.getName(), files);
		}
	}

	List<File> getInstanceFiles(
		final Study study, final DatasetDTO dataSetDto, final Map<Integer, StudyInstance> selectedDatasetInstancesMap,
		final Map<Integer, List<ObservationUnitRow>> observationUnitRowMap, final List<MeasurementVariable> columns,
		final DatasetFileGenerator generator, final String fileExtension, final File temporaryFolder) throws IOException {
		final List<File> files = new ArrayList<>();
		for(final Integer instanceDBID: observationUnitRowMap.keySet()) {
			// Build the filename with the following format:
			// study_name + TRIAL_INSTANCE number + location_abbr +  dataset_type + dataset_name
			final String sanitizedFileName = FileUtils.sanitizeFileName(String
				.format(
					"%s_%s_%s_%s." + fileExtension, study.getName() + "-" + selectedDatasetInstancesMap.get(instanceDBID).getInstanceNumber(), selectedDatasetInstancesMap.get(instanceDBID).getLocationAbbreviation(),
					DataSetType.findById(dataSetDto.getDatasetTypeId()).getReadableName(), dataSetDto.getName()));
			final String fileNameFullPath = temporaryFolder.getAbsolutePath() + File.separator + sanitizedFileName;
			files.add(
				generator.generateSingleInstanceFile(study.getId(), dataSetDto, columns, observationUnitRowMap.get(instanceDBID), fileNameFullPath, selectedDatasetInstancesMap.get(instanceDBID)));
		}
		return files;
	}

	Map<Integer, StudyInstance> getSelectedDatasetInstancesMap(final List<StudyInstance> studyInstances, final Set<Integer> instanceIds) {
		Map<Integer, StudyInstance> studyInstanceMap = new HashMap<>();
		for(StudyInstance studyInstance: studyInstances) {
			if (instanceIds.contains(studyInstance.getInstanceDbId())) {
				studyInstanceMap.put(studyInstance.getInstanceDbId(), studyInstance);
			}
		}
		return studyInstanceMap;
	}
	
	List<MeasurementVariable> moveSelectedVariableInTheFirstColumn(List<MeasurementVariable> columns, final int variableId) {
		int trialInstanceIndex = 0;
		for(final MeasurementVariable column: columns) {
			if(variableId == column.getTermId()) {
				final MeasurementVariable trialInstanceMeasurementVariable = columns.remove(trialInstanceIndex);
				columns.add(0, trialInstanceMeasurementVariable);
				break;
			}
			trialInstanceIndex++;
		}
		return columns;
	}

	protected abstract List<MeasurementVariable> getColumns(int studyId, int datasetId);

	protected abstract Map<Integer, List<ObservationUnitRow>> getObservationUnitRowMap(Study study, DatasetDTO dataset, Map<Integer, StudyInstance> selectedDatasetInstancesMap);

	void setZipUtil(final ZipUtil zipUtil) {
		this.zipUtil = zipUtil;
	}
}