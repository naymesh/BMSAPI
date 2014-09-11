package org.generationcp.bms.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.generationcp.bms.dao.SimpleDao;
import org.generationcp.bms.domain.DatasetDetails;
import org.generationcp.bms.domain.DatasetSummary;
import org.generationcp.bms.domain.StudyDetails;
import org.generationcp.bms.domain.StudySummary;
import org.generationcp.bms.domain.Trait;
import org.generationcp.bms.domain.TraitObservation;
import org.generationcp.bms.domain.TraitObservationDetails;
import org.generationcp.bms.exception.NotFoundException;
import org.generationcp.bms.util.Utils;
import org.generationcp.middleware.domain.dms.DataSet;
import org.generationcp.middleware.domain.dms.DatasetReference;
import org.generationcp.middleware.domain.dms.Study;
import org.generationcp.middleware.domain.dms.Variable;
import org.generationcp.middleware.domain.dms.VariableType;
import org.generationcp.middleware.domain.dms.VariableTypeList;
import org.generationcp.middleware.exceptions.MiddlewareQueryException;
import org.generationcp.middleware.manager.api.StudyDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/study")
public class StudyResource {

	private final StudyDataManager studyDataManager;
	
	private final SimpleDao simpleDao;
	
	@Autowired
	public StudyResource(StudyDataManager studyDataManager, SimpleDao simpleDao) {
		if(studyDataManager == null) {
			throw new IllegalArgumentException(StudyDataManager.class.getSimpleName() + " is required to instantiate " + StudyResource.class.getSimpleName());
		}
		if(simpleDao == null) {
			throw new IllegalArgumentException(SimpleDao.class.getSimpleName() + " is required to instantiate " + StudyResource.class.getSimpleName());
		}
		this.studyDataManager = studyDataManager;
		this.simpleDao = simpleDao;
	}
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home() {
		return "redirect:/api-docs/default/study-resource";
	}
	
	@RequestMapping(value="/{studyId}", method = RequestMethod.GET)
	@ResponseBody
	public StudySummary getStudySummary(@PathVariable Integer studyId, HttpServletRequest httpRequest)
			throws MiddlewareQueryException {

		Study study = studyDataManager.getStudy(studyId);

		if (study == null) {
			throw new NotFoundException();
		}
	
		StudySummary studySummary = new StudySummary(study.getId());
		populateSummary(studySummary, study, httpRequest);

		return studySummary;

	}
	
	private void populateSummary(StudySummary studySummary, Study study, HttpServletRequest httpRequest) throws MiddlewareQueryException {
		studySummary.setName(study.getName());
		studySummary.setTitle(study.getTitle());
		studySummary.setObjective(study.getObjective());
		studySummary.setType(study.getType());
		studySummary.setStartDate(String.valueOf(study.getStartDate()));
		studySummary.setEndDate(String.valueOf(study.getEndDate()));
		
		String baseUrl = Utils.getBaseUrl(httpRequest);	
		studySummary.setStudyDetailsUrl(String.format("%s/study/%s/details", baseUrl, study.getId()));
	}
	
	@RequestMapping(value="/{studyId}/details", method = RequestMethod.GET)
	@ResponseBody
	public StudyDetails getStudyDetails(@PathVariable Integer studyId, HttpServletRequest httpRequest) throws MiddlewareQueryException {
		
        Study study = studyDataManager.getStudy(studyId);
        if (study == null) {
			throw new NotFoundException();
		}
        
        StudyDetails studyDetails = new StudyDetails(study.getId());
        populateSummary(studyDetails, study, httpRequest);
        
        String baseUrl = Utils.getBaseUrl(httpRequest);
		List<DatasetReference> datasetReferences = studyDataManager.getDatasetReferences(study.getId());
		if(datasetReferences != null && !datasetReferences.isEmpty()) {
			for(DatasetReference dsRef : datasetReferences) {
				DatasetSummary dsSummary = new DatasetSummary();
				dsSummary.setId(dsRef.getId());
				dsSummary.setName(dsRef.getName());
				dsSummary.setDescription(dsRef.getDescription());
				dsSummary.setDatasetDetailUrl(String.format("%s/study/dataset/%s", baseUrl, dsRef.getId()));
				studyDetails.addDatasetSummary(dsSummary);
			}
		}
		    
        //factors/metadaa/properties/information/conditions of the study        
        
        List<Variable> conditions = study.getConditions().getVariables();
        VariableTypeList factors = studyDataManager.getAllStudyFactors(Integer.valueOf(studyId));
        List<VariableType> factorDetails = factors.getVariableTypes();
        for(VariableType factorDetail : factorDetails){
            String value = null;           
            for(Variable condition : conditions){
                String conditionName = condition.getVariableType().getLocalName();
                if(factorDetail.getLocalName().equals(conditionName)){
                    value = condition.getDisplayValue();
                }
            }
            org.generationcp.bms.domain.Variable factor = new org.generationcp.bms.domain.Variable(factorDetail.getStandardVariable());
            factor.setValue(value);
            factor.setLocalName(factorDetail.getLocalName());
            factor.setLocalDescription(factorDetail.getLocalDescription());
            studyDetails.addFactor(factor);      
        }
        
        studyDetails.addMeasuredTraits(simpleDao.getMeasuredTraitsForStudy(studyId));
        
        setTraitObservationDetailsUrl(httpRequest, studyDetails.getId(), studyDetails.getMeasuredTraits());
        
        return studyDetails;    
	}

	private void setTraitObservationDetailsUrl(HttpServletRequest httpRequest, Integer studyId, List<Trait> traits) {
		
		String baseUrl = Utils.getBaseUrl(httpRequest);
        for(Trait trait : traits) {
			trait.setObservationDetailsUrl(String.format("%s/study/%s/trait/%s", baseUrl, studyId, trait.getId()));
		}
	}
	
	@RequestMapping(value = "/dataset/{dataSetId}", method = RequestMethod.GET)
	@ResponseBody
	public DatasetDetails getDatasetDetails(@PathVariable Integer dataSetId, HttpServletRequest httpRequest) throws MiddlewareQueryException {
		
		DataSet dataSet = studyDataManager.getDataSet(dataSetId);		
		if(dataSet == null) {
			throw new NotFoundException();
		}
		String baseUrl = Utils.getBaseUrl(httpRequest);	
		
		DatasetDetails details = new DatasetDetails();
		details.setId(dataSet.getId());
		details.setName(dataSet.getName());
		details.setDescription(dataSet.getDescription());
		details.setStudySummaryUrl(String.format("%s/study/%s", baseUrl, dataSet.getStudyId()));
		details.addMeasuredTraits(simpleDao.getMeasuredTraitsForDataset(dataSetId));
		details.setDatasetDetailUrl(String.format("%s/study/dataset/%s", baseUrl, dataSet.getId()));
		
		setTraitObservationDetailsUrl(httpRequest, dataSet.getStudyId(), details.getMeasuredTraits());
		
		return details;
	}
	
	@RequestMapping(value="/{studyId}/trait/{traitId}", method = RequestMethod.GET)
	@ResponseBody
	public TraitObservationDetails getTraitObservationDetails(@PathVariable Integer studyId, @PathVariable Integer traitId) {
		
		List<TraitObservation> traitObservations = simpleDao.getTraitObservations(studyId, traitId);
		
		if(!traitObservations.isEmpty()) {
			TraitObservationDetails details = new TraitObservationDetails(traitId, studyId);
			details.addObservations(traitObservations);
			return details;
		}
		throw new NotFoundException();
	}
}
