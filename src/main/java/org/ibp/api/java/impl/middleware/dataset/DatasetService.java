package org.ibp.api.java.impl.middleware.dataset;

import org.ibp.api.rest.dataset.DatasetDTO;

import java.util.List;
import java.util.Set;

/**
 * Created by clarysabel on 10/24/18.
 */
public interface DatasetService {


	List<DatasetDTO> getDatasetByStudyId(final Integer studyId, final Set<Integer> filterByTypeIds);

}
