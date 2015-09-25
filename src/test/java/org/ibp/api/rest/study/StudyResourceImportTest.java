
package org.ibp.api.rest.study;

import org.generationcp.middleware.manager.api.GermplasmListManager;
import org.generationcp.middleware.service.api.DataImportService;
import org.generationcp.middleware.service.api.FieldbookService;
import org.ibp.ApiUnitTestBase;
import org.ibp.api.domain.germplasm.GermplasmListEntrySummary;
import org.ibp.api.domain.study.StudyGermplasm;
import org.ibp.api.domain.study.StudyImportDTO;
import org.ibp.api.domain.study.Trait;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

public class StudyResourceImportTest extends ApiUnitTestBase {

	@Autowired
	private FieldbookService fieldbookService;

	@Autowired
	private GermplasmListManager germplasmListManager;

	@Autowired
	private DataImportService dataImportService;

	@Configuration
	public static class TestConfiguration {

		@Bean
		@Primary
		public FieldbookService fieldbookService() {
			return Mockito.mock(FieldbookService.class);
		}

		@Bean
		@Primary
		public GermplasmListManager germplasmListManager() {
			return Mockito.mock(GermplasmListManager.class);
		}

		@Bean
		@Primary
		public DataImportService dataImportService() {
			return Mockito.mock(DataImportService.class);
		}
	}

	@Test
	public void testImport() throws Exception {

		final StudyImportDTO inputDTO = new StudyImportDTO();
		inputDTO.setStudyType("N");
		inputDTO.setName("Maize Nursery");
		inputDTO.setObjective("Grow more seeds");
		inputDTO.setTitle("Maize Nursery title");
		inputDTO.setStartDate("20150101");
		inputDTO.setEndDate("20151201");
		inputDTO.setUserId(1);
		inputDTO.setFolderId(1L);
		inputDTO.setSiteName("CIMMYT");

		final Trait trait1 = new Trait(1, "Plant Height");
		final Trait trait2 = new Trait(2, "Grain Yield");
		inputDTO.setTraits(Lists.newArrayList(trait1, trait2));

		final StudyGermplasm g1 = new StudyGermplasm();
		g1.setEntryNumber(1);
		g1.setEntryType("Test");
		g1.setPosition("1");
		final GermplasmListEntrySummary g1Summary = new GermplasmListEntrySummary();
		g1Summary.setGid(1);
		g1Summary.setEntryCode("Entry Code 1");
		g1Summary.setSeedSource("Seed Source 1");
		g1Summary.setDesignation("Designation 1");
		g1Summary.setCross("Cross 1");
		g1.setGermplasmListEntrySummary(g1Summary);

		final StudyGermplasm g2 = new StudyGermplasm();
		g2.setEntryNumber(2);
		g2.setEntryType("Test");
		g2.setPosition("2");
		final GermplasmListEntrySummary g2Summary = new GermplasmListEntrySummary();
		g2Summary.setGid(2);
		g2Summary.setEntryCode("Entry Code 2");
		g2Summary.setSeedSource("Seed Source 2");
		g2Summary.setDesignation("Designation 2");
		g2Summary.setCross("Cross 2");
		g2.setGermplasmListEntrySummary(g2Summary);

		inputDTO.setGermplasms(Lists.newArrayList(g1, g2));

		final String inptJSON = new ObjectMapper().writeValueAsString(inputDTO);

		this.mockMvc.perform(
				MockMvcRequestBuilders.post("/study/{cropName}/import?programUUID={programUUID}", this.cropName, this.programUuid) //
				.contentType(this.contentType).content(inptJSON)) //
				.andExpect(MockMvcResultMatchers.status().is(HttpStatus.CREATED.value()));
	}
}
