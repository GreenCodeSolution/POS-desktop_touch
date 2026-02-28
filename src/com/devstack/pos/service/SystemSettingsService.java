package com.devstack.pos.service;

import com.devstack.pos.entity.SystemSettings;
import com.devstack.pos.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;

    public SystemSettings getSystemSettings() {
        return systemSettingsRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    // Create default settings if none exist
                    SystemSettings defaultSettings = new SystemSettings(
                            "Kumara Enterprises",
                            "",
                            "",
                            "",
                            "",
                            "Thank you for your business!");
                    defaultSettings.setTrialEnabled(false);
                    defaultSettings.setTrialEndDate(null);
                    return systemSettingsRepository.save(defaultSettings);
                });
    }


    private final java.util.List<Runnable> listeners = new java.util.ArrayList<>();

    public void addSettingsUpdateListener(Runnable listener) {
        listeners.add(listener);
    }


}
