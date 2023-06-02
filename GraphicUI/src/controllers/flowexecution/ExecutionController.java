package controllers.flowexecution;


import controllers.AppController;
import datadefinition.Relation;
import dto.*;
import elementlogic.ElementLogic;
import enginemanager.EngineApi;
import javafx.animation.FillTransition;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.*;
import javafx.util.Duration;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class ExecutionController {

    @FXML
    private FlowPane mandatoryInputsView;

    @FXML
    private FlowPane optionalInputsView;

    @FXML
    private Button executeButton;

    @FXML
    private ChoiceBox<String> choiceBoxView;

    @FXML
    private VBox elementDetailsView;

    @FXML
    private VBox elementChoiceView;

    @FXML
    private ProgressBar progressBarView;

    @FXML
    private Button continuationButton;

    private AppController appController;

    private EngineApi engine;

    private List<Button> mandatoryInputButtons;

    private List<Button> optionalInputButtons;

    private ElementLogic elementLogic;




    @FXML
    public void initialize() {
        continuationButton.setDisable(true);
        continuationButton.disableProperty().bind(choiceBoxView.valueProperty().isNull());
        choiceBoxView.setDisable(true);
    }



    public void setAppController(AppController appController)
    {
        this.appController=appController;
    }

    public void setStage(Stage stage)
    {
        elementLogic=new ElementLogic(elementChoiceView,elementDetailsView,stage);
    }

    public void setEngine(EngineApi engine) {
        this.engine = engine;
    }

    public void setTabView(InputsDTO inputsDTO)
    {
        clearTab();
        int numberOfMandatoryInputs=0;
        int numberOfInsertedMandatoryInputs=0;

        mandatoryInputButtons=new ArrayList<>();
        optionalInputButtons=new ArrayList<>();

        for(int i=0;i<inputsDTO.getNumberOfInputs();i++)
        {
            InputData inputData=inputsDTO.getFreeInput(i);
            Button button=new Button();
            button.setId(inputData.getSystemName());


            button.setOnMouseClicked(event -> {
                if(event.getButton()==MouseButton.PRIMARY)
                    inputClick(button,new ActionEvent());
                else if (event.getButton()==MouseButton.SECONDARY) {
                    rightInputClick(button);
                }
            });

            String inputPresentation =inputData.getSystemName().replace("_"," ").toLowerCase();
            inputPresentation+="\nDescription: "+inputData.getUserString();
            button.setText(inputPresentation);
            FlowPane.setMargin(button,new Insets(0,10,10,0));


            if(inputData.getNecessity())
            {
                button.setStyle("-fx-background-color: #ff0000; ");
                mandatoryInputsView.getChildren().add(button);
                mandatoryInputButtons.add(button);
                numberOfMandatoryInputs++;
            }
            else {
                optionalInputsView.getChildren().add(button);
                optionalInputButtons.add(button);
            }

            if (inputData.IsInserted()) {
                button.setStyle("-fx-background-color: #40ff00; ");
                if(inputData.getNecessity())
                    numberOfInsertedMandatoryInputs++;
            }
        }
        executeButton.setDisable(numberOfInsertedMandatoryInputs != numberOfMandatoryInputs);
    }

    public void clearTab() {
        mandatoryInputsView.getChildren().clear();
        optionalInputsView.getChildren().clear();
        choiceBoxView.getItems().clear();
        elementLogic.clear();
        progressBarView.setProgress(0);
        choiceBoxView.setDisable(true);
    }


    @FXML
    public void inputClick(Button button,ActionEvent event)
    {
        TextInputDialog inputDialog = new TextInputDialog();

        inputDialog.setTitle("submit input");
        inputDialog.setHeaderText(null);
        inputDialog.setGraphic(null);
        inputDialog.setContentText("Please enter the input here:");
        inputDialog.getDialogPane().setPrefWidth(400);


        Button submitButton=(Button) inputDialog.getDialogPane().lookupButton(ButtonType.OK);

        submitButton.setText("Submit");
        if(appController.getPrimaryStage().getScene().getStylesheets().size()!=0)
            inputDialog.getDialogPane().getStylesheets().add(appController.getPrimaryStage().getScene().getStylesheets().get(0));

        String inputType =engine.getInputData(button.getId()).getType();
        Optional<String> result=Optional.empty();
        switch (inputType)
        {
            case "Enumerator":
                ChoiceBox<String> enumerationSetChoice = new ChoiceBox<>();
                enumerationSetChoice.getItems().addAll(engine.getEnumerationAllowedValues(button.getId()));
                enumerationSetChoice.setStyle("-fx-pref-width: 200px;");
                inputDialog.getDialogPane().setContent(new HBox(10, new Label("Please choose an input:"), enumerationSetChoice));
                inputDialog.setResultConverter(dialogButton -> {
                    if (dialogButton == ButtonType.OK) {
                        String selectedOption = enumerationSetChoice.getValue();
                        return selectedOption;
                    }
                    return null;
                });
                result = inputDialog.showAndWait();
                break;
            case "Number":
                Spinner<Integer> spinner=new Spinner<>();

                SpinnerValueFactory<Integer> spinnerValueFactory =
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(1,Integer.MAX_VALUE);
                spinnerValueFactory.setValue(1);

                spinner.setValueFactory(spinnerValueFactory);

                spinner.setEditable(true);

                TextField textField =inputDialog.getEditor();
                inputDialog.getDialogPane().setContent(spinner);
                result =inputDialog.showAndWait();
                break;
            case "String":
                String inputDefaultName = engine.getInputDefaultName(button.getId());
                switch (inputDefaultName) {
                    case "FOLDER_NAME":
                        result = openFolderChooser();
                        break;
                    case "FILE_NAME":
                        result = openFileChooser();
                        break;
                    case "SOURCE":
                        Dialog<ButtonType> dialog = new Dialog<>();
                        dialog.setTitle("Choose zipping source");
                        dialog.setHeaderText("Please select an option:");
                        if(appController.getPrimaryStage().getScene().getStylesheets().size()!=0)
                           dialog.getDialogPane().getStylesheets().add(appController.getPrimaryStage().getScene().getStylesheets().get(0));

                        RadioButton option1 = new RadioButton("Zip folder");
                        RadioButton option2 = new RadioButton("Zip file");

                        ToggleGroup toggleGroup = new ToggleGroup();
                        option1.setToggleGroup(toggleGroup);
                        option2.setToggleGroup(toggleGroup);

                        HBox hbox = new HBox(10, option1, option2);
                        hbox.setPadding(new Insets(10));

                        dialog.getDialogPane().setContent(hbox);
                        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                        Optional<ButtonType> zipResult = dialog.showAndWait();

                        if(zipResult.isPresent()) {
                            ButtonType selectedButton = zipResult.get();
                            if (selectedButton == ButtonType.OK) {
                                RadioButton selectedRadioButton = (RadioButton) toggleGroup.getSelectedToggle();
                                if (selectedRadioButton != null) {
                                    String selectedOption = selectedRadioButton.getText();
                                    if (selectedOption.equals("Zip folder"))
                                        result = openFolderChooser();
                                    else
                                        result = openFileChooser();
                                }
                            }
                        }
                        break;
                    default:
                        result =inputDialog.showAndWait();
                        break;
                }
                break;
        }



        if(result.isPresent())
        {
            ResultDTO resultDTO=engine.processInput(button.getId(),result.get());
            if(resultDTO.getStatus())
            {
                button.setStyle("-fx-background-color: #40ff00; ");
                if(engine.isFlowReady())
                    executeButton.setDisable(false);
            }
            else
            {
                Alert alert =new Alert(Alert.AlertType.ERROR);

                ObservableList<String> stylesheets = appController.getPrimaryStage().getScene().getStylesheets();
                if(stylesheets.size()!=0)
                    alert.getDialogPane().getStylesheets().add(stylesheets.get(0));

                alert.setTitle("Error");
                alert.setContentText(resultDTO.getMessage());
                alert.showAndWait();
            }
        }
    }

    public Optional<String> openFolderChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");
        File selectedFolder = directoryChooser.showDialog(appController.getPrimaryStage());
        if (selectedFolder != null)
            return Optional.of(selectedFolder.getAbsolutePath());
        else
            return Optional.empty();
    }

    public Optional<String> openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        File selectedFolder = fileChooser.showOpenDialog(appController.getPrimaryStage());
        if (selectedFolder != null)
            return Optional.of(selectedFolder.getAbsolutePath());
        else
            return Optional.empty();
    }


    public void rightInputClick(Button button)
    {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem item1 = new MenuItem("Clear input's data");
        MenuItem item2 = new MenuItem("Show input's data");

        contextMenu.getItems().addAll(item1,item2);

        item1.setOnAction(event -> {
            boolean necessity =engine.clearInputData(button.getId()).getNecessity();
            if(necessity){
                executeButton.setDisable(true);
                button.setStyle("-fx-background-color: #ff0000; ");
            }
            else
                button.setStyle("");
        });

        item2.setOnAction(event -> {
            String data =engine.getInputData(button.getId()).getData();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);

            ObservableList<String> stylesheets = appController.getPrimaryStage().getScene().getStylesheets();
            if(stylesheets.size()!=0)
                alert.getDialogPane().getStylesheets().add(stylesheets.get(0));

            alert.setGraphic(null);
            alert.setTitle("input's data");
            if(data!=null)
                alert.setHeaderText(data);
            else
                alert.setHeaderText("no data");
            alert.showAndWait();
        });

        button.setContextMenu(contextMenu);
    }




    @FXML
    private void executeFlow(ActionEvent event)
    {
        elementDetailsView.getChildren().clear();
        String flowId=engine.runFlow();
        appController.addFlowId(flowId);
        executeButton.setDisable(true);

        for(Button button:mandatoryInputButtons) {
            button.setStyle("-fx-background-color: #ff0000; ");
            RotateTransition rotationY = new RotateTransition();
            rotationY.setAxis(Rotate.Z_AXIS);
            rotationY.setDuration(Duration.seconds(1));
            rotationY.setByAngle(-360);
            rotationY.setNode(button);
            rotationY.setCycleCount(1);
            rotationY.play();
        }
        for (Button button:optionalInputButtons) {
            button.setStyle("");
            RotateTransition rotationY = new RotateTransition();
            rotationY.setAxis(Rotate.Z_AXIS);
            rotationY.setDuration(Duration.seconds(1));
            rotationY.setByAngle(360);
            rotationY.setNode(button);
            rotationY.setCycleCount(1);
            rotationY.play();
        }




    }

    public void updateProgressFlow(FlowExecutionDTO flowExecutionDTO)
    {
        elementLogic.setElementDetailsView(flowExecutionDTO);
        progressBarView.setProgress(flowExecutionDTO.getProgress());
        if(flowExecutionDTO.getStateAfterRun()!=null)
        {
            ContinutionMenuDTO continutionMenuDTO=engine.getContinutionMenuDTO();
            if(continutionMenuDTO!=null)
            {
                List<String> targetFlows = continutionMenuDTO.getTargetFlows();
                choiceBoxView.setItems(FXCollections.observableArrayList(targetFlows));
                choiceBoxView.setDisable(false);
            }
            progressBarView.setProgress(1);
        }

    }



    @FXML
    void showFlowInfo(MouseEvent event) {
        elementLogic.updateFlowInfoView();
    }

    @FXML
    void continueToFlow(ActionEvent event) {
        String targetName = choiceBoxView.getValue();
        engine.doContinuation(engine.getFlowExecution(elementLogic.getID()),targetName);
        int index=engine.getFlowIndexByName(targetName);
        appController.streamFlow(index);
    }

}
