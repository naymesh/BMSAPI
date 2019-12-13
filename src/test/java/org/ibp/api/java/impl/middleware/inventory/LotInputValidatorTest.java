package org.ibp.api.java.impl.middleware.inventory;

import org.generationcp.middleware.domain.inventory.manager.LotGeneratorInputDto;
import org.generationcp.middleware.service.api.inventory.LotService;
import org.ibp.api.exception.ApiRequestValidationException;
import org.ibp.api.java.germplasm.GermplasmService;
import org.ibp.api.java.impl.middleware.common.validator.GermplasmValidator;
import org.ibp.api.java.impl.middleware.common.validator.InventoryScaleValidator;
import org.ibp.api.java.impl.middleware.common.validator.LocationValidator;
import org.ibp.api.java.impl.middleware.inventory.manager.validator.LotInputValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LotInputValidatorTest {

	@InjectMocks
	private LotInputValidator lotInputValidator;

	@Mock
	private LocationValidator locationValidator;

	@Mock
	private InventoryScaleValidator inventoryScaleValidator;

	@Mock
	private GermplasmService germplasmService;

	@Mock
	private LotService lotService;

	private LotGeneratorInputDto lotGeneratorInputDto;

	@Mock
	private GermplasmValidator germplasmValidator;

	@Before
	public void setup() {
		this.lotGeneratorInputDto = new LotGeneratorInputDto();
	}

	@Test(expected = ApiRequestValidationException.class)
	public void testValidateDataComments() {
		this.lotGeneratorInputDto.setGid(1);
		this.lotGeneratorInputDto.setLocationId(6000);
		this.lotGeneratorInputDto.setGenerateStock(false);
		this.lotGeneratorInputDto.setScaleId(8264);
		this.lotGeneratorInputDto.setStockId("ABCD");
		this.lotGeneratorInputDto.setComments(
			"CommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsCommentsComments");

		this.lotInputValidator.validate(this.lotGeneratorInputDto);
	}

	@Test(expected = ApiRequestValidationException.class)
	public void testValidateDataGenerateStockNull() {
		this.lotGeneratorInputDto.setGid(1);
		this.lotGeneratorInputDto.setLocationId(6000);
		this.lotGeneratorInputDto.setScaleId(8264);
		this.lotGeneratorInputDto.setStockId("ABCD");
		this.lotGeneratorInputDto.setComments("Comments");

		this.lotInputValidator.validate(this.lotGeneratorInputDto);
	}

	@Test(expected = ApiRequestValidationException.class)
	public void testValidateDataGenerateStockTrue() {
		this.lotGeneratorInputDto.setGid(1);
		this.lotGeneratorInputDto.setLocationId(6000);
		this.lotGeneratorInputDto.setScaleId(8264);
		this.lotGeneratorInputDto.setStockId("ABCD");
		this.lotGeneratorInputDto.setComments("Comments");
		this.lotGeneratorInputDto.setGenerateStock(true);

		this.lotInputValidator.validate(this.lotGeneratorInputDto);
	}

	@Test(expected = ApiRequestValidationException.class)
	public void testValidateDataGenerateStockTrueWithPrefix() {
		this.lotGeneratorInputDto.setGid(1);
		this.lotGeneratorInputDto.setLocationId(6000);
		this.lotGeneratorInputDto.setScaleId(8264);
		this.lotGeneratorInputDto.setStockId("ABCD");
		this.lotGeneratorInputDto.setStockPrefix("123");
		this.lotGeneratorInputDto.setComments("Comments");
		this.lotGeneratorInputDto.setGenerateStock(true);

		this.lotInputValidator.validate(this.lotGeneratorInputDto);
	}

	@Test(expected = ApiRequestValidationException.class)
	public void testValidateDataGenerateStockFalse() {
		this.lotGeneratorInputDto.setGid(1);
		this.lotGeneratorInputDto.setLocationId(6000);
		this.lotGeneratorInputDto.setScaleId(8264);
		this.lotGeneratorInputDto.setComments("Comments");
		this.lotGeneratorInputDto.setGenerateStock(false);
		this.lotGeneratorInputDto.setStockPrefix("123");

		this.lotInputValidator.validate(this.lotGeneratorInputDto);
	}
}