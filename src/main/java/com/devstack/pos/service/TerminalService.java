package com.devstack.pos.service;

import com.devstack.pos.entity.Business;
import com.devstack.pos.entity.PosTerminal;
import com.devstack.pos.repository.BusinessRepository;
import com.devstack.pos.repository.PosTerminalRepository;
import com.devstack.pos.util.TerminalConfigManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TerminalService {

    private final BusinessRepository businessRepository;
    private final PosTerminalRepository terminalRepository;

    public PosTerminal pairTerminal(String businessCode, String pairingKey) {
        if (businessCode == null || businessCode.isBlank()) {
            throw new IllegalArgumentException("Business code is required.");
        }
        if (pairingKey == null || pairingKey.isBlank()) {
            throw new IllegalArgumentException("Pairing key is required.");
        }

        String cleanCode = businessCode.trim().toUpperCase();
        String cleanKey = pairingKey.trim();

        // 1. Verify Business
        Business business = businessRepository.findByBusinessCode(cleanCode)
                .orElseThrow(() -> new IllegalArgumentException("Business code '" + cleanCode + "' was not found. Please verify with your store administrator."));

        if (!"ACTIVE".equalsIgnoreCase(business.getStatus())) {
            if ("PENDING_APPROVAL".equalsIgnoreCase(business.getStatus())) {
                throw new IllegalStateException("Business registration is pending approval by the Super Admin. Please wait for activation.");
            }
            throw new IllegalStateException("Business '" + business.getName() + "' is currently " + business.getStatus() + ". Please contact support.");
        }

        // 2. Verify Terminal Key
        PosTerminal terminal = terminalRepository.findByPairingKey(cleanKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Pairing Key. Please check the key in the Web Admin Portal under Cashier Terminals."));

        if (!terminal.getBusiness().getId().equals(business.getId())) {
            throw new IllegalArgumentException("Pairing Key does not match the specified Business Code.");
        }

        if (!"ACTIVE".equalsIgnoreCase(terminal.getStatus())) {
            throw new IllegalStateException("This terminal key has been " + terminal.getStatus() + ".");
        }

        // 3. Register Device UUID and timestamp
        String deviceUuid = TerminalConfigManager.getDeviceUuid();
        terminal.setDeviceUuid(deviceUuid);
        terminal.setLastActiveAt(LocalDateTime.now());
        PosTerminal updatedTerminal = terminalRepository.save(terminal);

        // 4. Save local hardware config
        TerminalConfigManager.saveConfig(business, updatedTerminal);

        return updatedTerminal;
    }

    @Transactional(readOnly = true)
    public boolean isTerminalValid() {
        if (!TerminalConfigManager.isPaired()) {
            return false;
        }

        Long businessId = TerminalConfigManager.getBusinessId();
        Long terminalId = TerminalConfigManager.getTerminalId();

        if (businessId == null || terminalId == null) {
            System.err.println(">>> Terminal configuration missing businessId or terminalId. Unpairing...");
            TerminalConfigManager.unpair();
            return false;
        }

        // 1. Verify business exists and is ACTIVE
        var bizOpt = businessRepository.findById(businessId);
        if (bizOpt.isEmpty()) {
            System.err.println(">>> Business #" + businessId + " no longer exists in DB. Clearing cached terminal...");
            TerminalConfigManager.unpair();
            return false;
        }

        Business business = bizOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(business.getStatus())) {
            System.err.println(">>> Business #" + businessId + " is " + business.getStatus() + ". Clearing cached terminal...");
            TerminalConfigManager.unpair();
            return false;
        }

        // 2. Verify terminal exists and is ACTIVE
        var termOpt = terminalRepository.findById(terminalId);
        if (termOpt.isEmpty()) {
            System.err.println(">>> Terminal #" + terminalId + " no longer exists in DB. Clearing cached terminal...");
            TerminalConfigManager.unpair();
            return false;
        }

        PosTerminal terminal = termOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(terminal.getStatus())) {
            System.err.println(">>> Terminal #" + terminalId + " status is " + terminal.getStatus() + ". Clearing cached terminal...");
            TerminalConfigManager.unpair();
            return false;
        }

        // 3. Verify terminal belongs to the business
        if (terminal.getBusiness() == null || !terminal.getBusiness().getId().equals(businessId)) {
            System.err.println(">>> Terminal #" + terminalId + " does not belong to business #" + businessId + ". Clearing cached terminal...");
            TerminalConfigManager.unpair();
            return false;
        }

        return true;
    }
}
