package com.ganzithon.pulse.repository;

import com.ganzithon.pulse.entity.ActivityRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<ActivityRecord, Long> {
    List<ActivityRecord> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    Optional<ActivityRecord> findByIdAndUserId(Long id, Long userId);
}
