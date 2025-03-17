module com.fxmlpreviewer {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.base;

    exports tn.ZeroS.FXMLPreviewer;
    opens tn.ZeroS.FXMLPreviewer to javafx.fxml;
}