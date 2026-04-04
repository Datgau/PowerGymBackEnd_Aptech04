package com.example.project_backend04.enums;

public enum MembershipLevel {
    SILVER(0, 4999, "Bạc"),
    GOLD(5000, 9999, "Vàng"),
    PLATINUM(10000, Integer.MAX_VALUE, "Bạch Kim");

    private final int minPoints;
    private final int maxPoints;
    private final String displayName;

    MembershipLevel(int minPoints, int maxPoints, String displayName) {
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.displayName = displayName;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MembershipLevel fromPoints(int points) {
        if (points >= PLATINUM.minPoints) return PLATINUM;
        if (points >= GOLD.minPoints) return GOLD;
        return SILVER;
    }

    public int getPointsToNext() {
        if (this == PLATINUM) return 0;
        MembershipLevel next = values()[ordinal() + 1];
        return next.minPoints;
    }
}
