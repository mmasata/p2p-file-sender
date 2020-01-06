/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dsv.mma.filesender.client;

import dsv.mma.filesender.GlobalVariables;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * FXML Controller class
 *
 * @author Martas-NTB
 */
public class SelectPortController implements Initializable {

    @FXML
    private Button portSubmitBtn;
    
    @FXML
    private TextField portField;
    
    
    @FXML
    private void selectPortAction(ActionEvent event) throws IOException{
        int port = Integer.valueOf(portField.getText());
        GlobalVariables.setMyPort(port);
        
        Stage stage = (Stage) portSubmitBtn.getScene().getWindow();
        
         
        //GUI file Senderu
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/FileSender.fxml"));  
        Scene scene = new Scene(root);      
        stage.setTitle("File sender");
        stage.setScene(scene);
        stage.show();
        
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
}
