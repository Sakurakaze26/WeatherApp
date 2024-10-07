module guissica_soho {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    opens guissica_soho to javafx.fxml;
    exports guissica_soho;
}
