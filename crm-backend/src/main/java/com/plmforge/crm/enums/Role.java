package com.plmforge.crm.enums;

/**
 * The three business roles of the CRM module.
 *
 * The stored/authority value keeps the Spring Security "ROLE_" prefix, while
 * {@link #getDisplayName()} is the clean business name used in API responses.
 */
public enum Role {
    ROLE_CUSTOMER,
    ROLE_SALES_REPRESENTATIVE,
    ROLE_SALES_MANAGER;

    public String getDisplayName() {
        return name().substring("ROLE_".length());
    }
}
