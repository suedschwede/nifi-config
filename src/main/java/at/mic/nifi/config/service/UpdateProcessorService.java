package at.mic.nifi.config.service;

import at.mic.nifi.config.model.ConfigException;
import at.mic.nifi.config.model.GroupProcessorsEntity;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.FlowApi;
import at.mic.nifi.swagger.client.ProcessorsApi;
import at.mic.nifi.swagger.client.model.*;
import at.mic.nifi.swagger.client.ProcessGroupsApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static at.mic.nifi.config.utils.FunctionUtils.findByComponentName;
import static at.mic.nifi.swagger.client.model.ControllerServiceReferencingComponentDTO.ReferenceTypeEnum.CONTROLLERSERVICE;
import static at.mic.nifi.swagger.client.model.ControllerServiceReferencingComponentDTO.ReferenceTypeEnum.PROCESSOR;

/**
 * Class that offer service for nifi processor
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class UpdateProcessorService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(UpdateProcessorService.class);

    @Inject
    private ProcessGroupService processGroupService;

    @Inject
    private ControllerServicesService controllerServicesService;

    @Inject
    private CreateRouteService createRouteService;

    @Inject
    private FlowApi flowapi;

    @Inject
    private ConnectionsUpdater connectionsUpdater;

    @Inject
    private ProcessorsApi processorsApi;
    
    @Inject
    private ProcessGroupsApi processGroupApi;


    /**
     * @param branch the branch
     * @param fileConfiguration fileConfiguration
     * @param optionNoStartProcessors if optionNoStartProcessors
     * @throws IOException when io problem
     * @throws ApiException when api problem
     */
    public void updateByBranch(List<String> branch, String fileConfiguration, boolean optionNoStartProcessors) throws IOException, ApiException {
        File file = new File(fileConfiguration);
        
        
        if (!file.exists()) {
            throw new FileNotFoundException("File configuration " + file.getName() + " is empty or doesn't exist");
        }

        LOG.info("Processing : " + file.getName());

        GroupProcessorsEntity configuration = loadConfiguration(file);

        ProcessGroupFlowEntity componentSearch = processGroupService.changeDirectory(branch)
                .orElseThrow(() -> new ConfigException(("cannot find " + Arrays.toString(branch.toArray()))));
        String processorGroupFlowId = componentSearch.getProcessGroupFlow().getId();
        //System.out.println(processorGroupFlowId);

     
        //Stop branch
        processGroupService.stop(componentSearch);
        LOG.info(Arrays.toString(branch.toArray()) + " is stopped");

        //Stop connexion ??

        //the state change, then the revision also in nifi 1.3.0 (only?) reload processGroup
        String processGroupFlowId = componentSearch.getProcessGroupFlow().getId();
        componentSearch = flowapi.getFlow(processGroupFlowId);
                
        ParameterContextsEntity contexts = flowapi.getParameterContexts();
     
    
        ParameterContextReferenceEntity parameterContextReference = new ParameterContextReferenceEntity();
        ParameterContextReferenceDTO parameterContextReferenceDTO = new ParameterContextReferenceDTO();
        PermissionsDTO permissions = componentSearch.getPermissions();
        for ( ParameterContextEntity context : contexts.getParameterContexts()) {
        	if (configuration.getContext().contains(context.getComponent().getName()))   {
        		parameterContextReference.setId(context.getId());
        		parameterContextReferenceDTO.setId(context.getId());
        		parameterContextReferenceDTO.setName(context.getComponent().getName());
				parameterContextReference.setComponent(parameterContextReferenceDTO);
				parameterContextReference.setPermissions(permissions);

        	}
        }
      
       

        //generate clientID
        String clientId = flowapi.generateClientId();
        updateComponent(configuration, componentSearch, clientId,parameterContextReference);
        
        ProcessGroupEntity processGroup = processGroupApi.getProcessGroup(componentSearch.getProcessGroupFlow().getId());
        
        updateProcessGroup(componentSearch.getProcessGroupFlow().getId(),processGroup,parameterContextReference);
        

        //controller
        updateControllers(configuration, processGroupFlowId, clientId);
        
     
        createRouteService.createRoutes(configuration.getConnectionPorts(), optionNoStartProcessors);

        if (!optionNoStartProcessors) {
            //Run all nifi processors
            componentSearch = flowapi.getFlow(processGroupFlowId);
            //processGroupService.start(componentSearch);
            //setState(componentSearch, ProcessorDTO.StateEnum.RUNNING);
            LOG.info(Arrays.toString(branch.toArray()) + " is running");
        }

        LOG.debug("updateByBranch end");
    }

    private GroupProcessorsEntity loadConfiguration(File file) throws IOException {
        Gson gson = new GsonBuilder().serializeNulls().create();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            return gson.fromJson(reader, GroupProcessorsEntity.class);
        }
    }


    /**
     * @param configuration configuration
     * @param idComponent idComponent
     * @throws ApiException when api problem
     */
    private void updateControllers(GroupProcessorsEntity configuration, String idComponent, String clientId) throws ApiException {
        //TODO verify if must include ancestor and descendant
        ControllerServicesEntity controllerServicesEntity = flowapi.getControllerServicesFromGroup(idComponent, true, false);
        //must we use flowapi.getControllerServicesFromController() ??
        /*ControllerServicesEntity controllerServiceController = flowapi.getControllerServicesFromController();
        for (ControllerServiceEntity controllerServiceEntity: controllerServiceController.getControllerServices()) {
            controllerServicesEntity.addControllerServicesItem(controllerServiceEntity);
        }*/
        List<ControllerServiceEntity> controllerUpdated = new ArrayList<>();
        List<ControllerServiceEntity> controllerDeleted = new ArrayList<>();


        for (ControllerServiceDTO controllerServiceDTO : configuration.getControllerServicesDTO()) {
            List<ControllerServiceEntity> all = controllerServicesEntity.getControllerServices().stream().filter(item -> item.getComponent().getName().trim().equals(controllerServiceDTO.getName().trim())).collect(Collectors.toList());

            ControllerServiceEntity controllerServiceEntityFind = null;
            Map<String, ControllerServiceEntity> oldControllersService = new HashMap<>();
            if (all.size() > 1) {
                for (ControllerServiceEntity controllerServiceEntity : all) {
                    if (idComponent.equals(controllerServiceEntity.getComponent().getParentGroupId())) {
                        //add to old
                        oldControllersService.put(controllerServiceEntity.getId(), controllerServiceEntity);
                    } else {
                        controllerServiceEntityFind = controllerServiceEntity;
                    }
                }
                if (controllerServiceEntityFind == null) {
                    throw new ConfigException("Cannot choose controller, multiple controller find with the same name " + controllerServiceDTO.getName() + " on the same group " + idComponent);
                }
            } else if (all.size() == 1) {
                //find controller for have id
                controllerServiceEntityFind = all.get(0);
            } else {
                throw new ConfigException("Cannot find controller " + controllerServiceDTO.getName());
            }
            //remove old
            stopOldReference(oldControllersService.values());
            updateOldReference(oldControllersService.values(), controllerServiceEntityFind.getId(), clientId);
            controllerDeleted.addAll(oldControllersService.values());
            controllerUpdated.add(controllerServiceEntityFind);
            if (controllerServiceDTO.getProperties() != null && !controllerServiceDTO.getProperties().isEmpty()) {
                //stopping referencing processors and reporting tasks
                controllerServicesService.setStateReferenceProcessors(controllerServiceEntityFind, UpdateControllerServiceReferenceRequestEntity.StateEnum.STOPPED);

                //Disabling referencing controller services
                controllerServicesService.setStateReferencingControllerServices(controllerServiceEntityFind.getId(), UpdateControllerServiceReferenceRequestEntity.StateEnum.DISABLED);

                //Disabling this controller service
                ControllerServiceEntity controllerServiceEntityUpdate = controllerServicesService.setStateControllerService(controllerServiceEntityFind, ControllerServiceDTO.StateEnum.DISABLED);
                controllerServiceEntityUpdate = controllerServicesService.updateControllerService(controllerServiceDTO, controllerServiceEntityUpdate, false);
            }
        }

        //remove old
        removeOldReference(controllerDeleted);

        // start enabling service
        for (ControllerServiceEntity controllerServiceEntity : controllerUpdated) {
            //Enabling this controller service
            controllerServicesService.setStateControllerService(controllerServiceEntity, ControllerServiceDTO.StateEnum.ENABLED);
        }
        //enabling ref controller service in separate way because ref conroller is may be not configured
        for (ControllerServiceEntity controllerServiceEntity : controllerUpdated) {
            //Enabling referencing controller services
            controllerServicesService.setStateReferencingControllerServices(controllerServiceEntity.getId(), UpdateControllerServiceReferenceRequestEntity.StateEnum.ENABLED);
        }
        //start ref processor in separate way because the processor can have multiple controller
        for (ControllerServiceEntity controllerServiceEntity : controllerUpdated) {
            //Starting referencing processors and reporting tasks
            controllerServicesService.setStateReferenceProcessors(controllerServiceEntity, UpdateControllerServiceReferenceRequestEntity.StateEnum.RUNNING);
        }

        //must we start all controller referencing on the group ?
        // for (ControllerServiceEntity controllerServiceEntity :  controllerServicesEntity.getControllerServices()) {
        //Enabling this controller service
        //    controllerServicesService.setStateControllerService(controllerServiceEntity, ControllerServiceDTO.StateEnum.ENABLED);
        //    controllerServicesService.setStateReferenceProcessors(controllerServiceEntity, UpdateControllerServiceReferenceRequestEntity.StateEnum.RUNNING);
        //}
    }

    /**
     * Update controller to newControllerServiceId for ReferencingComponents on oldControllersService
     *
     * @param newControllerServiceId newControllerServiceId
     * @param oldControllersService oldControllersService
     * @throws ApiException 
     */
    private void updateOldReference(Collection<ControllerServiceEntity> oldControllersService, String newControllerServiceId, String clientId) throws ApiException {
        for (ControllerServiceEntity oldControllerService : oldControllersService) {
            for (ControllerServiceReferencingComponentEntity component : oldControllerService.getComponent().getReferencingComponents()) {
                if (component.getComponent().getReferenceType().equals(PROCESSOR)) {
                    ProcessorEntity newProc = processorsApi.getProcessor(component.getId());
                    newProc.getComponent().getConfig().setProperties(createUpdateProperty(newProc.getComponent().getConfig().getProperties(), oldControllerService.getId(), newControllerServiceId));
                    updateProcessor(newProc, newProc.getComponent(), true, clientId);
                } else if (component.getComponent().getReferenceType().equals(CONTROLLERSERVICE)) {
                    ControllerServiceEntity newControllerService = controllerServicesService.getControllerServices(component.getId());
                    newControllerService.getComponent().setProperties(createUpdateProperty(newControllerService.getComponent().getProperties(), oldControllerService.getId(), newControllerServiceId));
                    controllerServicesService.updateControllerService(newControllerService.getComponent(), newControllerService, true);
                }// else TODO for reporting task ??
            }
            LOG.info(" {} ({}) is replaced by ({})", oldControllerService.getComponent().getName(), oldControllerService.getComponent().getId(), newControllerServiceId);
        }
    }

    private void stopOldReference(Collection<ControllerServiceEntity> oldControllersService) throws ApiException {
        for (ControllerServiceEntity oldControllerService : oldControllersService) {
            try {
                //maybe there are already remove
                controllerServicesService.getControllerServices(oldControllerService.getId());
                //stopping referencing processors and reporting tasks
                controllerServicesService.setStateReferenceProcessors(oldControllerService, UpdateControllerServiceReferenceRequestEntity.StateEnum.STOPPED);

                //Disabling referencing controller services
                controllerServicesService.setStateReferencingControllerServices(oldControllerService.getId(), UpdateControllerServiceReferenceRequestEntity.StateEnum.DISABLED);
            } catch (ApiException e) {
                //maybe there are already remove
                if (!e.getMessage().contains("Not Found")) {
                    throw e;
                }
            }
        }
    }

    private void removeOldReference(Collection<ControllerServiceEntity> oldControllersService) throws ApiException {
        for (ControllerServiceEntity oldControllerService : oldControllersService) {
            try {
                //maybe there are already remove
                controllerServicesService.getControllerServices(oldControllerService.getId());
                //remove
                controllerServicesService.remove(oldControllerService);
            } catch (ApiException e) {
                //maybe there are already remove
                if (!e.getMessage().contains("Not Found")) {
                    throw e;
                }
            }
        }
    }

    private Map<String, String> createUpdateProperty(Map<String, String> properties, String oldValue, String newValue) {
        Map<String, String> newProperties = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (oldValue.equals(entry.getValue())) {
                newProperties.put(entry.getKey(), newValue);
            }
        }
        return newProperties;
    }

    /**
     * @param configuration configuration
     * @param componentSearch componentSearch
     * @param clientId clientId
     * @param parameterContextReference 
     * @throws ApiException when api problem
     */
    private void updateComponent(GroupProcessorsEntity configuration, ProcessGroupFlowEntity componentSearch, String clientId, ParameterContextReferenceEntity parameterContextReference) throws ApiException {
        FlowDTO flow = componentSearch.getProcessGroupFlow().getFlow();

        
   
        
        List<ProcessGroupEntity> processGroups = flow.getProcessGroups();
        for (ProcessGroupEntity processGroup : processGroups) {
        	updateProcessGroup(processGroup.getId(),processGroup,parameterContextReference);
        };

        configuration.getProcessors()
                .forEach(processorOnConfig -> updateProcessor(findProcByComponentName(flow.getProcessors(), processorOnConfig.getName()), processorOnConfig, false, clientId));

        for (GroupProcessorsEntity procGroupInConf : configuration.getGroupProcessorsEntity()) {

            ProcessGroupEntity processorGroupToUpdate = findByComponentName(flow.getProcessGroups(), procGroupInConf.getName())
                    .orElseThrow(() -> new ConfigException(("cannot find " + procGroupInConf.getName())));
            updateComponent(procGroupInConf, flowapi.getFlow(processorGroupToUpdate.getId()), clientId, parameterContextReference);
        }
        
        //connectionsUpdater.updateConnections(configuration.getConnections(), componentSearch);
    }
    
  
    private void updateProcessGroup(String id, ProcessGroupEntity body,ParameterContextReferenceEntity parameterContextReference) throws ApiException {
        //System.out.println(parameterContextReference);
    	        
        body.getComponent().setParameterContext(parameterContextReference);
          	
    	processGroupApi.updateProcessGroup( id,  body);
        
        LOG.info("ProcessGroup {} ({}) have config updated ", body.getComponent().getName(), body.getId());
    }

    /**
     * update processor configuration with valueToPutInProc
     * at first find id of each processor and in second way update it
     *
     * @param processorToUpdate processorToUpdate
     * @param componentToPutInProc componentToPutInProc
     * @param clientId clientId
     */
    private void updateProcessor(ProcessorEntity processorToUpdate, ProcessorDTO componentToPutInProc, boolean forceByController, String clientId) {
        try {
            componentToPutInProc.setId(processorToUpdate.getId());
            LOG.info("Update config processor {} ({}) ", processorToUpdate.getComponent().getName(), processorToUpdate.getId());
            //update on nifi
            List<String> autoTerminatedRelationships = new ArrayList<>();
            if (processorToUpdate.getComponent().getRelationships() == null) processorToUpdate.getComponent().setRelationships(new ArrayList<>());
            processorToUpdate.getComponent().getRelationships().stream()
                    .filter(relationships -> relationships.getAutoTerminate())
                    .forEach(relationships -> autoTerminatedRelationships.add(relationships.getName()));
            componentToPutInProc.getConfig().setAutoTerminatedRelationships(autoTerminatedRelationships);
            componentToPutInProc.getConfig().setDescriptors(processorToUpdate.getComponent().getConfig().getDescriptors());
            componentToPutInProc.getConfig().setDefaultConcurrentTasks(processorToUpdate.getComponent().getConfig().getDefaultConcurrentTasks());
            componentToPutInProc.getConfig().setDefaultSchedulingPeriod(processorToUpdate.getComponent().getConfig().getDefaultSchedulingPeriod());
            componentToPutInProc.setRelationships(processorToUpdate.getComponent().getRelationships());
            componentToPutInProc.setStyle(processorToUpdate.getComponent().getStyle());
            componentToPutInProc.setSupportsBatching(processorToUpdate.getComponent().getSupportsBatching());
            componentToPutInProc.setSupportsEventDriven(processorToUpdate.getComponent().getSupportsEventDriven());
            componentToPutInProc.setSupportsParallelProcessing(processorToUpdate.getComponent().getSupportsParallelProcessing());
            componentToPutInProc.setPersistsState(processorToUpdate.getComponent().getPersistsState());
            componentToPutInProc.setRestricted(null);//processorToUpdate.getComponent().getRestricted());
            componentToPutInProc.setValidationErrors(processorToUpdate.getComponent().getValidationErrors());
            //remove controller link if not forceBy controller
            if (!forceByController) {
                if (processorToUpdate.getComponent().getConfig().getDescriptors() == null) processorToUpdate.getComponent().getConfig().setDescriptors(new HashMap<>());
                for (Map.Entry<String, PropertyDescriptorDTO> entry : processorToUpdate.getComponent().getConfig().getDescriptors().entrySet()) {
                    if (entry.getValue().getIdentifiesControllerService() != null) {
                        componentToPutInProc.getConfig().getProperties().remove(entry.getKey());
                    }
                }
            }
            processorToUpdate.setComponent(componentToPutInProc);
            processorToUpdate.getRevision().setClientId(clientId);

            processorsApi.updateProcessor(processorToUpdate.getId(), processorToUpdate);

            //nifiService.updateProcessorProperties(toUpdate, componentToPutInProc.getString("id"));
            LOG.info("Processor {} ({}) have config updated ", processorToUpdate.getComponent().getName(), processorToUpdate.getId());
        } catch (ApiException e) {
            throw new ConfigException(e.getMessage() + ": " + e.getResponseBody(), e);
        }
    }

    //can static => utils
    public static ProcessorEntity findProcByComponentName(List<ProcessorEntity> listGroup, String name) {
        return listGroup.stream()
                .filter(item -> item.getComponent().getName().trim().equals(name.trim()))
                .findFirst().orElseThrow(() -> new ConfigException(("cannot find " + name)));
    }

}
