
package org.ibp.api.brapi.v1.program;

import com.jayway.jsonassert.impl.matcher.IsCollectionWithSize;
import org.apache.commons.lang.RandomStringUtils;
import org.generationcp.middleware.pojos.workbench.CropType;
import org.generationcp.middleware.pojos.workbench.Project;
import org.generationcp.middleware.pojos.workbench.WorkbenchUser;
import org.generationcp.middleware.service.api.program.ProgramDetailsDto;
import org.generationcp.middleware.service.api.program.ProgramSearchRequest;
import org.generationcp.middleware.service.api.user.UserService;
import org.hamcrest.Matchers;
import org.ibp.ApiUnitTestBase;
import org.ibp.api.brapi.v1.common.BrapiPagedResult;
import org.ibp.api.java.crop.CropService;
import org.ibp.api.java.impl.middleware.security.SecurityService;
import org.ibp.api.java.program.ProgramService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProgramResourceBrapiTest extends ApiUnitTestBase {

	private static final String WHEAT = "Wheat";
	private static final String RICE = "Rice";
	private static final String MAIZE = "Maize";
	private static final String INVALID_CROP = "maize";
	private static final String INVALID_BRAPI_V1_PROGRAMS = "/" + ProgramResourceBrapiTest.INVALID_CROP + "/brapi/v1/programs";
	private static final String MAIZE_BRAPI_V1_PROGRAMS = "/maize/brapi/v1/programs";
	final static String PROGRAM_UUID_RICE = "92c47f83-4427-44c9-982f-b611b8917a2d";
	final static String PROGRAM_UUID_WHEAT = "2ca55832-5c5d-404f-b05c-bc6e305c8b76";
	final static String PROGRAM_UUID_MAIZE = "d1a052d0-65eb-4d0e-8813-6c770d10f253";

	private List<CropType> crops = new ArrayList<>();

	@Autowired
	private UserService userService;

	@Autowired
	private SecurityService securityService;

	@Autowired
	private ProgramService programService;

	@Autowired
	private CropService cropService;

	@Before
	public void setup() {
		this.crops = this.getAllCrops();
		Mockito.when(this.workbenchDataManager.getInstalledCropDatabses()).thenReturn(this.crops);
		Mockito.when(this.workbenchDataManager.getAvailableCropsForUser(Mockito.anyInt())).thenReturn(this.crops);
		final WorkbenchUser user = new WorkbenchUser();
		user.setName(RandomStringUtils.randomAlphabetic(10));
		user.setUserid(Integer.parseInt(RandomStringUtils.randomNumeric(5)));
		Mockito.when(this.userService.getUserById(Mockito.anyInt())).thenReturn(user);
		Mockito.when(this.securityService.getCurrentlyLoggedInUser()).thenReturn(user);
		Mockito.when(this.cropService.getInstalledCrops())
			.thenReturn(this.crops.stream().map(CropType::getCropName).collect(Collectors.toList()));
		Mockito.when(this.cropService.getAvailableCropsForUser(Mockito.anyInt()))
			.thenReturn(this.crops.stream().map(CropType::getCropName).collect(Collectors.toList()));

	}

	@Test
	public void testListProgramsNoAdditionalInfo() throws Exception {
		final List<ProgramDetailsDto> programDetailsDtoList = this.getProgramDetails();
		Mockito.when(this.programService.countProgramsByFilter(org.mockito.Matchers.any(ProgramSearchRequest.class)))
			.thenReturn(new Long(this.crops.size()));
		final List<ProgramDetailsDto> projectList = this.getProgramDetailsList();
		Mockito.when(this.programService.getProgramDetailsByFilter(org.mockito.Mockito.any(Pageable.class),
			org.mockito.Matchers.any(ProgramSearchRequest.class))).thenReturn(projectList);

		final UriComponents uriComponents =
			UriComponentsBuilder.newInstance().path(ProgramResourceBrapiTest.MAIZE_BRAPI_V1_PROGRAMS).build().encode();
		this.mockMvc.perform(MockMvcRequestBuilders.get(uriComponents.toString()).contentType(this.contentType)) //
			.andExpect(MockMvcResultMatchers.status().isOk()) //
			.andDo(MockMvcResultHandlers.print()) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data", IsCollectionWithSize.hasSize(programDetailsDtoList.size()))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[0].programDbId",
				Matchers.is(ProgramResourceBrapiTest.PROGRAM_UUID_MAIZE))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[0].name", Matchers.is(ProgramResourceBrapiTest.MAIZE))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[1].programDbId",
				Matchers.is(ProgramResourceBrapiTest.PROGRAM_UUID_RICE))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[1].name", Matchers.is(ProgramResourceBrapiTest.RICE)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[2].programDbId",
				Matchers.is(ProgramResourceBrapiTest.PROGRAM_UUID_WHEAT))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[2].name", Matchers.is(ProgramResourceBrapiTest.WHEAT)))
			// Default starting page index is 0
			.andExpect(MockMvcResultMatchers.jsonPath("$.metadata.pagination.currentPage",
				Matchers.is(BrapiPagedResult.DEFAULT_PAGE_NUMBER)))
			// Default page size is 1000
			.andExpect(
				MockMvcResultMatchers.jsonPath("$.metadata.pagination.pageSize", Matchers.is(BrapiPagedResult.DEFAULT_PAGE_SIZE)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.metadata.pagination.totalCount", Matchers.is(this.crops.size()))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.metadata.pagination.totalPages", Matchers.is(1)));
	}

	@Test
	public void testListProgramsWithPaging() throws Exception {
		final List<ProgramDetailsDto> programDetailsDtoList = this.getProgramDetails();
		Mockito.when(this.programService.countProgramsByFilter(org.mockito.Matchers.any(ProgramSearchRequest.class)))
			.thenReturn(new Long(this.crops.size()));
		final List<ProgramDetailsDto> projectList = this.getProgramDetailsList();
		Mockito.when(this.programService.getProgramDetailsByFilter(org.mockito.Mockito.any(Pageable.class),
			org.mockito.Matchers.any(ProgramSearchRequest.class))).thenReturn(projectList);

		final int page = 1;
		final int pageSize = 2;
		final UriComponents uriComponents = UriComponentsBuilder.newInstance().path(ProgramResourceBrapiTest.MAIZE_BRAPI_V1_PROGRAMS)
			.queryParam("page", page).queryParam("pageSize", pageSize).build().encode();
		this.mockMvc.perform(MockMvcRequestBuilders.get(uriComponents.toString()).contentType(this.contentType)) //
			.andExpect(MockMvcResultMatchers.status().isOk()) //
			.andDo(MockMvcResultHandlers.print()) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data", IsCollectionWithSize.hasSize(programDetailsDtoList.size()))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[0].programDbId",
				Matchers.is(ProgramResourceBrapiTest.PROGRAM_UUID_MAIZE))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[0].name", Matchers.is(ProgramResourceBrapiTest.MAIZE))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[1].programDbId",
				Matchers.is(ProgramResourceBrapiTest.PROGRAM_UUID_RICE))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[1].name", Matchers.is(ProgramResourceBrapiTest.RICE)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[2].programDbId",
				Matchers.is(ProgramResourceBrapiTest.PROGRAM_UUID_WHEAT))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[2].name", Matchers.is(ProgramResourceBrapiTest.WHEAT)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.metadata.pagination.currentPage", Matchers.is(page)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.metadata.pagination.pageSize", Matchers.is(pageSize)))
			.andExpect(MockMvcResultMatchers.jsonPath("$.metadata.pagination.totalCount", Matchers.is(this.crops.size()))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.metadata.pagination.totalPages", Matchers.is(2)));
	}

	@Test
	public void testListProgramFilterByName() throws Exception {
		Mockito.when(this.programService.countProgramsByFilter(org.mockito.Matchers.any(ProgramSearchRequest.class)))
			.thenReturn(1L);

		final List<ProgramDetailsDto> programDetailsDtoList = new ArrayList<>();
		programDetailsDtoList
			.add(new ProgramDetailsDto(ProgramResourceBrapiTest.PROGRAM_UUID_RICE, ProgramResourceBrapiTest.RICE, null, null, null, null,
				null, null));
		final List<ProgramDetailsDto> projectList = new ArrayList<>();
		projectList.add(
			new ProgramDetailsDto(ProgramResourceBrapiTest.PROGRAM_UUID_RICE, ProgramResourceBrapiTest.RICE, null, null, null, null, null,
				null));

		Mockito.when(this.programService.getProgramDetailsByFilter(org.mockito.Mockito.any(Pageable.class),
			org.mockito.Matchers.any(ProgramSearchRequest.class))).thenReturn(projectList);

		final UriComponents uriComponents = UriComponentsBuilder.newInstance().path(ProgramResourceBrapiTest.MAIZE_BRAPI_V1_PROGRAMS)
			.queryParam("programName", ProgramResourceBrapiTest.RICE).build().encode();
		this.mockMvc.perform(MockMvcRequestBuilders.get(uriComponents.toString()).contentType(this.contentType)) //
			.andExpect(MockMvcResultMatchers.status().isOk()) //
			.andDo(MockMvcResultHandlers.print()) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data", IsCollectionWithSize.hasSize(programDetailsDtoList.size()))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[0].programDbId",
				Matchers.is(ProgramResourceBrapiTest.PROGRAM_UUID_RICE))) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.result.data[0].name", Matchers.is(ProgramResourceBrapiTest.RICE))) //
		;
	}

	@Test
	@Ignore
	public void testListProgramFilterByAbbreviation() throws Exception {
		Mockito.when(this.programService.countProgramsByFilter(org.mockito.Matchers.any(ProgramSearchRequest.class)))
			.thenReturn(1L);

		Mockito.when(this.programService.getProgramDetailsByFilter(org.mockito.Mockito.any(Pageable.class),
			org.mockito.Matchers.any(ProgramSearchRequest.class))).thenReturn(new ArrayList<>());

		final UriComponents uriComponents = UriComponentsBuilder.newInstance().path(ProgramResourceBrapiTest.MAIZE_BRAPI_V1_PROGRAMS)
			.queryParam("abbreviation", "AAAB").build().encode();
		this.mockMvc.perform(MockMvcRequestBuilders.get(uriComponents.toString()).contentType(this.contentType)) //
			.andExpect(MockMvcResultMatchers.status().isNotImplemented()) //
			.andDo(MockMvcResultHandlers.print()) //
			.andExpect(MockMvcResultMatchers.jsonPath("$.metadata.status[0].message", Matchers.is("Abbreviation is not yet supported")));

	}

	private List<ProgramDetailsDto> getProgramDetails() {
		final List<ProgramDetailsDto> programDetailsDtoList = new ArrayList<>();
		programDetailsDtoList
			.add(new ProgramDetailsDto(ProgramResourceBrapiTest.PROGRAM_UUID_MAIZE, ProgramResourceBrapiTest.MAIZE, null, null, null, null,
				null, null));
		programDetailsDtoList
			.add(new ProgramDetailsDto(ProgramResourceBrapiTest.PROGRAM_UUID_RICE, ProgramResourceBrapiTest.RICE, null, null, null, null,
				null, null));
		programDetailsDtoList
			.add(new ProgramDetailsDto(ProgramResourceBrapiTest.PROGRAM_UUID_WHEAT, ProgramResourceBrapiTest.WHEAT, null, null, null, null,
				null, null));
		return programDetailsDtoList;
	}

	private List<CropType> getAllCrops() {
		final List<CropType> cropTypes = new ArrayList<>();
		cropTypes.add(this.getCropType("maize", "ibdbv2_maize_merged", "4.0.0"));
		cropTypes.add(this.getCropType("rice", "ibdbv2_rice_merged", "4.0.0"));
		cropTypes.add(this.getCropType("wheat", "ibdbv2_wheat_merged", "4.0.0"));
		return cropTypes;
	}

	private CropType getCropType(final String cropName, final String dbName, final String version) {
		final CropType cropType = new CropType();
		cropType.setCropName(cropName);
		cropType.setDbName(dbName);
		cropType.setVersion(version);
		return cropType;
	}

	private List<Project> getProjectList() {
		final List<Project> projectList = new ArrayList<>();
		projectList.add(this.getProject(12L, ProgramResourceBrapiTest.PROGRAM_UUID_MAIZE, ProgramResourceBrapiTest.MAIZE));
		projectList.add(this.getProject(11L, ProgramResourceBrapiTest.PROGRAM_UUID_RICE, ProgramResourceBrapiTest.RICE));
		projectList.add(this.getProject(10L, ProgramResourceBrapiTest.PROGRAM_UUID_WHEAT, ProgramResourceBrapiTest.WHEAT));
		return projectList;
	}

	private List<ProgramDetailsDto> getProgramDetailsList() {
		final ProgramDetailsDto p1 =
			new ProgramDetailsDto(ProgramResourceBrapiTest.PROGRAM_UUID_MAIZE, ProgramResourceBrapiTest.MAIZE, null, null,
				null, null, null, null);
		final ProgramDetailsDto p2 =
			new ProgramDetailsDto(ProgramResourceBrapiTest.PROGRAM_UUID_RICE, ProgramResourceBrapiTest.RICE, null, null,
				null, null, null, null);
		final ProgramDetailsDto p3 =
			new ProgramDetailsDto(ProgramResourceBrapiTest.PROGRAM_UUID_WHEAT, ProgramResourceBrapiTest.WHEAT, null, null,
				null, null, null, null);
		return Arrays.asList(p1, p2, p3);
	}

	private Project getProject(final Long id, final String programUniqueID, final String projectName) {
		final Project project = new Project();
		project.setProjectId(id);
		project.setProjectName(projectName);
		project.setUniqueID(programUniqueID);
		project.setCropType(this.getCropType("maize", "ibdbv2_maize_merged", "4.0.0"));
		return project;
	}
}
