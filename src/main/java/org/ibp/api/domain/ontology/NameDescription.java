package org.ibp.api.domain.ontology;

public class NameDescription {

	private String name;

	private String description;

	public NameDescription() {

	}

	public NameDescription(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
