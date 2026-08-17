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
import uz.infosec.risk.service.ThreatService;
import uz.infosec.risk.web.dto.RegistryDtos.ThreatRequest;
import uz.infosec.risk.web.dto.RegistryDtos.ThreatResponse;

/** Реестр угроз. */
@RestController
@RequestMapping("/api/threats")
public class ThreatController {

    private final ThreatService threatService;

    public ThreatController(ThreatService threatService) {
        this.threatService = threatService;
    }

    @GetMapping
    @RequireModulePermission(module = AppModule.THREATS, action = Action.READ)
    public Page<ThreatResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return threatService.search(search, pageable);
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
