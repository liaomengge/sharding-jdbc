/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingjdbc.spring.boot;

import com.google.common.base.Preconditions;
import io.shardingjdbc.core.api.MasterSlaveDataSourceFactory;
import io.shardingjdbc.core.api.ShardingDataSourceFactory;
import io.shardingjdbc.core.exception.ShardingJdbcException;
import io.shardingjdbc.core.util.DataSourceUtil;
import io.shardingjdbc.spring.boot.masterslave.SpringBootMasterSlaveRuleConfigurationProperties;
import io.shardingjdbc.spring.boot.sharding.SpringBootShardingRuleConfigurationProperties;
import io.shardingjdbc.spring.boot.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring boot sharding and master-slave configuration.
 *
 * @author caohao
 */
@Configuration
@EnableConfigurationProperties({SpringBootShardingRuleConfigurationProperties.class, SpringBootMasterSlaveRuleConfigurationProperties.class})
public class SpringBootConfiguration implements EnvironmentAware {
    
    @Autowired
    private SpringBootShardingRuleConfigurationProperties shardingProperties;
    
    @Autowired
    private SpringBootMasterSlaveRuleConfigurationProperties masterSlaveProperties;
    
    private final Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();
    
    /**
     * Get data source bean.
     * 
     * @return data source bean
     * @throws SQLException SQL exception
     */
    @Bean
    public DataSource dataSource() throws SQLException {
        return null == masterSlaveProperties.getMasterDataSourceName() 
                ? ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingProperties.getShardingRuleConfiguration(), shardingProperties.getConfigMap(), shardingProperties.getProps())
                : MasterSlaveDataSourceFactory.createDataSource(dataSourceMap, masterSlaveProperties.getMasterSlaveRuleConfiguration(), masterSlaveProperties.getConfigMap());
    }
    
    @Override
    public void setEnvironment(final Environment environment) {
        setDataSourceMap(environment);
    }
    
    @SuppressWarnings("unchecked")
    private void setDataSourceMap(final Environment environment) {
        String prefix = "sharding.jdbc.datasource.";
        String dataSources = environment.getProperty(prefix + "names");
        for (String each : dataSources.split(",")) {
            try {
                Map<String, Object> dataSourceProps = PropertyUtil.handle(environment, prefix + each, Map.class);
                Preconditions.checkState(!dataSourceProps.isEmpty(), "Wrong datasource properties!");
                DataSource dataSource = DataSourceUtil.getDataSource(dataSourceProps.get("type").toString(), dataSourceProps);
                dataSourceMap.put(each, dataSource);
            } catch (final ReflectiveOperationException ex) {
                throw new ShardingJdbcException("Can't find datasource type!", ex);
            }
        }
    }
}
