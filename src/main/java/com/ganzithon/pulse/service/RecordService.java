package com.ganzithon.pulse.service;

import com.ganzithon.pulse.dto.record.RecordRequest;
import com.ganzithon.pulse.dto.record.RecordResponse;
import com.ganzithon.pulse.entity.ActivityRecord;
import com.ganzithon.pulse.repository.RecordRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordService {
    /** 모드(색/사물/현실 미션)가 확정되면 요청값으로 받도록 바꿀 자리 */
    static final String DEFAULT_ACTIVITY = "숨은그림찾기";

    private final RecordRepository records;

    public RecordService(RecordRepository records) {
        this.records = records;
    }

    @Transactional
    public RecordResponse save(Long userId, RecordRequest r) {
        ActivityRecord saved = records.save(new ActivityRecord(userId, DEFAULT_ACTIVITY,
                r.pictureId(), r.alertId(), r.beforeScore(), r.afterScore()));
        return RecordResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> list(Long userId) {
        return records.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(RecordResponse::from).toList();
    }
}
