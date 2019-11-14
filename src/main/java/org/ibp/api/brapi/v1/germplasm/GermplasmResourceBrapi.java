package org.ibp.api.brapi.v1.germplasm;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.generationcp.middleware.domain.germplasm.GermplasmDTO;
import org.generationcp.middleware.domain.germplasm.PedigreeDTO;
import org.generationcp.middleware.domain.germplasm.ProgenyDTO;
import org.generationcp.middleware.domain.search_request.GermplasmSearchRequestDto;
import org.generationcp.middleware.exceptions.MiddlewareException;
import org.generationcp.middleware.manager.api.SearchRequestService;
import org.ibp.api.brapi.v1.common.BrapiPagedResult;
import org.ibp.api.brapi.v1.common.EntityListResponse;
import org.ibp.api.brapi.v1.common.Metadata;
import org.ibp.api.brapi.v1.common.Pagination;
import org.ibp.api.brapi.v1.common.Result;
import org.ibp.api.brapi.v1.common.SingleEntityResponse;
import org.ibp.api.domain.common.PagedResult;
import org.ibp.api.java.germplasm.GermplasmService;
import org.ibp.api.java.study.StudyService;
import org.ibp.api.rest.common.PaginatedSearch;
import org.ibp.api.rest.common.SearchSpec;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Api(value = "BrAPI Germplasm Services")
@Controller
public class GermplasmResourceBrapi {

	@Autowired
	private GermplasmService germplasmService;

	@Autowired
	private SearchRequestService searchRequestService;

	@Autowired
	private StudyService studyService;

	@ApiOperation(value = "Search germplasms", notes = "Search germplasms")
	@RequestMapping(value = "/{crop}/brapi/v1/germplasm-search", method = RequestMethod.GET)
	@ResponseBody
	@JsonView(Germplasm.View.GermplasmBrapiV1_2.class)
	public ResponseEntity<EntityListResponse<Germplasm>> searchGermplasms(
		@PathVariable final String crop,
		@ApiParam(value = BrapiPagedResult.CURRENT_PAGE_DESCRIPTION, required = false)
		@RequestParam(value = "page",
			required = false) final Integer currentPage,
		@ApiParam(value = BrapiPagedResult.PAGE_SIZE_DESCRIPTION, required = false)
		@RequestParam(value = "pageSize",
			required = false) final Integer pageSize,
		@ApiParam(value = "Permanent unique identifier", required = false)
		@RequestParam(value = "germplasmPUI",
			required = false) final String germplasmPUI,
		@ApiParam(value = "Internal database identifier", required = false)
		@RequestParam(value = "germplasmDbId",
			required = false) final String germplasmDbId,
		@ApiParam(value = "Name of the germplasm", required = false)
		@RequestParam(value = "germplasmName",
			required = false) final String germplasmName,
		@ApiParam(value = "The common crop name. This value is discarded, crop needs to be included as part of the URL", required = false)
		@RequestParam(value = "commonCropName",
			required = false) final String commonCropName) {

		final int gid;

		final GermplasmSearchRequestDto germplasmSearchRequestDTO = new GermplasmSearchRequestDto();

		germplasmSearchRequestDTO.setPreferredName(germplasmName);
		if (germplasmPUI != null) {
			germplasmSearchRequestDTO.setGermplasmPUIs(Lists.newArrayList(germplasmPUI));
		}

		try {
			if (germplasmDbId != null) {
				gid = Integer.parseInt(germplasmDbId);
				germplasmSearchRequestDTO.setGermplasmDbIds(Lists.newArrayList(Integer.toString(gid)));
			}
		} catch (final NumberFormatException e) {
			if (germplasmName == null && germplasmPUI == null) {
				return new ResponseEntity<>(new EntityListResponse<>(new Result<>(new ArrayList<Germplasm>())), HttpStatus.OK);
			}
		}

		final PagedResult<GermplasmDTO> resultPage = this.getGermplasmDTOPagedResult(germplasmSearchRequestDTO, currentPage, pageSize);

		final List<Germplasm> germplasmList = new ArrayList<>();

		if (resultPage.getPageResults() != null) {
			final ModelMapper mapper = new ModelMapper();
			for (final GermplasmDTO germplasmDTO : resultPage.getPageResults()) {
				final Germplasm germplasm = mapper.map(germplasmDTO, Germplasm.class);
				germplasm.setCommonCropName(crop);
				germplasmList.add(germplasm);
			}
		}

		final Result<Germplasm> results = new Result<Germplasm>().withData(germplasmList);
		final Pagination pagination = new Pagination().withPageNumber(resultPage.getPageNumber()).withPageSize(resultPage.getPageSize())
			.withTotalCount(resultPage.getTotalResults()).withTotalPages(resultPage.getTotalPages());

		final Metadata metadata = new Metadata().withPagination(pagination);

		final EntityListResponse<Germplasm> entityListResponse = new EntityListResponse<>(metadata, results);

		return new ResponseEntity<>(entityListResponse, HttpStatus.OK);

	}

	@ApiOperation(value = "Germplasm search by germplasmDbId", notes = "Germplasm search by germplasmDbId")
	@RequestMapping(value = "/{crop}/brapi/v1/germplasm/{germplasmDbId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<SingleEntityResponse<Germplasm>> searchGermplasm(
		@PathVariable final String crop,
		@PathVariable final String germplasmDbId) {

		final Integer gid;
		try {
			gid = Integer.parseInt(germplasmDbId);
		} catch (final NumberFormatException e) {
			return new ResponseEntity<>(new SingleEntityResponse<Germplasm>().withMessage("no germplasm found"), HttpStatus.NOT_FOUND);

		}

		final GermplasmDTO germplasmDTO = this.germplasmService.getGermplasmDTObyGID(gid);

		if (germplasmDTO != null) {
			final ModelMapper mapper = new ModelMapper();
			final Germplasm germplasm = mapper.map(germplasmDTO, Germplasm.class);
			germplasm.setCommonCropName(crop);

			final SingleEntityResponse<Germplasm> singleGermplasmResponse = new SingleEntityResponse<>(germplasm);

			return new ResponseEntity<>(singleGermplasmResponse, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(new SingleEntityResponse<Germplasm>().withMessage("no germplasm found"), HttpStatus.NOT_FOUND);
		}

	}

	@ApiOperation(value = "Germplasm pedigree by id", notes = "")
	@RequestMapping(value = "/{crop}/brapi/v1/germplasm/{germplasmDbId}/pedigree", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<SingleEntityResponse<PedigreeDTO>> getPedigree(
		@PathVariable final String crop,
		@ApiParam(value = "the internal id of the germplasm", required = true)
		@PathVariable(value = "germplasmDbId") final String germplasmDbId,
		@ApiParam(value = "text representation of the pedigree <strong style='color: red'>(Not Implemented)</strong>", required = false)
		@RequestParam(value = "notation", required = false) final String notation,
		@ApiParam(value = "include array of siblings in response", required = false)
		@RequestParam(value = "includeSiblings", required = false) final Boolean includeSiblings
	) {

		// TODO
		if (notation != null) {
			return new ResponseEntity<>(
				new SingleEntityResponse<PedigreeDTO>().withMessage("Search by pedigree not implemented"), HttpStatus.NOT_IMPLEMENTED);
		}

		final Integer gid;
		try {
			gid = Integer.valueOf(germplasmDbId);
		} catch (final NumberFormatException e) {
			return new ResponseEntity<>(new SingleEntityResponse<PedigreeDTO>().withMessage("no germplasm found"), HttpStatus.NOT_FOUND);
		}

		final PedigreeDTO pedigreeDTO = this.germplasmService.getPedigree(gid, null, includeSiblings);
		if (pedigreeDTO == null) {
			return new ResponseEntity<>(new SingleEntityResponse<PedigreeDTO>().withMessage("no germplasm found"), HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(new SingleEntityResponse<>(pedigreeDTO), HttpStatus.OK);
	}

	@ApiOperation(value = "Germplasm progeny by id", notes = "")
	@RequestMapping(value = "/{crop}/brapi/v1/germplasm/{germplasmDbId}/progeny", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<SingleEntityResponse<ProgenyDTO>> getProgeny(
		@PathVariable final String crop,
		@ApiParam(value = "the internal id of the germplasm", required = true)
		@PathVariable(value = "germplasmDbId") final String germplasmDbId
	) {

		final Integer gid;
		try {
			gid = Integer.valueOf(germplasmDbId);
		} catch (final NumberFormatException e) {
			return new ResponseEntity<>(new SingleEntityResponse<ProgenyDTO>().withMessage("no germplasm found"), HttpStatus.NOT_FOUND);
		}

		final ProgenyDTO progenyDTO = this.germplasmService.getProgeny(gid);
		if (progenyDTO == null) {
			return new ResponseEntity<>(new SingleEntityResponse<ProgenyDTO>().withMessage("no germplasm found"), HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(new SingleEntityResponse<>(progenyDTO), HttpStatus.OK);
	}

	@ApiOperation(value = "Get germplasm search", notes = "Get the results of a Germplasm search request")
	@RequestMapping(value = "/{crop}/brapi/v1/search/germplasm/{searchResultsDbid}", method = RequestMethod.GET)
	@ResponseBody
	@JsonView(Germplasm.View.GermplasmBrapiV1_3.class)
	public ResponseEntity<EntityListResponse<Germplasm>> getSearchGermplasm(
		@PathVariable final String crop, @PathVariable final String searchResultsDbid,
		@ApiParam(value = BrapiPagedResult.CURRENT_PAGE_DESCRIPTION, required = false)
		@RequestParam(value = "page",
			required = false) final Integer currentPage,
		@ApiParam(value = BrapiPagedResult.PAGE_SIZE_DESCRIPTION, required = false)
		@RequestParam(value = "pageSize",
			required = false) final Integer pageSize
	) {
		final GermplasmSearchRequestDto germplasmSearchRequestDTO;

		try {
			germplasmSearchRequestDTO =
				(GermplasmSearchRequestDto) this.searchRequestService
					.getSearchRequest(Integer.valueOf(searchResultsDbid), GermplasmSearchRequestDto.class);
		} catch (final NumberFormatException | MiddlewareException e) {
			return new ResponseEntity<>(
				new EntityListResponse<>(new Result<>(new ArrayList<Germplasm>())).withMessage("no search request found"),
				HttpStatus.NOT_FOUND);
		}

		final PagedResult<GermplasmDTO> resultPage = this.getGermplasmDTOPagedResult(germplasmSearchRequestDTO, currentPage, pageSize);

		final List<Germplasm> germplasmList = new ArrayList<>();

		if (resultPage.getPageResults() != null) {
			final ModelMapper mapper = new ModelMapper();
			for (final GermplasmDTO germplasmDTO : resultPage.getPageResults()) {
				final Germplasm germplasm = mapper.map(germplasmDTO, Germplasm.class);
				germplasm.setCommonCropName(crop);
				germplasmList.add(germplasm);
			}
		}

		final Result<Germplasm> results = new Result<Germplasm>().withData(germplasmList);
		final Pagination pagination = new Pagination().withPageNumber(resultPage.getPageNumber()).withPageSize(resultPage.getPageSize())
			.withTotalCount(resultPage.getTotalResults()).withTotalPages(resultPage.getTotalPages());

		final Metadata metadata = new Metadata().withPagination(pagination);

		final EntityListResponse<Germplasm> entityListResponse = new EntityListResponse<>(metadata, results);

		return new ResponseEntity<>(entityListResponse, HttpStatus.OK);

	}

	private PagedResult<GermplasmDTO> getGermplasmDTOPagedResult(final GermplasmSearchRequestDto germplasmSearchRequestDTO,
		final Integer currentPage, final Integer pageSize) {
		final Integer finalPageNumber = currentPage == null ? BrapiPagedResult.DEFAULT_PAGE_NUMBER : currentPage;
		final Integer finalPageSize = pageSize == null ? BrapiPagedResult.DEFAULT_PAGE_SIZE : pageSize;
		return new PaginatedSearch()
			.executeBrapiSearch(finalPageNumber, finalPageSize,
				new SearchSpec<GermplasmDTO>() {

					@Override
					public long getCount() {
						return GermplasmResourceBrapi.this.germplasmService.countGermplasmDTOs(germplasmSearchRequestDTO);
					}

					@Override
					public List<GermplasmDTO> getResults(final PagedResult<GermplasmDTO> pagedResult) {
						return GermplasmResourceBrapi.this.germplasmService
							.searchGermplasmDTO(germplasmSearchRequestDTO, finalPageNumber, finalPageSize);
					}
				});
	}

	@ApiOperation(value = "Search germplasms by study", notes = "Search germplasms by study")
	@RequestMapping(value = "/{crop}/brapi/v1/studies/{studyDbId}/germplasm", method = RequestMethod.GET)
	@ResponseBody
	@JsonView(Germplasm.View.GermplasmBrapiV1_3.class)
	public ResponseEntity<EntityListResponse<Germplasm>> searchGermplasmsByStudy(
		@PathVariable final String crop,
		@ApiParam(value = BrapiPagedResult.CURRENT_PAGE_DESCRIPTION, required = false)
		@RequestParam(value = "page",
			required = false) final Integer currentPage,
		@ApiParam(value = BrapiPagedResult.PAGE_SIZE_DESCRIPTION, required = false)
		@RequestParam(value = "pageSize",
			required = false) final Integer pageSize,
		@PathVariable final Integer studyDbId) {

		final PagedResult<GermplasmDTO> resultPage =
			new PaginatedSearch().executeBrapiSearch(currentPage, pageSize, new SearchSpec<GermplasmDTO>() {

				@Override
				public long getCount() {
					return GermplasmResourceBrapi.this.germplasmService.countGermplasmByStudy(studyDbId);
				}

				@Override
				public List<GermplasmDTO> getResults(final PagedResult<GermplasmDTO> pagedResult) {
					final Integer finalPageNumber = currentPage == null ? BrapiPagedResult.DEFAULT_PAGE_NUMBER : currentPage;
					final Integer finalPageSize = pageSize == null ? BrapiPagedResult.DEFAULT_PAGE_SIZE : pageSize;
					return GermplasmResourceBrapi.this.germplasmService
						.getGermplasmByStudy(studyDbId, finalPageSize, finalPageNumber);
				}
			});

		final List<Germplasm> germplasmList = new ArrayList<>();

		if (resultPage.getPageResults() != null) {
			final ModelMapper mapper = new ModelMapper();
			for (final GermplasmDTO germplasmDTO : resultPage.getPageResults()) {
				final Germplasm germplasm = mapper.map(germplasmDTO, Germplasm.class);
				germplasm.setCommonCropName(crop);
				germplasmList.add(germplasm);
			}
		}

		final Result<Germplasm> results = new Result<Germplasm>().withData(germplasmList);
		final Pagination pagination = new Pagination().withPageNumber(resultPage.getPageNumber()).withPageSize(resultPage.getPageSize())
			.withTotalCount(resultPage.getTotalResults()).withTotalPages(resultPage.getTotalPages());

		final Metadata metadata = new Metadata().withPagination(pagination);

		final EntityListResponse<Germplasm> entityListResponse = new EntityListResponse<>(metadata, results);

		return new ResponseEntity<>(entityListResponse, HttpStatus.OK);
	}
}
