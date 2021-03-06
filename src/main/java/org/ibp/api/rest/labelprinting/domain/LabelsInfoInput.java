package org.ibp.api.rest.labelprinting.domain;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

@AutoProperty
public class LabelsInfoInput {

	private Integer studyId;
	private Integer datasetId;
	private Integer searchRequestId;

	public Integer getStudyId() {
		return studyId;
	}

	public void setStudyId(final Integer studyId) {
		this.studyId = studyId;
	}

	public Integer getDatasetId() {
		return datasetId;
	}

	public void setDatasetId(final Integer datasetId) {
		this.datasetId = datasetId;
	}

	public Integer getSearchRequestId() {
		return this.searchRequestId;
	}

	public void setSearchRequestId(final Integer searchRequestId) {
		this.searchRequestId = searchRequestId;
	}


	@Override
	public int hashCode() {
		return Pojomatic.hashCode(this);
	}

	@Override
	public String toString() {
		return Pojomatic.toString(this);
	}

	@Override
	public boolean equals(final Object o) {
		return Pojomatic.equals(this, o);
	}

}
