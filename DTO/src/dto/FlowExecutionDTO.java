package dto;

import java.util.List;

public class FlowExecutionDTO extends FlowExecutionDetailsDTO{
    private final List<StepExecutionDTO> steps;
    private final List<FreeInputExecutionDTO> freeInputs;
    private final List<DataExecutionDTO> outputs;

    private double progress;

    public FlowExecutionDTO(FlowExecutionDetailsDTO other, List<StepExecutionDTO> steps, List<FreeInputExecutionDTO> freeInputs, List<DataExecutionDTO> outputs,double progress) {
        super(other);
        this.steps = steps;
        this.freeInputs = freeInputs;
        this.outputs = outputs;
        this.progress=progress;
    }




    public List<StepExecutionDTO> getSteps() {
        return steps;
    }

    public List<FreeInputExecutionDTO> getFreeInputs() {
        return freeInputs;
    }

    public List<DataExecutionDTO> getOutputs() {
        return outputs;
    }

    public double getProgress() {
        return progress;
    }

}
