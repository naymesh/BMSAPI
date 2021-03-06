
package org.ibp.api.domain.ontology;

import java.util.Date;

import org.ibp.api.ISO8601DateParser;

public class MetadataSummary {

	private String dateCreated;
	private String dateLastModified;

	public String getDateCreated() {
		return this.dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		if (dateCreated != null) {
			this.dateCreated = ISO8601DateParser.toString(dateCreated);
		}
	}

	public String getDateLastModified() {
		return this.dateLastModified;
	}

	public void setDateLastModified(Date dateLastModified) {
		if (dateLastModified != null) {
			this.dateLastModified = ISO8601DateParser.toString(dateLastModified);
		}
	}
}
