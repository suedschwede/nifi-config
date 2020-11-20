package at.mic.nifi.config.service;

import at.mic.nifi.swagger.ApiClient;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.Configuration;
import at.mic.nifi.swagger.auth.OAuth;
import at.mic.nifi.swagger.client.AccessApi;
import at.mic.nifi.swagger.client.FlowApi;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by SFRJ2737 on 2017-05-28.
 *
 * @author hermann pencolé
 */
@Singleton
public class AccessService {

    @Inject
    private AccessApi apiInstance;
    
    @Inject
    private FlowApi flowApi;


    /**
     * add token on http client. The token is ask to nifi.
     *
     * @param accessFromTicket accessFromTicket
     * @param username username
     * @param password password
     * @throws ApiException when communication probem
     */
    public void addTokenOnConfiguration(boolean accessFromTicket, String username, String password) throws ApiException {
        ApiClient client = Configuration.getDefaultApiClient();


        if (accessFromTicket) {
            String token = apiInstance.createAccessTokenFromTicket();
            System.out.println(token);
            client.setAccessToken(token);
        } else if (username != null) {
            String token = apiInstance.createAccessToken(username, password);
            //Workaround for setAccessToken - setAccessToken does not work
            client.addDefaultHeader("Authorization", "Bearer " + token);
            //client.setUsername(username);
            //client.setPassword(password);
            //client.setAccessToken(token);
        }
        Configuration.setDefaultApiClient(client);
    }

    /**
     * Configure the default http client
     *
     * @param basePath set the basePath
     * @param verifySsl set theverifySsl
     * @param debugging set the debugging
     * @param connectionTimeout set the  connectionTimeout
     * @param readTimeout set the readTimeout
     * @param writeTimeout set the writeTimeout
     * @throws ApiException when problem
     */
    public void setConfiguration(String basePath, boolean verifySsl, boolean debugging,
                                        int connectionTimeout, int readTimeout, int writeTimeout) throws ApiException {
        ApiClient client = Configuration.getDefaultApiClient()
        //ApiClient client = new ApiClient()
                .setBasePath(basePath)
                .setVerifyingSsl(verifySsl)
                .setConnectTimeout(connectionTimeout)
                //.setReadTimeout(readTimeout)
                //.setWriteTimeout(writeTimeout)
                .setDebugging(debugging);
        Configuration.setDefaultApiClient(client);
    }
}
