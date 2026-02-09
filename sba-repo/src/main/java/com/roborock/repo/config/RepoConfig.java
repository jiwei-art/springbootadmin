package com.roborock.repo.config;

import com.github.pagehelper.PageInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.roborock.repo.props.DatabaseProperties;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "com.roborock", sqlSessionTemplateRef = "sqlSessionTemplate", annotationClass = Mapper.class)
public class RepoConfig {

    private DatabaseProperties databaseProperties;

    RepoConfig(DatabaseProperties databaseProperties) {
        this.databaseProperties = databaseProperties;
    }

    @Bean
    public DataSource dataSource() {
        HikariDataSource hikariDataSource = DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .driverClassName(databaseProperties.getDriverClassName())
            .url(databaseProperties.getDbUrl())
            .username(databaseProperties.getUsername())
            .password(databaseProperties.getPassword())
            .build();

        hikariDataSource.setPoolName("Postgres-CP");
        return hikariDataSource;
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource);

        PageInterceptor pageInterceptor = new PageInterceptor();
        pageInterceptor.setProperties(new Properties() {{
            setProperty("helperDialect", "postgresql");
        }});
        sqlSessionFactory.setPlugins(pageInterceptor);

        return sqlSessionFactory;
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        configuration.setCallSettersOnNulls(true);
        configuration.setMapUnderscoreToCamelCase(true);
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}
