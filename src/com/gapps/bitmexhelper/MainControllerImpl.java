package com.gapps.bitmexhelper;

import com.gapps.bitmexhelper.kotlin.MainUiDelegate;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

@SuppressWarnings("NullableProblems")
public class MainControllerImpl implements com.gapps.bitmexhelper.kotlin.MainController {

    private final MainUiDelegate delegate = MainUiDelegate.INSTANCE;

    @FXML
    private ComboBox pair;

    @FXML
    private Spinner highPirce;

    @FXML
    private Spinner lowPirce;

    @FXML
    private Spinner amount;

    @FXML
    private ComboBox orderType;

    @FXML
    private ComboBox side;

    @FXML
    private ComboBox distribution;

    @FXML
    private Spinner parameter;

    @FXML
    private Spinner minAmount;

    @FXML
    private CheckBox reversed;

    @FXML
    private CheckBox postOnly;

    @FXML
    private CheckBox reduceOnly;

    @FXML
    private Button execute;

    @FXML
    private TableView review;

    @FXML
    private TextArea stats;

    public MainControllerImpl() {
        super();
        delegate.onControllerAvailable(this);
    }

    @Override
    public ComboBox getPair() {
        return pair;
    }

    @Override
    public Spinner getHighPirce() {
        return highPirce;
    }

    @Override
    public Spinner getLowPirce() {
        return lowPirce;
    }

    @Override
    public Spinner getAmount() {
        return amount;
    }

    @Override
    public ComboBox getOrderType() {
        return orderType;
    }

    @Override
    public ComboBox getSide() {
        return side;
    }

    @Override
    public ComboBox getDistribution() {
        return distribution;
    }

    @Override
    public Spinner getParameter() {
        return parameter;
    }

    @Override
    public Spinner getMinAmount() {
        return minAmount;
    }

    @Override
    public CheckBox getReversed() {
        return reversed;
    }

    @Override
    public CheckBox getPostOnly() {
        return postOnly;
    }

    @Override
    public CheckBox getReduceOnly() {
        return reduceOnly;
    }

    @Override
    public Button getExecute() {
        return execute;
    }

    @Override
    public TableView getReview() {
        return review;
    }

    @Override
    public TextArea getStats() {
        return stats;
    }

    @FXML
    protected void onAboutClicked() {
        delegate.onAboutClicked();
    }

    @FXML
    protected void onSettingsClicked() {
        delegate.onSettingsClicked();
    }

    @FXML
    protected void onQuitClicked() {
        delegate.onQuitClicked();
    }

    @Override
    public void exitApp() {
        Platform.exit();
    }
}
