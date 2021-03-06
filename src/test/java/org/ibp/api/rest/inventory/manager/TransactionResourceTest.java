package org.ibp.api.rest.inventory.manager;

import com.beust.jcommander.internal.Lists;
import com.jayway.jsonassert.impl.matcher.IsCollectionWithSize;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.generationcp.middleware.domain.inventory.manager.TransactionDto;
import org.generationcp.middleware.domain.inventory.manager.TransactionsSearchDto;
import org.generationcp.middleware.manager.api.SearchRequestService;
import org.generationcp.middleware.pojos.ims.TransactionType;
import org.ibp.ApiUnitTestBase;
import org.ibp.api.java.inventory.manager.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class TransactionResourceTest extends ApiUnitTestBase {

	@Autowired
	private TransactionService transactionService;

	@Autowired
	private SearchRequestService searchRequestService;

	@Configuration
	public static class TestConfiguration {
		@Bean
		@Primary
		public TransactionService transactionService() {
			return Mockito.mock(TransactionService.class);
		}
	}

	@Before
	public void setup() throws Exception {
		super.setUp();
	}

	@Test
	public void testSearchTransactions() throws Exception {

		final TransactionsSearchDto transactionsSearchDto = new TransactionsSearchDto();
		transactionsSearchDto.setDesignation("germplasm");
		transactionsSearchDto.setGids(Lists.newArrayList(1));
		transactionsSearchDto.setLotIds(Lists.newArrayList(2));
		transactionsSearchDto.setMaxAmount(10.0);
		transactionsSearchDto.setMinAmount(-10.0);
		transactionsSearchDto.setNotes("Deposit");
		transactionsSearchDto.setUnitIds(Lists.newArrayList(8264));
		transactionsSearchDto.setStockId("ABC-1");
		transactionsSearchDto.setTransactionIds(Lists.newArrayList(100, 200));
		transactionsSearchDto.setTransactionTypes(Lists.newArrayList(TransactionType.DEPOSIT.getId()));
		transactionsSearchDto.setCreatedByUsername("admin");

		final int searchResultsDbid = 1;
		final Pageable pageable = Mockito.mock(Pageable.class);
		Mockito.when(pageable.getPageSize()).thenReturn(20);
		Mockito.when(pageable.getPageNumber()).thenReturn(0);
		Mockito.when(pageable.getSort()).thenReturn(null);

		Mockito.doReturn(transactionsSearchDto).when(this.searchRequestService).getSearchRequest(searchResultsDbid,
			TransactionsSearchDto.class);

		final List<TransactionDto> list = new ArrayList<>();
		final TransactionDto transactionDto = new TransactionDto();
		transactionDto.setAmount(10.0);
		transactionDto.getLot().setDesignation("germplasm");
		transactionDto.getLot().setGid(1);
		transactionDto.getLot().setLotId(2);
		transactionDto.setNotes("Deposit");
		transactionDto.getLot().setUnitId(8264);
		transactionDto.getLot().setUnitName("SEED_AMOUNT_g");
		transactionDto.getLot().setStockId("ABC-1");
		transactionDto.setTransactionType("Deposit");
		transactionDto.setCreatedByUsername("admin");
		list.add(transactionDto);

		Mockito.doReturn(list).when(this.transactionService).searchTransactions(Mockito.any(TransactionsSearchDto.class),
			Mockito.any(Pageable.class));

		this.mockMvc
			.perform(MockMvcRequestBuilders
				.get("/crops/{cropName}/transactions/search", this.cropName)
				.param("searchRequestId", "1").contentType(this.contentType))
			.andDo(MockMvcResultHandlers.print())
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(list.size())))
			.andExpect(jsonPath("$[0].lot.lotId", is(2)))
			.andExpect(jsonPath("$[0].lot.gid", is(1)))
			.andExpect(jsonPath("$[0].lot.designation", is("germplasm")))
			.andExpect(jsonPath("$[0].notes", is("Deposit")))
			.andExpect(jsonPath("$[0].lot.unitId", is(8264)))
			.andExpect(jsonPath("$[0].lot.unitName", is("SEED_AMOUNT_g")))
			.andExpect(jsonPath("$[0].lot.stockId", is("ABC-1")))
			.andExpect(jsonPath("$[0].transactionType", is("Deposit")))
			.andExpect(jsonPath("$[0].createdByUsername", is("admin")))
		;
	}

	@Test
	public void testGetAvailableBalanceTransactions() throws Exception {

		final String lotUUID = RandomStringUtils.randomAlphanumeric(16);
		this.mockMvc
			.perform(MockMvcRequestBuilders
				.get("/crops/{cropName}/lots/{lotUUID}/available-balance-transactions", this.cropName, lotUUID))
			.andExpect(MockMvcResultMatchers.status().isOk());

		Mockito.verify(this.transactionService).getAvailableBalanceTransactions(lotUUID);
	}
}
