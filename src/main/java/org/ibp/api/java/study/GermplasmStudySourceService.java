package org.ibp.api.java.study;

import org.generationcp.middleware.service.api.study.germplasm.source.GermplasmStudySourceDto;
import org.generationcp.middleware.service.api.study.germplasm.source.GermplasmStudySourceSearchRequest;

import java.util.List;

public interface GermplasmStudySourceService {

	public List<GermplasmStudySourceDto> getGermplasmStudySources(GermplasmStudySourceSearchRequest germplasmStudySourceSearchRequest);

	public long countGermplasmStudySources(GermplasmStudySourceSearchRequest germplasmStudySourceSearchRequest);

	public long countFilteredGermplasmStudySources(GermplasmStudySourceSearchRequest germplasmStudySourceSearchRequest);

}
