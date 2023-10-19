package it.gov.pagopa.miladapter.services;

import it.gov.pagopa.miladapter.enums.RequiredProcessVariables;
import it.gov.pagopa.miladapter.model.AuthParameters;
import it.gov.pagopa.miladapter.model.KeyToken;
import it.gov.pagopa.miladapter.model.Token;
import it.gov.pagopa.miladapter.properties.RestConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Calendar;
import java.util.Optional;

@Service
@Slf4j
public class TokenService {

    @Autowired
    private RestConfigurationProperties restConfigurationProperties;
    @Autowired
    CacheService cacheService;
    @Autowired
    RestTemplate restTemplate;


    public void injectAuthToken(HttpHeaders restHeaders, AuthParameters authParameters) {
        String accessToken = this.getToken(authParameters);
        log.info("MIL Authentication Completed. JWT Token acquired");
        restHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer".concat(" ").concat(accessToken));
    }

    private String getToken(AuthParameters authParameters) {
        KeyToken keyToken = new KeyToken();
        keyToken.setChannel(authParameters.getChannel());
        keyToken.setAcquirerId(authParameters.getAcquirerId());
        keyToken.setTerminalId(authParameters.getTerminalId());
        Optional<Token> optionalToken = cacheService.getToken(keyToken);
        if (optionalToken.isPresent()) {
            log.info("Recovering still valid access Token");
            return optionalToken.get().getAccess_token();
        }
        return generateToken(authParameters, keyToken).getAccess_token();
    }

    public Token generateToken(AuthParameters authParameters, KeyToken keyToken) {
        log.info("MIL Authentication flow started");
        HttpHeaders headers = prepareAuthHeaders(authParameters);

        HttpEntity<MultiValueMap<String, String>> request = prepareAuthBody(headers);
        String externalApiUrl = restConfigurationProperties.getMilBasePath() + restConfigurationProperties.getAuth().getMilAuthPath();
        ResponseEntity<Token> response = restTemplate.exchange(externalApiUrl, HttpMethod.POST, request, Token.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("There was an error during the API call" + response.getStatusCode());
        }

        Token token = response.getBody();

        if (token != null) {
            token.setTime(Calendar.getInstance().getTimeInMillis());
            token.setExpires_in(cacheService.calculateTokenDurationInSeconds(token.getExpires_in()));
            cacheService.insertToken(keyToken, token);
        }

        return token;
    }


    private HttpEntity<MultiValueMap<String, String>> prepareAuthBody(HttpHeaders headers) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add(RequiredProcessVariables.CLIENT_ID.getMilValue(), restConfigurationProperties.getAuth().getClientId());
        body.add(RequiredProcessVariables.CLIENT_SECRET.getMilValue(), restConfigurationProperties.getAuth().getClientSecret());
        body.add(RequiredProcessVariables.GRANT_TYPE.getMilValue(), restConfigurationProperties.getAuth().getGrantType());

        return new HttpEntity<>(body, headers);
    }

    private static HttpHeaders prepareAuthHeaders(AuthParameters authParameters) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        headers.add(RequiredProcessVariables.REQUEST_ID.getMilValue(), authParameters.getRequestId());
        headers.add(RequiredProcessVariables.ACQUIRER_ID.getMilValue(), authParameters.getAcquirerId());
        headers.add(RequiredProcessVariables.CHANNEL.getMilValue(), authParameters.getChannel());
        headers.add(RequiredProcessVariables.TERMINAL_ID.getMilValue(), authParameters.getTerminalId());
        return headers;
    }
}
