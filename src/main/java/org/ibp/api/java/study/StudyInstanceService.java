package org.ibp.api.java.study;

public interface StudyInstanceService {

	void createStudyInstance(final String cropName, final Integer studyId, final String instanceNumber);

	void removeStudyInstance(final String cropName, final Integer studyId, final String instanceNumber);

}