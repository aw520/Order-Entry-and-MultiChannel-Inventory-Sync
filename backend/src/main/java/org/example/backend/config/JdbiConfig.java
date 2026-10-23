package org.example.backend.config;

import javax.sql.DataSource;

import org.example.backend.repository.InventoryRepository;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.spring.EnableJdbiRepositories;
import org.jdbi.v3.spring.SpringConnectionFactory;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableJdbiRepositories(basePackageClasses = InventoryRepository.class)
public class JdbiConfig {

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(new SpringConnectionFactory(dataSource));
        jdbi.installPlugin(new SqlObjectPlugin());
        return jdbi;
    }
}
