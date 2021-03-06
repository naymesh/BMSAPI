package org.ibp.api.java.impl.middleware.location;

import org.generationcp.middleware.api.location.LocationDTO;
import org.generationcp.middleware.api.location.LocationTypeDTO;
import org.generationcp.middleware.api.location.search.LocationSearchRequest;
import org.ibp.api.domain.location.LocationDto;
import org.ibp.api.domain.location.LocationMapper;
import org.ibp.api.java.impl.middleware.location.validator.LocationSearchRequestValidator;
import org.ibp.api.java.location.LocationService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationServiceImpl implements LocationService {

	@Autowired
	private org.generationcp.middleware.api.location.LocationService locationMiddlewareService;

	@Autowired
	private LocationSearchRequestValidator locationSearchRequestValidator;

	@Override
	public LocationDTO getLocation(final Integer locationId) {
		return this.locationMiddlewareService.getLocation(locationId);
	}

	@Override
	public List<LocationTypeDTO> getLocationTypes() {
		return this.locationMiddlewareService.getLocationTypes();
	}

	@Override
	public long countLocations(final String crop, final LocationSearchRequest locationSearchRequest) {

		this.locationSearchRequestValidator.validate(crop, locationSearchRequest);

		return this.locationMiddlewareService
			.countFilteredLocations(locationSearchRequest);
	}

	@Override
	public List<LocationDto> getLocations(final String crop, final LocationSearchRequest locationSearchRequest, final Pageable pageable) {

		this.locationSearchRequestValidator.validate(crop, locationSearchRequest);

		final List<org.generationcp.middleware.pojos.Location> locations =
			this.locationMiddlewareService
				.getFilteredLocations(locationSearchRequest,
					pageable);

		final ModelMapper mapper = LocationMapper.getInstance();
		return locations.stream().map(o -> mapper.map(o, LocationDto.class)).collect(Collectors.toList());
	}
}
