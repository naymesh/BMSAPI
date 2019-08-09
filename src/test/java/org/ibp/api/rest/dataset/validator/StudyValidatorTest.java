package org.ibp.api.rest.dataset.validator;

import org.generationcp.middleware.domain.dms.Study;
import org.generationcp.middleware.manager.api.StudyDataManager;
import org.generationcp.middleware.pojos.workbench.Role;
import org.generationcp.middleware.pojos.workbench.WorkbenchUser;
import org.ibp.api.exception.ForbiddenException;
import org.ibp.api.exception.ResourceNotFoundException;
import org.ibp.api.java.impl.middleware.UserTestDataGenerator;
import org.ibp.api.java.impl.middleware.dataset.validator.StudyValidator;
import org.ibp.api.java.impl.middleware.security.SecurityService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Random;

import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class StudyValidatorTest {

	public static final int USER_ID = 10;

	@Mock
	private SecurityService securityService;

	@Mock
	private StudyDataManager studyDataManager;

	@InjectMocks
	private StudyValidator studyValidator;

	@Before
	public void setup() {
	}

	@Test (expected = ResourceNotFoundException.class)
	public void testStudyDoesNotExist() {
		final Random ran = new Random();
		final Integer studyId = ran.nextInt();
		Mockito.when(studyDataManager.getStudy(studyId)).thenReturn(null);
		studyValidator.validate(studyId, ran.nextBoolean());
	}

	@Test (expected = ForbiddenException.class)
	public void testStudyIsLocked() {
		final WorkbenchUser user = UserTestDataGenerator.initializeWorkbenchUser(USER_ID, new Role(2, "Breeder"));
		doReturn(user).when(this.securityService).getCurrentlyLoggedInUser();
		final Random ran = new Random();
		final Integer studyId = ran.nextInt();
		final Study study = new Study();
		study.setId(studyId);
		study.setLocked(true);
		study.setCreatedBy("1");
		Mockito.when(studyDataManager.getStudy(studyId)).thenReturn(study);
		studyValidator.validate(studyId, true);
	}

	@Test
	public void testStudyIsLockedButUserIsOwner() {
		final WorkbenchUser user = UserTestDataGenerator.initializeWorkbenchUser(USER_ID, new Role(2, "Breeder"));
		doReturn(user).when(this.securityService).getCurrentlyLoggedInUser();
		final Random ran = new Random();
		final int studyId = ran.nextInt();
		final Study study = new Study();
		study.setId(studyId);
		study.setLocked(true);
		study.setCreatedBy(String.valueOf(USER_ID));
		Mockito.when(studyDataManager.getStudy(studyId)).thenReturn(study);
		studyValidator.validate(studyId, true);
		// no exceptions thrown
	}

	@Test
	public void testStudyIsLockedButUserIsSuperAdmin() {
		final WorkbenchUser user = UserTestDataGenerator.initializeWorkbenchUser(USER_ID, new Role(2, Role.SUPERADMIN));
		doReturn(user).when(this.securityService).getCurrentlyLoggedInUser();

		final Random ran = new Random();
		final int studyId = ran.nextInt();
		final Study study = new Study();
		study.setId(studyId);
		study.setLocked(true);
		study.setCreatedBy("1");

		Mockito.when(studyDataManager.getStudy(studyId)).thenReturn(study);
		System.out.println(studyValidator);
		studyValidator.validate(studyId, true);
		// no exceptions thrown
	}

}
