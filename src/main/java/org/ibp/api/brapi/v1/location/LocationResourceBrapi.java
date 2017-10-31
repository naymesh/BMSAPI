
package org.ibp.api.brapi.v1.location;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.generationcp.middleware.manager.api.LocationDataManager;
import org.generationcp.middleware.service.api.location.LocationDetailsDto;
import org.generationcp.middleware.service.api.location.LocationFilters;
import org.ibp.api.brapi.v1.common.Metadata;
import org.ibp.api.brapi.v1.common.Pagination;
import org.ibp.api.brapi.v1.common.Result;
import org.ibp.api.domain.common.PagedResult;
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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * BMS implementation of the <a href="http://docs.brapi.apiary.io/">BrAPI</a> Location services.
 *
 * @author Naymesh Mistry
 *
 */
@Api(value = "BrAPI Location Services")
@Controller
public class LocationResourceBrapi {

	@Autowired
	private LocationDataManager locationDataManager;

	@ApiOperation(value = "List locations", notes = "Get a list of locations.")
	@RequestMapping(value = "/{crop}/brapi/v1/locations", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Locations> listLocations(@PathVariable final String crop,
			@ApiParam(value = PagedResult.CURRENT_PAGE_DESCRIPTION,
					required = false) @RequestParam(value = Pagination.CURRENT_PAGE, required = false) Integer currentPage,
			@ApiParam(value = PagedResult.PAGE_SIZE_DESCRIPTION,
					required = false) @RequestParam(value = Pagination.PAGE_SIZE, required = false) Integer pageSize,
			@ApiParam(value = "name of location type", required = false) @RequestParam(value = "locationType",
					required = false) String locationType) {

		final Map<LocationFilters, Object> filters = new EnumMap<>(LocationFilters.class);
		PagedResult<LocationDetailsDto> resultPage = null;
		final boolean validation = this.validateParameter(locationType, filters);

		if (validation) {
			resultPage = new PaginatedSearch().execute(currentPage, pageSize, new SearchSpec<LocationDetailsDto>() {

				@Override
				public long getCount() {
					return locationDataManager.countLocationsByFilter(filters);
				}

				@Override
				public List<LocationDetailsDto> getResults(PagedResult<LocationDetailsDto> pagedResult) {
					return locationDataManager.getLocationsByFilter(pagedResult.getPageNumber(), pagedResult.getPageSize(), filters);
				}
			});
		}

		if (resultPage!= null && resultPage.getTotalResults() > 0) {
			
			final ModelMapper mapper = LocationMapper.getInstance();
			final List<Location> locations = new ArrayList<>();

			for (final LocationDetailsDto locationDetailsDto : resultPage.getPageResults()) {
				final Location location = mapper.map(locationDetailsDto, Location.class);
				locations.add(location);
			}

			final Result<Location> results = new Result<Location>().withData(locations);
			final Pagination pagination = new Pagination().withPageNumber(resultPage.getPageNumber()).withPageSize(resultPage.getPageSize())
					.withTotalCount(resultPage.getTotalResults()).withTotalPages(resultPage.getTotalPages());

			final Metadata metadata = new Metadata().withPagination(pagination);
			final Locations locationList = new Locations().withMetadata(metadata).withResult(results);
			return new ResponseEntity<>(locationList, HttpStatus.OK);
			
		} else {

			final Map<String, String> status = new HashMap<>();
			status.put("message", "not found locations");
			final Metadata metadata = new Metadata(null, status);
			final Locations locationList = new Locations().withMetadata(metadata);
			return new ResponseEntity<>(locationList, HttpStatus.NOT_FOUND);
		}
	}

	private boolean validateParameter(final String locationType, final Map<LocationFilters, Object> filters) {
		if (!StringUtils.isBlank(locationType)) {
			final Integer locationTypeId = this.locationDataManager
				.getUserDefinedFieldIdOfName(org.generationcp.middleware.pojos.UDTableType.LOCATION_LTYPE, locationType);
			if (locationTypeId != null) {
				filters.put(LocationFilters.LOCATION_TYPE, locationTypeId.toString());
			} else {
				return false;
			}
		}
		return true;
	}
}
