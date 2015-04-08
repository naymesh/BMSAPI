package org.ibp.api.domain.ontology;

import java.util.Date;

public class MetaData {
	private Date dateCreated;
	private Date dateLastModified;
	private Integer observations;

	public Date getDateCreated() {
		return this.dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Date getDateLastModified() {
		return this.dateLastModified;
	}

	public void setDateLastModified(Date dateLastModified) {
		this.dateLastModified = dateLastModified;
	}

	public Integer getObservations() {
		return this.observations;
	}

	public void setObservations(Integer observations) {
		this.observations = observations;
	}
}
