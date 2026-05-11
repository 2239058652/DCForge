package com.forge.dc.staff.entity;

public enum StaffType {
    DOCTOR(0), NURSE(1), RECEPTIONIST(2);
    public final int value;

    StaffType(int value) {
        this.value = value;
    }

    public static StaffType of(int v) {
        for (StaffType t : values()) if (t.value == v) return t;
        throw new IllegalArgumentException("Unknown staff type: " + v);
    }
}