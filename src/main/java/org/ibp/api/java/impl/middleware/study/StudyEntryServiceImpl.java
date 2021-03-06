package org.ibp.api.java.impl.middleware.study;

import com.google.common.collect.Lists;
import org.generationcp.middleware.domain.dms.Enumeration;
import org.generationcp.middleware.domain.etl.MeasurementVariable;
import org.generationcp.middleware.domain.gms.SystemDefinedEntryType;
import org.generationcp.middleware.domain.inventory.common.SearchCompositeDto;
import org.generationcp.middleware.domain.oms.Term;
import org.generationcp.middleware.domain.oms.TermId;
import org.generationcp.middleware.domain.ontology.VariableType;
import org.generationcp.middleware.domain.study.StudyEntryGeneratorRequestDto;
import org.generationcp.middleware.domain.study.StudyEntryPropertyBatchUpdateRequest;
import org.generationcp.middleware.domain.study.StudyEntrySearchDto;
import org.generationcp.middleware.enumeration.DatasetTypeEnum;
import org.generationcp.middleware.manager.api.GermplasmDataManager;
import org.generationcp.middleware.manager.api.OntologyDataManager;
import org.generationcp.middleware.pojos.Germplasm;
import org.generationcp.middleware.pojos.GermplasmList;
import org.generationcp.middleware.pojos.GermplasmListData;
import org.generationcp.middleware.service.api.PedigreeService;
import org.generationcp.middleware.service.api.dataset.DatasetService;
import org.generationcp.middleware.service.api.study.StudyEntryDto;
import org.generationcp.middleware.util.CrossExpansionProperties;
import org.ibp.api.java.entrytype.EntryTypeService;
import org.ibp.api.java.germplasm.GermplamListService;
import org.ibp.api.java.impl.middleware.common.validator.EntryTypeValidator;
import org.ibp.api.java.impl.middleware.common.validator.GermplasmListValidator;
import org.ibp.api.java.impl.middleware.common.validator.GermplasmValidator;
import org.ibp.api.java.impl.middleware.common.validator.SearchCompositeDtoValidator;
import org.ibp.api.java.impl.middleware.inventory.manager.common.SearchRequestDtoResolver;
import org.ibp.api.java.impl.middleware.ontology.validator.TermValidator;
import org.ibp.api.java.impl.middleware.study.validator.StudyEntryValidator;
import org.ibp.api.java.impl.middleware.study.validator.StudyValidator;
import org.ibp.api.java.study.StudyEntryService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudyEntryServiceImpl implements StudyEntryService {

	@Resource
	private StudyValidator studyValidator;

	@Autowired
	private PedigreeService pedigreeService;

	@Autowired
	private CrossExpansionProperties crossExpansionProperties;

	@Resource
	private StudyEntryValidator studyEntryValidator;

	@Autowired
	private GermplasmListValidator germplasmListValidator;

	@Autowired
	private GermplasmValidator germplasmValidator;

	@Autowired
	private EntryTypeValidator entryTypeValidator;

	@Autowired
	private GermplamListService germplasmListService;

	@Autowired
	private SearchRequestDtoResolver searchRequestDtoResolver;

	@Autowired
	private TermValidator termValidator;

	@Autowired
	private SearchCompositeDtoValidator searchCompositeDtoValidator;

	@Resource
	private org.generationcp.middleware.service.api.study.StudyEntryService middlewareStudyEntryService;

	@Resource
	private DatasetService datasetService;

	@Resource
	private EntryTypeService entryTypeService;

	@Resource
	private OntologyDataManager ontologyDataManager;

	@Autowired
	private GermplasmDataManager germplasmDataManager;

	@Override
	public StudyEntryDto replaceStudyEntry(final Integer studyId, final Integer entryId,
		final StudyEntryDto studyEntryDto) {
		final Integer gid = studyEntryDto.getGid();
		this.studyValidator.validate(studyId, true);
		this.studyEntryValidator.validate(studyId, entryId, gid);

		return this.middlewareStudyEntryService
			.replaceStudyEntry(studyId, entryId, gid, this.pedigreeService.getCrossExpansion(gid, this.crossExpansionProperties));
	}

	@Override
	public List<StudyEntryDto> createStudyEntries(final Integer studyId,
		final StudyEntryGeneratorRequestDto studyEntryGeneratorRequestDto) {
		this.studyValidator.validate(studyId, true);

		//Validate EntryType
		this.entryTypeValidator.validateEntryType(studyEntryGeneratorRequestDto.getEntryTypeId());

		final SearchCompositeDto<Integer, Integer> searchComposite = studyEntryGeneratorRequestDto.getSearchComposite();
		final BindingResult errors = new MapBindingResult(new HashMap<>(), StudyEntryGeneratorRequestDto.class.getName());
		this.searchCompositeDtoValidator.validateSearchCompositeDto(searchComposite, errors);
		final List<Integer> gids = this.searchRequestDtoResolver.resolveGidSearchDto(searchComposite);
		this.germplasmValidator.validateGids(errors, gids);

		final List<Germplasm> germplasmList = this.germplasmDataManager.getGermplasms(gids);
		final Map<Integer, String> gidDesignationMap = this.germplasmDataManager.getPreferredNamesByGids(gids);
		final List<Integer> germplasmDescriptorIds = this.getEntryDescriptorColumns(studyId).stream()
			.map(measurementVariable -> measurementVariable.getTermId()).collect(Collectors.toList());

		//Get the next entry number
		final Integer startingEntryNumber = this.middlewareStudyEntryService.getNextEntryNumber(studyId);

		//Retrieve the map if Cross is in Germplasm descriptors
		final Map<Integer, String> gidCrossMap = germplasmDescriptorIds.contains(TermId.CROSS.getId())?
			this.pedigreeService.getCrossExpansions(new HashSet<>(gids), null, this.crossExpansionProperties) : new HashMap<>();

		final List<StudyEntryDto> studyEntryDtoList = StudyEntryMapper.map(germplasmList, gidDesignationMap, startingEntryNumber,
			germplasmDescriptorIds, studyEntryGeneratorRequestDto.getEntryTypeId(), gidCrossMap);
		return this.middlewareStudyEntryService.saveStudyEntries(studyId, studyEntryDtoList);
	}

	@Override
	public List<StudyEntryDto> createStudyEntries(final Integer studyId, final Integer listId) {
		final GermplasmList germplasmList = this.germplasmListService.getGermplasmList(listId);
		this.studyValidator.validate(studyId, true);

		this.germplasmListValidator.validateGermplasmList(listId);
		this.studyEntryValidator.validateStudyAlreadyHasStudyEntries(studyId);

		final ModelMapper mapper = StudyEntryMapper.getInstance();
		final List<StudyEntryDto> studyEntryDtoList =
			germplasmList.getListData().stream().map(l -> mapper.map(l, StudyEntryDto.class)).collect(Collectors.toList());

		final Map<Integer, GermplasmListData> germplasmListDataMap =
			germplasmList.getListData().stream().collect(Collectors.toMap(GermplasmListData::getEntryId, g -> g));
		final List<Integer> germplasmDescriptorIds = this.getEntryDescriptorColumns(studyId).stream()
			.map(measurementVariable -> measurementVariable.getTermId()).collect(Collectors.toList());

		for(final StudyEntryDto studyEntryDto: studyEntryDtoList) {
			studyEntryDto.setProperties(
				StudyEntryPropertiesMapper.map(germplasmListDataMap.get(studyEntryDto.getEntryId()), germplasmDescriptorIds));
		}

		return this.middlewareStudyEntryService.saveStudyEntries(studyId, studyEntryDtoList);
	}

	@Override
	public void deleteStudyEntries(final Integer studyId) {
		this.studyValidator.validate(studyId, true);
		this.studyValidator.validateStudyShouldNotHaveObservation(studyId);
		this.middlewareStudyEntryService.deleteStudyEntries(studyId);
	}

	@Override
	public void updateStudyEntriesProperty(final Integer studyId,
		final StudyEntryPropertyBatchUpdateRequest batchUpdateRequest) {
		this.studyValidator.validate(studyId, true);
		this.studyValidator.validateStudyShouldNotHaveObservation(studyId);
		this.studyEntryValidator.validateStudyContainsEntries(studyId, new ArrayList<>(batchUpdateRequest.getSearchComposite().getItemIds()));
		this.termValidator.validate(batchUpdateRequest.getVariableId());
		this.middlewareStudyEntryService.updateStudyEntriesProperty(batchUpdateRequest);
	}

	@Override
	public long countFilteredStudyEntries(final Integer studyId, final StudyEntrySearchDto.Filter filter) {
		return this.middlewareStudyEntryService.countFilteredStudyEntries(studyId, filter);
	}

	@Override
	public List<StudyEntryDto> getStudyEntries(final Integer studyId, final StudyEntrySearchDto.Filter filter, final Pageable pageable) {
		this.studyValidator.validate(studyId, false);

		Pageable convertedPageable = null;
		if (pageable != null && pageable.getSort() != null) {
			final Iterator<Sort.Order> iterator = pageable.getSort().iterator();
			if (iterator.hasNext()) {
				// Convert the sort property name from termid to actual term name.
				final Sort.Order sort = iterator.next();
				final Term term = this.ontologyDataManager.getTermById(Integer.valueOf(sort.getProperty()));
				final String sortProperty = Objects.isNull(term) ? sort.getProperty() : term.getName();
				pageable.getSort().and(new Sort(sort.getDirection(), sortProperty));
				convertedPageable =
					new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort.getDirection(),
						sortProperty);
			}
		}

		return this.middlewareStudyEntryService.getStudyEntries(studyId, filter, convertedPageable);
	}

	@Override
	public long countAllStudyEntries(final Integer studyId) {
		this.studyValidator.validate(studyId, false);
		return this.middlewareStudyEntryService.countStudyEntries(studyId);
	}

	@Override
	public List<MeasurementVariable> getEntryDescriptorColumns(final Integer studyId) {
		this.studyValidator.validate(studyId, false);
		final Integer plotDatasetId =
			this.datasetService.getDatasets(studyId, new HashSet<>(Arrays.asList(DatasetTypeEnum.PLOT_DATA.getId()))).get(0).getDatasetId();

		final List<Integer> termsToRemove = Lists
			.newArrayList(TermId.OBS_UNIT_ID.getId());

		final List<MeasurementVariable> entryDescriptors =
			this.datasetService.getObservationSetVariables(plotDatasetId, Lists
				.newArrayList(VariableType.GERMPLASM_DESCRIPTOR.getId()));

		//Remove OBS_UNIT_ID column if present
		entryDescriptors.removeIf(entry -> termsToRemove.contains(entry.getTermId()));

		//Add Inventory related columns
		entryDescriptors.add(this.buildVirtualColumn("LOTS", TermId.GID_ACTIVE_LOTS_COUNT));
		entryDescriptors.add(this.buildVirtualColumn("AVAILABLE", TermId.GID_AVAILABLE_BALANCE));
		entryDescriptors.add(this.buildVirtualColumn("UNIT", TermId.GID_UNIT));

		return entryDescriptors;
	}

	@Override
	public long countAllStudyTestEntries(final Integer studyId) {
		return this.middlewareStudyEntryService.countStudyGermplasmByEntryTypeIds(studyId,
			Collections.singletonList(String.valueOf(SystemDefinedEntryType.TEST_ENTRY.getEntryTypeCategoricalId())));
	}

	@Override
	public long countAllCheckTestEntries(final Integer studyId, final String programUuid, final Boolean checkOnly) {
		if(checkOnly) {
			return this.middlewareStudyEntryService.countStudyGermplasmByEntryTypeIds(studyId,
				Collections.singletonList(String.valueOf(SystemDefinedEntryType.CHECK_ENTRY.getEntryTypeCategoricalId())));
		} else {
			final List<Enumeration> entryTypes = this.entryTypeService.getEntryTypes(programUuid);
			final List<String> checkEntryTypeIds = entryTypes.stream()
				.filter(entryType -> entryType.getId() != SystemDefinedEntryType.TEST_ENTRY.getEntryTypeCategoricalId())
				.map(entryType -> String.valueOf(entryType.getId())).collect(Collectors.toList());
			return this.middlewareStudyEntryService.countStudyGermplasmByEntryTypeIds(studyId, checkEntryTypeIds);
		}
	}

	@Override
	public StudyEntryMetadata getStudyEntriesMetadata(final Integer studyId, final String programUuid) {
		this.studyValidator.validate(studyId, false);
		final StudyEntryMetadata studyEntryMetadata = new StudyEntryMetadata();
		studyEntryMetadata.setTestEntriesCount(this.countAllStudyTestEntries(studyId));
		studyEntryMetadata.setCheckEntriesCount(this.countAllCheckTestEntries(studyId, programUuid, true));
		studyEntryMetadata.setNonTestEntriesCount(this.countAllCheckTestEntries(studyId, programUuid, false));
		studyEntryMetadata.setHasUnassignedEntries(this.middlewareStudyEntryService.hasUnassignedEntries(studyId));
		return studyEntryMetadata;
	}

	private MeasurementVariable buildVirtualColumn(final String name, final TermId termId) {
		final MeasurementVariable sampleColumn = new MeasurementVariable();
		sampleColumn.setName(name);
		sampleColumn.setAlias(name);
		sampleColumn.setTermId(termId.getId());
		sampleColumn.setFactor(true);
		return sampleColumn;
	}

	public void setDatasetService(final DatasetService datasetService) {
		this.datasetService = datasetService;
	}

}
