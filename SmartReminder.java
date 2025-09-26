import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class SmartReminder extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Pass the primary stage to TaskApp
        Font f1 = Font.loadFont(getClass().getResourceAsStream("/fonts/Philosopher-Regular.ttf"), 14);
        System.out.println("Regular: " + (f1 != null ? f1.getName() : "FAILED"));

        Font f2 = Font.loadFont(getClass().getResourceAsStream("/fonts/Philosopher-Bold.ttf"), 14);
        System.out.println("Bold: " + (f2 != null ? f2.getName() : "FAILED"));
        new TaskApp(primaryStage);
    }


    public static void main(String[] args) {
        launch(args); // Launch JavaFX
    }
}

