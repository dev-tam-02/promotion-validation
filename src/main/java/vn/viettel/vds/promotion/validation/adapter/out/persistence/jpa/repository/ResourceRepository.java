package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ResourceEntity;

import java.util.List;
import java.util.Optional;

@ConditionalOnPromixJpa
@Repository
public interface ResourceRepository extends JpaRepository<ResourceEntity, String> {

    /**
     * Find resource by label.
     */
    Optional<ResourceEntity> findByLabel(String label);

    /**
     * Find resources by method.
     */
    List<ResourceEntity> findByMethod(String method);

    /**
     * Find resources by auth method.
     */
    List<ResourceEntity> findByAuthMethod(String authMethod);

    /**
     * Find resources by endpoint containing.
     */
    List<ResourceEntity> findByEndpointContaining(String endpoint);
}