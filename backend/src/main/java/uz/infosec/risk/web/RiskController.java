package uz.infosec.risk.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uz.infosec.risk.domain.Action;
import uz.infosec.risk.domain.AppModule;
import uz.infosec.risk.security.RequireModulePermission;
import uz.infosec.risk.service.RiskService;
import uz.infosec.risk.web.dto.RiskDtos.*;

import java.util.List;

/** Реестр рисков + attaching controls. */
@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    /**
     * assetRating / threatRating power the risk-matrix drill-down: clicking a
     * cell links here with both set, and gets exactly that cell's risks.
     */
    @GetMapping
    @RequireModulePermission(module = AppModule.RISKS, action = Action.READ)
    public Page<RiskResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer assetRating,
            @RequestParam(required = false) Integer threatRating,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return riskService.search(search, assetRating, threatRating, pageable);
    }

    @GetMapping("/{id}")
    @RequireModulePermission(module = AppModule.RISKS, action = Action.READ)
    public RiskResponse get(@PathVariable Long id) {
        return riskService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireModulePermission(module = AppModule.RISKS, action = Action.CREATE)
    public RiskResponse create(@Valid @RequestBody RiskRequest request) {
        return riskService.create(request);
    }

    @PutMapping("/{id}")
    @RequireModulePermission(module = AppModule.RISKS, action = Action.UPDATE)
    public RiskResponse update(@PathVariable Long id, @Valid @RequestBody RiskRequest request) {
        return riskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireModulePermission(module = AppModule.RISKS, action = Action.DELETE)
    public void delete(@PathVariable Long id) {
        riskService.delete(id);
    }

    // --------------------------------------------------- attached controls

    @GetMapping("/{id}/controls")
    @RequireModulePermission(module = AppModule.RISKS, action = Action.READ)
    public List<RiskControlDto> listControls(@PathVariable Long id) {
        return riskService.listControls(id);
    }

    /**
     * Guarded by RISK_CONTROLS, not RISKS: attaching a mitigation is a distinct
     * responsibility, so an admin can let someone manage controls on risks they
     * are not allowed to edit otherwise.
     *
     * <p>Returns the whole risk so the UI sees the recalculated levels
     * immediately, without a second round trip.
     */
    @PostMapping("/{id}/controls")
    @RequireModulePermission(module = AppModule.RISK_CONTROLS, action = Action.CREATE)
    public RiskResponse attachControl(@PathVariable Long id,
                                      @Valid @RequestBody AttachControlRequest request) {
        return riskService.attachControl(id, request);
    }

    @DeleteMapping("/{id}/controls/{linkId}")
    @RequireModulePermission(module = AppModule.RISK_CONTROLS, action = Action.DELETE)
    public RiskResponse detachControl(@PathVariable Long id, @PathVariable Long linkId) {
        return riskService.detachControl(id, linkId);
    }
}
