package it.gov.pagopa.miladapter.services.impl;

import it.gov.pagopa.miladapter.model.HTTPConfiguration;
import it.gov.pagopa.miladapter.properties.RestConfigurationProperties;
import it.gov.pagopa.miladapter.services.MILRestService;
import it.gov.pagopa.miladapter.services.TokenService;
import it.gov.pagopa.miladapter.util.HttpRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
@Slf4j
public class MILRestServiceImpl implements MILRestService {

    @Autowired
    RestConfigurationProperties restConfigurationProperties;

    @Autowired
    RestTemplate restTemplate;


    @Autowired
    TokenService tokenService;

    @Override
    public void injectAuthToken(HTTPConfiguration httpConfiguration) {
        tokenService.injectAuthToken(httpConfiguration.getHeaders(), httpConfiguration.getAuthParameters());
    }

    @Override
    public URI prepareUri(HTTPConfiguration httpConfiguration) {
        return HttpRequestUtils.buildURI(restConfigurationProperties.getMilBasePath(), httpConfiguration.getEndpoint(), httpConfiguration.getPathParams());
    }

    @Override
    public RestTemplate getRestTemplate() {
        return this.restTemplate;
    }

    @Override
    public HttpEntity<String> buildHttpEntity(HTTPConfiguration httpConfiguration) {
        return HttpRequestUtils.buildHttpEntity(httpConfiguration.getBody(), httpConfiguration.getHeaders());
    }
}
