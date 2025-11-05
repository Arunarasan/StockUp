package app.models;

public class Transaction {
    private String symbol;
    private String type;
    private int quantity;
    private double price;
    private String date;

    public Transaction(String symbol, String type, int quantity, double price, String date) {
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.date = date;
    }

    public String getSymbol() { return symbol; }
    public String getType() { return type; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getDate() { return date; }
}
