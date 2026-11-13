package com.plmforge.crm.enums;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean isFinalStatus() {
        return this == RESOLVED || this == CLOSED;
    }
}
