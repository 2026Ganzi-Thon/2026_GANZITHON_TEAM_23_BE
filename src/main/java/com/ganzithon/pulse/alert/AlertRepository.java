package com.ganzithon.pulse.alert;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findByAlertIdAndUserId(String alertId, Long userId);
}
