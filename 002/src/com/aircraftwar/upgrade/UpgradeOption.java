package com.aircraftwar.upgrade;

public enum UpgradeOption {
    FIRE("增加弹道"),
    SPEED("移速增加"),
    FIRE_RATE("增加射速");

    private final String label;
    UpgradeOption(String label) { this.label = label; }
    public String getLabel() { return label; }
}
