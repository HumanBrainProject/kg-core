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
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;

public class DockerComposeRunner {

    private static final long DOCKER_LAUNCH_TIMEOUT = 5 * 60 * 1000;
    private static final Logger logger = LoggerFactory.getLogger(DockerComposeRunner.class);
    private static final String DOCKER_EXECUTION_PATH = "/tmp";
    private static final String TARGET_DOCKER_FILE_PREFIX = DOCKER_EXECUTION_PATH + "/test-";
    private static final String DOCKER_COMPOSE_BIN = "docker-compose";
    private static final String SOURCE_DOCKER_COMPOSE_FILE = "docker-compose.yml";
    private static final String SOURCE_DOCKER_INFRA_COMPOSE_FILE = "docker-compose-infra.yml";

    private static final Path TARGET_DOCKER_COMPOSE_FILE_PATH =  Paths.get(TARGET_DOCKER_FILE_PREFIX + SOURCE_DOCKER_COMPOSE_FILE);

    private static final List<String> networkInterfaces = Arrays.asList("docker0", "en0", "en6");

    protected final String externalIp;
    private final boolean printDockerLogs = true;

    public DockerComposeRunner() {
        this.externalIp = findExternalIp();
    }

    protected Set<String> ensureInfrastructure() throws IOException {
        Path path = moveDockerComposeInfra();
        Map<String, String> servicesWithPort = readDockerComposeYml(path);
        Set<String> launchedServices = new HashSet<>();
        for (String service : servicesWithPort.keySet()) {
            String port = servicesWithPort.get(service);
            if (!isServiceUpBySocket(port)) {
                dockerUp(path, service);
                launchedServices.add(service);
                waitForService(port, new Date(), true, this::isServiceUpBySocket);
            }
        }
        return launchedServices;
    }


    public boolean isServiceUpByNameAndPort(String name){
        Map<String, String> serviceWithPort = readDockerComposeYml(TARGET_DOCKER_COMPOSE_FILE_PATH);
        String port = serviceWithPort.get(name);
        if(port!=null){
            return isServiceUpBySocket(port);
        }
        throw new RuntimeException(String.format("Wasn't able to find service %s in docker compose!", name));
    }

    public boolean isServiceUpBySocket(String port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(externalIp, Integer.valueOf(port)), 30000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Map<String, String> readDockerComposeYml(Path dockerCompose)  {
        Map<String, String> serviceAndPort = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            Map map = mapper.readValue(dockerCompose.toFile(), Map.class);
            Object services = map.get("services");
            if (services instanceof Map) {
                Map servicesMap = (Map) services;
                for (Object key : servicesMap.keySet()) {
                    Object service = servicesMap.get(key);
                    if (service instanceof Map) {
                        Map serviceMap = (Map) service;
                        Object ports = serviceMap.get("ports");
                        if (ports instanceof Collection) {
                            Collection portList = (Collection) ports;
                            if (!portList.isEmpty()) {
                                Object firstPort = portList.iterator().next();
                                if (firstPort instanceof String) {
                                    String externalPort = ((String) firstPort).split(":")[0];
                                    serviceAndPort.put((String) key, externalPort);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serviceAndPort;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        DockerComposeRunner dockerComposeRunner = new DockerComposeRunner();
        if (args.length > 0 && "down".equals(args[0].toLowerCase())) {
            dockerComposeRunner.invokeDockerComposeDown();
        } else if (args.length > 0 && "remove".equals(args[0].toLowerCase())) {
            dockerComposeRunner.dockerComposeRemove(Arrays.copyOfRange(args, 1, args.length));
        } else {
            dockerComposeRunner.invokeDockerComposeUp(args);
        }
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }


    private Path moveDockerComposeInfra() throws IOException {
        Path target = Paths.get(TARGET_DOCKER_FILE_PREFIX + SOURCE_DOCKER_INFRA_COMPOSE_FILE);
        //Files.copy(Paths.get(getClass().getClassLoader().getResource("prometheus.yml").getFile()), Paths.get(DOCKER_EXECUTION_PATH + "/prometheus.yml"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(getClass().getClassLoader().getResource(SOURCE_DOCKER_INFRA_COMPOSE_FILE).getFile()), target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static String findExternalIp() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            String externalIp = null;
            while (nets.hasMoreElements()) {
                NetworkInterface networkInterface = nets.nextElement();
                if (networkInterfaces.contains(networkInterface.getName())) {
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if (inetAddress instanceof Inet4Address) {
                            externalIp = inetAddress.getHostName();
                            break;
                        }
                    }
                    if (externalIp != null) {
                        break;
                    }
                }
            }
            if (externalIp == null) {
                throw new IllegalArgumentException("Was not able to determine the external IP address... :( ");
            }
            return externalIp;
        } catch (SocketException socketException) {
            throw new RuntimeException(socketException);
        }
    }

    public void invokeDockerComposeUp(String... services) throws IOException {
        ensureInfrastructure();
        Files.copy(Paths.get(getClass().getClassLoader().getResource(SOURCE_DOCKER_COMPOSE_FILE).getFile()), TARGET_DOCKER_COMPOSE_FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
        if (services != null && services.length > 0) {
            dockerUp(TARGET_DOCKER_COMPOSE_FILE_PATH, services);
        }
    }

    private void dockerUp(Path dockerFile, String... service) throws IOException {
        if (service.length > 0) {
            dockerComposePull(dockerFile.toString(), service);
            List<String> attributes = new ArrayList<>();
            attributes.add(0, "-d");
            attributes.addAll(Arrays.asList(service));
            invokeDocker(dockerFile.toString(), "up", attributes.toArray(new String[0]));
        }
    }

    protected int dockerComposeRemove(String... service) throws IOException, InterruptedException {
        List<String> attributes = new ArrayList<>();
        attributes.add("-s");
        attributes.add("-f");
        attributes.add("-v");
        attributes.addAll(Arrays.asList(service));
        Process remove = invokeDocker(TARGET_DOCKER_FILE_PREFIX + SOURCE_DOCKER_COMPOSE_FILE, "rm", attributes.toArray(new String[0]));
        return remove.waitFor();
    }

    private void dockerComposeStart(String... service) throws IOException {
        invokeDocker(TARGET_DOCKER_FILE_PREFIX + SOURCE_DOCKER_COMPOSE_FILE, "start", service);
    }

    private void dockerComposePull(String file, String... service) throws IOException {
        invokeDocker(file, "pull", service);
    }

    public void invokeDockerComposeDown() throws IOException {
        invokeDocker(TARGET_DOCKER_FILE_PREFIX + SOURCE_DOCKER_COMPOSE_FILE, "down", "-v");
    }


    private Process invokeDocker(String file, String cmd, String... attributes) throws IOException {
        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList(DOCKER_COMPOSE_BIN, "-f", file, cmd));
        command.addAll(Arrays.asList(attributes));
        ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
        pb.environment().put("EXTERNAL_IP", externalIp);
        if (printDockerLogs) {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        return pb.start();

    }

    protected void waitForService(String service, Date initialLaunch, boolean waitForServiceToAppear, Function<String, Boolean> isServiceUp) {
        if (waitForServiceToAppear != isServiceUp.apply(service)) {
            if (new Date().getTime() - initialLaunch.getTime() > DOCKER_LAUNCH_TIMEOUT) {
                if (waitForServiceToAppear) {
                    logger.error(String.format("Was not able to connect to service %s in time", service));
                } else {
                    logger.error(String.format("Was not able to shut down service %s in time", service));
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                logger.error("Waiting for service state change was interrupted", ie);
            } finally {
                waitForService(service, initialLaunch, waitForServiceToAppear, isServiceUp);
            }
        } else {
            if (waitForServiceToAppear) {
                logger.info(String.format("Service %s is now up and running!", service));
            } else {
                logger.info(String.format("Service %s is now down!", service));
            }
        }
    }

}
