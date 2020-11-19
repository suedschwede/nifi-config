package at.mic.nifi.config.service;

import at.mic.nifi.config.service.ConnectableService;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.model.ConnectableDTO;
import at.mic.nifi.swagger.client.model.PortDTO;
import at.mic.nifi.swagger.client.model.PortEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectableServiceTest {

    @Test
    public void createConnectableDTOFromPortInpputTest() throws ApiException, IOException, URISyntaxException {
        PortEntity port = new PortEntity();
        port.setComponent(new PortDTO());
        port.getComponent().setParentGroupId("parentId");
        port.getComponent().setName("name");
        port.getComponent().setId("id");
        port.getComponent().setType(PortDTO.TypeEnum.INPUT_PORT);
        ConnectableDTO result = (new ConnectableService()).createConnectableDTOFromPort(port);
        assertEquals("parentId", result.getGroupId());
        assertEquals("name", result.getName());
        assertEquals("id", result.getId());
        assertEquals(ConnectableDTO.TypeEnum.INPUT_PORT, result.getType());
    }

    @Test
    public void createConnectableDTOFromPortOutputTest() throws ApiException, IOException, URISyntaxException {
        PortEntity port = new PortEntity();
        port.setComponent(new PortDTO());
        port.getComponent().setParentGroupId("parentId");
        port.getComponent().setName("name");
        port.getComponent().setId("id");
        port.getComponent().setType(PortDTO.TypeEnum.OUTPUT_PORT);
        ConnectableDTO result = (new ConnectableService()).createConnectableDTOFromPort(port);
        assertEquals("parentId", result.getGroupId());
        assertEquals("name", result.getName());
        assertEquals("id", result.getId());
        assertEquals(ConnectableDTO.TypeEnum.OUTPUT_PORT, result.getType());
    }


}