package org.ibp.api.java.impl.middleware.ontology.validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.generationcp.middleware.domain.oms.CvId;
import org.generationcp.middleware.domain.oms.Property;
import org.generationcp.middleware.domain.oms.Term;
import org.generationcp.middleware.exceptions.MiddlewareException;
import org.generationcp.middleware.exceptions.MiddlewareQueryException;
import org.generationcp.middleware.service.api.OntologyManagerService;
import org.ibp.ApiUnitTestBase;
import org.ibp.api.domain.ontology.PropertyRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

public class PropertyRequestValidatorTest extends ApiUnitTestBase {

	@Configuration
	public static class TestConfiguration {

		@Bean
		@Primary
		public OntologyManagerService ontologyManagerService() {
			return Mockito.mock(OntologyManagerService.class);
		}

		@Bean
		@Primary
		public PropertyRequestValidator propertyRequestValidator() {
			return Mockito.mock(PropertyRequestValidator.class);
		}
	}

	@Autowired
	OntologyManagerService ontologyManagerService;

	@Autowired
	PropertyRequestValidator propertyRequestValidator;

	Integer cvId = CvId.PROPERTIES.getId();
	String propertyName = "MyProperty";
	String description = "Property Description";

	@Before
	public void reset() {
		Mockito.reset(this.ontologyManagerService);
	}

	@After
	public void validate() {
		Mockito.validateMockitoUsage();
	}

	/**
	 * Test for Name is required
	 * 
	 * @throws MiddlewareQueryException
	 */
	@Test
	public void testWithNullNameRequest() throws MiddlewareQueryException {

		Mockito.doReturn(null).when(this.ontologyManagerService)
				.getTermByNameAndCvId(this.propertyName, this.cvId);

		BindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(),
				"Property");

		PropertyRequest request = new PropertyRequest();
		request.setName("");
		request.setDescription(this.description);

		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertTrue(bindingResult.hasErrors());
		Assert.assertNotNull(bindingResult.getFieldError("name"));
	}

	/**
	 * Test for Name is unique
	 * 
	 * @throws MiddlewareQueryException
	 */
	@Test
	public void testWithUniqueNonNullPropertyName() throws MiddlewareQueryException {

		Mockito.doReturn(new Term(10, this.propertyName, this.description))
				.when(this.ontologyManagerService)
				.getTermByNameAndCvId(this.propertyName, this.cvId);

		BindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(),
				"Property");

		PropertyRequest request = new PropertyRequest();
		request.setName(this.propertyName);
		request.setDescription(this.description);

		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertTrue(bindingResult.hasErrors());
		Assert.assertNotNull(bindingResult.getFieldError("name"));
	}

	/**
	 * Test for should have at least one class and that is valid
	 * 
	 * @throws MiddlewareQueryException
	 */
	@Test
	public void testWithClassNameNonEmptyUniqueValues() throws MiddlewareQueryException {

		Mockito.doReturn(null).when(this.ontologyManagerService)
				.getTermByNameAndCvId(this.propertyName, this.cvId);

		PropertyRequest request = new PropertyRequest();
		request.setName(this.propertyName);
		request.setDescription(this.description);

		// Assert for no class defined
		BindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(),
				"Property");

		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertTrue(bindingResult.hasErrors());
		Assert.assertNotNull(bindingResult.getFieldError("classes"));

		// Assert for empty class is defined
		bindingResult = new MapBindingResult(new HashMap<String, String>(), "Property");

		request.setClasses(Collections.singletonList(""));
		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertTrue(bindingResult.hasErrors());
		Assert.assertNotNull(bindingResult.getFieldError("classes[0]"));

		// Assert for duplicate class names
		bindingResult = new MapBindingResult(new HashMap<String, String>(), "Property");

		request.setClasses(Arrays.asList("class", "class"));
		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertTrue(bindingResult.hasErrors());
		Assert.assertNotNull(bindingResult.getFieldError("classes"));
	}

	/**
	 * Test for Name cannot change if the property is already in use
	 * 
	 * @throws MiddlewareQueryException
	 */
	@Test
	public void testWithNonEditableRequest() throws MiddlewareQueryException, MiddlewareException {

		Term dbTerm = new Term(10, this.propertyName, this.description);
		Property toReturn = new Property(dbTerm);

		PropertyRequest request = new PropertyRequest();
		request.setId(10);
		request.setName(this.propertyName + "0");
		request.setDescription(this.description);

		Mockito.doReturn(dbTerm).when(this.ontologyManagerService)
				.getTermByNameAndCvId(this.propertyName, this.cvId);
		Mockito.doReturn(true).when(this.ontologyManagerService).isTermReferred(request.getId());
		Mockito.doReturn(toReturn).when(this.ontologyManagerService).getProperty(request.getId());

		BindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(),
				"Property");

		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertTrue(bindingResult.hasErrors());
	}

	/**
	 * Test for to check name length not exceed 200 characters
	 * 
	 * @throws MiddlewareQueryException
	 */
	@Test
	public void testWithNameLengthExceedMaxLimit() throws MiddlewareQueryException {

		Mockito.doReturn(null).when(this.ontologyManagerService)
				.getTermByNameAndCvId(this.propertyName, this.cvId);

		BindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(),
				"Property");

		PropertyRequest request = new PropertyRequest();
		request.setName(this.randomString(205));
		request.setDescription(this.description);

		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertTrue(bindingResult.hasErrors());
		Assert.assertNotNull(bindingResult.getFieldError("name"));
	}

	/**
	 * Test for to check description length not exceed 255 characters
	 * 
	 * @throws MiddlewareQueryException
	 */
	@Test
	public void testWithDescriptionLengthExceedMaxLimit() throws MiddlewareQueryException {

		Mockito.doReturn(null).when(this.ontologyManagerService)
				.getTermByNameAndCvId(this.propertyName, this.cvId);

		BindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(),
				"Property");

		PropertyRequest request = new PropertyRequest();
		request.setName(this.propertyName);
		request.setDescription(this.randomString(260));

		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertTrue(bindingResult.hasErrors());
		Assert.assertNotNull(bindingResult.getFieldError("description"));
	}

	/**
	 * Test for valid request
	 * 
	 * @throws MiddlewareQueryException
	 */
	@Test
	public void testWithValidRequest() throws MiddlewareQueryException {

		Mockito.doReturn(null).when(this.ontologyManagerService)
				.getTermByNameAndCvId(this.propertyName, this.cvId);
		BindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(),
				"Property");

		PropertyRequest request = new PropertyRequest();
		request.setName(this.propertyName);
		request.setDescription(this.description);
		request.setClasses(Arrays.asList("Class1", "Class2"));

		this.propertyRequestValidator.validate(request, bindingResult);
		Assert.assertFalse(bindingResult.hasErrors());
	}
}