-- =====================================================
-- Desktop App OTA Auto-Update - Database Migration
-- =====================================================
-- Run this SQL ONCE on your PostgreSQL database.
-- Note: Hibernate ddl-auto=update will auto-create these,
-- but it's good practice to have the migration script.
-- =====================================================

-- Add columns for desktop app OTA update
ALTER TABLE system_settings 
    ADD COLUMN IF NOT EXISTS latest_app_version VARCHAR(20),
    ADD COLUMN IF NOT EXISTS update_release_notes VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS update_mandatory BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS update_download_url VARCHAR(500);


-- =====================================================
-- EXAMPLE: Push a new update to all desktop POS apps
-- =====================================================

-- Option 1: Auto-download update (recommended)
-- Upload your .exe to a server first, then set the URL here
--
-- UPDATE system_settings 
-- SET latest_app_version = '1.0.1',
--     update_release_notes = 'Fixed receipt printer issue',
--     update_download_url = 'https://your-server.com/updates/KumaraPOS-1.0.1.exe',
--     update_mandatory = false
-- WHERE is_active = true;

-- Option 2: GitHub Releases (free hosting)
--
-- UPDATE system_settings 
-- SET latest_app_version = '1.0.1',
--     update_release_notes = 'Fixed receipt printer issue',
--     update_download_url = 'https://github.com/YourRepo/POS-desktop_touch/releases/download/v1.0.1/KumaraPOS-1.0.1.exe',
--     update_mandatory = false
-- WHERE is_active = true;

-- Option 3: Notification only (no auto-download, pen drive workflow)
--
-- UPDATE system_settings 
-- SET latest_app_version = '1.0.1',
--     update_release_notes = 'Fixed receipt printer issue',
--     update_download_url = NULL,
--     update_mandatory = false
-- WHERE is_active = true;

-- Option 4: MANDATORY update (blocks login until installed)
--
-- UPDATE system_settings 
-- SET latest_app_version = '1.0.1',
--     update_release_notes = 'Critical security fix - must update',
--     update_download_url = 'https://your-server.com/updates/KumaraPOS-1.0.1.exe',
--     update_mandatory = true
-- WHERE is_active = true;

-- =====================================================
-- CLEAR the update notification (after all clients updated)
-- =====================================================
-- UPDATE system_settings 
-- SET latest_app_version = NULL,
--     update_release_notes = NULL,
--     update_download_url = NULL,
--     update_mandatory = false
-- WHERE is_active = true;
