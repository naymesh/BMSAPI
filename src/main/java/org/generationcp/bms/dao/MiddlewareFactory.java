
package org.generationcp.bms.dao;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.generationcp.bms.context.ContextResolver;
import org.generationcp.middleware.hibernate.HibernateSessionPerRequestProvider;
import org.generationcp.middleware.hibernate.SessionFactoryUtil;
import org.generationcp.middleware.manager.DatabaseConnectionParameters;
import org.generationcp.middleware.manager.GenotypicDataManagerImpl;
import org.generationcp.middleware.manager.GermplasmDataManagerImpl;
import org.generationcp.middleware.manager.GermplasmListManagerImpl;
import org.generationcp.middleware.manager.InventoryDataManagerImpl;
import org.generationcp.middleware.manager.LocationDataManagerImpl;
import org.generationcp.middleware.manager.OntologyDataManagerImpl;
import org.generationcp.middleware.manager.StudyDataManagerImpl;
import org.generationcp.middleware.manager.UserDataManagerImpl;
import org.generationcp.middleware.manager.api.GenotypicDataManager;
import org.generationcp.middleware.manager.api.GermplasmDataManager;
import org.generationcp.middleware.manager.api.GermplasmListManager;
import org.generationcp.middleware.manager.api.InventoryDataManager;
import org.generationcp.middleware.manager.api.LocationDataManager;
import org.generationcp.middleware.manager.api.OntologyDataManager;
import org.generationcp.middleware.manager.api.StudyDataManager;
import org.generationcp.middleware.manager.api.UserDataManager;
import org.generationcp.middleware.service.DataImportServiceImpl;
import org.generationcp.middleware.service.FieldbookServiceImpl;
import org.generationcp.middleware.service.OntologyServiceImpl;
import org.generationcp.middleware.service.api.DataImportService;
import org.generationcp.middleware.service.api.FieldbookService;
import org.generationcp.middleware.service.api.OntologyService;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class MiddlewareFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MiddlewareFactory.class); 
	
	private final Map<String, SessionFactory> sessionFactoryCache = new HashMap<String, SessionFactory>();
	
	@Autowired
	private ApiEnvironmentConfiguration config;
	
	@Autowired
	private ContextResolver contextResolver;
	
	@PreDestroy
	public void preDestroy() {
		LOGGER.info("Closing cached session factories.");
		for(String key : sessionFactoryCache.keySet()) {
			sessionFactoryCache.get(key).close();
		}
	}
	
	private SessionFactory getCentralSessionFactory() throws FileNotFoundException {
		String selectedCentralDB = getCurrentlySelectedCentralDBName();
		SessionFactory sessionFactory;

		if (this.sessionFactoryCache.get(selectedCentralDB) == null) {
			DatabaseConnectionParameters centralConnectionParams = new DatabaseConnectionParameters(
					config.getDbHost(), config.getDbPort(), selectedCentralDB, config.getDbUsername(), config.getDbPassword());
			sessionFactory = SessionFactoryUtil.openSessionFactory(centralConnectionParams);
			sessionFactoryCache.put(selectedCentralDB, sessionFactory);
		} else {
			sessionFactory = this.sessionFactoryCache.get(selectedCentralDB);
		}
		return sessionFactory;
	}
	
	private SessionFactory getLocalSessionFactory() throws FileNotFoundException {
		String selectedLocalDB = getCurrentlySelectedLocalDBName();
		SessionFactory sessionFactory;

		if (this.sessionFactoryCache.get(selectedLocalDB) == null) {
			DatabaseConnectionParameters localConnectionParams = new DatabaseConnectionParameters(
					config.getDbHost(), config.getDbPort(), selectedLocalDB, config.getDbUsername(), config.getDbPassword());
			sessionFactory = SessionFactoryUtil.openSessionFactory(localConnectionParams);
			sessionFactoryCache.put(selectedLocalDB, sessionFactory);
		} else {
			sessionFactory = this.sessionFactoryCache.get(selectedLocalDB);
		}
		return sessionFactory;
	}
	
	private String getCurrentlySelectedCentralDBName() {
		return this.contextResolver.resolveProgram().getCentralDbName();
	}
	
	private String getCurrentlySelectedLocalDBName() {
		return this.contextResolver.resolveProgram().getLocalDbName();
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public StudyDataManager getStudyDataManager() throws FileNotFoundException {		
		return new StudyDataManagerImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public FieldbookService getFieldbookService() throws FileNotFoundException {
		return new FieldbookServiceImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()), getCurrentlySelectedLocalDBName(), getCurrentlySelectedCentralDBName());
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GenotypicDataManager getGenotypicDataManager() throws FileNotFoundException {
		return new GenotypicDataManagerImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public OntologyDataManager getOntologyDataManager() throws FileNotFoundException {
		return new OntologyDataManagerImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public OntologyService getOntologyService() throws FileNotFoundException {
		return new OntologyServiceImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public InventoryDataManager getInventoryDataManager() throws FileNotFoundException {
		return new InventoryDataManagerImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public LocationDataManager getLocationDataManager() throws FileNotFoundException {
		return new LocationDataManagerImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public UserDataManager getUserDataManager() throws FileNotFoundException {
		return new UserDataManagerImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GermplasmListManager getGermplasmListManager() throws FileNotFoundException {
		return new GermplasmListManagerImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()), getCurrentlySelectedLocalDBName(), getCurrentlySelectedCentralDBName());
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GermplasmDataManager getGermplasmDataManager() throws FileNotFoundException {
		return new GermplasmDataManagerImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()), getCurrentlySelectedLocalDBName(), getCurrentlySelectedCentralDBName());
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public DataImportService getDataImportService() throws FileNotFoundException {
		return new DataImportServiceImpl(new HibernateSessionPerRequestProvider(getLocalSessionFactory()), 
				new HibernateSessionPerRequestProvider(getCentralSessionFactory()));
	}

	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public JdbcTemplate getJDBCTemplate() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				String.format("jdbc:mysql://%s:%s/%s", config.getDbHost(), config.getDbPort(), getCurrentlySelectedCentralDBName()), 
				config.getDbUsername(), config.getDbPassword());
		
		return new JdbcTemplate(dataSource);
	}
}
