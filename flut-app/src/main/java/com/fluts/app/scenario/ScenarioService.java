package com.fluts.app.scenario;

import com.fluts.app.security.ScenarioScope;
import com.fluts.domain.FlutTradingOptimizer;
import com.fluts.domain.Scenario;
import com.fluts.persistence.ScenarioEntity;
import com.fluts.persistence.ScenarioInputType;
import com.fluts.persistence.ScenarioRepository;
import com.fluts.persistence.ScenarioSpecifications;
import java.time.Clock;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores scenarios and reads them back with their result. Results are computed on every read and
 * never stored, so they can never be stale.
 */
@Service
@Transactional
public class ScenarioService {

    private final ScenarioRepository repository;
    private final FlutTradingOptimizer optimizer;
    private final Clock clock;

    public ScenarioService(final ScenarioRepository repository, final FlutTradingOptimizer optimizer,
            final Clock clock) {
        this.repository = repository;
        this.optimizer = optimizer;
        this.clock = clock;
    }

    /** Creates a scenario of the merchant, sent as JSON through the scenario API. */
    public ScenarioView create(final String merchantCode, final Scenario scenario) {
        return toView(repository.save(new ScenarioEntity(merchantCode, scenario, ScenarioInputType.JSON, clock.instant())));
    }

    /** Saves evaluated or imported scenarios of the merchant. */
    public List<Long> saveAll(final String merchantCode, final List<Scenario> scenarios, final ScenarioInputType inputType) {
        final List<ScenarioEntity> entities = scenarios.stream()
                .map(scenario -> new ScenarioEntity(merchantCode, scenario, inputType, clock.instant()))
                .toList();
        return repository.saveAll(entities).stream().map(ScenarioEntity::getId).toList();
    }

    /**
     * @param scope        the merchant whose scenarios are listed, or all merchants
     * @param merchantCode a further filter, only meaningful for a scope of all merchants
     */
    @Transactional(readOnly = true)
    public Page<ScenarioView> list(final ScenarioScope scope, final @Nullable String merchantCode,
            final @Nullable ScenarioInputType inputType, final @Nullable String nameContains, final Pageable pageable) {
        final String merchant = scope.merchant().orElse(merchantCode);
        return repository.findAll(ScenarioSpecifications.matching(merchant, inputType, nameContains), pageable)
                .map(this::toView);
    }

    @Transactional(readOnly = true)
    public ScenarioView get(final ScenarioScope scope, final long id) {
        return toView(find(scope, id));
    }

    /** Replaces name and schuurs; merchant, input type and creation time stay. */
    public ScenarioView replace(final ScenarioScope scope, final long id, final Scenario scenario) {
        final ScenarioEntity entity = find(scope, id);
        entity.replaceWith(scenario, clock.instant());
        return toView(repository.saveAndFlush(entity));
    }

    public void delete(final ScenarioScope scope, final long id) {
        repository.delete(find(scope, id));
    }

    /** Outside the scope a scenario does not exist: another merchant's id gives the same 404 as a missing one. */
    private ScenarioEntity find(final ScenarioScope scope, final long id) {
        return scope.merchant()
                .map(merchant -> repository.findByIdAndMerchantCode(id, merchant))
                .orElseGet(() -> repository.findById(id))
                .orElseThrow(() -> new ScenarioNotFoundException(id));
    }

    private ScenarioView toView(final ScenarioEntity entity) {
        final Scenario scenario = entity.toDomain();
        return new ScenarioView(entity.getId(), entity.getMerchantCode(), scenario, entity.getInputType(), entity.getCreatedAt(),
                entity.getUpdatedAt(), optimizer.optimize(scenario),
                scenario.schuurs().stream().map(optimizer::optimizeSchuur).toList());
    }
}
