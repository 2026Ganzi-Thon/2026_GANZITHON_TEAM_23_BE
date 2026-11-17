package com.ganzithon.pulse.record;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<ActivityRecord, Long> {
    List<ActivityRecord> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
