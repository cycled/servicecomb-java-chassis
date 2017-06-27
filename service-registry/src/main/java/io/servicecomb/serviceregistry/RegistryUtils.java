/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.serviceregistry;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.netflix.config.DynamicPropertyFactory;

import io.servicecomb.config.ConfigUtil;
import io.servicecomb.config.archaius.sources.MicroserviceConfigLoader;
import io.servicecomb.foundation.common.net.IpPort;
import io.servicecomb.foundation.common.net.NetUtils;
import io.servicecomb.serviceregistry.api.registry.Microservice;
import io.servicecomb.serviceregistry.api.registry.MicroserviceInstance;
import io.servicecomb.serviceregistry.api.registry.MicroserviceManager;
import io.servicecomb.serviceregistry.cache.InstanceCacheManager;
import io.servicecomb.serviceregistry.cache.InstanceVersionCacheManager;
import io.servicecomb.serviceregistry.client.ServiceRegistryClient;
import io.servicecomb.serviceregistry.config.ServiceRegistryConfig;
import io.servicecomb.serviceregistry.registry.ServiceRegistryFactory;

public final class RegistryUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryUtils.class);

    private static ServiceRegistry serviceRegistry;

    // value is ip or {interface name}
    public static final String PUBLISH_ADDRESS = "cse.service.publishAddress";

    private static final String PUBLISH_PORT = "cse.{transport_name}.publishPort";

    private RegistryUtils() {
    }

    public static void init() {
        EventBus eventBus = new EventBus();
        MicroserviceConfigLoader loader = ConfigUtil.getMicroserviceConfigLoader();
        serviceRegistry = ServiceRegistryFactory.getOrCreate(eventBus, ServiceRegistryConfig.INSTANCE, loader);
        serviceRegistry.init();
    }

    public static void run() {
        serviceRegistry.run();
    }

    public static void destory() {
        serviceRegistry.destory();
    }

    public static ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public static void setServiceRegistry(ServiceRegistry serviceRegistry) {
        RegistryUtils.serviceRegistry = serviceRegistry;
    }

    public static ServiceRegistryClient getServiceRegistryClient() {
        return serviceRegistry.getServiceRegistryClient();
    }

    public static InstanceCacheManager getInstanceCacheManager() {
        return serviceRegistry.getInstanceCacheManager();
    }

    public static InstanceVersionCacheManager getInstanceVersionCacheManager() {
        return serviceRegistry.getInstanceVersionCacheManager();
    }

    public static MicroserviceManager getMicroserviceManager() {
        return serviceRegistry.getMicroserviceManager();
    }

    public static String getAppId() {
        return serviceRegistry.getMicroserviceManager().getAppId();
    }

    public static Microservice getMicroservice() {
        return serviceRegistry.getMicroservice();
    }

    public static MicroserviceInstance getMicroserviceInstance() {
        return serviceRegistry.getMicroserviceInstance();
    }

    public static String getPublishAddress() {
        String publicAddressSetting =
            DynamicPropertyFactory.getInstance().getStringProperty(PUBLISH_ADDRESS, "").get();
        publicAddressSetting = publicAddressSetting.trim();
        if (publicAddressSetting.isEmpty()) {
            return NetUtils.getHostAddress();
        }

        // placeholder is network interface name
        if (publicAddressSetting.startsWith("{") && publicAddressSetting.endsWith("}")) {
            return NetUtils
                    .ensureGetInterfaceAddress(publicAddressSetting.substring(1, publicAddressSetting.length() - 1))
                    .getHostAddress();
        }

        return publicAddressSetting;
    }

    public static String getPublishHostName() {
        String publicAddressSetting =
            DynamicPropertyFactory.getInstance().getStringProperty(PUBLISH_ADDRESS, "").get();
        publicAddressSetting = publicAddressSetting.trim();
        if (publicAddressSetting.isEmpty()) {
            return NetUtils.getHostName();
        }

        if (publicAddressSetting.startsWith("{") && publicAddressSetting.endsWith("}")) {
            return NetUtils
                    .ensureGetInterfaceAddress(publicAddressSetting.substring(1, publicAddressSetting.length() - 1))
                    .getHostName();
        }

        return publicAddressSetting;
    }

    /**
     * 对于配置为0.0.0.0的地址，通过查询网卡地址，转换为实际监听的地址。
     */
    public static String getPublishAddress(String schema, String address) {
        if (address == null) {
            return address;
        }

        try {
            String publicAddressSetting = DynamicPropertyFactory.getInstance()
                    .getStringProperty(PUBLISH_ADDRESS, "")
                    .get();
            publicAddressSetting = publicAddressSetting.trim();

            URI originalURI = new URI(schema + "://" + address);
            IpPort ipPort = NetUtils.parseIpPort(originalURI.getAuthority());
            if (ipPort == null) {
                LOGGER.warn("address {} not valid.", address);
                return null;
            }

            InetSocketAddress socketAddress = ipPort.getSocketAddress();
            String host = socketAddress.getAddress().getHostAddress();

            if (publicAddressSetting.isEmpty()) {
                if (socketAddress.getAddress().isAnyLocalAddress()) {
                    host = NetUtils.getHostAddress();
                    LOGGER.warn("address {}, auto select a host address to publish {}:{}, maybe not the correct one",
                            address,
                            host,
                            socketAddress.getPort());
                    URI newURI = new URI(originalURI.getScheme(), originalURI.getUserInfo(), host,
                            originalURI.getPort(), originalURI.getPath(), originalURI.getQuery(),
                            originalURI.getFragment());
                    return newURI.toString();
                } else {
                    return originalURI.toString();
                }
            }

            if (publicAddressSetting.startsWith("{") && publicAddressSetting.endsWith("}")) {
                publicAddressSetting = NetUtils
                        .ensureGetInterfaceAddress(
                                publicAddressSetting.substring(1, publicAddressSetting.length() - 1))
                        .getHostAddress();
            }

            String publishPortKey = PUBLISH_PORT.replace("{transport_name}", originalURI.getScheme());
            int publishPortSetting = DynamicPropertyFactory.getInstance()
                    .getIntProperty(publishPortKey, 0)
                    .get();
            int publishPort = publishPortSetting == 0 ? originalURI.getPort() : publishPortSetting;
            URI newURI = new URI(originalURI.getScheme(), originalURI.getUserInfo(), publicAddressSetting,
                    publishPort, originalURI.getPath(), originalURI.getQuery(), originalURI.getFragment());
            return newURI.toString();

        } catch (URISyntaxException e) {
            LOGGER.warn("address {} not valid.", address);
            return null;
        }
    }

    public static List<MicroserviceInstance> findServiceInstance(String appId, String serviceName,
            String versionRule) {
        return serviceRegistry.findServiceInstance(appId, serviceName, versionRule);
    }

    // update microservice instance properties
    // if there are multiple microservice, then throw exception
    public static boolean updateInstanceProperties(Map<String, String> instanceProperties) {
        return serviceRegistry.updateInstanceProperties(instanceProperties);
    }

    public static boolean updateInstanceProperties(String microserviceName, Map<String, String> instanceProperties) {
        return serviceRegistry.updateInstanceProperties(microserviceName, instanceProperties);
    }

    public static Microservice getMicroservice(String microserviceId) {
        return serviceRegistry.getRemoteMicroservice(microserviceId);
    }
}