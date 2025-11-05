package app.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class Stock {

    private final SimpleStringProperty symbol;
    private final SimpleStringProperty name;
    private final SimpleDoubleProperty price;

    public Stock(String symbol, String name, double price) {
        this.symbol = new SimpleStringProperty(symbol);
        this.name = new SimpleStringProperty(name);
        this.price = new SimpleDoubleProperty(price);
    }

    // ✅ Required getters for PropertyValueFactory
    public String getSymbol() {
        return symbol.get();
    }

    public String getName() {
        return name.get();
    }

    public double getPrice() {
        return price.get();
    }

    // ✅ Optional setters (if you want to edit values)
    public void setSymbol(String symbol) {
        this.symbol.set(symbol);
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public void setPrice(double price) {
        this.price.set(price);
    }

    // ✅ Property accessors (used by JavaFX bindings)
    public SimpleStringProperty symbolProperty() {
        return symbol;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public SimpleDoubleProperty priceProperty() {
        return price;
    }
}
