package com.hazelcast.k8s.ci;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscoveryAcceptanceTest {

    private HazelcastInstance client;

    @Before
    public void before() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream stream = classLoader.getResource("hazelcast-client.xml").openStream();
        ClientConfig cfg = new XmlClientConfigBuilder(stream).build();

        assertTrue(cfg.getNetworkConfig().getDiscoveryConfig().isEnabled());
        assertFalse(cfg.getNetworkConfig().getDiscoveryConfig().getDiscoveryStrategyConfigs().isEmpty());

        client = HazelcastClient.newHazelcastClient(cfg);
    }

    @Test
    public void shouldDiscoverMember() {
        final String key = RandomStringUtils.randomAlphanumeric(42);
        final String value = RandomStringUtils.randomAlphanumeric(42);
        IMap<String, String> map = client.getMap("data");
        map.put(key, value);
        assertEquals(map.get(key), value);
    }
}
