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
import uz.infosec.risk.service.AssetService;
import uz.infosec.risk.web.dto.RegistryDtos.AssetRequest;
import uz.infosec.risk.web.dto.RegistryDtos.AssetResponse;

/**
 * Реестр ключевых ИА.
 *
 * <p>Every registry controller has this exact shape: five methods, each
 * annotated with the permission it needs, each delegating to a service. No
 * business logic here - that is what keeps the rules testable in isolation.
 *
 * <p>Spring resolves the Pageable from ?page=&size=&sort= automatically.
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    @RequireModulePermission(module = AppModule.ASSETS, action = Action.READ)
    public Page<AssetResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return assetService.search(search, pageable);
    }

    @GetMapping("/{id}")
    @RequireModulePermission(module = AppModule.ASSETS, action = Action.READ)
    public AssetResponse get(@PathVariable Long id) {
        return assetService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireModulePermission(module = AppModule.ASSETS, action = Action.CREATE)
    public AssetResponse create(@Valid @RequestBody AssetRequest request) {
        return assetService.create(request);
    }

    @PutMapping("/{id}")
    @RequireModulePermission(module = AppModule.ASSETS, action = Action.UPDATE)
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return assetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireModulePermission(module = AppModule.ASSETS, action = Action.DELETE)
    public void delete(@PathVariable Long id) {
        assetService.delete(id);
    }
}
