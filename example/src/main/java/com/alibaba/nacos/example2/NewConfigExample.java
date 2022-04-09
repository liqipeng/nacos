/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.example2;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Config example.
 *
 * @author liqipeng
 */
public class NewConfigExample {
    
    @SuppressWarnings("AlibabaMethodTooLong")
    public static void main(String[] args) throws NacosException, InterruptedException {
        final Logger logger = LoggerFactory.getLogger(NewConfigExample.class);
        
        final String serverAddr = "127.0.0.1:8848";
        final String time = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        final String configContent = "content: " + time;
        final String dataId = "test-" + time;
        final String group = "DEFAULT_GROUP";
        final int timeout = 5000;
        
        final Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.setProperty("username", "nacos");
        
        {
            // use error password
            properties.setProperty("password", "nacos1");
            ConfigService configService = NacosFactory.createConfigService(properties);
            Throwable exception = null;
            try {
                // read operation also be rejected, and it is not the same with naming.
                configService.getConfig(dataId, group, timeout);
            } catch (Throwable ex) {
                exception = ex;
            } finally {
                configService.shutDown();
            }
            if (!(exception instanceof NacosException)
                    || ((NacosException) exception).getErrCode() != 403) {
                throw new RuntimeException("Unexcepted result!");
            }
        }
        
        // use incorrect password
        properties.setProperty("password", "nacos");
        ConfigService configService = NacosFactory.createConfigService(properties);
        String content = configService.getConfig(dataId, group, timeout);
        if (content != null) {
            throw new RuntimeException("Unexcepted result!");
        }
        
        configService.addListener(dataId, group, new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                logger.info("listener received: {}", configInfo);
            }
            
            @Override
            public Executor getExecutor() {
                return null;
            }
        });
        
        boolean isPublishOk = configService.publishConfig(dataId, group, configContent);
        logger.info("isPublishOk: {}", isPublishOk);
        
        String readContent;
        int readCnt = 0;
        while ((readContent = configService.getConfig(dataId, group, timeout)) == null) {
            readCnt++;
            logger.info("Config content not found, sleep 100 ms. - {}", readCnt);
            Thread.sleep(100);
        }
        
        if (readContent.equals(configContent)) {
            logger.info("readContent: {}", readContent);
        } else {
            throw new RuntimeException("Unexcepted content from ConfigService!");
        }
        
        boolean isRemoveOk = configService.removeConfig(dataId, group);
        logger.info("isRemoveOk: {}", isRemoveOk);
        
        readCnt = 0;
        while (configService.getConfig(dataId, group, timeout) != null) {
            readCnt++;
            logger.info("Config content still found after remove, sleep 100 ms. - {}", readCnt);
            Thread.sleep(100);
        }
        
        logger.info("Removing config confirmed - {}.", configService.getConfig(dataId, group, timeout));
        
        configService.shutDown();
    }
}
