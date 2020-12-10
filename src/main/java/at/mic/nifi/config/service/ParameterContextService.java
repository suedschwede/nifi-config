package at.mic.nifi.config.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import at.mic.nifi.config.model.GroupProcessorsEntity;
import at.mic.nifi.config.model.ParametersContextEntity;
import at.mic.nifi.swagger.ApiException;
import at.mic.nifi.swagger.client.FlowApi;
import at.mic.nifi.swagger.client.ParameterContextsApi;
import at.mic.nifi.swagger.client.model.ParameterContextDTO;
import at.mic.nifi.swagger.client.model.ParameterContextEntity;
import at.mic.nifi.swagger.client.model.ParameterEntity;
import at.mic.nifi.swagger.client.model.ProcessGroupDTO;
import at.mic.nifi.swagger.client.model.RevisionDTO;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Singleton
public class ParameterContextService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ProcessorService.class);
   
    @Inject
    private ParameterContextsApi parameterContextsApi;
    
    @Inject
    private FlowApi flowApi;
    
    
    public void extractParameter(String fileConfiguration, String contextname) throws ApiException, UnsupportedEncodingException, FileNotFoundException, IOException {
    	File file = new File(fileConfiguration);
    	
    	List<ParameterContextEntity> contexts = flowApi.getParameterContexts().getParameterContexts();
    	
    	ParameterContextEntity context = null;
    	
    	for (ParameterContextEntity con : contexts) {
    		//System.out.println(con.getComponent().getName());
    		if (contextname.contains(con.getComponent().getName())) {
    			context = con;
    		}
    	}
    	
    	if (context==null) {
    		LOG.debug("No Parameter Context found", contextname);
    		return;
    	}
    	

    	List<ParameterEntity> parameters = context.getComponent().getParameters();
    	ParametersContextEntity  parametersContextEntity = new ParametersContextEntity();
    	
    	parametersContextEntity.setName(contextname);
    	parametersContextEntity.setParameters(parameters);
    	
    	Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LOG.debug("saving in file {}", fileConfiguration);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            gson.toJson(parametersContextEntity, writer);
        } finally {
            LOG.debug("extract Parameters end");
        }
    }

    
    public void updateParameter(String fileConfiguration) throws ApiException, UnsupportedEncodingException, FileNotFoundException, IOException {
    	File file = new File(fileConfiguration);
    	
    	 
        if (!file.exists()) {
            throw new FileNotFoundException("File configuration " + file.getName() + " is empty or doesn't exist");
        }

        String clientId = flowApi.generateClientId();
        
        LOG.info("Processing : " + file.getName());

        ParametersContextEntity configuration = loadConfiguration(file);
    	
    	List<ParameterContextEntity> contexts = flowApi.getParameterContexts().getParameterContexts();
    	
    	ParameterContextEntity context = null;
    	
    	String contextname = configuration.getName();
  
    	for (ParameterContextEntity con : contexts) {
    		if (contextname.toLowerCase().contains(con.getComponent().getName().toLowerCase())) {
    			context = con;
    		}
    	}
    
     	ParameterContextEntity body = new ParameterContextEntity();
     	
     	boolean update = false;
    	
    	if (context!=null) {
    		LOG.info("Parameter Context already created", contextname);
    		body = context;
    		update = true;
    	} else {
          	body.setRevision(new RevisionDTO());
        	body.setComponent(new ParameterContextDTO());
        	body.getRevision().setVersion(0L);
        	body.getRevision().setClientId(clientId);
        	body.getComponent().setName(contextname);
    	}
    	    	

    	
    	
    	if (!update) {
    	  List<ParameterEntity>  parameters = configuration.getParameters();	
    	  body.getComponent().setParameters(parameters);	
		  parameterContextsApi.createParameterContext(body);
    	} else {
    	  List<ParameterEntity>  parameters1 = configuration.getParameters();
    	  List<ParameterEntity>  parameters2 = context.getComponent().getParameters();
    	  List<ParameterEntity>  parametersnew = new ArrayList() ;
    		
    	  for (ParameterEntity parameter : parameters1) {
    		  ParameterEntity parameternew = findparameter(parameters2,parameter.getParameter().getName());

    		  if (parameternew != null) {
    			  parametersnew.add(parameternew);
    		  } else {
    			  LOG.info("New parameter : " + parameter.getParameter().getName());
    			  parametersnew.add(parameter);
    		  }
    	  }
    	  body.getComponent().setParameters(parametersnew);	
    	  parameterContextsApi.updateParameterContext(body.getId(), body);	
    	}

    }
    
    private ParameterEntity findparameter(List<ParameterEntity> parameters,String name) {
		for (ParameterEntity parameter : parameters) {
			if (parameter.getParameter().getName().toLowerCase().contains(name.toLowerCase())) {
				return parameter;
			}
		}
    	
    	return null;
    	
    }


    private ParametersContextEntity loadConfiguration(File file) throws IOException {
        Gson gson = new GsonBuilder().serializeNulls().create();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            return gson.fromJson(reader, ParametersContextEntity.class);
        }
    }


	
    
}