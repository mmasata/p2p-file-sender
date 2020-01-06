package dsv.mma.filesender;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        //GUI pro select portu
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/SelectPort.fxml"));  
        Scene scene = new Scene(root);  
        stage.setTitle("Select port");
        stage.setScene(scene);
        stage.show();
       
    }

    public static void main(String[] args) {
        launch(args);
    }

}
