package at.mic.nifi.config.service;

import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.FlowApi;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by SFRJ2737 on 2017-05-28.
 *
 * @author hermann pencolé
 */
@Singleton
public class InformationService {

    @Inject
    private FlowApi flowApi;

    /**
     * get the nifi version.
     *
     * @return the version of nifi
     * @throws ApiException when api problem
     */
    public String getVersion() throws ApiException {
        return flowApi.getAboutInfo().getAbout().getVersion();
    }
}
