package org.ibp.api.rest.study;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.generationcp.middleware.domain.etl.MeasurementVariable;
import org.generationcp.middleware.pojos.workbench.PermissionsEnum;
import org.generationcp.middleware.service.api.study.StudyEntryDto;
import org.generationcp.middleware.service.api.study.StudyEntryPropertyData;
import org.generationcp.middleware.service.api.study.StudyGermplasmDto;
import org.ibp.api.domain.common.PagedResult;
import org.ibp.api.java.study.StudyGermplasmService;
import org.ibp.api.rest.common.PaginatedSearch;
import org.ibp.api.rest.common.SearchSpec;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.util.List;

// TODO: Move these services to StudyResource
@Api(value = "Study Germplasm Services")
@Controller
@RequestMapping("/crops")
public class StudyGermplasmResource {

	@Resource
	private StudyGermplasmService studyGermplasmService;

	@ApiOperation(value = "Replace germplasm entry in study",
		notes = "Replace germplasm entry in study")
	@RequestMapping(value = "/{cropname}/programs/{programUUID}/studies/{studyId}/entries/{entryId}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<StudyGermplasmDto> replaceStudyGermplasm(final @PathVariable String cropname,
		@PathVariable final String programUUID,
		@PathVariable final Integer studyId, @PathVariable final Integer entryId, @RequestBody final StudyGermplasmDto studyGermplasmDto) {
		return new ResponseEntity<>(this.studyGermplasmService.replaceStudyEntry(studyId, entryId, studyGermplasmDto),
			HttpStatus.OK);

	}

	@ApiOperation(value = "Create germplasm entries in study based on the specified germplasm list",
		notes = "Create germplasm entries in study based on the specified germplasm list")
	@RequestMapping(value = "/{cropname}/programs/{programUUID}/studies/{studyId}/entries/generation", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<List<StudyGermplasmDto>> createStudyEntries(final @PathVariable String cropname,
		@PathVariable final String programUUID,
		@PathVariable final Integer studyId, @RequestBody final GermplasmEntryRequestDto germplasmEntryRequestDto) {

		return new ResponseEntity<>(
			this.studyGermplasmService.createStudyEntries(studyId, germplasmEntryRequestDto.getGermplasmListId()),
			HttpStatus.OK);

	}

	@ApiOperation(value = "Delete germplasm entries in study",
		notes = "Delete germplasm entries in study")
	@RequestMapping(value = "/{cropname}/programs/{programUUID}/studies/{studyId}/entries", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity deleteStudyEntries(final @PathVariable String cropname,
		@PathVariable final String programUUID,
		@PathVariable final Integer studyId) {

		this.studyGermplasmService.deleteStudyEntries(studyId);

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);

	}

	@ApiOperation(value = "Update germplasm entry property",
		notes = "Update germplasm entry property")
	@RequestMapping(value = "/{cropname}/programs/{programUUID}/studies/{studyId}/entries/{entryId}/properties/{propertyId}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity updateStudyEntryProperty(final @PathVariable String cropname,
		@PathVariable final String programUUID,
		@PathVariable final Integer studyId, @PathVariable final Integer entryId, @PathVariable final Integer propertyId,
		@RequestBody StudyEntryPropertyData studyEntryPropertyData) {

		this.studyGermplasmService.updateStudyEntryProperty(studyId, entryId, studyEntryPropertyData);

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);

	}

	@ApiOperation(value = "Get study entries",
		notes = "Get study entries as table")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "page", dataType = "integer", paramType = "query",
			value = "Results page you want to retrieve (0..N)"),
		@ApiImplicitParam(name = "size", dataType = "integer", paramType = "query",
			value = "Number of records per page."),
		@ApiImplicitParam(name = "sort", allowMultiple = true, dataType = "string", paramType = "query",
			value = "Sorting criteria in the format: property(,asc|desc). " +
				"Default sort order is ascending. " +
				"Multiple sort criteria are supported.")
	})
	@RequestMapping(value = "/{cropName}/programs/{programUUID}/studies/{studyId}/entries", method = RequestMethod.GET)
	@PreAuthorize("hasAnyAuthority('ADMIN','STUDIES','MANAGE_STUDIES')"+ PermissionsEnum.HAS_MANAGE_STUDIES_VIEW)
	@ResponseBody
	public ResponseEntity<List<StudyEntryDto>> getEntriesAsTable(final @PathVariable String cropName,
		@PathVariable final String programUUID,
		@PathVariable final Integer studyId, @ApiIgnore final Pageable pageable) {

		final PagedResult<StudyEntryDto> resultPage =
			new PaginatedSearch().executeBrapiSearch(pageable.getPageNumber(), pageable.getPageSize(), new SearchSpec<StudyEntryDto>() {

				@Override
				public long getCount() {
					return StudyGermplasmResource.this.studyGermplasmService.countAllStudyEntries(studyId);
				}

				@Override
				public List<StudyEntryDto> getResults(final PagedResult<StudyEntryDto> pagedResult) {
					return StudyGermplasmResource.this.studyGermplasmService.getStudyEntries(studyId, null, pageable);
				}
			});

		final HttpHeaders headers = new HttpHeaders();
		headers.add("X-Total-Count", Long.toString(resultPage.getTotalResults()));
		headers.add("X-Total-Pages", Long.toString(resultPage.getTotalPages()));
		return new ResponseEntity<>(resultPage.getPageResults(), headers, HttpStatus.OK);
	}

	@ApiOperation(value = "Get Entry Descriptors as Columns", notes = "Retrieves ALL MeasurementVariables associated to the entry plus "
		+ "some calculated inventory columns")
	@PreAuthorize("hasAnyAuthority('ADMIN','STUDIES','MANAGE_STUDIES')"+ PermissionsEnum.HAS_MANAGE_STUDIES_VIEW)
	@RequestMapping(value = "/{crop}/programs/{programUUID}/studies/{studyId}/entries/table/columns", method = RequestMethod.GET)
	public ResponseEntity<List<MeasurementVariable>> getEntryTableColumns(@PathVariable final String crop,
		@PathVariable final String programUUID,
		@PathVariable final Integer studyId) {

		final List<MeasurementVariable> entryDescriptors =
			this.studyGermplasmService.getEntryDescriptorColumns(studyId);

		return new ResponseEntity<>(entryDescriptors, HttpStatus.OK);
	}
}
