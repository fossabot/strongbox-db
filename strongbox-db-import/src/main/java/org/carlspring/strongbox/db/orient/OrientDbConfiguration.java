package org.carlspring.strongbox.db.orient;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Properties;

import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.jdbc.OrientDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.ReflectionUtils;

/**
 * @author Przemyslaw Fusik
 */
@Slf4j
@Configuration
class OrientDbConfiguration
{

    @Autowired
    private OrientDbProperties orientDbProperties;

    @Bean(destroyMethod = "close")
    @DependsOn("orientDbServer")
    OrientDB orientDB()
    {
        OrientDB orientDB = new OrientDB(StringUtils.substringBeforeLast(orientDbProperties.getUrl(), "/"),
                                         orientDbProperties.getUsername(),
                                         orientDbProperties.getPassword(),
                                         OrientDBConfig.defaultConfig());
        String database = orientDbProperties.getDatabase();

        if (!orientDB.exists(database))
        {
            log.info(String.format("Creating database [%s]...", database));

            orientDB.create(database, ODatabaseType.PLOCAL);
        }
        else
        {
            log.info("Re-using existing database " + database + ".");
        }
        return orientDB;
    }

    @Bean
    @LiquibaseDataSource
    DataSource dataSource(ODatabasePool pool,
                          OrientDB orientDB)
    {
        OrientDataSource ds = new OrientDataSource(orientDB);

        ds.setInfo(new Properties());

        // DEV note:
        // NPEx hotfix for OrientDataSource.java:134 :)
        Field poolField = ReflectionUtils.findField(OrientDataSource.class, "pool");
        ReflectionUtils.makeAccessible(poolField);
        ReflectionUtils.setField(poolField, ds, pool);

        return ds;
    }

    @Bean(destroyMethod = "close")
    ODatabasePool databasePool(OrientDB orientDB)
    {
        return new ODatabasePool(orientDB,
                                 orientDbProperties.getDatabase(),
                                 orientDbProperties.getUsername(),
                                 orientDbProperties.getPassword());
    }

}
