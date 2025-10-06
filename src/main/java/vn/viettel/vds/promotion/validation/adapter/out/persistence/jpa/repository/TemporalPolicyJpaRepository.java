package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TemporalPolicyEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ConditionalOnPromixJpa
@Repository
@Primary
public interface TemporalPolicyJpaRepository extends JpaRepository<TemporalPolicyEntity, String> {

    /**
     * Find temporal policy by name.
     */
    Optional<TemporalPolicyEntity> findByName(String name);

    /**
     * Find temporal policies by timezone.
     */
    List<TemporalPolicyEntity> findByTz(String tz);

    /**
     * Find active temporal policies at a specific time.
     */
    @Query("SELECT tp FROM TemporalPolicyEntity tp WHERE " +
            "(tp.startTs IS NULL OR tp.startTs <= :checkTime) AND " +
            "(tp.endTs IS NULL OR tp.endTs >= :checkTime)")
    List<TemporalPolicyEntity> findActivePoliciesAt(@Param("checkTime") Instant checkTime);

    /**
     * Find policies with RRULE.
     */
    List<TemporalPolicyEntity> findByRruleIsNotNull();
}