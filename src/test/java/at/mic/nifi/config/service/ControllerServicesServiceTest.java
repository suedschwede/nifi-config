package at.mic.nifi.config.service;

import at.mic.nifi.config.service.ControllerServicesService;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.ControllerServicesApi;
import at.mic.nifi.swagger.client.model.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ControllerServicesServiceTest {

    @Mock
    private ControllerServicesApi controllerServicesApiMock;

    @Test
    public void updateControllerServiceTest() throws InterruptedException, ApiException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ControllerServicesApi.class).toInstance(controllerServicesApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        ControllerServiceEntity controllerServiceDisabled = TestUtils.createControllerServiceEntity("id","name");
        controllerServiceDisabled.getComponent().setState(ControllerServiceDTO.StateEnum.DISABLED);
        ControllerServiceEntity controllerServiceEnabled = TestUtils.createControllerServiceEntity("id","name");
        controllerServiceEnabled.getComponent().setState(ControllerServiceDTO.StateEnum.ENABLED);
        ControllerServiceEntity controllerService = TestUtils.createControllerServiceEntity("id","name");

        when(controllerServicesApiMock.getControllerService("id")).thenReturn(controllerServiceDisabled).thenReturn(controllerServiceEnabled);

        when(controllerServicesApiMock.updateControllerService(eq("id"), any()))
                .thenReturn(controllerServiceDisabled)
                .thenReturn(controllerService)
                .thenReturn(controllerServiceEnabled);

        ControllerServicesService controllerServicesService = injector.getInstance(ControllerServicesService.class);

        ControllerServiceDTO component = new ControllerServiceDTO();
        component.setProperties(new HashMap<>());
        component.getProperties().put("key", "value");
        controllerServicesService.updateControllerService(component, controllerService, false);

        ArgumentCaptor<ControllerServiceEntity> controllerServiceCapture = ArgumentCaptor.forClass(ControllerServiceEntity.class);
        verify(controllerServicesApiMock, times(1)).updateControllerService(eq("id"),controllerServiceCapture.capture());
       // assertEquals("id", controllerServiceCapture.getAllValues().get(0).getComponent().getId());
       // assertEquals(ControllerServiceDTO.StateEnum.DISABLED, controllerServiceCapture.getAllValues().get(0).getComponent().getState());
        assertEquals("id", controllerServiceCapture.getAllValues().get(0).getComponent().getId());
        assertEquals("value", controllerServiceCapture.getAllValues().get(0).getComponent().getProperties().get("key"));
     //   assertEquals("id", controllerServiceCapture.getAllValues().get(2).getComponent().getId());
     //   assertEquals(ControllerServiceDTO.StateEnum.ENABLED, controllerServiceCapture.getAllValues().get(2).getComponent().getState());
    }

    @Test
    public void setStateReferencingControllerServicesTest() throws ApiException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ControllerServicesApi.class).toInstance(controllerServicesApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        when(controllerServicesApiMock.updateControllerServiceReferences(eq("id"), any())).thenReturn(new ControllerServiceReferencingComponentsEntity());
        ControllerServiceEntity reponse = new ControllerServiceEntity();
        reponse.setComponent(new ControllerServiceDTO());
        reponse.getComponent().setReferencingComponents(new ArrayList<>());
        ControllerServiceReferencingComponentEntity ref = new ControllerServiceReferencingComponentEntity();
        ref.setComponent(new ControllerServiceReferencingComponentDTO());
        ref.getComponent().setReferenceType(ControllerServiceReferencingComponentDTO.ReferenceTypeEnum.CONTROLLERSERVICE);
        ref.getComponent().setState("DISABLED");
        ref.setId("idRef");
        ref.setRevision(new RevisionDTO());
        reponse.getComponent().getReferencingComponents().add(ref);
        when(controllerServicesApiMock.getControllerService("id")).thenReturn(reponse);

        ControllerServicesService controllerServicesService = injector.getInstance(ControllerServicesService.class);
        controllerServicesService.setStateReferencingControllerServices("id", UpdateControllerServiceReferenceRequestEntity.StateEnum.RUNNING);

        ArgumentCaptor<UpdateControllerServiceReferenceRequestEntity> updateControllerServiceReferenceRequestCapture= ArgumentCaptor.forClass(UpdateControllerServiceReferenceRequestEntity.class);
        verify(controllerServicesApiMock, times(1)).updateControllerServiceReferences(eq("id"),updateControllerServiceReferenceRequestCapture.capture());
        assertEquals(UpdateControllerServiceReferenceRequestEntity.StateEnum.RUNNING, updateControllerServiceReferenceRequestCapture.getValue().getState());
        assertEquals(1, updateControllerServiceReferenceRequestCapture.getValue().getReferencingComponentRevisions().size());
        assertTrue(updateControllerServiceReferenceRequestCapture.getValue().getReferencingComponentRevisions().containsKey("idRef"));
    }

}