package com.devstack.pos.repository;

import com.devstack.pos.entity.PosTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosTerminalRepository extends JpaRepository<PosTerminal, Long> {
    Optional<PosTerminal> findByPairingKey(String pairingKey);
    Optional<PosTerminal> findByBusinessIdAndTerminalCode(Long businessId, String terminalCode);
    List<PosTerminal> findByBusinessId(Long businessId);
    boolean existsByPairingKey(String pairingKey);
    long countByBusinessId(Long businessId);
}
