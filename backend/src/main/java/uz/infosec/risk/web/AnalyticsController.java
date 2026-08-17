package uz.infosec.risk.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.infosec.risk.service.AnalyticsService;
import uz.infosec.risk.web.dto.AnalyticsDtos.DashboardResponse;
import uz.infosec.risk.web.dto.AnalyticsDtos.RiskMatrixResponse;

/**
 * Read-only aggregates.
 *
 * <p>Per the spec these are readable by any authenticated user and carry no
 * @RequireModulePermission: they are management reporting views, and they
 * expose counts rather than record detail.
 */
@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/risk-matrix")
    public RiskMatrixResponse riskMatrix() {
        return analyticsService.riskMatrix();
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard() {
        return analyticsService.dashboard();
    }
}
