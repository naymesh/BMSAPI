package org.ibp.api.rest.germplasm;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.generationcp.middleware.api.germplasm.search.GermplasmSearchResponse;
import org.generationcp.middleware.api.germplasm.search.GermplasmSearchRequest;
import org.ibp.api.domain.common.PagedResult;
import org.ibp.api.java.germplasm.GermplasmService;
import org.ibp.api.java.impl.middleware.common.validator.BaseValidator;
import org.ibp.api.rest.common.PaginatedSearch;
import org.ibp.api.rest.common.SearchSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@Api(value = "Germplasm Services")
// TODO @PreAuthorize("hasAnyAuthority('ADMIN', ...)")
@Controller
public class GermplasmResource {

	@Autowired
	private GermplasmService germplasmService;

	@ApiOperation(value = "Search germplasm")
	@RequestMapping(value = "/crops/{cropName}/germplasm/search", method = RequestMethod.POST)
	@ApiImplicitParams({
		@ApiImplicitParam(name = "page", dataType = "integer", paramType = "query",
			value = "page number. Start at " + PagedResult.DEFAULT_PAGE_NUMBER),
		@ApiImplicitParam(name = "size", dataType = "integer", paramType = "query",
			value = "Number of records per page. <b>Note:</b> this query may return additional records using some filters"),
		@ApiImplicitParam(name = "sort", allowMultiple = false, dataType = "string", paramType = "query",
			value = "Sorting criteria in the format: property,asc|desc. ")
	})
	@ResponseBody
	public ResponseEntity<List<GermplasmSearchResponse>> searchGermplasm(
		@PathVariable final String cropName,
		@RequestParam(required = false) final String programUUID,
		@RequestBody final GermplasmSearchRequest germplasmSearchRequest,
		@ApiIgnore @PageableDefault(page = PagedResult.DEFAULT_PAGE_NUMBER, size = PagedResult.DEFAULT_PAGE_SIZE)
		final Pageable pageable
	) {

		BaseValidator.checkNotNull(germplasmSearchRequest, "param.null", new String[] {"germplasmSearchDTO"});

		final PagedResult<GermplasmSearchResponse> result =
			new PaginatedSearch().execute(pageable.getPageNumber(), pageable.getPageSize(), new SearchSpec<GermplasmSearchResponse>() {

				@Override
				public long getCount() {
					// TODO
					return 0;
				}

				@Override
				public long getFilteredCount() {
					// TODO
					return 0;
				}

				@Override
				public List<GermplasmSearchResponse> getResults(final PagedResult<GermplasmSearchResponse> pagedResult) {
					return germplasmService.searchGermplasm(germplasmSearchRequest, pageable, programUUID);
				}
			});

		final List<GermplasmSearchResponse> pageResults = result.getPageResults();

		return new ResponseEntity<>(pageResults, HttpStatus.OK);
	}

}
