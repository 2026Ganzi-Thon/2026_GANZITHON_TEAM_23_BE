package com.ganzithon.pulse.service;

import com.ganzithon.pulse.dto.alert.SpikeRequest;
import com.ganzithon.pulse.dto.alert.SpikeResponse;
import com.ganzithon.pulse.entity.Alert;
import com.ganzithon.pulse.entity.User;
import com.ganzithon.pulse.global.exception.BusinessException;
import com.ganzithon.pulse.global.exception.ErrorCode;
import com.ganzithon.pulse.global.util.TimeFormat;
import com.ganzithon.pulse.repository.AlertRepository;
import com.ganzithon.pulse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
    private final AlertRepository alerts;
    private final UserRepository users;
    private final int dismissStep;
    private final int maxThreshold;

    public AlertService(AlertRepository alerts, UserRepository users,
                        @Value("${pulse.alert.dismiss-step}") int dismissStep,
                        @Value("${pulse.alert.max-threshold}") int maxThreshold) {
        this.alerts = alerts;
        this.users = users;
        this.dismissStep = dismissStep;
        this.maxThreshold = maxThreshold;
    }

    /**
     * 알림 여부는 서버가 결정: 걸음이 없고, 평소보다 사용자 기준값 이상 올랐을 때만 알린다.
     * 알린 경우에만 alerts에 저장한다.
     */
    @Transactional
    public SpikeResponse report(Long userId, SpikeRequest r) {
        User user = users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_003));
        boolean resting = r.steps() == null || r.steps() == 0;
        boolean spike = r.bpm() - r.baselineBpm() >= user.getAlertThreshold();
        if (!resting || !spike) {
            return new SpikeResponse(false, null);
        }
        Alert alert = alerts.saveAndFlush(new Alert(userId, r.bpm(), r.baselineBpm(), r.steps(),
                r.measuredAt().toInstant()));
        alert.assignAlertId(String.format("alert_%s_%04d",
                TimeFormat.yyyymmdd(alert.getCreatedAt()), alert.getId()));
        return new SpikeResponse(true, alert.getAlertId());
    }

    /** "괜찮아요": 같은 알림이 덜 뜨도록 기준값을 올린다. 이미 거절한 알림은 다시 올리지 않는다. */
    @Transactional
    public void dismiss(Long userId, String alertId) {
        Alert alert = alerts.findByAlertIdAndUserId(alertId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALERT_001));
        if (alert.getStatus() == Alert.Status.DISMISSED) return;
        alert.dismiss();
        users.findById(userId).ifPresent(u -> u.raiseThreshold(dismissStep, maxThreshold));
    }
}
