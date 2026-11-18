package com.ganzithon.pulse.repository;

import com.ganzithon.pulse.entity.ActivityRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<ActivityRecord, Long> {
    List<ActivityRecord> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
