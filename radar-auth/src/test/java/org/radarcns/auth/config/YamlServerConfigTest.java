package org.radarcns.auth.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;


/**
 * Created by dverbeec on 19/06/2017.
 */
public class YamlServerConfigTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void testLoadYamlFileFromClasspath() throws URISyntaxException {
        ServerConfig config = YamlServerConfig.readFromFileOrClasspath();
        checkConfig(config);
    }

    @Test
    public void testLoadYamlFileFromEnv() throws URISyntaxException {
        ClassLoader loader = getClass().getClassLoader();
        File configFile = new File(loader.getResource(YamlServerConfig.CONFIG_FILE_NAME).getFile());
        environmentVariables.set(YamlServerConfig.LOCATION_ENV, configFile.getAbsolutePath());
        ServerConfig config = YamlServerConfig.readFromFileOrClasspath();
        checkConfig(config);
    }

    private void checkConfig(ServerConfig config) throws URISyntaxException {
        List<URI> uris = config.getPublicKeyEndpoints();
        assertThat(uris, hasItems(new URI("http://localhost:8089/oauth/token_key"),
                new URI("http://localhost:8089/oauth/token_key")));
        assertEquals(2, uris.size());
        assertEquals("unit_test", config.getResourceName());
        assertEquals(2, config.getPublicKeys().size());
        List<String> algs = config.getPublicKeys().stream()
                .map(key -> key.getAlgorithm())
                .collect(Collectors.toList());
        assertThat(algs, hasItems("RSA", "EC"));
    }
}
