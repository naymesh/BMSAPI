
package org.ibp.api.brapi.v1.common;

import com.google.common.base.Preconditions;
import org.ibp.api.domain.common.PagedResult;

public class BrapiPagedResult<T> extends PagedResult<T> {

	public static final String CURRENT_PAGE_DESCRIPTION = "Page number to retrieve in case of multi paged results. Defaults to "
			+ BrapiPagedResult.DEFAULT_PAGE_NUMBER + " (first page) if not supplied.";
	public static final String PAGE_SIZE_DESCRIPTION = "Number of results to retrieve per page.";

	public BrapiPagedResult(final int pageNumber, final int pageSize, final long totalResults, final long filteredResults) {
		super();
		this.totalResults = totalResults;
		if (filteredResults == 0) {
			this.filteredResults = totalResults;
		} else {
			this.filteredResults = filteredResults;
		}
		Preconditions.checkArgument(this.filteredResults <= totalResults, "Filtered results must be less than or equal to total results");

		if (pageSize < 1 || pageSize > BrapiPagedResult.MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Page size must between 1 and " + BrapiPagedResult.MAX_PAGE_SIZE + ".");
		}
		this.pageSize = pageSize;

		if (totalResults != 0 && (pageNumber < BrapiPagedResult.DEFAULT_PAGE_NUMBER || pageNumber >= this.getTotalPages())) {
			throw new IllegalArgumentException(
					"A total of " + this.getTotalPages() + " pages are available, so the page number must between "
							+ BrapiPagedResult.DEFAULT_PAGE_NUMBER + " and " + this.getTotalPages() + " (exclusive).");
		}

		this.pageNumber = pageNumber;
	}

}
