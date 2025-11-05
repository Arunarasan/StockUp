package app.db;

import java.sql.*;
import java.util.*;
import app.models.*;

public class DBManager {

    private static DBManager instance;
    private Connection conn;

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    private DBManager(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    // ✅ Singleton pattern
    public static DBManager getInstance(String host, int port, String database, String user, String password) {
        if (instance == null) {
            instance = new DBManager(host, port, database, user, password);
            instance.connect();
        }
        return instance;
    }

    public static DBManager getInstance() {
        if (instance == null) throw new IllegalStateException("DBManager not initialized!");
        return instance;
    }

    // ✅ MySQL Connection (auto reconnect)
    private Connection connect() {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = String.format(
                	    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true&tcpKeepAlive=true",
                	    host, port, database
                	);
                conn = DriverManager.getConnection(url, user, password);
                System.out.println("✅ Connected to MySQL: " + database);
                System.out.println("✅ Connected (new connection opened): " + new java.util.Date());
            }
        } catch (Exception e) {
            System.err.println("❌ Database connection failed!");
            e.printStackTrace();
        }
        return conn;
    }

    public synchronized Connection getConnection() {
        try {
            // Check if connection exists and is open
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                System.out.println("⚠️ Reconnecting to MySQL...");
                connect();  // Force reconnect
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Connection invalid, attempting reconnect...");
            connect();
        }

        // Ensure we never return null
        if (conn == null) {
            connect();
        }

        return conn;
    }


    // 🔑 LOGIN
    public Optional<Integer> login(String username, String password) {
        String sql = "SELECT id FROM users WHERE username=? AND password=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(rs.getInt("id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // 🧾 SIGNUP
    public boolean signup(String username, String password) {
        String check = "SELECT id FROM users WHERE username=?";
        String insert = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement psCheck = getConnection().prepareStatement(check)) {
            psCheck.setString(1, username);
            ResultSet rs = psCheck.executeQuery();
            if (rs.next()) return false; // username exists
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement ps = getConnection().prepareStatement(insert)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 💼 LOAD PORTFOLIO
    public List<PortfolioItem> loadPortfolio(int userId) {
        List<PortfolioItem> list = new ArrayList<>();
        String sql = "SELECT symbol, company_name, quantity, avg_price FROM portfolio WHERE user_id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String symbol = rs.getString("symbol");
                String company = rs.getString("company_name");
                int qty = rs.getInt("quantity");
                double avgPrice = rs.getDouble("avg_price");

                double randomFactor = (Math.random() * 0.1) - 0.05;
                double currentValue = avgPrice * qty * (1 + randomFactor);

                list.add(new PortfolioItem(symbol, company, qty, avgPrice, currentValue));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 💳 LOAD TRANSACTIONS
    public List<Transaction> loadTransactions(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT symbol, type, quantity, price, created_at AS date FROM transactions WHERE user_id=? ORDER BY created_at DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getString("symbol"),
                        rs.getString("type"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 👀 LOAD WATCHLIST
    public List<String> loadWatchlist(int userId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT symbol FROM watchlist WHERE user_id=? ORDER BY symbol ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("symbol"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ➕ ADD TO WATCHLIST
    public boolean addToWatchlist(int userId, String symbol) {
        String sql = "INSERT INTO watchlist (user_id, symbol) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, symbol.toUpperCase());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("Duplicate entry")) e.printStackTrace();
        }
        return false;
    }

    // 🔒 Safe Close
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("🔒 Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
