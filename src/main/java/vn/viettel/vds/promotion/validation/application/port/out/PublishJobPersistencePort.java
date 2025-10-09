package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.PublishJob;
import java.util.List;
import java.util.Optional;

public interface PublishJobPersistencePort {
    PublishJob save(PublishJob job);
    Optional<PublishJob> findById(String id);
    boolean existsByRuleIdAndTargetVersion(String ruleId, Integer targetVersion);
    List<PublishJob> findByRuleIdOrderByTargetVersionDesc(String ruleId);
    void delete(PublishJob job);
}
