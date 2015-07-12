
package org.ibp.api.rest.program;

import java.util.ArrayList;
import java.util.List;

import org.generationcp.middleware.pojos.workbench.CropType;
import org.generationcp.middleware.pojos.workbench.Project;
import org.hamcrest.Matchers;
import org.ibp.ApiUnitTestBase;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class ProgramResourceTest extends ApiUnitTestBase {

	@Test
	public void listAllMethods() throws Exception {

		CropType cropType = new CropType();
		cropType.setCropName("MAIZE");

		List<Project> projectList = new ArrayList<>();
		Project project = new Project();
		project.setProjectId(1L);
		project.setProjectName("projectName");
		project.setCropType(cropType);
		project.setUniqueID("123-456");
		project.setUserId(1);

		projectList.add(project);

		Mockito.doReturn(projectList).when(this.workbenchDataManager).getProjects();

		this.mockMvc.perform(MockMvcRequestBuilders.get("/program/list").contentType(this.contentType))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].projectName", Matchers.is(projectList.get(0).getProjectName())))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].uniqueID", Matchers.is(projectList.get(0).getUniqueID())))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].userId", Matchers.is(String.valueOf(projectList.get(0).getUserId()))))
				.andDo(MockMvcResultHandlers.print());

		Mockito.verify(this.workbenchDataManager, Mockito.times(1)).getProjects();
	}
}
