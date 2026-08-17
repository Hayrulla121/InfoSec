package uz.infosec.risk.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.infosec.risk.service.DemoDataService;
import uz.infosec.risk.service.DemoDataService.DemoDataSummary;

/**
 * Seeds a demonstration dataset.
 *
 * <p>ADMIN only, and guarded by role rather than the module permission grid:
 * this writes across every registry at once, so it is not something a
 * per-module grant should be able to unlock.
 */
@RestController
@RequestMapping("/api/admin/demo-data")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDemoDataController {

    private final DemoDataService demoDataService;

    public AdminDemoDataController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    /** 409 when the database already holds data - see DemoDataService.seed(). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemoDataSummary seed() {
        return demoDataService.seed();
    }
}
