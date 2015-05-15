package org.ibp.api.java.impl.middleware.ontology;

import com.google.common.base.Function;
import org.generationcp.middleware.domain.oms.CvId;
import org.generationcp.middleware.domain.oms.DataType;
import org.generationcp.middleware.domain.oms.Term;
import org.generationcp.middleware.domain.oms.VariableType;
import org.generationcp.middleware.exceptions.MiddlewareException;
import org.generationcp.middleware.manager.ontology.api.TermDataManager;
import org.generationcp.middleware.util.Util;
import org.ibp.api.domain.ontology.IdName;
import org.ibp.api.domain.ontology.VariableTypeResponse;
import org.ibp.api.exception.ApiRuntimeException;
import org.ibp.api.java.ontology.OntologyModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class OntologyModelServiceImpl implements OntologyModelService {

	@Autowired
	private TermDataManager termDataManager;

	@Override
	public List<IdName> getAllDataTypes() {
		return Util.convertAll(Arrays.asList(DataType.values()), new Function<DataType, IdName>() {

			@Override
			public IdName apply(DataType dataType) {
				return new IdName(dataType.getId(), dataType.getName());
			}
		});
	}

	@Override
	public List<String> getAllClasses() {
		try {
			List<Term> classes = this.termDataManager.getTermByCvId(CvId.TRAIT_CLASS.getId());
			List<String> classList = new ArrayList<>();

			for (Term term : classes) {
				classList.add(term.getName());
			}

			Comparator<String> ALPHABETICAL_ORDER = new Comparator<String>() {
				public int compare(String str1, String str2) {
					int res = String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
					if (res == 0) {
						res = str1.compareTo(str2);
					}
					return res;
				}
			};

			Collections.sort(classList, ALPHABETICAL_ORDER);

			return classList;
		} catch (MiddlewareException e) {
			throw new ApiRuntimeException("Error!", e);
		}
	}

	@Override
	public List<VariableTypeResponse> getAllVariableTypes() {

		List<VariableTypeResponse> variableTypes =  Util.convertAll(Arrays.asList(VariableType.values()),
				new Function<VariableType, VariableTypeResponse>() {

					@Override
					public VariableTypeResponse apply(VariableType variableType) {
						return new VariableTypeResponse(variableType.getId(), variableType
								.getName(), variableType.getDescription());
					}
				});

	  	Collections.sort(variableTypes, new Comparator<VariableTypeResponse>() {
			@Override
			public int compare(VariableTypeResponse o1, VariableTypeResponse o2) {
			  	return o1.getName().compareTo(o2.getName());
			}
		});
	  	return variableTypes;
	}
}
