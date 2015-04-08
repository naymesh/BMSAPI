package org.ibp.api.domain.ontology;

public class IdName {

	private Integer id;
	private String name;

	public IdName() {
	}

	public IdName(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "DataTypeSummary[" + "id=" + this.id + ", name='" + this.name + '\'' + ']';
	}
}
