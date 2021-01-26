package org.ibp.api.java.impl.middleware.common.validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.generationcp.commons.util.DateUtil;
import org.generationcp.middleware.api.attribute.AttributeDTO;
import org.generationcp.middleware.api.breedingmethod.BreedingMethodDTO;
import org.generationcp.middleware.api.breedingmethod.BreedingMethodService;
import org.generationcp.middleware.api.location.LocationService;
import org.generationcp.middleware.api.location.search.LocationSearchRequest;
import org.generationcp.middleware.api.nametype.GermplasmNameTypeDTO;
import org.generationcp.middleware.domain.germplasm.GermplasmUpdateDTO;
import org.generationcp.middleware.pojos.Germplasm;
import org.generationcp.middleware.pojos.Location;
import org.ibp.api.java.germplasm.GermplasmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GermplasmUpdateValidator {

	public static final String PROGENITOR_1 = "PROGENITOR 1";
	public static final String PROGENITOR_2 = "PROGENITOR 2";
	@Autowired
	private GermplasmService germplasmService;

	@Autowired
	private org.generationcp.middleware.api.germplasm.GermplasmService germplasmMiddlewareService;

	@Autowired
	private LocationService locationService;

	@Autowired
	private BreedingMethodService breedingMethodService;

	public void validateEmptyList(final BindingResult errors, final List<GermplasmUpdateDTO> germplasmUpdateDTOList) {
		if (germplasmUpdateDTOList == null || germplasmUpdateDTOList.isEmpty()) {
			errors.reject("germplasm.update.empty.list", "");
		}
	}

	public void validateAttributeAndNameCodes(final BindingResult errors, final List<GermplasmUpdateDTO> germplasmUpdateDTOList) {

		final Set<String> nameCodes = new HashSet<>();
		germplasmUpdateDTOList
			.forEach(g -> nameCodes.addAll(g.getNames().keySet().stream().map(n -> n.toUpperCase()).collect(Collectors.toList())));
		nameCodes.addAll(
			germplasmUpdateDTOList.stream()
				.map(o -> StringUtils.isNotEmpty(o.getPreferredNameType()) ? o.getPreferredNameType().toUpperCase() : null)
				.filter(Objects::nonNull).collect(
				Collectors.toSet()));
		final Set<String> existingNamesCodes =
			this.germplasmService.filterGermplasmNameTypes(nameCodes).stream().map(GermplasmNameTypeDTO::getCode).collect(
				Collectors.toSet());

		final Set<String> attributesCodes = new HashSet<>();
		germplasmUpdateDTOList.stream().filter(germ -> germ.getAttributes() != null).collect(Collectors.toList())
			.forEach(
				g -> attributesCodes.addAll(g.getAttributes().keySet().stream().map(n -> n.toUpperCase()).collect(Collectors.toList())));
		final Set<String> existingAttributesCodes =
			this.germplasmService.filterGermplasmAttributes(attributesCodes).stream().map(AttributeDTO::getCode).collect(
				Collectors.toSet());

		if (!nameCodes.equals(existingNamesCodes)) {
			errors.reject("germplasm.update.invalid.name.code", new String[] {
				String.join(",", nameCodes.stream().filter((name) -> !existingNamesCodes.contains(name)).collect(
					Collectors.toList()))}, "");
		}
		if (!attributesCodes.equals(existingAttributesCodes)) {
			errors.reject("germplasm.update.invalid.attribute.code", new String[] {
				String.join(",", attributesCodes.stream().filter((attribute) -> !existingAttributesCodes.contains(attribute)).collect(
					Collectors.toList()))}, "");
		}
		final List<String> ambiguosCodes = new ArrayList<>(CollectionUtils.intersection(existingNamesCodes, existingAttributesCodes));
		if (!ambiguosCodes.isEmpty()) {
			errors.reject("germplasm.update.code.is.defined.as.name.and.attribute", new String[] {String.join(",", ambiguosCodes)}, "");
		}

	}

	public void validateGermplasmIdAndGermplasmUUID(final BindingResult errors, final List<GermplasmUpdateDTO> germplasmUpdateDTOList) {
		// Find rows (GermplasmUpdateDTO) with blank GID and UUID.
		final Optional<GermplasmUpdateDTO> germplasmWithNoIdentifier =
			germplasmUpdateDTOList.stream().filter(dto -> StringUtils.isEmpty(dto.getGermplasmUUID()) && dto.getGid() == null).findAny();
		if (germplasmWithNoIdentifier.isPresent()) {
			errors.reject("germplasm.update.missing.gid.and.uuid", "");
		}

		final Set<Integer> gids =
			germplasmUpdateDTOList.stream().map(dto -> dto.getGid()).filter(Objects::nonNull).collect(Collectors.toSet());
		final Set<String> germplasmUUIDs =
			germplasmUpdateDTOList.stream().map(dto -> StringUtils.isNotEmpty(dto.getGermplasmUUID()) ? dto.getGermplasmUUID() : null)
				.filter(
					Objects::nonNull).collect(Collectors.toSet());

		final List<Germplasm> germplasmByGIDs = this.germplasmMiddlewareService.getGermplasmByGIDs(new ArrayList<>(gids));
		final List<Germplasm> germplasmByUUIDs = this.germplasmMiddlewareService.getGermplasmByGUIDs(new ArrayList<>(germplasmUUIDs));

		final Set<Integer> existingGids = germplasmByGIDs.stream().map(Germplasm::getGid).collect(Collectors.toSet());
		if (!gids.equals(existingGids)) {
			errors.reject("germplasm.update.invalid.gid", new String[] {
				String.join(",", gids.stream().filter((gid) -> !existingGids.contains(gid)).map(o -> String.valueOf(o)).collect(
					Collectors.toSet()))}, "");
		}
		final Set<String> existingUUIDs = germplasmByUUIDs.stream().map(Germplasm::getGermplasmUUID).collect(Collectors.toSet());
		if (!germplasmUUIDs.equals(germplasmByUUIDs.stream().map(Germplasm::getGermplasmUUID).collect(Collectors.toSet()))) {
			errors.reject("germplasm.update.invalid.uuid", new String[] {
				String.join(",", germplasmUUIDs.stream().filter((uuid) -> !existingUUIDs.contains(uuid)).collect(
					Collectors.toList()))}, "");
		}

	}

	public void validateLocationAbbreviation(final BindingResult errors, final String programUUID,
		final List<GermplasmUpdateDTO> germplasmUpdateDTOList) {

		final Set<String> locationAbbrs =
			germplasmUpdateDTOList.stream().filter(dto -> StringUtils.isNotEmpty(dto.getLocationAbbreviation()))
				.map(dto -> dto.getLocationAbbreviation().toUpperCase()).collect(Collectors.toSet());

		final List<String> abbreviations =
			this.locationService
				.getFilteredLocations(new LocationSearchRequest(programUUID, null, null, new ArrayList<>(locationAbbrs), null, false), null)
				.stream()
				.map(Location::getLabbr).collect(
				Collectors.toList());

		locationAbbrs.removeAll(abbreviations);

		if (!locationAbbrs.isEmpty()) {
			errors.reject("germplasm.update.invalid.location.abbreviation", new String[] {String.join(",", locationAbbrs)}, "");
		}

	}

	public void validateBreedingMethod(final BindingResult errors, final String programUUID,
		final List<GermplasmUpdateDTO> germplasmUpdateDTOList) {

		final Set<String> breedingMethodsAbbrs =
			germplasmUpdateDTOList.stream().filter(dto -> StringUtils.isNotEmpty(dto.getBreedingMethodAbbr()))
				.map(dto -> dto.getBreedingMethodAbbr()).collect(Collectors.toSet());

		final List<String> abbreviations = this.breedingMethodService.getBreedingMethods(programUUID, breedingMethodsAbbrs, false).stream()
			.map(BreedingMethodDTO::getCode).collect(
				Collectors.toList());

		breedingMethodsAbbrs.removeAll(abbreviations);

		if (!breedingMethodsAbbrs.isEmpty()) {
			errors.reject("germplasm.update.invalid.breeding.method", new String[] {String.join(",", breedingMethodsAbbrs)}, "");
		}

	}

	public void validateCreationDate(final BindingResult errors, final List<GermplasmUpdateDTO> germplasmUpdateDTOList) {

		final Optional<GermplasmUpdateDTO> optionalGermplasmUpdateDTOWithInvalidDate =
			germplasmUpdateDTOList.stream()
				.filter(o -> StringUtils.isNotEmpty(o.getCreationDate()) && !DateUtil.isValidDate(o.getCreationDate())).findAny();

		if (optionalGermplasmUpdateDTOWithInvalidDate.isPresent()) {
			errors.reject("germplasm.update.invalid.creation.date", "");
		}

	}

	public void validateProgenitorsGids(final BindingResult errors, final List<GermplasmUpdateDTO> germplasmUpdateDTOList) {

		try {
			final Set<Integer> progenitorGids =
				germplasmUpdateDTOList.stream().map(dto -> dto.getProgenitors().values()).flatMap(Collection::stream)
					.filter(value -> StringUtils.isNotEmpty(value)).map(value -> Integer.parseInt(value))
					.collect(Collectors.toSet());
			final List<Germplasm> germplasmByGIDs = this.germplasmMiddlewareService.getGermplasmByGIDs(new ArrayList<>(progenitorGids));
			final Set<Integer> existingGids = germplasmByGIDs.stream().map(Germplasm::getGid).collect(Collectors.toSet());
			if (!progenitorGids.equals(existingGids)) {
				errors.reject("germplasm.update.invalid.progenitors.gids", new String[] {
					String.join(",",
						progenitorGids.stream().filter((gid) -> !existingGids.contains(gid)).map(o -> String.valueOf(o)).collect(
							Collectors.toSet()))}, "");
			}
		} catch (final NumberFormatException numberFormatException) {
			errors.reject("germplasm.update.invalid.progenitors.should.be.numeric", "");
		}

	}

	public void validateProgenitorsBothMustBeSpecified(final BindingResult errors, final List<GermplasmUpdateDTO> germplasmUpdateDTOList) {
		final long
			progenitorsWithEmptyValuesCount =
			germplasmUpdateDTOList.stream().filter(dto -> {
				final String progenitor1 = dto.getProgenitors().getOrDefault(PROGENITOR_1, null);
				final String progenitor2 = dto.getProgenitors().getOrDefault(PROGENITOR_2, null);
				return (StringUtils.isNotEmpty(progenitor1) && StringUtils.isEmpty(progenitor2)) || (StringUtils.isEmpty(progenitor1)
					&& StringUtils.isNotEmpty(progenitor2));
			}).count();
		if (progenitorsWithEmptyValuesCount > 0) {
			errors.reject("germplasm.update.invalid.progenitors", "");
		}

	}

}
