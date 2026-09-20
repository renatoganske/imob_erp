package com.imobcrm.notification.infra;

import com.imobcrm.notification.domain.EmailOutbox;
import com.imobcrm.notification.domain.EmailOutboxRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaEmailOutboxRepository extends JpaRepository<EmailOutbox, UUID>, EmailOutboxRepository {

    @Override
    @Query(value = """
            SELECT * FROM email_outbox
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EmailOutbox> lockDue(@Param("now") OffsetDateTime now, @Param("limit") int limit);
}
