
package org.generationcp.bms.dao;

import org.generationcp.bms.context.ContextResolver;
import org.generationcp.middleware.exceptions.MiddlewareQueryException;
import org.generationcp.middleware.hibernate.HibernateSessionPerRequestProvider;
import org.generationcp.middleware.hibernate.SessionFactoryUtil;
import org.generationcp.middleware.manager.*;
import org.generationcp.middleware.manager.api.*;
import org.generationcp.middleware.service.DataImportServiceImpl;
import org.generationcp.middleware.service.FieldbookServiceImpl;
import org.generationcp.middleware.service.OntologyManagerServiceImpl;
import org.generationcp.middleware.service.OntologyServiceImpl;
import org.generationcp.middleware.service.api.DataImportService;
import org.generationcp.middleware.service.api.FieldbookService;
import org.generationcp.middleware.service.api.OntologyManagerService;
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

import javax.annotation.PreDestroy;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MiddlewareFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MiddlewareFactory.class); 
	
	private final Map<String, SessionFactory> sessionFactoryCache = new HashMap<>();
	
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
	
	private SessionFactory getSessionFactory() throws FileNotFoundException {
		String selectedCropDB = getCurrentlySelectedCropDBName();
        SessionFactory sessionFactory;

		if (this.sessionFactoryCache.get(selectedCropDB) == null) {

			//NOTE: This will check weather selected crop db exist or not.
			//TODO: Add proper exception that handle this scenario.
			try {
				Connection conn = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s", config.getDbHost(), config.getDbPort(), selectedCropDB), config.getDbUsername(), config.getDbPassword());
				conn.isValid(5);// 5 sec
			} catch (SQLException e) {
				throw new FileNotFoundException("selected.crop.not.valid");
			}

			DatabaseConnectionParameters connectionParams = new DatabaseConnectionParameters(config.getDbHost(), config.getDbPort(), selectedCropDB, config.getDbUsername(), config.getDbPassword());

			sessionFactory = SessionFactoryUtil.openSessionFactory(connectionParams);
			sessionFactoryCache.put(selectedCropDB, sessionFactory);
		} else {
			sessionFactory = this.sessionFactoryCache.get(selectedCropDB);
		}
		return sessionFactory;
	}

	private String getCurrentlySelectedCropDBName() {
        return this.contextResolver.resolveDatabaseFromUrl();
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public StudyDataManager getStudyDataManager() throws FileNotFoundException {		
		return new StudyDataManagerImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public FieldbookService getFieldbookService() throws FileNotFoundException {
		return new FieldbookServiceImpl(new HibernateSessionPerRequestProvider(getSessionFactory()), getCurrentlySelectedCropDBName());
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GenotypicDataManager getGenotypicDataManager() throws FileNotFoundException {
		return new GenotypicDataManagerImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public OntologyDataManager getOntologyDataManager() throws FileNotFoundException {
		return new OntologyDataManagerImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public OntologyService getOntologyService() throws FileNotFoundException {
		return new OntologyServiceImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
	}

    @Bean
    @Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public OntologyManagerService getOntologyManagerService() throws FileNotFoundException {
        return new OntologyManagerServiceImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
    }
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public InventoryDataManager getInventoryDataManager() throws FileNotFoundException {
		return new InventoryDataManagerImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public LocationDataManager getLocationDataManager() throws FileNotFoundException {
		return new LocationDataManagerImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public UserDataManager getUserDataManager() throws FileNotFoundException {
		return new UserDataManagerImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GermplasmListManager getGermplasmListManager() throws FileNotFoundException {
		return new GermplasmListManagerImpl(new HibernateSessionPerRequestProvider(getSessionFactory()), getCurrentlySelectedCropDBName());
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public GermplasmDataManager getGermplasmDataManager() throws FileNotFoundException {
		return new GermplasmDataManagerImpl(new HibernateSessionPerRequestProvider(getSessionFactory()), getCurrentlySelectedCropDBName());
	}
	
	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public DataImportService getDataImportService() throws FileNotFoundException {
		return new DataImportServiceImpl(new HibernateSessionPerRequestProvider(getSessionFactory()));
	}

	@Bean
	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public JdbcTemplate getJDBCTemplate() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				String.format("jdbc:mysql://%s:%s/%s", config.getDbHost(), config.getDbPort(), getCurrentlySelectedCropDBName()), 
				config.getDbUsername(), config.getDbPassword());
		
		return new JdbcTemplate(dataSource);
	}
	
	@Bean
	@Scope(value = "singleton")
	public WorkbenchDataManager getWorkbenchDataManager() throws FileNotFoundException, MiddlewareQueryException {
		DatabaseConnectionParameters workbenchConnectionParameters = new DatabaseConnectionParameters(
				config.getDbHost(), config.getDbPort(), config.getWorkbenchDBName(), config.getDbUsername(), config.getDbPassword());		
		SessionFactory sessionFactory = SessionFactoryUtil.openSessionFactory(workbenchConnectionParameters);
		HibernateSessionPerRequestProvider sessionProvider = new HibernateSessionPerRequestProvider(sessionFactory);
		return new WorkbenchDataManagerImpl(sessionProvider);
	}
}
