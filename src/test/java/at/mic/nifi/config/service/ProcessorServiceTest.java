package at.mic.nifi.config.service;

import at.mic.nifi.config.model.ConfigException;
import at.mic.nifi.config.service.ProcessorService;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.ProcessorsApi;
import at.mic.nifi.swagger.client.model.ProcessorDTO;
import at.mic.nifi.swagger.client.model.ProcessorEntity;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessorServiceTest {

    @Mock
    private ProcessorsApi processorsApiMock;

    @Test
    public void setStateAlreadyTest() throws ApiException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ProcessorsApi.class).toInstance(processorsApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        ProcessorService processorService = injector.getInstance(ProcessorService.class);
        ProcessorEntity processor = TestUtils.createProcessorEntity("id", "name");
        processor.getComponent().setState(ProcessorDTO.StateEnum.RUNNING);
        processorService.setState(processor, ProcessorDTO.StateEnum.RUNNING);
        verify(processorsApiMock, never()).updateProcessor(anyString(), anyObject());
    }

    @Test
    public void setStateTest() throws ApiException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ProcessorsApi.class).toInstance(processorsApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        ProcessorService processorService = injector.getInstance(ProcessorService.class);
        ProcessorEntity processor = TestUtils.createProcessorEntity("id", "name");
        processor.getComponent().setState(ProcessorDTO.StateEnum.STOPPED);

        ProcessorEntity processorResponse = TestUtils.createProcessorEntity("id", "name");
        processorResponse.getComponent().setState(ProcessorDTO.StateEnum.RUNNING);
        when(processorsApiMock.updateProcessor(eq("id"), any() )).thenReturn(processorResponse);
        when(processorsApiMock.getProcessor(eq("id"))).thenReturn(processorResponse);

        processorService.setState(processor, ProcessorDTO.StateEnum.RUNNING);
        ArgumentCaptor<ProcessorEntity> processorEntity = ArgumentCaptor.forClass(ProcessorEntity.class);
        verify(processorsApiMock).updateProcessor(eq("id"), processorEntity.capture());
        assertEquals("id", processorEntity.getValue().getComponent().getId());
        assertEquals( ProcessorDTO.StateEnum.RUNNING, processorEntity.getValue().getComponent().getState());
    }

    @Test(expected = ConfigException.class)
    public void setStateExceptionTest() throws ApiException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ProcessorsApi.class).toInstance(processorsApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        ProcessorService processorService = injector.getInstance(ProcessorService.class);
        ProcessorEntity processor = TestUtils.createProcessorEntity("id", "name");
        processor.getComponent().setState(ProcessorDTO.StateEnum.STOPPED);

        ProcessorEntity processorResponse = TestUtils.createProcessorEntity("id", "name");
        processorResponse.getComponent().setState(ProcessorDTO.StateEnum.RUNNING);
        when(processorsApiMock.updateProcessor(eq("id"), any() )).thenThrow(new ApiException());
        when(processorsApiMock.getProcessor(eq("id") )).thenReturn(processorResponse);

        processorService.setState(processor, ProcessorDTO.StateEnum.RUNNING);
    }

}