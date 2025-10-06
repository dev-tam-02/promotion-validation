package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorRegistryEntity;

import java.util.List;
import java.util.Optional;

@ConditionalOnPromixJpa
@Repository
public interface OperatorRegistryRepository extends JpaRepository<OperatorRegistryEntity, String> {

    /**
     * Find operator registry by context.
     */
    Optional<OperatorRegistryEntity> findByContext(String context);

    /**
     * Find operator registries by compiler ID.
     */
    List<OperatorRegistryEntity> findByCompilerId(String compilerId);

    /**
     * Check if context exists.
     */
    boolean existsByContext(String context);
}