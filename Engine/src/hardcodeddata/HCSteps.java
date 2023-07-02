package hardcodeddata;

import enginemanager.Statistics;
import exception.StepNameNotExistException;
import step.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum HCSteps {
    SPEND_SOME_TIME("Spend Some Time"),
    COLLECT_FILES("Collect Files In Folder"),
    FILES_RENAMER("Files Renamer"),
    FILES_CONTENT("Files Content Extractor"),
    CSV_EXPORTER("CSV Exporter"),
    PROPERTIES_EXPORTER("Properties Exporter"),
    FILE_DUMPER("File Dumper"),
    FILE_DELETER("Files Deleter"),
    ZIPPER("Zipper"),
    COMMAND_LINE("Command Line"),
    HTTP_CALL("HTTP Call");

    private String stepName;

    HCSteps(String stepName) {
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }

    public static List<String> getAllStepsName() {
        List<String> names=new ArrayList<>();
        for(HCSteps step: HCSteps.values())
        {
            names.add(step.getStepName());
        }
        return names;
    }


    public static Map<String, Statistics> getStatisticsMap() {
        Map<String, Statistics> res = new LinkedHashMap<>();
        for (HCSteps step : HCSteps.values()) {
            res.put(step.getStepName(), new Statistics());
        }
        return res;
    }

    public static Step CreateStep(String name,String finalName,boolean continueIfFailing)
    {
        Step newStep=null;
        switch (name) {
            case "Spend Some Time":
                newStep = new SpendSomeTime(finalName, continueIfFailing);
                break;
            case "Collect Files In Folder":
                newStep = new CollectFiles(finalName, continueIfFailing);
                break;
            case "Files Renamer":
                newStep = new FilesRenamer(finalName, continueIfFailing);
                break;
            case "Files Content Extractor":
                newStep = new FilesContentExtractor(finalName, continueIfFailing);
                break;
            case "CSV Exporter":
                newStep = new CSVExporter(finalName, continueIfFailing);
                break;
            case "Properties Exporter":
                newStep = new PropertiesExporter(finalName, continueIfFailing);
                break;
            case "File Dumper":
                newStep = new FileDumper(finalName, continueIfFailing);
                break;
            case "Files Deleter":
                newStep = new FilesDeleter(finalName, continueIfFailing);
                break;
            case "Zipper":
                newStep = new Zipper(finalName, continueIfFailing);
                break;
            case "Command Line":
                newStep = new CommandLine(finalName, continueIfFailing);
                break;
            case "HTTP Call":
                newStep =new HttpCall(finalName,continueIfFailing);
                break;

        }
        return newStep;
    }
}
