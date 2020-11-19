package at.mic.nifi.config.model;

import at.mic.nifi.swagger.client.model.ControllerServiceDTO;
import at.mic.nifi.swagger.client.model.ProcessorDTO;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SFRJ2737 on 2017-05-26.
 */
public class GroupProcessorsEntity {

    @SerializedName("processors")
    private List<ProcessorDTO> processors = new ArrayList<>();

    @SerializedName("groupProcessorsEntity")
    private List<GroupProcessorsEntity> groupProcessorsEntity = new ArrayList<>();

    @SerializedName("controllerServices")
    private List<ControllerServiceDTO> controllerServicesDTO = new ArrayList<>();

    @SerializedName("connections")
    private List<ConnectionPort> connectionPorts = new ArrayList<>();

    @SerializedName("namedConnections")
    private List<Connection> connections = new ArrayList<>();

    @SerializedName("name")
    private String name;

    @SerializedName("context")
    private String context;

    public List<ProcessorDTO> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorDTO> processors) {
        this.processors = processors;
    }

    public List<GroupProcessorsEntity> getGroupProcessorsEntity() {
        return groupProcessorsEntity;
    }

    /**
     * set the groupProcessorsEntity
     * @param groupProcessorsEntity the groupProcessorsEntity
     */
    public void setGroupProcessorsEntity(List<GroupProcessorsEntity> groupProcessorsEntity) {
        this.groupProcessorsEntity = groupProcessorsEntity;
    }

    public List<ControllerServiceDTO> getControllerServicesDTO() {
        return controllerServicesDTO;
    }

    public void setControllerServicesDTO(List<ControllerServiceDTO> controllerServicesDTO) {
        this.controllerServicesDTO = controllerServicesDTO;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public List<ConnectionPort> getConnectionPorts() {
        return connectionPorts;
    }

    public void setConnectionPorts(List<ConnectionPort> connectionPorts) {
        this.connectionPorts = connectionPorts;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void setConnections(List<Connection> connections) {
        this.connections = connections;
    }
}
