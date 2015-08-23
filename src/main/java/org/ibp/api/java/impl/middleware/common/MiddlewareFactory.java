
package org.ibp.api.java.impl.middleware.common;

import java.io.FileNotFoundException;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.generationcp.middleware.hibernate.HibernateSessionPerRequestProvider;
import org.generationcp.middleware.hibernate.XADataSources;
import org.generationcp.middleware.manager.GenotypicDataManagerImpl;
import org.generationcp.middleware.manager.GermplasmDataManagerImpl;
import org.generationcp.middleware.manager.GermplasmListManagerImpl;
import org.generationcp.middleware.manager.InventoryDataManagerImpl;
import org.generationcp.middleware.manager.LocationDataManagerImpl;
import org.generationcp.middleware.manager.StudyDataManagerImpl;
import org.generationcp.middleware.manager.UserDataManagerImpl;
import org.generationcp.middleware.manager.WorkbenchDataManagerImpl;
import org.generationcp.middleware.manager.api.GenotypicDataManager;
import org.generationcp.middleware.manager.api.GermplasmDataManager;
import org.generationcp.middleware.manager.api.GermplasmListManager;
import org.generationcp.middleware.manager.api.InventoryDataManager;
import org.generationcp.middleware.manager.api.LocationDataManager;
import org.generationcp.middleware.manager.api.StudyDataManager;
import org.generationcp.middleware.manager.api.UserDataManager;
import org.generationcp.middleware.manager.api.WorkbenchDataManager;
import org.generationcp.middleware.service.DataImportServiceImpl;
import org.generationcp.middleware.service.FieldbookServiceImpl;
import org.generationcp.middleware.service.api.DataImportService;
import org.generationcp.middleware.service.api.FieldbookService;
import org.generationcp.middleware.service.api.PedigreeService;
import org.generationcp.middleware.service.pedigree.PedigreeDefaultServiceImpl;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;

@Configuration
@EnableTransactionManagement
public class MiddlewareFactory {

	@Autowired
	private ApiEnvironmentConfiguration config;

	@Autowired
	private ContextResolver contextResolver;

	@Autowired
	@Qualifier("WORKBENCH_SessionFactory")
	private SessionFactory WORKBENCH_SessionFactory;

	@Autowired 
	private ApplicationContext applicationContext;
	
	public MiddlewareFactory() {
		
	}
	
	private SessionFactory getSessionFactory() throws FileNotFoundException {
		return (SessionFactory) applicationContext.getBean(XADataSources.computeSessionFactoryName(getCurrentlySelectedCropDBName()));
	}

	private String getCurrentlySelectedCropDBName() {
		return this.contextResolver.resolveDatabaseFromUrl();
	}
	
	
	@Bean
	public UserTransaction userTransaction() throws Throwable {
		UserTransactionImp userTransactionImp = new UserTransactionImp();
		userTransactionImp.setTransactionTimeout(1000);
		return userTransactionImp;
	}

	@Bean(initMethod = "init", destroyMethod = "close")
	public TransactionManager transactionManager() throws Throwable {
		UserTransactionManager userTransactionManager = new UserTransactionManager();
		userTransactionManager.setForceShutdown(false);
		return userTransactionManager;
	}

	// We do not want the platform transaction manager created per request but in order to handle different corps we need to seaarch for it
	// per request. A hash map to cache
	@Bean
	public PlatformTransactionManager platformTransactionManager()  throws Throwable {
		
		return new JtaTransactionManager(userTransaction(), transactionManager());
	}
	
	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public StudyDataManager getStudyDataManager() throws FileNotFoundException {
		return new StudyDataManagerImpl(this.getCropDatabaseSessionProvider());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public FieldbookService getFieldbookService() throws FileNotFoundException {
		return new FieldbookServiceImpl(this.getCropDatabaseSessionProvider(), this.getCurrentlySelectedCropDBName());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public org.generationcp.middleware.service.api.study.StudyService getStudyService() throws FileNotFoundException {
		return new org.generationcp.middleware.service.impl.study.StudyServiceImpl(this.getCropDatabaseSessionProvider());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GenotypicDataManager getGenotypicDataManager() throws FileNotFoundException {
		return new GenotypicDataManagerImpl(this.getCropDatabaseSessionProvider());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public InventoryDataManager getInventoryDataManager() throws FileNotFoundException {
		return new InventoryDataManagerImpl(this.getCropDatabaseSessionProvider());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public LocationDataManager getLocationDataManager() throws FileNotFoundException {
		return new LocationDataManagerImpl(this.getCropDatabaseSessionProvider());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public UserDataManager getUserDataManager() throws FileNotFoundException {
		return new UserDataManagerImpl(this.getCropDatabaseSessionProvider());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GermplasmListManager getGermplasmListManager() throws FileNotFoundException {
		return new GermplasmListManagerImpl(this.getCropDatabaseSessionProvider(), this.getCurrentlySelectedCropDBName());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GermplasmDataManager getGermplasmDataManager() throws FileNotFoundException {
		return new GermplasmDataManagerImpl(this.getCropDatabaseSessionProvider(), this.getCurrentlySelectedCropDBName());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public DataImportService getDataImportService() throws FileNotFoundException {
		return new DataImportServiceImpl(this.getCropDatabaseSessionProvider());
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public PedigreeService getPedigreeService() throws FileNotFoundException {
		// FIXME - producing the default pedigree service impl. Make it configurable for CIMMYT wheat pedigree generation.
		return new PedigreeDefaultServiceImpl(this.getCropDatabaseSessionProvider());
	}

	@Bean()
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public HibernateSessionPerRequestProvider getCropDatabaseSessionProvider() throws FileNotFoundException {
		return new HibernateSessionPerRequestProvider(this.getSessionFactory());
	}

	@Bean
	@DependsOn("WORKBENCH_SessionFactory")
	public WorkbenchDataManager getWorkbenchDataManager() throws FileNotFoundException {
		return new WorkbenchDataManagerImpl(this.getWorkbenchSessionProvider());
	}
	

	private HibernateSessionPerRequestProvider getWorkbenchSessionProvider() throws FileNotFoundException {
		return new HibernateSessionPerRequestProvider(this.WORKBENCH_SessionFactory);
	}
	
}
