package fr.softsf.sudofx2024.utils.database.configuration;

import com.zaxxer.hikari.HikariDataSource;
import fr.softsf.sudofx2024.utils.MyLogback;
import fr.softsf.sudofx2024.utils.database.DatabaseMigration;
import fr.softsf.sudofx2024.utils.database.keystore.ApplicationKeystore;
import fr.softsf.sudofx2024.utils.os.OsFolderFactoryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

import static fr.softsf.sudofx2024.utils.MyEnums.Paths.DATABASE_NAME;

/**
 * Configuration class for setting up dynamic data sources and related beans.
 */
@Configuration
public class DynamicDataSourceConfiguration {

    /**
     * Initializes Logback logging framework.
     *
     * @param myLogback Custom Logback configuration bean
     * @return Always returns 0
     */
    @Bean
    int logbackInitialization(@Autowired MyLogback myLogback) {
        myLogback.printLogEntryMessage();
        return 0;
    }

    /**
     * Configures and performs database migration. This bean depends on
     * logbackInitialization to ensure logging is set up first.
     *
     * @param osFolderFactory Factory for creating OS-specific folders
     * @param keystore        Application keystore for secure storage
     * @return Always returns 0
     */
    @DependsOn({"logbackInitialization"})
    @Bean
    int databaseMigration(@Autowired OsFolderFactoryManager osFolderFactory, @Autowired ApplicationKeystore keystore) {
        keystore.setupApplicationKeystore();
        DatabaseMigration.configure(keystore, osFolderFactory.osFolderFactory());
        return 0;
    }

    /**
     * Creates and configures the main DataSource for the application. This bean
     * depends on databaseMigration to ensure the database is properly set up.
     *
     * @param osFolderFactory Factory for creating OS-specific folders
     * @param keystore        Application keystore for secure storage
     * @return Configured DataSource
     */
    @DependsOn({"databaseMigration"})
    @Bean
    public DataSource dataSourceInitialization(@Autowired OsFolderFactoryManager osFolderFactory, @Autowired ApplicationKeystore keystore) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.hsqldb.jdbc.JDBCDriver")
                .url("jdbc:hsqldb:file:" + osFolderFactory.osFolderFactory().getOsDataFolderPath() + "/" + DATABASE_NAME.getPath() + ";shutdown=true")
                .username(keystore.getUsername())
                .password(keystore.getPassword())
                .build();
    }

    /**
     * Creates and configures the EntityManagerFactory for JPA.
     *
     * @param dataSourceInitialization The initialized DataSource
     * @return Configured LocalContainerEntityManagerFactoryBean
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Autowired DataSource dataSourceInitialization) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSourceInitialization);
        entityManagerFactory.setPackagesToScan("fr.softsf.sudofx2024.model");
        entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactory.getJpaPropertyMap().put("hibernate.format_sql", "true");
        entityManagerFactory.getJpaPropertyMap().put("hibernate.use_sql_comments", "true");
        entityManagerFactory.getJpaPropertyMap().put("hibernate.show_sql", "true");
        entityManagerFactory.getJpaPropertyMap().put("spring.jpa.open-in-view", "false");
        entityManagerFactory.getJpaPropertyMap().put("spring.datasource.hikari.auto-commit", "false");
        return entityManagerFactory;
    }
}
