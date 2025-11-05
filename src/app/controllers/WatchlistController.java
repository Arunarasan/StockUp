package app.controllers;

import app.db.DBManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;

public class WatchlistController {

    @FXML
    private ListView<String> lvWatchlist;

    private DBManager db;
    private int userId;

    public void init(DBManager db, int userId) {
        this.db = db;
        this.userId = userId;
        load();
    }

    private void load() {
        try {
            List<String> list = db.loadWatchlist(userId);

            lvWatchlist.getItems().clear();

            if (list.isEmpty()) {
                lvWatchlist.getItems().add("⚠️ Your watchlist is empty.");
            } else {
                lvWatchlist.getItems().addAll(list);
            }

        } catch (Exception e) {
            e.printStackTrace();
            lvWatchlist.getItems().add("❌ Error loading watchlist");
        }
    }
}
