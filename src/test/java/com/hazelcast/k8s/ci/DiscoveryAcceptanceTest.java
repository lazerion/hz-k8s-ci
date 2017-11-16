package com.hazelcast.k8s.ci;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscoveryAcceptanceTest {
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryAcceptanceTest.class);

    private HazelcastInstance client;

    private KubernetesClient k8s;

    @Before
    public void before() throws IOException {
        initializeKubernetes();
        initializeHazelcast();
    }

    private void initializeKubernetes() {
        logger.info("initializing K8S client");
        Config config = new ConfigBuilder().build();
        logger.info("Config master url {}", config.getMasterUrl());
        k8s = new DefaultKubernetesClient(config);
    }

    private void initializeHazelcast() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream stream = classLoader.getResource("hazelcast-client.xml").openStream();
        ClientConfig cfg = new XmlClientConfigBuilder(stream).build();

        assertTrue(cfg.getNetworkConfig().getDiscoveryConfig().isEnabled());
        assertFalse(cfg.getNetworkConfig().getDiscoveryConfig().getDiscoveryStrategyConfigs().isEmpty());

        logger.info("loaded configuration from resource path, creating client");
        client = HazelcastClient.newHazelcastClient(cfg);
    }

    @Test
    public void shouldPutGetWhenClusterReady() {
        final String key = RandomStringUtils.randomAlphanumeric(42);
        final String value = RandomStringUtils.randomAlphanumeric(42);
        IMap<String, String> map = client.getMap("data");
        map.put(key, value);
        assertEquals(map.get(key), value);
    }

    @Test
    public void shouldDiscoverAtLeastOneMember() {
        int clusterSize = client.getCluster().getMembers().size();
        assertTrue(clusterSize >= 1);
    }

    @Test
    public void shouldFindMembersWhenScaleUp() throws InterruptedException {
        ReplicationController controller = k8s.replicationControllers()
                .inNamespace("default")
                .withName("hazelcast")
                .get();

        logger.info("Replication controller {}", controller.toString());
        k8s.replicationControllers().inNamespace("default").withName("hazelcast").scale(4);

        Thread.sleep(5000);
        int clusterSize = client.getCluster().getMembers().size();
        assertTrue(clusterSize == 4);
    }
}
