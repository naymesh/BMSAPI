package org.ibp.api.java.impl.middleware.inventory;

import com.google.common.collect.Lists;
import org.generationcp.middleware.domain.inventory.manager.LotDto;
import org.generationcp.middleware.domain.inventory.manager.LotGeneratorInputDto;
import org.generationcp.middleware.domain.oms.TermId;
import org.hamcrest.CoreMatchers;
import org.ibp.api.domain.ontology.VariableDetails;
import org.ibp.api.domain.ontology.VariableFilter;
import org.ibp.api.exception.ConflictException;
import org.ibp.api.java.impl.middleware.common.validator.InventoryScaleValidator;
import org.ibp.api.java.ontology.VariableService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

public class InventoryScaleValidatorTest {

	@InjectMocks
	private InventoryScaleValidator inventoryScaleValidator;

	@Mock
	private VariableService variableService;

	private LotGeneratorInputDto lotGeneratorInputDto;

	private BindingResult errors;

	@Before
	public void setup() {
		this.lotGeneratorInputDto = new LotGeneratorInputDto();
		MockitoAnnotations.initMocks(this);
	}

	@Test(expected = ConflictException.class)
	public void testValidateNotNullInventoryScaleId() {
		this.errors = new MapBindingResult(new HashMap<String, String>(), LotDto.class.getName());
		this.lotGeneratorInputDto.setGid(1);
		this.lotGeneratorInputDto.setLocationId(6000);
		this.lotGeneratorInputDto.setGenerateStock(false);
		final Integer scaleId = null;
		this.lotGeneratorInputDto.setScaleId(scaleId);
		this.lotGeneratorInputDto.setStockId("ABCD");
		this.lotGeneratorInputDto.setComments("Comments");
		final VariableFilter variableFilter = new VariableFilter();
		variableFilter.addPropertyId(TermId.INVENTORY_AMOUNT_PROPERTY.getId());
		Mockito.when(this.variableService.getVariablesByFilter(variableFilter)).thenReturn(Lists.newArrayList());
		this.inventoryScaleValidator.validateNotNullInventoryScaleId(this.errors, scaleId);

		Assert.assertTrue(this.errors.getAllErrors().size() == 1);
		final ObjectError objectError = this.errors.getAllErrors().get(0);
		assertThat(Arrays.asList(objectError.getCodes()), CoreMatchers.hasItem("inventory.scale.required"));
	}

	@Test
	public void testValidateInventoryScaleId() {
		this.errors = new MapBindingResult(new HashMap<String, String>(), LotDto.class.getName());
		this.lotGeneratorInputDto.setGid(1);
		this.lotGeneratorInputDto.setLocationId(6000);
		this.lotGeneratorInputDto.setGenerateStock(false);
		final int scaleId = 1;
		this.lotGeneratorInputDto.setScaleId(scaleId);
		this.lotGeneratorInputDto.setStockId("ABCD");
		this.lotGeneratorInputDto.setComments("Comments");
		final VariableFilter variableFilter = new VariableFilter();
		variableFilter.addPropertyId(TermId.INVENTORY_AMOUNT_PROPERTY.getId());
		Mockito.when(this.variableService.getVariablesByFilter(variableFilter)).thenReturn(Lists.newArrayList());
		this.inventoryScaleValidator.validateInventoryScaleId(this.errors, scaleId);

		Assert.assertTrue(this.errors.getAllErrors().size() == 1);
		final ObjectError objectError = this.errors.getAllErrors().get(0);
		assertThat(Arrays.asList(objectError.getCodes()), CoreMatchers.hasItem("inventory.scale.invalid"));
	}

	@Test
	public void testValidateValidInventoryScaleId() {
		this.errors = new MapBindingResult(new HashMap<String, String>(), LotDto.class.getName());
		this.lotGeneratorInputDto.setGid(1);
		this.lotGeneratorInputDto.setLocationId(6000);
		this.lotGeneratorInputDto.setGenerateStock(false);
		final int scaleId = 1;
		this.lotGeneratorInputDto.setScaleId(scaleId);
		this.lotGeneratorInputDto.setStockId("ABCD");
		this.lotGeneratorInputDto.setComments("Comments");
		final VariableFilter variableFilter = new VariableFilter();
		variableFilter.addPropertyId(TermId.INVENTORY_AMOUNT_PROPERTY.getId());
		final VariableDetails variableDetail = new VariableDetails();
		variableDetail.setId(String.valueOf(scaleId));

		final List<VariableDetails> variables = Lists.newArrayList(variableDetail);

		Mockito.when(this.variableService.getVariablesByFilter(variableFilter)).thenReturn(variables);
		this.inventoryScaleValidator.validateInventoryScaleId(this.errors, scaleId);

		Assert.assertTrue(this.errors.getAllErrors().size() == 0);
	}
}
