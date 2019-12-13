package org.ibp.api.rest.inventory.manager;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.generationcp.middleware.domain.inventory.manager.ExtendedLotDto;
import org.generationcp.middleware.domain.inventory.manager.LotGeneratorInputDto;
import org.generationcp.middleware.domain.inventory.manager.LotsSearchDto;
import org.generationcp.middleware.manager.api.SearchRequestService;
import org.ibp.api.brapi.v1.common.SingleEntityResponse;
import org.ibp.api.domain.common.PagedResult;
import org.ibp.api.domain.search.SearchDto;
import org.ibp.api.java.inventory.manager.LotService;
import org.ibp.api.rest.common.PaginatedSearch;
import org.ibp.api.rest.common.SearchSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@Api(value = "Lot Services")
@RestController
@PreAuthorize("hasAnyAuthority('ADMIN','CROP_MANAGEMENT')")
public class LotResource {


	@Autowired
	private LotService lotService;

	@Autowired
	private SearchRequestService searchRequestService;

	@ApiOperation(value = "Post lot search", notes = "Post lot search")
	@RequestMapping(value = "/crops/{cropName}/lots/search", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<SingleEntityResponse<SearchDto>> postSearchLots(
		@PathVariable final String cropName, @RequestBody final LotsSearchDto lotsSearchDto) {
		final String searchRequestId =
			this.searchRequestService.saveSearchRequest(lotsSearchDto, LotsSearchDto.class).toString();

		final SearchDto searchDto = new SearchDto(searchRequestId);
		final SingleEntityResponse<SearchDto> singleGermplasmResponse = new SingleEntityResponse<SearchDto>(searchDto);

		return new ResponseEntity<>(singleGermplasmResponse, HttpStatus.OK);

	}

	@ApiOperation(value = "It will retrieve lots that matches search conditions", notes = "It will retrieve lots that matches search conditions")
	@RequestMapping(value = "/crops/{cropName}/lots/search", method = RequestMethod.GET)
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
	@ResponseBody
	public ResponseEntity<List<ExtendedLotDto>> getLots(@PathVariable final String cropName, //
		@RequestParam final Integer searchRequestId, @ApiIgnore
	final Pageable pageable) {

		final LotsSearchDto searchDTO = (LotsSearchDto) this.searchRequestService
			.getSearchRequest(searchRequestId, LotsSearchDto.class);

		final PagedResult<ExtendedLotDto> resultPage =
			new PaginatedSearch().executeBrapiSearch(pageable.getPageNumber(), pageable.getPageSize(), new SearchSpec<ExtendedLotDto>() {

				@Override
				public long getCount() {
					return LotResource.this.lotService.countSearchLots(searchDTO);
				}

				@Override
				public List<ExtendedLotDto> getResults(final PagedResult<ExtendedLotDto> pagedResult) {
					return LotResource.this.lotService.searchLots(searchDTO, pageable);
				}
			});

		final List<ExtendedLotDto> extendedLotDtos = resultPage.getPageResults();

		final HttpHeaders headers = new HttpHeaders();
		headers.add("X-Total-Count", Long.toString(resultPage.getTotalResults()));

		return new ResponseEntity<>(extendedLotDtos, headers, HttpStatus.OK);

	}

	@ApiOperation(value = "Create Lot", notes = "Create a new lot")
	@RequestMapping(value = "/crops/{cropName}/lots", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Integer> createLot(
		@PathVariable final String cropName,
		@ApiParam("Lot to be created")
		@RequestBody final LotGeneratorInputDto lotGeneratorInputDto) {
		return new ResponseEntity<>(lotService.saveLot(lotGeneratorInputDto), HttpStatus.CREATED);
	}
}