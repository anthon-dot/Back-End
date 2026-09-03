package com.code.back_end.service;

import com.code.back_end.entity.SystemSettings;
import com.code.back_end.repository.SystemSettingsRepository;
import com.code.back_end.security.SecurityService;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final SystemSettingsRepository repo;
    private final SecurityService securityService;

    public SystemSettingsService(SystemSettingsRepository repo, SecurityService securityService) {
        this.repo = repo;
        this.securityService = securityService;
    }

    /** Returns the single settings row, creating defaults if it doesn't exist yet. */
    public SystemSettings getOrCreate() {
        securityService.requireAdmin();
        return repo.findById(SETTINGS_ID).orElseGet(() -> repo.save(new SystemSettings()));
    }

    public SystemSettings update(SystemSettings payload) {
        securityService.requireAdmin();
        SystemSettings settings = repo.findById(SETTINGS_ID).orElseGet(SystemSettings::new);
        settings.setId(SETTINGS_ID);

        if (payload.getSystemName()   != null) settings.setSystemName(payload.getSystemName());
        if (payload.getMunicipality() != null) settings.setMunicipality(payload.getMunicipality());
        if (payload.getOffice()       != null) settings.setOffice(payload.getOffice());
        if (payload.getContact()      != null) settings.setContact(payload.getContact());
        if (payload.getEmailAddress() != null) settings.setEmailAddress(payload.getEmailAddress());

        if (payload.getBillingFrequency()      != null) settings.setBillingFrequency(payload.getBillingFrequency());
        if (payload.getAdvancePaymentPeriod()  != null) settings.setAdvancePaymentPeriod(payload.getAdvancePaymentPeriod());
        if (payload.getGracePeriod()           != null) settings.setGracePeriod(payload.getGracePeriod());
        if (payload.getCurrency()              != null) settings.setCurrency(payload.getCurrency());

        if (payload.getApplicationNotifications() != null) settings.setApplicationNotifications(payload.getApplicationNotifications());
        if (payload.getPaymentNotifications()     != null) settings.setPaymentNotifications(payload.getPaymentNotifications());
        if (payload.getBillingReminders()         != null) settings.setBillingReminders(payload.getBillingReminders());
        if (payload.getContractExpirationAlerts() != null) settings.setContractExpirationAlerts(payload.getContractExpirationAlerts());

        return repo.save(settings);
    }
}
