package com.dgkncgty.demo.spring.configserver;

import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureMetrics
class ConfigServerApplicationTests {

  @Autowired private MockMvc mockMvc;

  @Value("${spring.security.user.name}")
  String basicAuthUser;

  @Value("${spring.security.user.password}")
  String basicAuthPassword;

  @Test
  void enabledPrometheusTest() throws Exception {
    mockMvc
        .perform(get("/actuator"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
        .andExpect(content().string(StringContains.containsStringIgnoringCase("prometheus")));

    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"));

    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
  }

  @Test
  void defaultConfigurationTest() throws Exception {
    String endpoint = "/default/master";
    mockMvc
        .perform(
            get(endpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + Base64Utils.encodeToString(
                            (basicAuthUser + ":" + basicAuthPassword).getBytes())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.propertySources.[0].source['info.foo']", Matchers.is("bar")));
  }

  @Test
  void defaultWebConfigTest() throws Exception {
    String endpoint = "/web/default/master";
    mockMvc
        .perform(
            get(endpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + Base64Utils.encodeToString(
                            (basicAuthUser + ":" + basicAuthPassword).getBytes())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.propertySources.[0].source['server.port']", Matchers.is(9080)))
        .andExpect(jsonPath("$.propertySources.[0].source['type']", Matchers.is("normal")))
        .andExpect(jsonPath("$.propertySources.[0].source['some.property']", Matchers.is("123")));
  }

  @Test
  void dcWebConfigTest() throws Exception {
    String endpoint = "/web/dc/master";
    mockMvc
        .perform(
            get(endpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + Base64Utils.encodeToString(
                            (basicAuthUser + ":" + basicAuthPassword).getBytes())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.propertySources.[0].source['server.port']", Matchers.is(8080)))
        .andExpect(jsonPath("$.propertySources.[0].source['type']", Matchers.is("dc")))
        .andExpect(jsonPath("$.propertySources.[1].source['some.property']", Matchers.is("123")))
        .andExpect(jsonPath("$.propertySources.[1].source['type']", Matchers.is("normal")))
        .andExpect(jsonPath("$.propertySources.[1].source['some.property']", Matchers.is("123")));
  }

  @Test
  void k8sWebConfigTest() throws Exception {
    String endpoint = "/web/k8s/master";
    mockMvc
        .perform(
            get(endpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + Base64Utils.encodeToString(
                            (basicAuthUser + ":" + basicAuthPassword).getBytes())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.propertySources.[0].source['server.port']", Matchers.is(8080)))
        .andExpect(jsonPath("$.propertySources.[0].source['type']", Matchers.is("k8s")))
        .andExpect(jsonPath("$.propertySources.[1].source['server.port']", Matchers.is(9080)))
        .andExpect(jsonPath("$.propertySources.[1].source['some.property']", Matchers.is("123")))
        .andExpect(jsonPath("$.propertySources.[1].source['type']", Matchers.is("normal")));
  }

  @Test
  void multiProfileWebConfigTest() throws Exception {
    String endpoint = "/web/k8s,dc/master";
    mockMvc
        .perform(
            get(endpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + Base64Utils.encodeToString(
                            (basicAuthUser + ":" + basicAuthPassword).getBytes())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.propertySources.[0].source['server.port']", Matchers.is(8080)))
        .andExpect(jsonPath("$.propertySources.[0].source['type']", Matchers.is("dc")))
        .andExpect(jsonPath("$.propertySources.[1].source['server.port']", Matchers.is(8080)))
        .andExpect(jsonPath("$.propertySources.[1].source['type']", Matchers.is("k8s")))
        .andExpect(jsonPath("$.propertySources.[2].source['some.property']", Matchers.is("123")));
  }

  @Test
  void fileCommonConfigTest() throws Exception {
    String endpoint = "/common/default/master/logback.xml";

    String fileContent =
        Files.readString(
            Paths.get("src", "test", "resources", "test-config-repo", "common", "logback.xml"));
    mockMvc
        .perform(
            get(endpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + Base64Utils.encodeToString(
                            (basicAuthUser + ":" + basicAuthPassword).getBytes())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/plain;charset=UTF-8"))
        .andExpect(content().string(fileContent));
  }
}
