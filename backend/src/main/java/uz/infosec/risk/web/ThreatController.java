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
import uz.infosec.risk.service.ThreatService;
import uz.infosec.risk.web.dto.RegistryDtos.ThreatRequest;
import uz.infosec.risk.web.dto.RegistryDtos.ThreatResponse;

import java.util.List;
import java.util.Map;

/** Реестр угроз. */
@RestController
@RequestMapping("/api/threats")
public class ThreatController {

    private final ThreatService threatService;
    private final FacetService facetService;

    public ThreatController(ThreatService threatService, FacetService facetService) {
        this.threatService = threatService;
        this.facetService = facetService;
    }

    @GetMapping
    @RequireModulePermission(module = AppModule.THREATS, action = Action.READ)
    public Page<ThreatResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String levelLabel,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return threatService.search(search, levelLabel, pageable);
    }

    /** Filter options for this registry, each with how many rows carry it. */
    @GetMapping("/facets")
    @RequireModulePermission(module = AppModule.THREATS, action = Action.READ)
    public Map<String, List<FacetValue>> facets() {
        return facetService.facets("threats");
    }


    @GetMapping("/{id}")
    @RequireModulePermission(module = AppModule.THREATS, action = Action.READ)
    public ThreatResponse get(@PathVariable Long id) {
        return threatService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireModulePermission(module = AppModule.THREATS, action = Action.CREATE)
    public ThreatResponse create(@Valid @RequestBody ThreatRequest request) {
        return threatService.create(request);
    }

    @PutMapping("/{id}")
    @RequireModulePermission(module = AppModule.THREATS, action = Action.UPDATE)
    public ThreatResponse update(@PathVariable Long id, @Valid @RequestBody ThreatRequest request) {
        return threatService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireModulePermission(module = AppModule.THREATS, action = Action.DELETE)
    public void delete(@PathVariable Long id) {
        threatService.delete(id);
    }
}
