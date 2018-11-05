package org.ibp.api.domain.dataset;

import javax.validation.constraints.NotNull;

public class ObservationValue {

	@NotNull
	private String value;

	private Integer categoricalValueId;

	private String status;

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public Integer getCategoricalValueId() {
		return categoricalValueId;
	}

	public void setCategoricalValueId(final Integer categoricalValueId) {
		this.categoricalValueId = categoricalValueId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}
}
