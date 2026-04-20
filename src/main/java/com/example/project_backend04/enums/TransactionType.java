package com.example.project_backend04.enums;

public enum TransactionType {
    EARN("Earn"),
    REDEEM("Redeem");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
