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
import uz.infosec.risk.service.FacetService;
import uz.infosec.risk.web.dto.FacetDtos.FacetValue;
import uz.infosec.risk.service.ControlService;
import uz.infosec.risk.web.dto.RegistryDtos.ControlRequest;
import uz.infosec.risk.web.dto.RegistryDtos.ControlResponse;

import java.util.List;
import java.util.Map;

/** Риск-контроль (catalog). */
@RestController
@RequestMapping("/api/controls")
public class ControlController {

    private final ControlService controlService;
    private final FacetService facetService;

    public ControlController(ControlService controlService, FacetService facetService) {
        this.controlService = controlService;
        this.facetService = facetService;
    }

    @GetMapping
    @RequireModulePermission(module = AppModule.CONTROLS, action = Action.READ)
    public Page<ControlResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String treatmentMethod,
            @RequestParam(required = false) Boolean implemented,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return controlService.search(search, treatmentMethod, implemented, pageable);
    }

    /** Filter options for this registry, each with how many rows carry it. */
    @GetMapping("/facets")
    @RequireModulePermission(module = AppModule.CONTROLS, action = Action.READ)
    public Map<String, List<FacetValue>> facets() {
        return facetService.facets("controls");
    }


    @GetMapping("/{id}")
    @RequireModulePermission(module = AppModule.CONTROLS, action = Action.READ)
    public ControlResponse get(@PathVariable Long id) {
        return controlService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireModulePermission(module = AppModule.CONTROLS, action = Action.CREATE)
    public ControlResponse create(@Valid @RequestBody ControlRequest request) {
        return controlService.create(request);
    }

    @PutMapping("/{id}")
    @RequireModulePermission(module = AppModule.CONTROLS, action = Action.UPDATE)
    public ControlResponse update(@PathVariable Long id, @Valid @RequestBody ControlRequest request) {
        return controlService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireModulePermission(module = AppModule.CONTROLS, action = Action.DELETE)
    public void delete(@PathVariable Long id) {
        controlService.delete(id);
    }
}
