package org.ibp.api.rest.dataset;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.ibp.api.domain.common.PagedResult;
import org.ibp.api.java.dataset.DatasetService;
import org.ibp.api.rest.common.PaginatedSearch;
import org.ibp.api.rest.common.SearchSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.List;

@Api(value = "Dataset Services")
@Controller
@RequestMapping("/crops")
public class DatasetResource {

	@Autowired
	private DatasetService studyDatasetService;

	@ApiOperation(value = "Count Phenotypes", notes = "Returns count of phenotypes for variables")
	@RequestMapping(value = "/{crop}/studies/{studyId}/datasets/{datasetId}/variables/observations", method = RequestMethod.HEAD)
	@Transactional
	public ResponseEntity<String> countPhenotypes(@PathVariable final String crop, @PathVariable final Integer studyId,
			@PathVariable final Integer datasetId,  @RequestParam(value = "variableIds", required = true) final Integer[] variableIds) {

		final long count = this.studyDatasetService.countPhenotypes(studyId, datasetId, Arrays.asList(variableIds));
		final HttpHeaders respHeaders = new HttpHeaders();
		respHeaders.add("X-Total-Count", String.valueOf(count));

		return new ResponseEntity<>("", respHeaders, HttpStatus.OK);
	}

	/*@ApiOperation(value = "It will retrieve a list of datasets", notes = "Retrieves the list of datasets for the specified study.")
	@RequestMapping(value = "/{cropname}/studies/{studyId}/datasets/list", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<DatasetDTO>> getDatasets(
		@PathVariable final String cropname,
		@PathVariable final Integer studyId,
		@RequestParam(value = "filterByTypeIds", required = false) final Set<Integer> filterByTypeIds) {
		return new ResponseEntity<>(this.studyDatasetService.getDatasetByStudyId(studyId, filterByTypeIds), HttpStatus.OK);
	}*/

	@ApiOperation(value = "It will retrieve all the observation units including observations and props values in a format that will be used by the Observations table.")
	@RequestMapping(value = "/{cropname}/studies/{studyId}/datasets/{datasetId}/instances/{instanceId}/observationUnits/table", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<ObservationUnitTable> getObservationUnitTable(@PathVariable final String cropname,
		@PathVariable final Integer studyId, @PathVariable final Integer datasetId,
		@PathVariable final Integer instanceId, @RequestParam(value = "pageNumber", required = false) final Integer pageNumber, //
		@ApiParam(value = "Number of results to retrieve per page. Defaults to 100 if not supplied. Max page size allowed is 200.", required = false)
		//
		@RequestParam(value = "pageSize", required = false) final Integer pageSize) {
		final PagedResult<ObservationUnitRow> pageResult =
			new PaginatedSearch().execute(pageNumber, pageSize, new SearchSpec<ObservationUnitRow>() {

				@Override
				public long getCount() {
					return DatasetResource.this.studyDatasetService.countTotalObservationUnitsForDataset (datasetId, instanceId);
				}

				@Override
				public List<ObservationUnitRow> getResults(final PagedResult<ObservationUnitRow> pagedResult) {
					// BRAPI services have zero-based indexing for pages but paging for Middleware method starts at 1
					final int pageNumber = pagedResult.getPageNumber() + 1;
					return DatasetResource.this.studyDatasetService.getObservationUnitRows(
						studyId,
						datasetId,
						instanceId,
						pagedResult.getPageNumber(),
						pagedResult.getPageSize(),
						pagedResult.getSortBy(),
						pagedResult.getSortOrder());
				}
			});

		final ObservationUnitTable observationUnitTable = new ObservationUnitTable();
		observationUnitTable.setData(pageResult.getPageResults());
		observationUnitTable.setDraw("1");
		observationUnitTable.setRecordsTotal((int) pageResult.getTotalResults());
		observationUnitTable.setRecordsFiltered((int) pageResult.getTotalResults());
		return new ResponseEntity<>(observationUnitTable, HttpStatus.OK);
	}
}
