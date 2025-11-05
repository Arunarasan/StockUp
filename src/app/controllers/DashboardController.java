package app.controllers;

import app.db.DBManager;
import app.models.Stock;
import app.models.PortfolioItem;
import app.models.Transaction;
import app.models.WatchlistItem;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.*;
import java.util.Optional;
import java.util.Random;

public class DashboardController {

    // 📈 Market Table
    @FXML private TableView<Stock> tblMarket;
    @FXML private TableColumn<Stock, String> colSymbol;
    @FXML private TableColumn<Stock, String> colName;
    @FXML private TableColumn<Stock, Double> colPrice;

    // 💼 Portfolio Table
    @FXML private TableView<PortfolioItem> tblPortfolio;
    @FXML private TableColumn<PortfolioItem, String> colPSymbol;
    @FXML private TableColumn<PortfolioItem, String> colPName;
    @FXML private TableColumn<PortfolioItem, Integer> colPQty;
    @FXML private TableColumn<PortfolioItem, Double> colPAvg;
    @FXML private TableColumn<PortfolioItem, Double> colPValue;

    // 🕒 Transactions Table
    @FXML private TableView<Transaction> tblTransactions;
    @FXML private TableColumn<Transaction, String> colTSymbol;
    @FXML private TableColumn<Transaction, String> colTType;
    @FXML private TableColumn<Transaction, Integer> colTQty;
    @FXML private TableColumn<Transaction, Double> colTPrice;
    @FXML private TableColumn<Transaction, String> colTDate;

    // 📊 Charts
    @FXML private PieChart pieChart;
    @FXML private LineChart<Number, Number> lineChart;

    // 💰 Controls
    @FXML private Button btnBuy;
    @FXML private Button btnSell;
    @FXML private Button btnLogout;
    @FXML private Button btnAddMoney;

    // 👀 Watchlist
    @FXML private TableView<WatchlistItem> tblWatchlist;
    @FXML private TableColumn<WatchlistItem, String> colWLSymbol;
    @FXML private TableColumn<WatchlistItem, String> colWLCompany;
    @FXML private TableColumn<WatchlistItem, Double> colWLPrice;
    @FXML private Button btnAddWatch;
    @FXML private Button btnRemoveWatch;

    // 🏦 Balance
    @FXML private Label lblBalance;

    // 🔧 App Data
    private DBManager db;
    private int userId;
    private final Random random = new Random();

    private ObservableList<Stock> marketData = FXCollections.observableArrayList();
    private ObservableList<WatchlistItem> watchlistData = FXCollections.observableArrayList();
    private XYChart.Series<Number, Number> portfolioValueSeries = new XYChart.Series<>();
    private int timeCounter = 0;

    // 🧠 Initialize
    @FXML
    public void initialize() {
        setupMarketTable();
        setupPortfolioTable();
        setupTransactionTable();
        setupLineChart();
        startLiveMarketSimulation();

        // 🎯 Button Actions
        btnBuy.setOnAction(e -> buyOrSell("BUY"));
        btnSell.setOnAction(e -> buyOrSell("SELL"));
        btnLogout.setOnAction(e -> handleLogout());
        btnAddMoney.setOnAction(e -> handleAddMoney());
        btnAddWatch.setOnAction(e -> handleAddToWatchlist());
        btnRemoveWatch.setOnAction(e -> handleRemoveFromWatchlist());

        // Watchlist setup
        colWLSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colWLCompany.setCellValueFactory(new PropertyValueFactory<>("company"));
        colWLPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        tblWatchlist.setItems(watchlistData);
    }

    // 🔌 Initialize DB and load user data
    public void init(DBManager db, int userId) {
        this.db = db;
        this.userId = userId;
        loadPortfolio();
        loadTransactions();
        loadWatchlist();
        refreshBalanceLabel();
        startWatchlistLiveUpdates();
    }

    // 🧾 Load Watchlist
    private void loadWatchlist() {
        watchlistData.clear();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT symbol, company_name FROM watchlist WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String symbol = rs.getString("symbol");
                String company = rs.getString("company_name");
                double price = 100 + random.nextDouble() * 1000;
                watchlistData.add(new WatchlistItem(symbol, company, price));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void startWatchlistLiveUpdates() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            for (WatchlistItem i : watchlistData) {
                double newPrice = i.getPrice() * (1 + (random.nextDouble() - 0.5) * 0.01);
                i.setPrice(Math.round(newPrice * 100.0) / 100.0);
            }
            tblWatchlist.refresh();
        }));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

    private void handleAddToWatchlist() {
        Stock s = tblMarket.getSelectionModel().getSelectedItem();
        if (s == null) {
            showError("Select a stock first!");
            return;
        }

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT IGNORE INTO watchlist (user_id, symbol, company_name) VALUES (?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, s.getSymbol());
            ps.setString(3, s.getName());
            ps.executeUpdate();
            loadWatchlist();
            showInfo(s.getSymbol() + " added to watchlist!");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void handleRemoveFromWatchlist() {
        WatchlistItem s = tblWatchlist.getSelectionModel().getSelectedItem();
        if (s == null) {
            showError("Select a watchlist item first!");
            return;
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM watchlist WHERE user_id=? AND symbol=?")) {
            ps.setInt(1, userId);
            ps.setString(2, s.getSymbol());
            ps.executeUpdate();
            loadWatchlist();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    // 📈 Setup Tables
    private void setupMarketTable() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        marketData.addAll(
                new Stock("TCS", "Tata Consultancy", 3821.50),
                new Stock("INFY", "Infosys Ltd", 1445.75),
                new Stock("HDFC", "HDFC Bank", 1602.90),
                new Stock("RELI", "Reliance Industries", 2904.40),
                new Stock("WIPR", "Wipro Ltd", 468.10)
        );
        tblMarket.setItems(marketData);
    }

    private void setupPortfolioTable() {
        colPSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPName.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colPQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPAvg.setCellValueFactory(new PropertyValueFactory<>("avgPrice"));
        colPValue.setCellValueFactory(new PropertyValueFactory<>("currentValue"));
    }

    private void setupTransactionTable() {
        colTSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colTType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colTQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTDate.setCellValueFactory(new PropertyValueFactory<>("date"));
    }

    private void setupLineChart() {
        ((NumberAxis) lineChart.getXAxis()).setLabel("Time (s)");
        ((NumberAxis) lineChart.getYAxis()).setLabel("Portfolio Value (₹)");
        lineChart.setTitle("Live Portfolio Value");
        lineChart.getData().add(portfolioValueSeries);
        startPortfolioValueTracking();
    }

    private void startPortfolioValueTracking() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            double val = calculatePortfolioValue();
            portfolioValueSeries.getData().add(new XYChart.Data<>(timeCounter++, val));
            if (portfolioValueSeries.getData().size() > 20)
                portfolioValueSeries.getData().remove(0);
        }));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

    private void startLiveMarketSimulation() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            for (Stock s : marketData) {
                double change = (random.nextDouble() - 0.5) * 0.02;
                s.setPrice(Math.round((s.getPrice() + s.getPrice() * change) * 100.0) / 100.0);
            }
            tblMarket.refresh();
        }));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

// 💸 Handle Buy / Sell
private void buyOrSell(String type) {
    Stock selected = tblMarket.getSelectionModel().getSelectedItem();
    if (selected == null) {
        showError("Please select a stock first!");
        return;
    }

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle(type + " " + selected.getSymbol());
    dialog.setHeaderText("Enter quantity to " + type.toLowerCase() + ":");
    Optional<String> result = dialog.showAndWait();

    result.ifPresent(qtyStr -> {
        try {
            int qty = Integer.parseInt(qtyStr);
            double price = selected.getPrice();

            if (qty <= 0) {
                showError("Quantity must be positive.");
                return;
            }

            try (Connection conn = db.getConnection()) {
                double totalCost = price * qty;
                double currentBalance = getUserBalance(conn);

                if (type.equalsIgnoreCase("BUY")) {
                    // 🟢 BUY logic
                    if (currentBalance < totalCost) {
                        showError("❌ Insufficient funds! Current balance: ₹" + String.format("%.2f", currentBalance));
                        return;
                    }

                    updatePortfolio(selected.getSymbol(), selected.getName(), qty, price, "BUY");
                    updateUserBalance( currentBalance - totalCost);

                } else if (type.equalsIgnoreCase("SELL")) {
                    // 🔴 SELL logic
                    if (!hasEnoughStock(selected.getSymbol(), qty)) {
                        showError("❌ You don’t have enough shares to sell.");
                        return;
                    }

                    updatePortfolio(selected.getSymbol(), selected.getName(), qty, price, "SELL");
                    updateUserBalance( currentBalance + totalCost);
                }

                // 💾 Record the transaction
                try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO transactions (user_id, symbol, type, quantity, price, created_at) VALUES (?, ?, ?, ?, ?, NOW())")) {
                    ps.setInt(1, userId);
                    ps.setString(2, selected.getSymbol());
                    ps.setString(3, type.toUpperCase());
                    ps.setInt(4, qty);
                    ps.setDouble(5, price);
                    ps.executeUpdate();
                }

                // ✅ Force UI updates after DB commit
                refreshAllUI();

                showInfo("✅ " + type + " successful for " + selected.getSymbol());
            }
        } catch (NumberFormatException ex) {
            showError("Invalid quantity entered!");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database error: " + e.getMessage());
        }
    });
}

//🔁 Refresh all UI tables and charts
private void refreshAllUI() {
 loadPortfolio();          // Refresh portfolio table + pie chart
 loadTransactions();       // Refresh transaction history
 updateBalanceLabel();     // Refresh wallet balance
 refreshPortfolioChart();  // Refresh portfolio value line chart
}

//🔄 Recalculate portfolio line chart value
private void refreshPortfolioChart() {
 double currentValue = calculatePortfolioValue();
 portfolioValueSeries.getData().add(new XYChart.Data<>(timeCounter++, currentValue));

 // Keep last 20 data points for smoothness
 if (portfolioValueSeries.getData().size() > 20)
     portfolioValueSeries.getData().remove(0);
}
private void loadPortfolio() {
    try (Connection conn = db.getConnection();
         PreparedStatement ps = conn.prepareStatement(
                 "SELECT symbol, company_name, quantity, avg_price FROM portfolio WHERE user_id=?")) {
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        tblPortfolio.getItems().clear();
        pieChart.getData().clear();

        while (rs.next()) {
            String symbol = rs.getString("symbol");
            String name = rs.getString("company_name");
            int qty = rs.getInt("quantity");
            double avg = rs.getDouble("avg_price");

            double marketPrice = marketData.stream()
                    .filter(s -> s.getSymbol().equals(symbol))
                    .map(Stock::getPrice)
                    .findFirst()
                    .orElse(avg);

            double value = qty * marketPrice;

            PortfolioItem item = new PortfolioItem(symbol, name, qty, avg, value);
            tblPortfolio.getItems().add(item);
            pieChart.getData().add(new PieChart.Data(symbol, value));
        }
    } catch (SQLException e) {
        e.printStackTrace();
        showError("Failed to load portfolio: " + e.getMessage());
    }
}

 // 🏦 Add Money
    private void handleAddMoney() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Money");
        dialog.setHeaderText("Enter amount to deposit:");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    showError("Amount must be positive!");
                    return;
                }

                try (Connection conn = db.getConnection()) {
                    double currentBalance = getUserBalance(conn);
                    double newBalance = currentBalance + amount;
                    updateUserBalance( newBalance); // ✅ Update DB
                    updateBalanceLabel();                // ✅ Refresh label
                    showInfo("₹" + String.format("%.2f", amount) + " added successfully!\nNew Balance: ₹" + String.format("%.2f", newBalance));
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid number.");
            } catch (SQLException e) {
                showError("Database Error: " + e.getMessage());
            }
        });
    }


    // 📈 Portfolio Updates
    private void updatePortfolio(String symbol, String name, int qty, double price, String type) {
    	try (Connection conn = db.getConnection()) {
        String checkSql = "SELECT quantity, avg_price FROM portfolio WHERE user_id=? AND symbol=?";
        PreparedStatement ps = conn.prepareStatement(checkSql);
        ps.setInt(1, userId);
        ps.setString(2, symbol);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int oldQty = rs.getInt("quantity");
            double oldAvg = rs.getDouble("avg_price");

            if (type.equalsIgnoreCase("BUY")) {
                int newQty = oldQty + qty;
                double newAvg = ((oldAvg * oldQty) + (price * qty)) / newQty;

                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE portfolio SET quantity=?, avg_price=? WHERE user_id=? AND symbol=?")) {
                    upd.setInt(1, newQty);
                    upd.setDouble(2, newAvg);
                    upd.setInt(3, userId);
                    upd.setString(4, symbol);
                    upd.executeUpdate();
                }
            } else if (type.equalsIgnoreCase("SELL")) {
                int newQty = oldQty - qty;
                if (newQty <= 0) {
                    try (PreparedStatement del = conn.prepareStatement(
                            "DELETE FROM portfolio WHERE user_id=? AND symbol=?")) {
                        del.setInt(1, userId);
                        del.setString(2, symbol);
                        del.executeUpdate();
                    }
                } else {
                    try (PreparedStatement upd = conn.prepareStatement(
                            "UPDATE portfolio SET quantity=? WHERE user_id=? AND symbol=?")) {
                        upd.setInt(1, newQty);
                        upd.setInt(2, userId);
                        upd.setString(3, symbol);
                        upd.executeUpdate();
                    }
                }
            }
        } else if (type.equalsIgnoreCase("BUY")) {
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO portfolio (user_id, symbol, company_name, quantity, avg_price) VALUES (?, ?, ?, ?, ?)")) {
                ins.setInt(1, userId);
                ins.setString(2, symbol);
                ins.setString(3, name);
                ins.setInt(4, qty);
                ins.setDouble(5, price);
                ins.executeUpdate();
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        showError("Portfolio update failed: " + e.getMessage());
    }
}
    
 // ✅ Refresh balance label in header bar
    private void updateBalanceLabel() {
        try (Connection conn = db.getConnection()) {
            double balance = getUserBalance(conn);
            Platform.runLater(() -> lblBalance.setText("₹" + String.format("%.2f", balance)));
        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> lblBalance.setText("Error"));
        }
    }


    // 🧮 Helpers
    private double calculatePortfolioValue() {
        double total = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT symbol, quantity, avg_price FROM portfolio WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String sym = rs.getString("symbol");
                int q = rs.getInt("quantity");
                double avg = rs.getDouble("avg_price");
                double price = marketData.stream()
                        .filter(s -> s.getSymbol().equals(sym))
                        .map(Stock::getPrice)
                        .findFirst()
                        .orElse(avg);
                total += q * price;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return total;
    }

   
    private void loadTransactions() {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT symbol, type, quantity, price, created_at AS date FROM transactions WHERE user_id=? ORDER BY created_at DESC")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            tblTransactions.getItems().clear();
            while (rs.next()) {
                tblTransactions.getItems().add(new Transaction(
                        rs.getString("symbol"),
                        rs.getString("type"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("date")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private boolean hasEnoughStock(String symbol, int qty) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM portfolio WHERE user_id=? AND symbol=?")) {
            ps.setInt(1, userId);
            ps.setString(2, symbol);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt("quantity") >= qty;
        }
    }

    private double getUserBalance(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT balance FROM users WHERE id=?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getDouble("balance") : 0;
    }

    private void updateUserBalance(double newBalance) {
        String query = "UPDATE users SET balance=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setDouble(1, newBalance);
            ps.setInt(2, userId);
            ps.executeUpdate();

            lblBalance.setText(String.format("₹%.2f", newBalance));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshBalanceLabel() {
        try (Connection conn = db.getConnection()) {
            double bal = getUserBalance(conn);
            lblBalance.setText(String.format("₹%.2f", bal));
        } catch (SQLException e) {
            lblBalance.setText("Error");
        }
    }

    // 🚪 Logout
    private void handleLogout() {
        try {
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            showError("Failed to logout: " + e.getMessage());
        }
    }

    // 🔔 Alerts
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
