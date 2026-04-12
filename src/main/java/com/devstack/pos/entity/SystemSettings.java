package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "footer_message", length = 200)
    private String footerMessage;

    @Column(name = "logo_path", length = 500)
    private String logoPath;

    // Trial version settings
    @Column(name = "trial_enabled")
    private Boolean trialEnabled = false;

    @Column(name = "trial_end_date")
    private java.time.LocalDate trialEndDate;

    @Column(name = "last_backup_time")
    private java.time.LocalDateTime lastBackupTime;

    @Column(name = "last_restore_time")
    private java.time.LocalDateTime lastRestoreTime;

    // Single row constraint - only one settings record should exist
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "loyalty_points_rate")
    private Double loyaltyPointsRate = 100.0;

    // Desktop app update notification fields
    // Set these from Web Admin or directly in DB when you release a new version
    @Column(name = "latest_app_version", length = 20)
    private String latestAppVersion;

    @Column(name = "update_release_notes", length = 1000)
    private String updateReleaseNotes;

    @Column(name = "update_mandatory")
    private Boolean updateMandatory = false;

    // URL where the new .exe installer is hosted
    // e.g., "https://your-server.com/updates/KumaraPOS-1.0.1.exe"
    // or GitHub Releases URL, Google Drive direct link, etc.
    @Column(name = "update_download_url", length = 500)
    private String updateDownloadUrl;

    public SystemSettings(String businessName, String address, String contactNumber,
            String email, String taxNumber, String footerMessage) {
        this.businessName = businessName;
        this.address = address;
        this.contactNumber = contactNumber;
        this.email = email;
        this.taxNumber = taxNumber;
        this.footerMessage = footerMessage;
        this.trialEnabled = false;
        this.trialEndDate = null;
        this.isActive = true;
        this.loyaltyPointsRate = 100.0;
        this.latestAppVersion = null;
        this.updateReleaseNotes = null;
        this.updateMandatory = false;
    }
}
