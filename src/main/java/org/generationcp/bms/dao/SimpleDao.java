package org.generationcp.bms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.generationcp.bms.domain.GermplasmScoreCard;
import org.generationcp.bms.domain.GermplasmSearchResult;
import org.generationcp.bms.domain.Trait;
import org.generationcp.bms.domain.TraitObservation;
import org.generationcp.middleware.domain.dms.PhenotypicType;
import org.generationcp.middleware.domain.dms.StandardVariableSummary;
import org.generationcp.middleware.domain.h2h.Observation;
import org.generationcp.middleware.domain.h2h.ObservationKey;
import org.generationcp.middleware.domain.h2h.TraitInfo;
import org.generationcp.middleware.domain.oms.TermId;
import org.generationcp.middleware.domain.oms.TermSummary;
import org.generationcp.middleware.exceptions.MiddlewareQueryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class SimpleDao {

	private JdbcTemplate jdbcTemplate;

	@Autowired
    public SimpleDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
	
	/**
	 * Reads summary of a standard variable from a database view <strong>standard_variable_summary</strong>.
	 * 
	 *  Note: view definition is in src/main/resources/sql/db_view_definitions.sql, make sure the view exists before using this method. 
	 * 
	 * @param standardVariableId
	 * @return
	 * @throws MiddlewareQueryException
	 */
	public StandardVariableSummary getStandardVariableSummary(Integer standardVariableId) throws MiddlewareQueryException {
        
		StandardVariableSummary result = this.jdbcTemplate.queryForObject(
				"SELECT * FROM standard_variable_summary where id = ?",
				new Object[] { standardVariableId },
				new RowMapper<StandardVariableSummary>() {

			@Override
			public StandardVariableSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
				
				StandardVariableSummary variable = new StandardVariableSummary(rs.getInt("id"), rs.getString("name"), rs.getString("definition"));
				
				variable.setProperty(new TermSummary(rs.getInt("property_id"), rs.getString("property_name"), rs.getString("property_def")));
				variable.setMethod(new TermSummary(rs.getInt("method_id"), rs.getString("method_name"), rs.getString("method_def")));
				variable.setScale(new TermSummary(rs.getInt("scale_id"), rs.getString("scale_name"), rs.getString("scale_def")));
				
				variable.setDataType(new TermSummary(rs.getInt("data_type_id"), rs.getString("data_type_name"), rs.getString("data_type_def")));
				variable.setStoredIn(new TermSummary(rs.getInt("stored_in_id"), rs.getString("stored_in_name"), rs.getString("stored_in_def")));
				variable.setIsA(new TermSummary(rs.getInt("is_a_id"), rs.getString("is_a_name"), rs.getString("is_a_def")));
				
				variable.setPhenotypicType(PhenotypicType.valueOf(rs.getString("phenotypic_type")));
											
				return variable;
			}
		});
		
		return result;
		
    }

	/**
	 * Returns a list of traits for the given study (identified by the {@code studyId}). 
	 * Study design may have many traits "intended" to be measured (defined as variates of the study). 
	 * However, not all traits are measured for all plants/germplasm necessarily.
	 *  
	 * This method returns only those traits for which measurements are avaibale. 
	 * 
	 * Relies on the database views <strong>germplasm_trial_details</strong> and <strong>standard_variable_details</strong>.
	 * 
	 * @param studyId the identifier of the study
	 * @return matching traits with measurement count for each. Empty list if no such match is found. Never returns {@code null}.
	 */
	public List<Trait> getMeasuredTraits(int studyId) {

		List<Trait> measuredTraits = new ArrayList<Trait>();
		List<Map<String, Object>> queryResults = this.jdbcTemplate
				.queryForList(""
						+ "select gtd.stdvar_id, svd.stdvar_name, svd.stdvar_definition, svd.property, svd.method, svd.scale, svd.type, svd.has_type, count(gtd.observed_value) as total_observations "
						+ " from germplasm_trial_details gtd "
						+ " inner join standard_variable_details svd on svd.cvterm_id =  gtd.stdvar_id"
						+ " where gtd.study_id = " + studyId
						+ " group by gtd.stdvar_id;");

		for (Map<String, Object> row : queryResults) {
			Trait trait = new Trait((Integer) row.get("stdvar_id"));
			trait.setName((String) row.get("stdvar_name"));
			trait.setDescription((String) row.get("stdvar_definition"));
			trait.setProperty((String) row.get("property"));
			trait.setMethod((String) row.get("method"));
			trait.setScale((String) row.get("scale"));
			trait.setType((String) row.get("type"));
			trait.setNumberOfMeasurements((Long) row.get("total_observations"));
			//TODO the query for some reason returns the has_type (which is the dataType id) as String type, fix later.
			Integer dataTypeId = Integer.valueOf((String) row.get("has_type"));
			trait.setNumeric(dataTypeId.equals(TermId.NUMERIC_VARIABLE.getId()));
			measuredTraits.add(trait);
		}
		return measuredTraits;
	}

	public List<GermplasmScoreCard> getTraitObservationsForTrial(Integer trialEnvironmentId, List<TraitInfo> traits) {
		
		StringBuilder traitQuery = new StringBuilder();
		for (TraitInfo traitInfo : traits) {
			traitQuery.append(traitInfo.getId());
			traitQuery.append(",");
		}
		
		Map<Integer, GermplasmScoreCard> scoreMap = new HashMap<Integer, GermplasmScoreCard>();
		List<Map<String, Object>> queryResults = jdbcTemplate.queryForList("select gtd.stdvar_id, gtd.envt_id, gtd.gid, gtd.entry_designation, gtd.observed_value from germplasm_trial_details gtd "
				+ "where gtd.envt_id = " + trialEnvironmentId 
				+ " and stdvar_id in (" + traitQuery.substring(0, traitQuery.toString().lastIndexOf(",")) + ");");
		
		for (Map<String, Object> row : queryResults) {
			ObservationKey obsKey = new ObservationKey(
					((Integer) row.get("stdvar_id")).intValue(), 
					((Integer)row.get("gid")).intValue(), 
					((Integer) row.get("envt_id")).intValue());
			Observation observation = new Observation(obsKey, (String) row.get("observed_value"));
			if (!scoreMap.containsKey(observation.getId().getGermplasmId())) {
				scoreMap.put(observation.getId().getGermplasmId(), new GermplasmScoreCard((Integer)row.get("gid"), (String) row.get("entry_designation")));
			}
			scoreMap.get(observation.getId().getGermplasmId()).addObservation(observation);
		}
		return new ArrayList<GermplasmScoreCard>(scoreMap.values());
	}
	
	public List<TraitObservation> getTraitObservations(int studyId, int traitId) {
		
		List<TraitObservation> traitObservations = new ArrayList<TraitObservation>();
		List<Map<String, Object>> queryResults = this.jdbcTemplate
				.queryForList("select gtd.experiment_id, gtd.observed_value, gtd.envt_id, gtd.entry_designation, gtd.gid from germplasm_trial_details gtd "
						+ " where gtd.study_id =" + studyId
						+ " and "
						+ " gtd.stdvar_id = " + traitId + ";");

		for (Map<String, Object> row : queryResults) {
			int experimentId = (Integer) row.get("experiment_id");
			int germplasmId = (Integer) row.get("gid");
			int environmentId = (Integer) row.get("envt_id");
			String observedValue = (String) row.get("observed_value");	
			String designation = (String) row.get("entry_designation");

			TraitObservation obs = new TraitObservation(experimentId, germplasmId, environmentId);
			obs.setDesignation(designation);
			obs.setValue(observedValue);
			traitObservations.add(obs);
		}
		
		return traitObservations;
	}
	
	public List<GermplasmSearchResult> searchGermplasm(String queryString) {
		
		BeanPropertyRowMapper<GermplasmSearchResult> rowMapper = new BeanPropertyRowMapper<GermplasmSearchResult>();
		rowMapper.setMappedClass(GermplasmSearchResult.class);
		
		return this.jdbcTemplate.query("select * from germplasm_summary where names like '%" + queryString + "%'", rowMapper);
	}
}