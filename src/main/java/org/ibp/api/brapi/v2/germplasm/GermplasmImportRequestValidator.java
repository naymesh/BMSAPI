package org.ibp.api.brapi.v2.germplasm;

import org.apache.commons.lang3.StringUtils;
import org.generationcp.middleware.api.brapi.v2.germplasm.GermplasmImportRequest;
import org.generationcp.middleware.api.brapi.v2.germplasm.Synonym;
import org.generationcp.middleware.api.breedingmethod.BreedingMethodSearchRequest;
import org.generationcp.middleware.api.breedingmethod.BreedingMethodService;
import org.generationcp.middleware.api.location.LocationService;
import org.generationcp.middleware.api.location.search.LocationSearchRequest;
import org.generationcp.middleware.pojos.Location;
import org.generationcp.middleware.util.Util;
import org.ibp.api.java.impl.middleware.common.validator.BaseValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class GermplasmImportRequestValidator {

	public static final Integer STOCK_ID_MAX_LENGTH = 35;
	public static final Integer REFERENCE_MAX_LENGTH = 255;
	public static final Integer NAME_MAX_LENGTH = 255;
	public static final Integer ATTRIBUTE_MAX_LENGTH = 255;

	private BindingResult errors;

	@Autowired
	private BreedingMethodService breedingMethodService;

	@Autowired
	private LocationService locationService;

	public BindingResult pruneGermplasmInvalidForImport(final List<GermplasmImportRequest> germplasmImportRequestDtoList) {
		BaseValidator.checkNotEmpty(germplasmImportRequestDtoList, "germplasm.import.list.null");
		this.errors = new MapBindingResult(new HashMap<String, String>(), GermplasmImportRequest.class.getName());

		final List<String> validBreedingMethodIds = this.getValidBreedingMethodDbIds(germplasmImportRequestDtoList);
		final List<String> validLocationAbbreviations = this.getValidLocationAbbreviations(germplasmImportRequestDtoList);

		final Map<GermplasmImportRequest, Integer> importRequestByIndexMap = IntStream.range(0, germplasmImportRequestDtoList.size())
			.boxed()
			.collect(Collectors.toMap(germplasmImportRequestDtoList::get, i -> i));
		germplasmImportRequestDtoList.removeIf( g -> {
			final Set<String> nameKeys = new HashSet<>();

			final Integer index = importRequestByIndexMap.get(g) + 1;
			if (StringUtils.isEmpty(g.getDefaultDisplayName())) {
				errors.reject("germplasm.create.null.name.types", new String[] {index.toString()}, "");
				return true;
			}
			if (StringUtils.isEmpty(g.getAcquisitionDate())) {
				errors.reject("germplasm.create.acquisition.date.null", new String[] {index.toString()}, "");
				return true;
			}
			if (Util.tryParseDate(g.getAcquisitionDate(), Util.FRONTEND_DATE_FORMAT) == null) {
				errors.reject("germplasm.create.acquisition.date.invalid.format", new String[] {index.toString()}, "");
				return true;
			}
			if (StringUtils.isEmpty(g.getBreedingMethodDbId())) {
				errors.reject("germplasm.create.breeding.method.null", new String[] {index.toString()}, "");
				return true;
			}
			if (!validBreedingMethodIds.contains(g.getBreedingMethodDbId())) {
				errors.reject("germplasm.create.breeding.method.invalid", new String[] {index.toString()}, "");
				return true;
			}
			if (StringUtils.isEmpty(g.getCountryOfOriginCode())) {
				errors.reject("germplasm.create.country.origin.null", new String[] {index.toString()}, "");
				return true;
			}

			if (!validLocationAbbreviations.contains(g.getCountryOfOriginCode())) {
				errors.reject("germplasm.create.country.origin.invalid", new String[] {index.toString()}, "");
				return true;
			}

			// Validations on names synonyms
			if (g.getSynonyms().stream().map(Synonym::getType).anyMatch(Objects::isNull)) {
				errors.reject("germplasm.create.null.name.types", new String[] {index.toString()}, "");
				return true;
			}
			g.getSynonyms().stream().map(Synonym::getType).forEach(name -> nameKeys.add(name.toUpperCase()));
			if (g.getSynonyms().size() != nameKeys.size()) {
				errors.reject("germplasm.create.duplicated.name.types", new String[] {index.toString()}, "");
				return true;
			}
			if (areNameValuesInvalid(g.getSynonyms().stream().map(Synonym::getSynonym).collect(Collectors.toList()))) {
				errors.reject("germplasm.create.name.exceeded.length", new String[] {index.toString(), "synonyms"}, "");
				return true;
			}

			// Validations on attributes
			if (areAttributesInvalid(g.getAdditionalInfo())) {
				errors.reject("germplasm.create.attribute.exceeded.length", new String[] {index.toString(), "additionalInfo"}, "");
				return true;
			}

			return false;
		});

		return this.errors;
	}

	private List<String> getValidBreedingMethodDbIds(final List<GermplasmImportRequest> germplasmImportRequestDtoList) {
		final List<Integer> breedingMethodIds =
			germplasmImportRequestDtoList.stream().filter(g -> StringUtils.isNotEmpty(g.getBreedingMethodDbId()))
				.map(g -> Integer.parseInt(g.getBreedingMethodDbId())).collect(Collectors.toList());
		final BreedingMethodSearchRequest searchRequest =
			new BreedingMethodSearchRequest(null, null, false);
		searchRequest.setMethodIds(breedingMethodIds);
		return
			this.breedingMethodService.getBreedingMethods(searchRequest, null).stream().map(m -> m.getMid().toString())
				.collect(Collectors.toList());

	}

	private List<String> getValidLocationAbbreviations(final List<GermplasmImportRequest> germplasmImportRequestDtoList) {
		final Set<String> locationAbbrs =
			germplasmImportRequestDtoList.stream().filter(g -> StringUtils.isNotEmpty(g.getCountryOfOriginCode()))
				.map(g -> g.getCountryOfOriginCode().toUpperCase()).collect(Collectors.toSet());

		return
			this.locationService
				.getFilteredLocations(new LocationSearchRequest(null, null, null, new ArrayList<>(locationAbbrs), null, false),
					null)
				.stream().map(
				Location::getLabbr).collect(
				Collectors.toList());

	}

	private boolean areNameValuesInvalid(final Collection<String> values) {
		return values.stream().anyMatch(n -> {
			if (StringUtils.isEmpty(n)) {
				return true;
			}
			if (n.length() > NAME_MAX_LENGTH) {
				return true;
			}
			return false;
		});
	}

	private boolean areAttributesInvalid(final Map<String, String> attributes) {
		if (attributes != null) {
			final Set<String> attributeKeys = new HashSet<>();

			if (attributes.keySet().stream().anyMatch(Objects::isNull)) {
				return true;
			}
			attributes.keySet().forEach(attr -> attributeKeys.add(attr.toUpperCase()));
			if (attributes.keySet().size() != attributeKeys.size()) {
				return true;
			}
			if (attributes.values().stream().anyMatch(n -> {
				if (StringUtils.isEmpty(n)) {
					return true;
				}
				if (n.length() > ATTRIBUTE_MAX_LENGTH) {
					return true;
				}
				return false;
			})) {
				return true;
			}
			return false;
		}
		return false;
	}

}
