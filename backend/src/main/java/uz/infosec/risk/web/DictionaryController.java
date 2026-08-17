package uz.infosec.risk.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import uz.infosec.risk.domain.Action;
import uz.infosec.risk.domain.AppModule;
import uz.infosec.risk.security.RequireModulePermission;
import uz.infosec.risk.service.DictionaryService;
import uz.infosec.risk.web.dto.DictionaryDtos.DictionaryGroupDto;
import uz.infosec.risk.web.dto.DictionaryDtos.UpdateDictionaryRequest;

import java.util.List;

/**
 * Техническая страница.
 *
 * <p>Reading is left open to any authenticated user on purpose: these values
 * populate dropdowns on every other screen, so gating them behind a DICTIONARIES
 * read grant would break forms for users who legitimately edit assets or risks.
 * Writing is guarded.
 */
@RestController
@RequestMapping("/api/dictionaries")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public List<DictionaryGroupDto> getAll() {
        return dictionaryService.findAllGrouped();
    }

    @PutMapping
    @RequireModulePermission(module = AppModule.DICTIONARIES, action = Action.UPDATE)
    public DictionaryGroupDto update(@Valid @RequestBody UpdateDictionaryRequest request) {
        return dictionaryService.replace(request.dictType(), request.items());
    }
}
