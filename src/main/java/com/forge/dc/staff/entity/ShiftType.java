package com.forge.dc.staff.entity;

public enum ShiftType {
    DAY(0), NIGHT(1), REST(2);
    public final int value;

    ShiftType(int value) {
        this.value = value;
    }
}