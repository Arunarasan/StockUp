package app.controllers;

import app.db.DBManager;
import app.models.Transaction;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class TransactionsController {

    @FXML private TableView<Transaction> tblTransactions;
    @FXML private TableColumn<Transaction, String> colSymbol;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, Integer> colQty;
    @FXML private TableColumn<Transaction, Double> colPrice;
    @FXML private TableColumn<Transaction, String> colDate;

    private DBManager db;
    private int userId;

    public void init(DBManager db, int userId) {
        this.db = db;
        this.userId = userId;
        setupTable();
        loadTransactions();
    }

    private void setupTable() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
    }

    private void loadTransactions() {
        try {
            List<Transaction> list = db.loadTransactions(userId);
            tblTransactions.getItems().setAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
