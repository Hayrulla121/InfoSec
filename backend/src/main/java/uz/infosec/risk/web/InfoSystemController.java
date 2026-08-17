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
import uz.infosec.risk.service.InfoSystemService;
import uz.infosec.risk.web.dto.RegistryDtos.InfoSystemRequest;
import uz.infosec.risk.web.dto.RegistryDtos.InfoSystemResponse;

/** Перечень инфосистем Банка. */
@RestController
@RequestMapping("/api/info-systems")
public class InfoSystemController {

    private final InfoSystemService infoSystemService;

    public InfoSystemController(InfoSystemService infoSystemService) {
        this.infoSystemService = infoSystemService;
    }

    @GetMapping
    @RequireModulePermission(module = AppModule.INFO_SYSTEMS, action = Action.READ)
    public Page<InfoSystemResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return infoSystemService.search(search, pageable);
    }

    @GetMapping("/{id}")
    @RequireModulePermission(module = AppModule.INFO_SYSTEMS, action = Action.READ)
    public InfoSystemResponse get(@PathVariable Long id) {
        return infoSystemService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireModulePermission(module = AppModule.INFO_SYSTEMS, action = Action.CREATE)
    public InfoSystemResponse create(@Valid @RequestBody InfoSystemRequest request) {
        return infoSystemService.create(request);
    }

    @PutMapping("/{id}")
    @RequireModulePermission(module = AppModule.INFO_SYSTEMS, action = Action.UPDATE)
    public InfoSystemResponse update(@PathVariable Long id,
                                     @Valid @RequestBody InfoSystemRequest request) {
        return infoSystemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireModulePermission(module = AppModule.INFO_SYSTEMS, action = Action.DELETE)
    public void delete(@PathVariable Long id) {
        infoSystemService.delete(id);
    }
}
