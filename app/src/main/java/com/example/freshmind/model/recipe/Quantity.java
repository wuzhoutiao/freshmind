package com.example.freshmind.model.recipe;

import java.util.Objects;

/**
 * Represents a measured ingredient amount and unit used by a recipe.
 */
public class Quantity {
    private final double amount;
    private final String unit;

    /**
     * Creates a quantity with a positive amount and a non-empty unit.
     */
    public Quantity(double amount, String unit) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException("Unit cannot be empty.");
        }
        this.amount = amount;
        this.unit = unit.trim();
    }

    /**
     * Returns the numeric amount required.
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns the measurement unit for the amount.
     */
    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return amount + " " + unit;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Quantity)) {
            return false;
        }
        Quantity other = (Quantity) obj;
        return Double.compare(amount, other.amount) == 0 &&
                Objects.equals(unit, other.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, unit);
    }
}
