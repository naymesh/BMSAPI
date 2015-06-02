
package org.ibp.api.domain.ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.generationcp.middleware.domain.ontology.Method;
import org.generationcp.middleware.domain.ontology.Property;

/**
 * Holds all Variable details. Extended from {@link TermSummary} for basic term details.
 */
public class VariableDetails extends TermSummary {

	private MetadataDetails metadata = new MetadataDetails();

	private String alias;
	private final TermSummary methodSummary = new TermSummary();
	private final TermSummary propertySummary = new TermSummary();
	private ScaleSummary scale;
	private final List<VariableType> variableTypes = new ArrayList<>();
	private boolean favourite;
	private final ExpectedRange expectedRange = new ExpectedRange();

	public MetadataDetails getMetadata() {
		return this.metadata;
	}

	public void setMetadata(MetadataDetails metadata) {
		this.metadata = metadata;
	}

	public ExpectedRange getExpectedRange() {
		return this.expectedRange;
	}

	public String getAlias() {
		return this.alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public TermSummary getMethodSummary() {
		return this.methodSummary;
	}

	public void setMethodSummary(Method method) {
		this.methodSummary.setId(String.valueOf(method.getId()));
		this.methodSummary.setName(method.getName());
		this.methodSummary.setDescription(method.getDefinition());
	}

	public TermSummary getPropertySummary() {
		return this.propertySummary;
	}

	public void setPropertySummary(Property property) {
		this.propertySummary.setId(String.valueOf(property.getId()));
		this.propertySummary.setName(property.getName());
		this.propertySummary.setDescription(property.getDefinition());
	}

	public ScaleSummary getScale() {
		return this.scale;
	}

	public void setScale(ScaleSummary scale) {
		this.scale = scale;
	}

	public boolean isFavourite() {
		return this.favourite;
	}

	public void setFavourite(boolean favourite) {
		this.favourite = favourite;
	}

	public void setExpectedMin(String min) {
		this.expectedRange.setMin(min);
	}

	public void setExpectedMax(String max) {
		this.expectedRange.setMax(max);
	}

	public List<org.ibp.api.domain.ontology.VariableType> getVariableTypes() {
		return this.variableTypes;
	}

	public void setVariableTypes(Set<VariableType> variables) {
		this.variableTypes.clear();
		for (VariableType variableType : variables) {
			this.variableTypes.add(variableType);
		}
	}

	public void setObservations(Integer observations) {
		if (observations == null) {
			this.metadata.setObservations(0);
		} else {
			this.metadata.setObservations(observations);
		}
	}

	public void setStudies(Integer studies) {
		if (studies == null) {
			this.metadata.setStudies(0);
		} else {
			this.metadata.setStudies(studies);
		}
	}
}
