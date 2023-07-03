package users;

import dto.AvailableFlowDTO;
import dto.UserInfoDTO;
import flow.Flow;
import roles.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String name;
    boolean isManager;
    private int numOfFlowsPerformed;
    private Map<String, Flow> flows;
    private Flow currentFlow;


    public User(String name) {
        this.name = name;
        this.flows = new HashMap<>();
        currentFlow =null;
        isManager = false;
        numOfFlowsPerformed = 0;
    }

    public void addFlow(Flow flow)
    {
        if(!flows.containsKey(flow.getName()))
            flows.put(flow.getName(), new Flow(flow));
    }

    public void removeFlow(String flowName) {
        if(flows.containsKey(flowName))
            flows.remove(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFlows(Map<String, Flow> flows) {
        this.flows = flows;
    }

    public void setCurrentFlow(String flowName) {
        if(flows.containsKey(flowName))
            currentFlow=flows.get(flowName);
    }

    public String getName() {
        return name;
    }

    public Map<String, Flow> getFlows() {
        return flows;
    }

    public Flow getCurrentFlow() {
        return currentFlow;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean manager) {
        isManager = manager;
    }

    public int getNumOfFlowsPerformed() {
        return numOfFlowsPerformed;
    }

    public void setNumOfFlowsPerformed(int numOfFlowsPerformed) {
        this.numOfFlowsPerformed = numOfFlowsPerformed;
    }

    public synchronized void incrementNumOfFlowsPerformed() {
        numOfFlowsPerformed++;
    }

    public UserInfoDTO getUserInformation()
    {
        return  new UserInfoDTO(name,flows.keySet().size(),numOfFlowsPerformed);
    }


}
