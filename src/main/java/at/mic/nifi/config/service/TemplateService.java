package at.mic.nifi.config.service;

import at.mic.nifi.config.model.ConfigException;
import at.mic.nifi.config.model.GroupProcessorsEntity;
import at.mic.nifi.config.model.TimeoutException;
import at.mic.nifi.config.utils.FunctionUtils;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.FlowApi;
import at.mic.nifi.swagger.client.ProcessGroupsApi;
import at.mic.nifi.swagger.client.TemplatesApi;
import at.mic.nifi.swagger.client.model.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that offer service for nifi template
 *
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class TemplateService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    /**
     * The processGroupService nifi.
     */
    @Inject
    private ProcessGroupService processGroupService;

    @Inject
    private ProcessGroupsApi processGroupsApi;

    @Inject
    private FlowApi flowApi;

    @Inject
    private TemplatesApi templatesApi;

    @Inject
    private ControllerServicesService controllerServicesService;

    @Named("timeout")
    @Inject
    public Integer timeout;

    @Named("interval")
    @Inject
    public Integer interval;

    /**
     * @param branch the branch
     * @param fileConfiguration the fileConfiguration
     * @param keepTemplate if keepTemplate
     * @throws ApiException when api problem
     */
    public void installOnBranch(List<String> branch, String fileConfiguration, boolean keepTemplate,String checkParamContext) throws ApiException {
        ProcessGroupFlowDTO processGroupFlow = processGroupService.createDirectory(branch).getProcessGroupFlow();
        File file = new File(fileConfiguration);
        
        
        boolean find = FindParameterContext(checkParamContext);
        
        if (!find) {
        	LOG.info("Context Parameter " + checkParamContext + " does not exist");
        	return;
        }
        


        TemplatesEntity templates = flowApi.getTemplates();
        String name = FilenameUtils.getBaseName(file.getName());
        
        if (templates.getTemplates() == null) templates.setTemplates(new ArrayList<>());
        
        Optional<TemplateEntity> oldTemplate = templates.getTemplates().stream().filter(templateParse -> templateParse.getTemplate().getName().equals(name)).findFirst();
        if (oldTemplate.isPresent()) {
            templatesApi.removeTemplate(oldTemplate.get().getTemplate().getId(), false);
        }
        
        Optional<TemplateEntity> template = null;
        try {
          template = Optional.of(processGroupsApi.uploadTemplate(processGroupFlow.getId(), file, false));
        } catch (Exception e) {
        	LOG.info(e.getMessage());
        }          
        
        //Workaround uploadTemplate always returns an error  !!!
        templates = flowApi.getTemplates();
        for (TemplateEntity  temp : templates.getTemplates()) {
        	if (name.contains(temp.getTemplate().getName())) {
        		template = Optional.of(temp);
        	}
        }
        
                
        InstantiateTemplateRequestEntity instantiateTemplate = new InstantiateTemplateRequestEntity(); // InstantiateTemplateRequestEntity | The instantiate template request.
        instantiateTemplate.setTemplateId(template.get().getTemplate().getId());
        instantiateTemplate.setOriginX(0d);
        instantiateTemplate.setOriginY(0d);
        processGroupsApi.instantiateTemplate(processGroupFlow.getId(), instantiateTemplate);
        if (!keepTemplate) {
            templatesApi.removeTemplate(template.get().getTemplate().getId(), false);
        }
    }

    public void undeploy(List<String> branch) throws ApiException {
        Optional<ProcessGroupFlowEntity> processGroupFlow = processGroupService.changeDirectory(branch);
        if (!processGroupFlow.isPresent()) {
            LOG.warn("cannot find " + Arrays.toString(branch.toArray()));
            return;
        }

        //Stop branch
        processGroupService.stop(processGroupFlow.get());
        LOG.info(Arrays.toString(branch.toArray()) + " is stopped");

        //delete template
        TemplatesEntity templates = flowApi.getTemplates();
        Stream<TemplateEntity> templatesInGroup = templates.getTemplates().stream()
                .filter(templateParse -> templateParse.getTemplate().getGroupId().equals(processGroupFlow.get().getProcessGroupFlow().getId()));
        for (TemplateEntity templateInGroup : templatesInGroup.collect(Collectors.toList())) {
            templatesApi.removeTemplate(templateInGroup.getId(), true);
        }

        //disable controllers
        //TODO verify if must include ancestor and descendant
        controllerServicesService.disableController(processGroupFlow.get());

        processGroupService.delete(processGroupFlow.get().getProcessGroupFlow().getId());

    }

    
    private boolean FindParameterContext(String paramcontext) throws ApiException {
   	 ParameterContextsEntity contexts = flowApi.getParameterContexts();
     
   	 if (paramcontext == null) {
    		return true;
    	 }
   	 
   	 if (paramcontext.length()<1) {
   		return true;
   	 }

     for ( ParameterContextEntity context : contexts.getParameterContexts()) {
       if (paramcontext.contains(context.getComponent().getName()))   {
         return true;
    	}
     }
	 return false;
   }



}
