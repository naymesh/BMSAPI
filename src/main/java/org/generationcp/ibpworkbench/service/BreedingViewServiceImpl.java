package org.generationcp.ibpworkbench.service;

import au.com.bytecode.opencsv.CSVReader;
import com.rits.cloning.Cloner;
import org.generationcp.ibpworkbench.constants.WebAPIConstants;
import org.generationcp.ibpworkbench.model.TraitsAndMeans;
import org.generationcp.ibpworkbench.util.TraitsAndMeansCSVUtil;
import org.generationcp.ibpworkbench.util.TraitsAndMeansCSVUtil2;
import org.generationcp.middleware.v2.domain.*;
import org.generationcp.middleware.v2.manager.OntologyDataManagerImpl;
import org.generationcp.middleware.v2.manager.StudyDataManagerImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/*******************************************************************************
 * Copyright (c) 2012, All Rights Reserved.
 *
 * Generation Challenge Programme (GCP)
 *
 *
 * This software is licensed for use under the terms of the GNU General Public
 * License (http://bit.ly/8Ztv8M) and the provisions of Part F of the Generation
 * Challenge Programme Amended Consortium Agreement (http://bit.ly/KQX1nL)
 *
 *******************************************************************************/

public class BreedingViewServiceImpl implements BreedingViewService {
    @Autowired
    private TraitsAndMeansCSVUtil2 traitsAndMeansCSVUtil2;
    @Autowired
    private StudyDataManagerImpl studyDataManagerV2;
    @Autowired
    private OntologyDataManagerImpl ontologyDataManagerV2;
    @Autowired
    private Cloner cloner;

    public void execute(Map<String, String> params, List<String> errors) throws Exception {
        String fileName = params.get(WebAPIConstants.FILENAME.getParamValue());
        String workbenchProjectId = params.get(WebAPIConstants.WORKBENCH_PROJECT_ID.getParamValue());
        Map<String, ArrayList<String>> traitsAndMeans = traitsAndMeansCSVUtil2.csvToMap(fileName);
        System.out.println("Traits and Means: " + traitsAndMeans);

        int ndLocationId;
        int stockId;

        //call middleware api and save
        if(!traitsAndMeans.isEmpty()) {
            String[] header = traitsAndMeans.keySet().toArray(new String[0]);         //csv header
            int inputDatasetId = Integer.valueOf(params.get(WebAPIConstants.INPUT_DATASET_ID.getParamValue()));
            TrialEnvironments trialEnvironments = studyDataManagerV2.getTrialEnvironmentsInDataset(inputDatasetId);
            //environment, env value
            TrialEnvironment trialEnv = trialEnvironments.findOnlyOneByLocalName(header[0], traitsAndMeans.get(header[0]).get(0));
            ndLocationId = trialEnv.getId();
            Stocks stocks = studyDataManagerV2.getStocksInDataset(inputDatasetId);

            DataSet dataSet = studyDataManagerV2.getDataSet(inputDatasetId);
            VariableTypeList variableTypeList = dataSet.getVariableTypes();
            //get variates only
            VariableTypeList variableTypeListVariates = variableTypeList.getVariates();
            VariableType mat50VariableType = null;
            VariableType mat50UnitErrorsVariableType = null;
            for(int i = 2; i < header.length; i += 2) {   //means and errors are in pair, so just get the word before _
                String root = header[i] != null ? header[i].split("_")[0] : "";
                if(!"".equals(root)) {
                    //Means
                    mat50VariableType = variableTypeListVariates.findByLocalName(root);
                    VariableType mat50MeansVariableType = cloner.deepClone(mat50VariableType);
                    mat50MeansVariableType.setLocalName(root + "_Means");
                    Term term = ontologyDataManagerV2.findMethodByName("LS MEAN");
                    if(term == null) {
                        String definitionMeans = mat50MeansVariableType.getStandardVariable().getMethod().getDefinition();
                        term = ontologyDataManagerV2.addMethod("LS MEAN", definitionMeans);
                    }
                    mat50MeansVariableType.getStandardVariable().setMethod(term);
                    variableTypeList.makeRoom(1);
                    mat50MeansVariableType.setRank(1);
                    variableTypeList.add(mat50MeansVariableType);
                    //Unit Errors
                    mat50UnitErrorsVariableType = cloner.deepClone(mat50VariableType);
                    mat50UnitErrorsVariableType.setLocalName(root + "_UnitErrors");
                    term = ontologyDataManagerV2.findMethodByName("ERROR ESTIMATE");
                    if(term == null) {
                        String definitionUErrors = mat50UnitErrorsVariableType.getStandardVariable().getMethod().getDefinition();
                        term = ontologyDataManagerV2.addMethod("ERROR ESTIMATE", definitionUErrors);
                    }
                    mat50UnitErrorsVariableType.getStandardVariable().setMethod(term);  //correct method for setting term?????
                    variableTypeList.makeRoom(1);
                    mat50UnitErrorsVariableType.setRank(1);
                    variableTypeList.add(mat50UnitErrorsVariableType);
                }
            }

            int studyId = Integer.valueOf(params.get(WebAPIConstants.STUDY_ID.getParamValue()));

            //please make sure that the study name is unique and does not exist in the db.
            VariableList variableList = new VariableList();
            Variable variable = createVariable(TermId.DATASET_NAME.getId(), fileName + "_" + workbenchProjectId + "_" + studyId , 1);
            variableTypeList.makeRoom(1);
            variable.getVariableType().setRank(1);
            variableTypeList.add(variable.getVariableType());
            updateVariableType(variable.getVariableType(), fileName + "_" + workbenchProjectId + "_" + studyId, "Dataset name (local)");
            variableList.add(variable);

            variable = createVariable(TermId.DATASET_TITLE.getId(), "My Dataset Description", 2);
            variableTypeList.makeRoom(1);
            variable.getVariableType().setRank(1);
            variableTypeList.add(variable.getVariableType());
            updateVariableType(variable.getVariableType(), "DATASET_TITLE", "Dataset title (local)");
            variableList.add(variable);

            variable = createVariable(TermId.DATASET_TYPE.getId(), "10070", 3);
            variableTypeList.makeRoom(1);
            variable.getVariableType().setRank(1);
            variableTypeList.add(variable.getVariableType());
            updateVariableType(variable.getVariableType(), "DATASET_TYPE", "Dataset type (local)");
            variableList.add(variable);
            DatasetValues datasetValues = new DatasetValues();
            datasetValues.setVariables(variableList);
            DatasetReference datasetReference = studyDataManagerV2.addDataSet(studyId, variableTypeList, datasetValues);

            //save data
            //get dataset using new datasetid
            DataSet newDataset = studyDataManagerV2.getDataSet(datasetReference.getId());
            ExperimentValues experimentValues = null;
            ArrayList<String> environments = traitsAndMeans.get(header[0]);
            for(int i = 0; i < environments.size(); i++) {
                for(int j = 2; j < header.length; j++) {   //means and errors are in pair, so just get the word before _
                    if(header[j] != null) {
                        Stock stock = stocks.findOnlyOneByLocalName(header[1], traitsAndMeans.get(header[1]).get(i));    //???
                        stockId = stock.getId();      //germPlasmId
                        experimentValues = createExperimentValues(newDataset,
                                traitsAndMeans.get(header[j]).get(i), ndLocationId, stockId);
                        studyDataManagerV2.addExperiment(newDataset.getId(), ExperimentType.AVERAGE, experimentValues);
                    }
                }
            }

        } else {
            throw new Exception("Input data is empty. No data was processed.");
        }
    }

    private ExperimentValues createExperimentValues(DataSet newDataset, String cellValue, int locationId, int stockId) {
        ExperimentValues experimentValues = new ExperimentValues();
        experimentValues.setLocationId(locationId);
        experimentValues.setGermplasmId(stockId);
        VariableTypeList newVariableTypeList = newDataset.getVariableTypes();
        VariableType variableType = newVariableTypeList.findByLocalName("MAT50_Means");
        List<Variable> varList = new ArrayList<Variable>();
        Variable variable = new Variable(variableType, cellValue);
        varList.add(variable);
        VariableList list = new VariableList();
        list.setVariables(varList);
        experimentValues.setVariableList(list);
        return experimentValues;
    }

    private Variable createVariable(int termId, String value, int rank) throws Exception {
        StandardVariable stVar = ontologyDataManagerV2.getStandardVariable(termId);

        VariableType vtype = new VariableType();
        vtype.setStandardVariable(stVar);
        vtype.setRank(rank);
        Variable var = new Variable();
        var.setValue(value);
        var.setVariableType(vtype);
        return var;
    }

    private void updateVariableType(VariableType type, String name, String description) {
        type.setLocalName(name);
        type.setLocalDescription(description);
    }
}
