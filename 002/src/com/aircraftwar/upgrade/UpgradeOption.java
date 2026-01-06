package com.aircraftwar.upgrade;

public enum UpgradeOption {
    FIRE("增加弹道"),
    SHIELD("增加旋转盾牌"),
    SPEED("移速增加");

    private final String label;
    UpgradeOption(String label) { this.label = label; }
    public String getLabel() { return label; }
}

