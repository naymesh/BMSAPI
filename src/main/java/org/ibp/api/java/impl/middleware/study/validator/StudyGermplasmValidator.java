package org.ibp.api.java.impl.middleware.study.validator;

import org.generationcp.middleware.enumeration.DatasetTypeEnum;
import org.generationcp.middleware.pojos.ims.TransactionStatus;
import org.generationcp.middleware.service.impl.inventory.PlantingServiceImpl;
import org.ibp.api.exception.ApiRequestValidationException;
import org.ibp.api.java.impl.middleware.common.validator.GermplasmValidator;
import org.ibp.api.java.study.StudyService;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import javax.annotation.Resource;
import java.util.HashMap;

@Component
public class StudyGermplasmValidator {

    @Resource
    private GermplasmValidator germplasmValidator;

    @Resource
    private PlantingServiceImpl plantingService;

    @Resource
    private StudyService studyService;

    @Resource
    private org.generationcp.middleware.service.api.study.StudyGermplasmService middlewareStudyGermplasmService;


    private BindingResult errors;

    public void validate(final Integer studyId, final Integer entryId, final Integer newGid) {

        this.errors = new MapBindingResult(new HashMap<String, String>(), Integer.class.getName());
        boolean isValidStudyGermplasm = this.middlewareStudyGermplasmService.isValidStudyGermplasm(studyId, entryId);
        if (!isValidStudyGermplasm){
            errors.reject("invalid.entryid.for.study");
        }

        this.germplasmValidator.validateGermplasmId(this.errors, newGid);

        // Check if means has dataset or advance or cross list
        boolean hasMeansDataset = this.studyService.studyHasGivenDatasetType(studyId, DatasetTypeEnum.MEANS_DATA.getId());
        if (hasMeansDataset) {
            errors.reject("study.has.means.dataset");
        }
        boolean hasAdvancedOrCrossesList = this.studyService.hasAdvancedOrCrossesList(studyId);
        if (hasAdvancedOrCrossesList) {
            errors.reject("study.has.advance.or.cross.list");
        }

        // TODO check that no samples for given entry

        // Check that study has no confirmed or pending transactions for given entry
        final Integer pendingTransactions =
                this.plantingService.getPlantingTransactionsByStudyAndEntryId(studyId, entryId, TransactionStatus.PENDING).size();
        final Integer confirmedTransactions =
                this.plantingService.getPlantingTransactionsByStudyAndEntryId(studyId, entryId, TransactionStatus.CONFIRMED).size();
        if (pendingTransactions > 0 || confirmedTransactions > 0) {
            errors.reject("entry.has.pending.or.confirmed.transactions");
        }

        if (this.errors.hasErrors()) {
            throw new ApiRequestValidationException(this.errors.getAllErrors());
        }

    }

}
