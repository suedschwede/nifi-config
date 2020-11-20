package at.mic.nifi.config.model;

import at.mic.nifi.swagger.client.model.ControllerServiceDTO;
import at.mic.nifi.swagger.client.model.ParameterContextEntity;
import at.mic.nifi.swagger.client.model.ParameterEntity;
import at.mic.nifi.swagger.client.model.ProcessorDTO;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SFRJ2737 on 2017-05-26.
 */
public class ParametersContextEntity {

  
    public ParametersContextEntity() {
		super();
	}

	public List<ParameterEntity> getParameters() {
		return parameters;
	}

	public void setParameters(List<ParameterEntity> parameters) {
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@SerializedName("parameters")
	List<ParameterEntity> parameters = new ArrayList<>();

    @SerializedName("name")
    private String name;


}
