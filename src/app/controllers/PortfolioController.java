package app.controllers;

import app.db.DBManager;
import app.models.Stock;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;

public class PortfolioController {
    @FXML private TableView<?> tblPortfolio;
    private DBManager db; private int userId;
    public void init(DBManager db, int userId){ this.db = db; this.userId = userId; }
}
