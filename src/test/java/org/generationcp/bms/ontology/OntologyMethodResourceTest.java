package org.generationcp.bms.ontology;

import org.generationcp.bms.ApiUnitTestBase;
import org.generationcp.bms.ontology.builders.MethodBuilder;
import org.generationcp.bms.ontology.dto.MethodRequest;
import org.generationcp.middleware.domain.oms.Term;
import org.generationcp.middleware.domain.oms.Method;
import org.generationcp.middleware.service.api.OntologyService;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.ArrayList;

import org.mockito.Mockito;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


public class OntologyMethodResourceTest extends ApiUnitTestBase {

    @Configuration
    public static class TestConfiguration {

        @Bean
        @Primary
        public OntologyService ontologyService() {
            return Mockito.mock(OntologyService.class);
        }
    }

    @Autowired
    private OntologyService ontologyService;

    @Before
    public void reset(){
        Mockito.reset(ontologyService);
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }

    @Test
    public void listAllMethods() throws Exception {

        String cropName = "maize";

        List<Method> methodList = new ArrayList<>();
        methodList.add(new MethodBuilder().build(1, "m1", "d1"));
        methodList.add(new MethodBuilder().build(2, "m2", "d2"));
        methodList.add(new MethodBuilder().build(3, "m3", "d3"));

        Mockito.doReturn(methodList).when(ontologyService).getAllMethods();

        mockMvc.perform(get("/ontology/{cropname}/methods", cropName).contentType(contentType)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(methodList.size())))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is(methodList.get(0).getName())))
                .andExpect(jsonPath("$[0].description", is(methodList.get(0).getDefinition())))
                .andDo(print());

        verify(ontologyService, times(1)).getAllMethods();
    }

    /**
     * Get a method with id. It should respond with 200 and method data.
     * * *
     * @throws Exception
     */
    @Test
    public void getMethodById() throws Exception{

        String cropName = "maize";
        Method method = new MethodBuilder().build(1, "m1", "d1");

        Mockito.doReturn(method).when(ontologyService).getMethod(1);

        //TODO: check editable and deletable fields.
        mockMvc.perform(get("/ontology/{cropname}/methods/{id}",cropName, 1).contentType(contentType)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is(method.getName())))
                .andExpect(jsonPath("$.description", is(method.getDefinition())))
                .andDo(print());

        verify(ontologyService, times(1)).getMethod(1);
    }

    /**
     * This test should expect 400
     * * *
     * @throws Exception
     */
    @Test
    public void getMethodById_Should_Respond_With_400_For_Invalid_Id() throws Exception{

        String cropName = "maize";

        mockMvc.perform(get("/ontology/{cropname}/methods/{id}",cropName, 1).contentType(contentType))
                .andExpect(status().isBadRequest())
                .andDo(print());

        verify(ontologyService, times(1)).getMethod(1);
    }

    /**
     * This test should expect 201 : Created*
     * @throws Exception
     */
    @Test
    public void addMethod() throws Exception {

        String cropName = "maize";

        MethodRequest methodDTO = new MethodRequest();
        methodDTO.setName("methodName");
        methodDTO.setDescription("methodDescription");

        Method method = new Method(new Term(10, methodDTO.getName(), methodDTO.getDescription()));

        Mockito.doReturn(method).when(ontologyService).addMethod(methodDTO.getName(), methodDTO.getDescription());

        mockMvc.perform(post("/ontology/{cropname}/methods",cropName)
                .contentType(contentType).content(convertObjectToByte(methodDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andDo(print());

        verify(ontologyService, times(1)).addMethod(methodDTO.getName(), methodDTO.getDescription());
    }

    /**
     * This test should expect 204 : No Content
     * @throws Exception
     */
    @Test
    public void updateMethod() throws Exception {

        String cropName = "maize";

        MethodRequest methodDTO = new MethodRequest();
        methodDTO.setName("methodName");
        methodDTO.setDescription("methodDescription");

        Method method = new Method(new Term(10, methodDTO.getName(), methodDTO.getDescription()));

        /**
         * We Need equals method inside Method (Middleware) because it throws hashcode matching error.
         * So Added ArgumentCaptor that will implement equals()
         */
        ArgumentCaptor<Method> captor = ArgumentCaptor.forClass(Method.class);

        Mockito.doNothing().when(ontologyService).updateMethod(any(Method.class));

        mockMvc.perform(put("/ontology/{cropname}/methods/{id}", cropName, method.getId())
                .contentType(contentType).content(convertObjectToByte(methodDTO)))
                .andExpect(status().isNoContent())
                .andDo(print());

        verify(ontologyService).updateMethod(captor.capture());

        Method captured = captor.getValue();

        assertEquals(method.getName(), captured.getName());
        assertEquals(method.getDefinition(), captured.getDefinition());
    }

    /**
     * This test should expect 204 : No Content
     * @throws Exception
     */
    @Test
    public void deleteMethod() throws Exception {

        String cropName = "maize";

        MethodRequest methodDTO = new MethodRequest();
        methodDTO.setName("methodName");
        methodDTO.setDescription("methodDescription");

        Method method = new Method(new Term(10, methodDTO.getName(), methodDTO.getDescription()));

        Mockito.doNothing().when(ontologyService).deleteMethod(method.getId());

        mockMvc.perform(delete("/ontology/{cropname}/methods/{id}", cropName, method.getId())
                .contentType(contentType))
                .andExpect(status().isNoContent())
                .andDo(print());

        verify(ontologyService, times(1)).deleteMethod(method.getId());
    }
}
