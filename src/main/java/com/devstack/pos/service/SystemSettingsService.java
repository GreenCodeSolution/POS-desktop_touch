package com.devstack.pos.service;

import com.devstack.pos.entity.SystemSettings;
import com.devstack.pos.repository.SystemSettingsRepository;
import com.devstack.pos.util.TerminalConfigManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;

    public SystemSettings getSystemSettings() {
        Long businessId = TerminalConfigManager.getBusinessId();
        if (businessId != null) {
            Optional<SystemSettings> bizSettings = systemSettingsRepository.findFirstByBusinessId(businessId);
            if (bizSettings.isPresent()) {
                return bizSettings.get();
            }
        }

        return systemSettingsRepository.findFirstByIsActiveTrue()
                .orElseGet(() -> {
                    // Create default settings if none exist
                    SystemSettings defaultSettings = new SystemSettings(
                            TerminalConfigManager.isPaired() ? TerminalConfigManager.getBusinessName() : "Kumara Enterprises",
                            "",
                            "",
                            "",
                            "",
                            "Thank you for your business!");
                    defaultSettings.setTrialEnabled(false);
                    defaultSettings.setTrialEndDate(null);
                    if (businessId != null) {
                        defaultSettings.setBusinessId(businessId);
                    }
                    return systemSettingsRepository.save(defaultSettings);
                });
    }

    private final java.util.List<Runnable> listeners = new java.util.ArrayList<>();

    public void addSettingsUpdateListener(Runnable listener) {
        listeners.add(listener);
    }

}
