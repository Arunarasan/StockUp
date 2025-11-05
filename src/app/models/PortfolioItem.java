package app.models;

public class PortfolioItem {
    private String symbol;
    private String companyName;
    private int quantity;
    private double avgPrice;
    private double currentValue;

    public PortfolioItem(String symbol, String companyName, int quantity, double avgPrice, double currentValue) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.currentValue = currentValue;
    }

    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public int getQuantity() { return quantity; }
    public double getAvgPrice() { return avgPrice; }
    public double getCurrentValue() { return currentValue; }
}
