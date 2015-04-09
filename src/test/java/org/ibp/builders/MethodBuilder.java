package org.ibp.builders;

import org.generationcp.middleware.domain.oms.Method;
import org.generationcp.middleware.domain.oms.Term;

public class MethodBuilder {

	private final Term term;

	public MethodBuilder() {
		this.term = new Term();
	}

	public Method build(int id, String name, String description) {
		this.term.setId(id);
		this.term.setName(name);
		this.term.setDefinition(description);
		return new Method(this.term);
	}
}