/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.docker;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class SpringDockerComposeRunner extends DockerComposeRunner {

    private static final Logger logger = LoggerFactory.getLogger(SpringDockerComposeRunner.class);
    private static final long SERVICE_DISCOVERY_REFRESH_INTERVAL = 30 * 1000;
    private final EurekaClient discoveryClient;
    private final List<String> reInitializeServices;
    private final List<String> requiredServices;
    private final boolean waitForServicesToBeLaunched;
    private final boolean restartExistingContainers;

    public SpringDockerComposeRunner(EurekaClient discoveryClient, String... requiredServices) {
        this(discoveryClient, true, false, requiredServices);
    }

    public SpringDockerComposeRunner(EurekaClient discoveryClient, List<String> reInitializeServices, String... requiredServices) {
        this(discoveryClient, true, false, reInitializeServices, requiredServices);
    }

    public SpringDockerComposeRunner(EurekaClient discoveryClient, boolean waitForServicesToBeLaunched, boolean restartExistingContainers, String... requiredServices) {
        this(discoveryClient, waitForServicesToBeLaunched, restartExistingContainers, Collections.emptyList(), requiredServices);
    }

    public SpringDockerComposeRunner(EurekaClient discoveryClient, boolean waitForServicesToBeLaunched, boolean restartExistingContainers, List<String> reInitializeServices, String... requiredServices) {
        super();
        this.discoveryClient = discoveryClient;
        this.reInitializeServices = reInitializeServices;
        this.requiredServices = Arrays.asList(requiredServices);
        this.restartExistingContainers = restartExistingContainers;
        this.waitForServicesToBeLaunched = waitForServicesToBeLaunched;
    }


    public void start() {
        try {

            if (restartExistingContainers) {
                invokeDockerComposeDown();
            }

            if (this.reInitializeServices != null) {
                String[] services = this.reInitializeServices.toArray(new String[0]);
                dockerComposeRemove(services);
                invokeDockerComposeUp(services);
                for (String service : services) {
                    waitForService(service, new Date(), true, this::isServiceUpByNameAndPort);
                }
            }
            Set<String> launchedServices = ensureInfrastructure();
            if (launchedServices != null && launchedServices.contains("kg-service-discovery")) {
                waitForOtherServicesToRegister();
            }
            String[] nonRunningRequiredServices = requiredServices.stream().filter(s -> !isServiceUpByDiscovery(s)).toArray(String[]::new);
            invokeDockerComposeUp(nonRunningRequiredServices);
            if (waitForServicesToBeLaunched) {
                for (String requiredService : requiredServices) {
                    waitForService(requiredService, new Date(), true, this::isServiceUpByDiscovery);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForOtherServicesToRegister() {
        logger.info(String.format("Waiting for already existing services to register in newly instantiated service discovery. We wait for %dms", SERVICE_DISCOVERY_REFRESH_INTERVAL));
        try {
            Thread.sleep(SERVICE_DISCOVERY_REFRESH_INTERVAL);
        } catch (InterruptedException e) {
            logger.warn("Was interrupted while waiting for other services to register", e);
        }
    }

    private boolean isServiceUpByDiscovery(String service) {
        Application application = discoveryClient.getApplication(service);
        if (application == null) {
            return false;
        }
        List<InstanceInfo> instancesById = application.getInstancesAsIsFromEureka();
        for (InstanceInfo instanceInfo : instancesById) {
            InstanceInfo.InstanceStatus status = instanceInfo.getStatus();
            switch (status) {
                case UP:
                case STARTING:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }


}
