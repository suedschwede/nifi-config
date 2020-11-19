package at.mic.nifi.config.service;

import at.mic.nifi.config.service.AccessService;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.AccessApi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class AccessServiceTest {
    @Mock
    private AccessApi accessApiMock;

    @InjectMocks
    private AccessService accessService;

    /**
     * Creates a token for accessing the REST API via username/password
     * <p>
     * The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header in the format &#39;Authorization: Bearer &lt;token&gt;&#39;.
     *
     * @throws ApiException if the Api call fails
     */
  
    public void createAccessFromTicketTest() throws ApiException, IOException, URISyntaxException {
        accessService.addTokenOnConfiguration(true,  null,null);
        verify(accessApiMock).createAccessTokenFromTicket();
    }

  
    public void createAccessFromUserTest() throws ApiException, IOException, URISyntaxException {
        accessService.addTokenOnConfiguration(false,  "user","pwd");
        verify(accessApiMock).createAccessToken("user","pwd");
    }

    @Test
    public void createAccessNohtingTest() throws ApiException, IOException, URISyntaxException {
        accessService.addTokenOnConfiguration(false,  null,null);
        verify(accessApiMock, never()).createAccessToken(anyString(), anyString());
    }


}