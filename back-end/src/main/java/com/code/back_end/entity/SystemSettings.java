package com.code.back_end.entity;

import jakarta.persistence.*;

/**
 * Single-row settings table (always id = 1).
 * SystemSettingsService.getOrCreate() ensures a default row exists.
 */
@Entity
@Table(name = "system_settings")
public class SystemSettings {

    @Id
    private Long id = 1L;

    private String systemName   = "Rental Management System for the Public Market of Manticao";
    private String municipality = "Manticao";
    private String office       = "Public Market Office";
    private String contact      = "";
    private String emailAddress = "";

    // Billing
    private String billingFrequency      = "MONTHLY";
    private Integer advancePaymentPeriod = 0;
    private Integer gracePeriod          = 0;
    private String currency              = "PHP";

    // Notification toggles
    private Boolean applicationNotifications  = true;
    private Boolean paymentNotifications      = true;
    private Boolean billingReminders          = true;
    private Boolean contractExpirationAlerts  = true;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getBillingFrequency() { return billingFrequency; }
    public void setBillingFrequency(String billingFrequency) { this.billingFrequency = billingFrequency; }

    public Integer getAdvancePaymentPeriod() { return advancePaymentPeriod; }
    public void setAdvancePaymentPeriod(Integer advancePaymentPeriod) { this.advancePaymentPeriod = advancePaymentPeriod; }

    public Integer getGracePeriod() { return gracePeriod; }
    public void setGracePeriod(Integer gracePeriod) { this.gracePeriod = gracePeriod; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Boolean getApplicationNotifications() { return applicationNotifications; }
    public void setApplicationNotifications(Boolean applicationNotifications) { this.applicationNotifications = applicationNotifications; }

    public Boolean getPaymentNotifications() { return paymentNotifications; }
    public void setPaymentNotifications(Boolean paymentNotifications) { this.paymentNotifications = paymentNotifications; }

    public Boolean getBillingReminders() { return billingReminders; }
    public void setBillingReminders(Boolean billingReminders) { this.billingReminders = billingReminders; }

    public Boolean getContractExpirationAlerts() { return contractExpirationAlerts; }
    public void setContractExpirationAlerts(Boolean contractExpirationAlerts) { this.contractExpirationAlerts = contractExpirationAlerts; }
}
