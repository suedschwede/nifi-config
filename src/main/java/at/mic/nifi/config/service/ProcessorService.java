package at.mic.nifi.config.service;

import at.mic.nifi.config.model.ConfigException;
import at.mic.nifi.config.utils.FunctionUtils;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.ProcessorsApi;
import at.mic.nifi.swagger.client.model.ProcessorDTO;
import at.mic.nifi.swagger.client.model.ProcessorEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;

/**
 * Class that offer service for process group
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class ProcessorService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ProcessorService.class);

    @Named("timeout")
    @Inject
    public Integer timeout;

    @Named("interval")
    @Inject
    public Integer interval;

    @Inject
    private ProcessorsApi processorsApi;

    /**
     * the the state of processor
     *
     * @param processor the processor
     * @param state the state
     */
    public void setState(ProcessorEntity processor, ProcessorDTO.StateEnum state) {
        //how obtain state of and don't have this bullshit trick
        //trick for don't have error : xxxx cannot be started because it is not stopped. Current state is STOPPING
        if (processor.getComponent().getState().equals(ProcessorDTO.StateEnum.DISABLED) && !state.equals(ProcessorDTO.StateEnum.STOPPED) ) {
            LOG.info(" {} ({}) is already disabled nifi-config make no update", processor.getComponent().getName() ,processor.getId());
            return;
        }
        boolean isReallyStopped = isReallyStopped(processor);
        if ((state.equals(ProcessorDTO.StateEnum.STOPPED) && state.equals(processor.getComponent().getState()) && isReallyStopped)
                || (state.equals(ProcessorDTO.StateEnum.RUNNING) && state.equals(processor.getComponent().getState()) ) ) {
            LOG.info(" {} ({}) is already {}", processor.getComponent().getName() ,processor.getId(), processor.getComponent().getState());
            return;
        }

        if (state.equals(ProcessorDTO.StateEnum.STOPPED) && state.equals(processor.getComponent().getState()) && !isReallyStopped) {
            //no update just waiting
        } else {
            try {
                ProcessorEntity body = new ProcessorEntity();
                body.setRevision(processor.getRevision());
                body.setComponent(new ProcessorDTO());
                body.getComponent().setState(state);
                body.getComponent().setId(processor.getId());
                body.getComponent().setRestricted(null);
                LOG.info(" {} ({}) update for {}", processor.getComponent().getName() ,processor.getId(), state);
                ProcessorEntity processorEntity = processorsApi.updateProcessor(processor.getId(), body);
                processor.setRevision(processorEntity.getRevision());
            } catch (ApiException e) {
                if (e.getResponseBody() == null || !e.getResponseBody().endsWith("Current state is STOPPING")) {
                    logErrors(processor);
                    throw new ConfigException(e.getMessage() + ": " + e.getResponseBody(), e);
                }
                LOG.info(e.getResponseBody());
            }
        }

        FunctionUtils.runWhile(()-> {
            LOG.info(" {} ({}) waiting for {}", processor.getComponent().getName() ,processor.getId(), state);
            ProcessorEntity processorEntity = null;
			try {
				processorEntity = processorsApi.getProcessor(processor.getId());
			} catch (ApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            boolean reallyStopped = isReallyStopped(processorEntity);
            LOG.info(" {} ({}) is {} (have thread active : {}) ", processorEntity.getComponent().getName(), processorEntity.getId(), processorEntity.getComponent().getState(), !reallyStopped);
            if ( (!state.equals(ProcessorDTO.StateEnum.RUNNING) && state.equals(processorEntity.getComponent().getState()) && isReallyStopped(processorEntity))
                || (state.equals(ProcessorDTO.StateEnum.RUNNING) && state.equals(processorEntity.getComponent().getState())) ) {
                return false;
            }
            return true;
        }, interval, timeout);

    }

    /**
     * is really stopped when there are no active thread
     * @param processor the processor
     * @return if is really stopped
     */
    private boolean isReallyStopped(ProcessorEntity processor) {
        return processor.getStatus() == null || processor.getStatus().getAggregateSnapshot() == null || processor.getStatus().getAggregateSnapshot().getActiveThreadCount() == null
                || processor.getStatus().getAggregateSnapshot().getActiveThreadCount() == 0;
    }

    /**
     * log the error reported by processor
     *
     * @param processor the processor
     */
    private void logErrors(ProcessorEntity processor) {
        try {
            ProcessorEntity procInError = processorsApi.getProcessor(processor.getId());
            if (procInError.getComponent().getValidationErrors() == null) procInError.getComponent().setValidationErrors(new ArrayList<>());
            procInError.getComponent().getValidationErrors().stream().forEach(msg -> LOG.error(msg));
        } catch (ApiException e1) {
            LOG.error(e1.getMessage());
        }
    }

    public ProcessorEntity getById(String id) throws ApiException {
        return processorsApi.getProcessor(id);
    }

}
