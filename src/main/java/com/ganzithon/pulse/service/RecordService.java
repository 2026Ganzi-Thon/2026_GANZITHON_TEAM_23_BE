package com.ganzithon.pulse.service;

import com.ganzithon.pulse.dto.record.NextActionRequest;
import com.ganzithon.pulse.dto.record.RecordRequest;
import com.ganzithon.pulse.dto.record.RecordResponse;
import com.ganzithon.pulse.entity.ActivityRecord;
import com.ganzithon.pulse.global.exception.BusinessException;
import com.ganzithon.pulse.global.exception.ErrorCode;
import com.ganzithon.pulse.repository.RecordRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordService {
    /** 팀 결정(명세 8.1 1번)으로 활동 이름은 고정이고, 모드는 요청의 mode 필드로 받는다 */
    static final String DEFAULT_ACTIVITY = "숨은그림찾기";

    private final RecordRepository records;

    public RecordService(RecordRepository records) {
        this.records = records;
    }

    @Transactional
    public RecordResponse save(Long userId, RecordRequest r) {
        ActivityRecord saved = records.save(new ActivityRecord(userId, DEFAULT_ACTIVITY, r.mode(),
                r.pictureId(), r.alertId(), r.beforeScore(), r.afterScore()));
        return RecordResponse.from(saved);
    }

    @Transactional
    public void recordNextAction(Long userId, Long recordId, NextActionRequest request) {
        ActivityRecord record = records.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_001));
        record.recordNextAction(request.action());
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> list(Long userId) {
        return records.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(RecordResponse::from).toList();
    }
}
