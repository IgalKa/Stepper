package enginemanager;

import dto.*;
import flow.Continuation;
import flow.Flow;
import flow.FlowExecution;
import flow.FlowHistory;
import generated.*;
import hardcodeddata.HCSteps;
import step.*;
import exception.*;
import javafx.util.Pair;
import users.User;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;



public class Manager implements EngineApi, Serializable {
    private List<Flow> flows;
    private Map<String,FlowExecution> flowExecutions;
    private List<FlowHistory> flowsHistory;
    private Map<String, Statistics> flowsStatistics;
    private Map<String, Statistics> stepsStatistics;
    private Flow currentFlow;
    private ExecutorService threadPool;
    private Map<String,Integer> flowNames2Index;

    private int historyVersion;





    public Manager() {
        flows = new ArrayList<>();
        flowsHistory = new ArrayList<>();
        flowsStatistics = new LinkedHashMap<>();
        stepsStatistics = new LinkedHashMap<>();
        flowExecutions=new HashMap<>();
        historyVersion = 0;
    }

    @Override
    public void endProcess()
    {
        if(threadPool!=null)
            threadPool.shutdown();
    }


    @Override
    public List<String> getFlowsNames() {
        List<String> namesList = new ArrayList<>();
        for (Flow flow : flows) {
            namesList.add(flow.getName());
        }
        return namesList;
    }

    @Override
    public void loadXmlFile(InputStream inputStream) throws Exception {
        STStepper stepper;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(STStepper.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            stepper = (STStepper) jaxbUnmarshaller.unmarshal(inputStream);
            createFlows(stepper);
            createThreadPool(stepper);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void loadXmlFile(String path) throws JAXBException {
        STStepper stepper;
        try {
            File file = checkXMLPathAndGetFile(path);
            JAXBContext jaxbContext = JAXBContext.newInstance(STStepper.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            stepper = (STStepper) jaxbUnmarshaller.unmarshal(file);
            createFlows(stepper);
            if(flowNames2Index==null)
                createThreadPool(stepper);
        } catch (JAXBException e) {
            throw e;
        }
    }


    private void createThreadPool(STStepper stepper)
    {
        int threadPoolSize=stepper.getSTThreadPool();
        if(threadPoolSize<=0)
            throw new RuntimeException("The given thread pool size is non-positive number");
        threadPool= Executors.newFixedThreadPool(stepper.getSTThreadPool());
    }

    private void createFlows(STStepper stepper) {
        Map<String,Integer> flowNames = new HashMap<>();
        List<STFlow> stFlows = stepper.getSTFlows().getSTFlow();
        List<Flow> flowList = new ArrayList<>();
        Map<String,List<Continuation>> continuationMap = new HashMap<>();
        Map<String, Statistics> statisticsMap = new LinkedHashMap<>();
        int i = 0;

        for (STFlow stFlow : stFlows) {
            String flowName = stFlow.getName();
            if (flowNames.containsKey(flowName)) {
                throw new FlowNameExistException("The xml file contains two flows with the same name");
            }

            if(flowNames2Index==null || !flowNames2Index.containsKey(flowName)) {
                flowNames.put(flowName, i);
                currentFlow = new Flow(flowName, stFlow.getSTFlowDescription());
                addFlowOutputs(stFlow);
                addSteps(stFlow);
                implementAliasing(stFlow);
                currentFlow.setInitialValues(getInitialValues(stFlow));
                currentFlow.customMapping(getCustomMappings(stFlow));
                currentFlow.automaticMapping();
                currentFlow.calculateFreeInputs();
                currentFlow.setFlowOutputs();
                if (getContinuations(stFlow).size() != 0)
                    continuationMap.put(currentFlow.getName(), getContinuations(stFlow));
                currentFlow.checkFlowIsValid();
                statisticsMap.put(currentFlow.getName(), new Statistics());
                flowList.add(currentFlow);
                i++;
            }
        }
        createContinuations(continuationMap,flowList, flowNames);


        currentFlow=null;

        if(flowNames2Index==null) {
            this.flowNames2Index = flowNames;
            flows = flowList;
            setFlowsContinuations(continuationMap);
            flowsStatistics = statisticsMap;
            stepsStatistics = HCSteps.getStatisticsMap();
        }
        else {
            i = flows.size();
            flows.addAll(flowList);

            int newSize = flows.size();

            for (; i < newSize; i++) {
                flowNames2Index.put(flows.get(i).getName(), i);
            }
            setFlowsContinuations(continuationMap);
            flowsStatistics.putAll(statisticsMap);
        }
    }

    private void setFlowsContinuations(Map<String, List<Continuation>> continuationMap) {
        for(String flowName: continuationMap.keySet()) {
            flows.get(flowNames2Index.get(flowName)).setContinuations(continuationMap.get(flowName));
        }
    }

    private void createContinuations(Map<String,List<Continuation>> mapping,List<Flow> flowList,  Map<String,Integer> flowNames) {
        for(String name : mapping.keySet()) {
            Integer sourceIndex = flowNames.get(name);
            List<Continuation> currContinuationList = mapping.get(name);
            for (Continuation continuation : currContinuationList) {
                Flow targetFlow,sourceFlow;
                Integer targetIndex = flowNames.get(continuation.getTargetFlow());

                if(targetIndex==null) {

                    if(flowNames2Index != null)
                       targetIndex = flowNames2Index.get(continuation.getTargetFlow());

                    if (targetIndex == null)
                        throw new ContinuationException("The flow \"" + name +
                                "\" contains a continuation to a flow that doesn't exists, flow name: "
                                + continuation.getTargetFlow());
                    else
                        targetFlow=flows.get(targetIndex);
                }
                else
                    targetFlow = flowList.get(targetIndex);

                sourceFlow = flowList.get(sourceIndex);

                continuation.createContinuation(sourceFlow, targetFlow);
            }
        }
    }

    private List<Continuation> getContinuations(STFlow stFlow) {
        List<Continuation> continuations = new ArrayList<>();
        if(stFlow.getSTContinuations() == null)
            return continuations;

        List<STContinuation> XMLContinuations = stFlow.getSTContinuations().getSTContinuation();
        for(STContinuation continuation : XMLContinuations) {
            Continuation currContinuation = new Continuation(continuation.getTargetFlow());
            List<STContinuationMapping> mapping = continuation.getSTContinuationMapping();
            if (mapping != null) {
                for (STContinuationMapping currMapping : mapping) {
                    String from = currMapping.getSourceData();
                    String to = currMapping.getTargetData();
                    currContinuation.addForcedConnection(from, to);
                }
            }
            continuations.add(currContinuation);
        }

        return continuations;
    }


    private Map<String,String> getInitialValues(STFlow stFlow)
    {
        Map<String,String> initialValues = new HashMap<>();
        if(stFlow.getSTInitialInputValues() == null)
            return initialValues;

        List<STInitialInputValue> initialInputValues = stFlow.getSTInitialInputValues().getSTInitialInputValue();
        for(STInitialInputValue currValue : initialInputValues) {
            initialValues.put(currValue.getInputName(),currValue.getInitialValue());
        }

        return initialValues;
    }



    private Map<Pair<String, String>, Pair<String, String>> getCustomMappings(STFlow stFlow) {
        Map<Pair<String, String>, Pair<String, String>> customMappings = new HashMap<>();

        if (stFlow.getSTCustomMappings() == null)
            return customMappings;

        List<STCustomMapping> customMappingsList = stFlow.getSTCustomMappings().getSTCustomMapping();


        for (STCustomMapping customMapping : customMappingsList) {
            customMappings.put(new Pair<>(customMapping.getTargetStep(), customMapping.getTargetData()),
                    new Pair<>(customMapping.getSourceStep(), customMapping.getSourceData()));
        }
        return customMappings;
    }

    private void implementAliasing(STFlow stFlow) {
        if (stFlow.getSTFlowLevelAliasing() == null)
            return;

        List<STFlowLevelAlias> aliases = stFlow.getSTFlowLevelAliasing().getSTFlowLevelAlias();

        for (STFlowLevelAlias alias : aliases) {
            implementAlias(alias);
        }
    }

    private void implementAlias(STFlowLevelAlias alias) {
        Boolean found = false;
        String stepName = alias.getStep();
        Integer stepIndex = currentFlow.getStepIndexByName(stepName);

        if (stepIndex == null) {
            throw new StepNameNotExistException("In the flow named: " + currentFlow.getName()
                    + "\nthere is an attempt to perform FlowLevelAliasing"
                    + " in the step by the name: " + stepName + " that was not defined in the flow");
        }

        Step step = currentFlow.getStep(stepIndex);
        String oldName = alias.getSourceDataName(), newName = alias.getAlias();

        if(oldName.equals(newName))
            return;

        if (step.getNameToInputIndex().get(newName) != null) {
            throw new InputNameExistException("In the flow named: " + currentFlow.getName()
                    + "\nThere is an attempt to perform FlowLevelAliasing for a data "
                    + "in the step: " + stepName
                    + "\nTo the name: " + newName +
                    " which is already used as a name for another input data");
        }

        if (step.getNameToOutputIndex().get(newName) != null) {
            throw new OutputNameExistException("In the flow named: " + currentFlow.getName()
                    + "\nThere is an attempt to perform FlowLevelAliasing for a data "
                    + "in the step: " + stepName
                    + "\nTo the name: " + newName +
                    " which is already used as a name for another output data");
        }

        if (step.getNameToInputIndex().get(oldName) != null) {
            step.changeInputName(oldName, newName);
            found = true;
        }

        if (!found && step.getNameToOutputIndex().get(oldName) != null) {
            step.changeOutputName(oldName, newName);
            found = true;
        }

        if (!found) {
            throw new InputOutputNotExistException("In the flow named: " + currentFlow.getName()
                    + "\nthere is an attempt to perform FlowLevelAliasing "
                    + "in the step: " + stepName + " for the data: "
                    + oldName + "\nthat was not defined in the step");
        }
    }

    private void addSteps(STFlow stFlow) {
        List<STStepInFlow> steps = stFlow.getSTStepsInFlow().getSTStepInFlow();
        for (STStepInFlow step : steps) {
            String alias = step.getAlias(), name = step.getName();
            if (alias != null) {
                if (currentFlow.getStepIndexByName(alias) != null) {
                    throw new StepNameExistException("In the flow named: " + currentFlow.getName()
                            + "\nthere is an attempt to perform aliasing "
                            + "for name of the step: " + name
                            + "\nTo the name: " + alias + " that was defined before");
                }
            } else if (currentFlow.getStepIndexByName(name) != null) {
                throw new StepNameExistException("In the flow named: " + currentFlow.getName()
                        + "\nthere is an attempt to define the step by the name: " + name
                        + " that was defined before");
            }
            currentFlow.addStep(createStep(step));
        }
    }

    private Step createStep(STStepInFlow step) {
        Step newStep = null;
        boolean continueIfFailing = false;
        String finalName = step.getName(), name = step.getName();

        if (step.getAlias() != null)
            finalName = step.getAlias();

        if (step.isContinueIfFailing() != null)
            continueIfFailing = true;

        newStep=HCSteps.CreateStep(name,finalName,continueIfFailing);

        if(newStep==null) {
            throw new StepNameNotExistException("In the flow named: " + currentFlow.getName()
                    + "\nthere is an attempt to define a step by the name: "
                    + step.getName() + " that does not exist");
        }
        return newStep;
    }


    private void addFlowOutputs(STFlow stFlow) {
        String[] outputs = stFlow.getSTFlowOutput().split(",");
        for (String output : outputs)
            currentFlow.addFormalOutput(output);
    }

    @Override
    public List<AvailableFlowDTO> getAvailableFlows() {
        List<AvailableFlowDTO> availableFlows = new ArrayList<>();
        for(Flow flow : flows) {
            AvailableFlowDTO currFlow = new AvailableFlowDTO(flow.getName(),flow.getDescription(),
                    flow.getNumberOfInputs(),flow.getNumberOfSteps(),flow.getNumberOfContinuations());
            availableFlows.add(currFlow);
        }

        return availableFlows;
    }

    @Override
    public FlowDefinitionDTO getFlowDefinition(int flowIndex) {
        return flows.get(flowIndex).getFlowDefinition();
    }

    @Override
    public FlowDefinitionDTO getFlowDefinition(String flowName) {
        int flowIndex = flowNames2Index.get(flowName);
        return flows.get(flowIndex).getFlowDefinition();
    }


    @Override
    public InputsDTO getFlowInputs(User user,String flowName) {
        //currentFlow = flows.get(flowIndex);
        //return currentFlow.getInputList();
        int flowIndex=flowNames2Index.get(flowName);
        //user.addFlow(flows.get(flowIndex));
        user.setCurrentFlow(flowName);
        return user.getCurrentFlow().getInputList();
    }


    @Override
    public int getFlowIndexByName(String name) {
        return flowNames2Index.get(name);
    }



    @Override
    public ResultDTO processInput(User user,String inputName, String data) {
        //return currentFlow.processInput(inputName, data);
        return user.getCurrentFlow().processInput(inputName,data);
    }


    @Override
    public boolean isFlowReady(User user) {
        return user.getCurrentFlow().isFlowReady();
    }


    @Override
    public String runFlow(User user) {

        FlowExecution flowExecution = new FlowExecution(user.getCurrentFlow(),this);
        String flowID=flowExecution.getFlowId();
        flowExecutions.put(flowID,flowExecution);
        threadPool.execute(flowExecution);


        user.getCurrentFlow().resetFlow();
        return flowID;
    }


    @Override
    public List<String> getInitialHistoryList() {
        List<String> res = new ArrayList<>();
        for (FlowHistory history : flowsHistory) {
            String currHistory = "Flow name: " + history.getFlowName() + "\nFlow ID: " + history.getID() + "\nFlow activation time: " + history.getActivationTime() + "\n";
            res.add(currHistory);
        }
        return res;
    }


    @Override
    public FlowExecutionDTO getFullHistoryData(int flowIndex) {
        return flowsHistory.get(flowIndex).getFullData();
    }

    public FlowExecutionDTO getHistoryDataOfFlow(String id)
    {
        return flowExecutions.get(id).getFlowHistoryData();
    }

    @Override
    public InputData clearInputData(User user, String inputName)
    {
        return user.getCurrentFlow().clearInputData(inputName);
    }

    @Override
    public FreeInputExecutionDTO getInputData(User user,String inputName)
    {
        return user.getCurrentFlow().getInputData(inputName);
    }

    @Override
    public List<String> getEnumerationAllowedValues(User user,String inputName) {
        return user.getCurrentFlow().getEnumerationAllowedValues(inputName);
    }

    @Override
    public List<FlowExecutionDTO> getFlowsHistoryDelta(int historyVersion)
    {
        List<FlowExecutionDTO> flowsList = new ArrayList<>();
        int delta = flowsHistory.size() - historyVersion;
        for(int i = 0 ; i < delta ; i++) {
            flowsList.add(getHistoryDataOfFlow(flowsHistory.get(i).getID()));
        }
        return flowsList;
    }



    @Override
    public ResultDTO saveDataOfSystemToFile(String FILE_NAME) {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(
                             new FileOutputStream(FILE_NAME))) {
            out.writeObject(flows);
            out.writeObject(flowsHistory);
            out.writeObject(flowsStatistics);
            out.writeObject(stepsStatistics);
            out.flush();
            return new ResultDTO(true, "System's parameters saved successfully");
        } catch (Exception e) {
            return new ResultDTO(false, "The System's parameters saving failed " +
                    "because: " + e.getMessage());
        }
    }

    @Override
    public ResultDTO loadDataOfSystemFromFile(String FILE_NAME) {
        File file = new File(FILE_NAME);
        Manager manager = null;
        if (file.exists()) {
            try (ObjectInputStream in =
                         new ObjectInputStream(
                                 new FileInputStream(FILE_NAME))) {
                flows = (List<Flow>) in.readObject();
                flowsHistory = (List<FlowHistory>) in.readObject();
                flowsStatistics = (Map<String, Statistics>) in.readObject();
                stepsStatistics = (Map<String, Statistics>) in.readObject();

                return new ResultDTO(true, "Loaded the system successfully");
            }
            catch (IOException  e) {
                return new ResultDTO(false, "Failed to load data from the given file " +
                        "because: " + e.getMessage());
            }
            catch (ClassNotFoundException e) {
                return new ResultDTO(false, "Failed to load data from the given file " +
                        "because: " + e.getMessage());
            }
            catch (NoClassDefFoundError e){
                return new ResultDTO(false, "Failed to load data from the given file " +
                        "because: " + e.getMessage());
            }



        }
        return new ResultDTO(false, "The file in the given path doesn't exist");
    }

    public void addFlowHistory(FlowExecution currentFlow) {
        FlowHistory flowHistory = new FlowHistory(currentFlow.getName(),
                currentFlow.getFlowId(), currentFlow.getActivationTime(), currentFlow.getFlowHistoryData());
        flowsHistory.add(0, flowHistory);
    }

    public void addStatistics(FlowExecution currentFlow) {
        Integer size = currentFlow.getNumberOfSteps();
        Statistics statistics = flowsStatistics.get(currentFlow.getName());
        statistics.addRunTime(currentFlow.getRunTime());
        boolean flowStopped = false;

        for (int i = 0; i < size && !flowStopped; i++) {
            Step step = currentFlow.getStep(i);
            statistics = stepsStatistics.get(step.getDefaultName());
            statistics.addRunTime(step.getRunTime());
            if (step.getStateAfterRun() == State.FAILURE && !step.isContinueIfFailing())
                flowStopped = true;
        }
    }

    @Override
    public StatisticsDTO getStatistics()
    {
        List<StatisticsUnitDTO> statisticsOfFlows= packageStatistics(flowsStatistics);
        List<StatisticsUnitDTO> statisticsOfSteps= packageStatistics(stepsStatistics);
        return new StatisticsDTO(statisticsOfFlows,statisticsOfSteps);
    }


    private List<StatisticsUnitDTO> packageStatistics(Map<String,Statistics> statisticsMap) {
        List<StatisticsUnitDTO> statisticsOfFlows=new ArrayList<>();

        for (String name : statisticsMap.keySet()) {
            Statistics statistics = statisticsMap.get(name);
            StatisticsUnitDTO statisticsUnitDTO= new StatisticsUnitDTO(statistics.getTimesActivated(),statistics.getAvgRunTime(), name);
            statisticsOfFlows.add(statisticsUnitDTO);
        }
        return  statisticsOfFlows;
    }



    private File checkXMLPathAndGetFile(String path) {
        File file = new File(path);

        if (!file.exists()) {
            throw new XmlFileException("The file does not exist in the given path");
        } else if (!path.endsWith(".xml")) {
            throw new XmlFileException("The file name does not end with an .xml extension");
        }

        return file;
    }

    public int getCurrInitializedFlowsCount() {
        return flows.size();
    }

    @Override
    public ContinutionMenuDTO getContinutionMenuDTO(User user) {
        return user.getCurrentFlow().getContinutionMenuDTO();
    }

    @Override
    public ContinutionMenuDTO getContinuationMenuDTOByName(User user, String flowName) {
        return user.getFlows().get(flowName).getContinutionMenuDTO();
    }

    @Override
    public void reUseInputsData(User user, List<FreeInputExecutionDTO> inputs, String flowName)
    {
        Flow flow = user.getFlows().get(flowName);
        flow.clearFlowInputsData();
        for(FreeInputExecutionDTO currInput : inputs) {
            if(currInput.getData() != null)
                flow.processInput(currInput.getName(), currInput.getData());
        }
    }


    @Override
    public void doContinuation(User user, FlowExecution flowExecution, String targetName) {
        String sourceFlowName = flowExecution.getName();
        Flow sourceFlow = user.getFlows().get(sourceFlowName);
        Continuation continuation = sourceFlow.getContinuation(targetName);
        user.getFlows().get(targetName).clearFlowInputsData();
        user.getFlows().get(targetName).applyContinuation(flowExecution, continuation);
    }

    @Override
    public FlowExecution getFlowExecution(String ID) {
        return flowExecutions.get(ID);
    }


    @Override
    public String getInputDefaultName(User user,String inputName) {
        return user.getCurrentFlow().getInputDefaultName(inputName);
    }

    @Override
    public void updateUserFlows(User user) {
        for(Flow flow: flows)
            user.addFlow(flow);
    }

    public int getHistoryVersion() {
        return historyVersion;
    }

    public void setHistoryVersion(int historyVersion) {
        this.historyVersion = historyVersion;
    }
}
