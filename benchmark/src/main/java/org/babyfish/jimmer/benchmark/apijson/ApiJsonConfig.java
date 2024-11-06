package org.babyfish.jimmer.benchmark.apijson;

import apijson.Log;
import apijson.framework.APIJSONCreator;
import apijson.framework.APIJSONParser;
import apijson.framework.APIJSONSQLConfig;
import apijson.framework.APIJSONSQLExecutor;
import apijson.orm.SQLConfig;
import apijson.orm.SQLExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class ApiJsonConfig {
    @Bean
    public APIJSONCreator<Long> apiJsonParse(DataSource dataSource) {
        APIJSONCreator<Long> apijsonCreator = new APIJSONCreator<>() {
            @Override
            public SQLExecutor<Long> createSQLExecutor() {
                return new APIJSONSQLExecutor<>() {
                    @Override
                    public Connection getConnection(SQLConfig<Long> config) throws Exception {
                        String key = config.getDatasource() + "-" + config.getDatabase();
                        Connection c = connectionMap.get(key);
                        if (c == null || c.isClosed()) {
                            connectionMap.put(key, dataSource == null ? null : dataSource.getConnection());
                        }
                        return super.getConnection(config);
                    }
                };
            }

            @Override
            public SQLConfig<Long> createSQLConfig() {
                return new APIJSONSQLConfig<>() {
                    @Override
                    public String getSQLSchema() {
                        return "";
                    }

                    @Override
                    public boolean limitSQLCount() {
                        return false;
                    }

                    @Override
                    public String getLimitString() {
                        return "";
                    }
                };
            }
        };
        Log.DEBUG = false;
        APIJSONParser.APIJSON_CREATOR = apijsonCreator;
        APIJSONSQLConfig.APIJSON_CREATOR = apijsonCreator;
        return apijsonCreator;
    }
}
