
package org.ibp.api.domain.ontology;

import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Contains basic data used for list, insert and update of property Extended from {@link TermSummary} for getting basic fields like id, name
 * and description
 */

public class PropertySummary extends TermSummary {

	private String cropOntologyId;
	private Set<String> classes;
	private MetadataSummary metadata = new MetadataSummary();

	public String getCropOntologyId() {
		return this.cropOntologyId;
	}

	public void setCropOntologyId(String cropOntologyId) {
		this.cropOntologyId = cropOntologyId;
	}

	public Set<String> getClasses() {
		return this.classes;
	}

	public void setClasses(Set<String> classes) {
		this.classes = classes;
	}

	public MetadataSummary getMetadata() {
		return this.metadata;
	}

	public void setMetadata(MetadataSummary metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return "PropertySummary{" + "cropOntologyId='" + this.cropOntologyId + '\'' + ", classes=" + this.classes + ", metadata="
				+ this.metadata + "} " + super.toString();
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof PropertySummary)) {
			return false;
		}
		PropertySummary castOther = (PropertySummary) other;
		return new EqualsBuilder().append(this.cropOntologyId, castOther.cropOntologyId).append(this.classes, castOther.classes)
				.append(this.metadata, castOther.metadata).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.cropOntologyId).append(this.classes).append(this.metadata).toHashCode();
	}

}
