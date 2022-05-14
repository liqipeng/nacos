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
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.alibaba.nacos.api.naming.PreservedMetadataKeys.HEART_BEAT_INTERVAL;
import static com.alibaba.nacos.api.naming.PreservedMetadataKeys.HEART_BEAT_TIMEOUT;
import static com.alibaba.nacos.api.naming.PreservedMetadataKeys.IP_DELETE_TIMEOUT;

/**
 * Naming example.
 *
 * @author liqipeng
 */
public class NewNamingExample {
    
    public static void main(String[] args) throws NacosException, InterruptedException {
        final Logger logger = LoggerFactory.getLogger(NewNamingExample.class);
        
        String serverAddr = "127.0.0.1:8848";
        String time = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String serviceName = "test-" + time;
        String group = "NOT_DEFAULT_GROUP";
        String instanceIp = "127.0.0.1";
        int instancePort = 8333;
        
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.setProperty("username", "nacos");
        
        {
            // use incorrect password
            properties.setProperty("password", "nacos1");
            NamingService namingService = NacosFactory.createNamingService(properties);
            Throwable exception = null;
            try {
                // only write operation will be reject when password is incorrect.
                namingService.registerInstance(serviceName, group, instanceIp, instancePort);
            } catch (Throwable ex) {
                exception = ex;
            } finally {
                namingService.shutDown();
            }
            if (exception == null || !(exception instanceof NacosException)
                    || ((NacosException) exception.getCause()).getErrCode() != 403) {
                throw new RuntimeException("Unexcepted result!");
            }
        }
        
        // use correct password
        properties.setProperty("password", "nacos");
        NamingService namingService = NacosFactory.createNamingService(properties);
        String status = namingService.getServerStatus();
        final String upStatus = "UP";
        if (!upStatus.equals(status)) {
            throw new RuntimeException("Unexcepted result!");
        }
        
        namingService.subscribe(serviceName, group, new EventListener() {
            @Override
            public void onEvent(Event event) {
                logger.info("listener received event: {}", event);
            }
        });
        
        Instance instance = new Instance();
        instance.setIp(instanceIp);
        instance.setPort(instancePort);
        Map<String, String> meta = new HashMap<>(3);
        meta.put(HEART_BEAT_TIMEOUT, "102410241024");
        meta.put(IP_DELETE_TIMEOUT, "102410241024");
        meta.put(HEART_BEAT_INTERVAL, "102410241024");
        instance.setMetadata(meta);
        namingService.registerInstance(serviceName, group, instance);
        logger.info("registerInstance ok.");
        
        int queryCount = 0;
        while (namingService.getAllInstances(serviceName, group).stream()
                .filter(i -> i.getIp().equals(instanceIp) && i.getPort() == instancePort).count() == 0) {
            queryCount++;
            logger.info("Instance not found, sleep 100 ms. - {}", queryCount);
            Thread.sleep(100);
        }
        logger.info("Instance found! - {}", queryCount);
        
        namingService.deregisterInstance(serviceName, group, instanceIp, instancePort);
        logger.info("deregisterInstance ok.");
        
        queryCount = 0;
        while (namingService.getAllInstances(serviceName, group).stream()
                .filter(i -> i.getIp().equals(instanceIp) && i.getPort() == instancePort).count() != 0) {
            queryCount++;
            logger.info("Instance still found, sleep 100 ms. - {}", queryCount);
            Thread.sleep(100);
        }
        
        logger.info("Intance really removed now.");
        
        namingService.shutDown();
    }
}
