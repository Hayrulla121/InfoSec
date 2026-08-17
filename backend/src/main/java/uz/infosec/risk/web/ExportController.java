package uz.infosec.risk.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.infosec.risk.domain.Action;
import uz.infosec.risk.domain.AppModule;
import uz.infosec.risk.security.RequireModulePermission;
import uz.infosec.risk.service.CsvExportService;
import uz.infosec.risk.service.excel.ExcelWorkbookExportService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * CSV downloads, one per registry.
 *
 * <p>Each endpoint requires READ on its own module: an export is a bulk read,
 * so it must not be an easier route to data than the table it mirrors.
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final CsvExportService csvExportService;
    private final ExcelWorkbookExportService workbookExportService;

    public ExportController(CsvExportService csvExportService,
                            ExcelWorkbookExportService workbookExportService) {
        this.csvExportService = csvExportService;
        this.workbookExportService = workbookExportService;
    }

    /**
     * The whole workbook: all eight sheets with live formulas, the same shape
     * as the file this platform replaced.
     *
     * <p>Guarded by RISKS read because it contains the entire register. Anything
     * the Excel layout could not represent (a risk with more than seven
     * implemented controls) comes back in the X-Export-Warnings header rather
     * than being silently dropped.
     */
    @GetMapping("/workbook")
    @RequireModulePermission(module = AppModule.RISKS, action = Action.READ)
    public ResponseEntity<byte[]> workbook() {
        ExcelWorkbookExportService.WorkbookExport result = workbookExportService.export();

        String filename = "Качественная оценка рисков ИТ и ИБ - %s.xlsx".formatted(LocalDate.now());
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        if (!result.warnings().isEmpty()) {
            // Header values must be ISO-8859-1, so the Russian text is encoded.
            response.header("X-Export-Warnings",
                    URLEncoder.encode(String.join(" | ", result.warnings()), StandardCharsets.UTF_8));
            response.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Export-Warnings");
        }
        return response.body(result.bytes());
    }

    /**
     * RFC 5987: a plain filename="..." header can only carry Latin-1, which
     * would mangle the Cyrillic name. filename* carries the UTF-8 version, and
     * the ASCII fallback keeps older clients working.
     */
    private String contentDisposition(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"risk-assessment.xlsx\"; filename*=UTF-8''" + encoded;
    }

    @GetMapping("/assets")
    @RequireModulePermission(module = AppModule.ASSETS, action = Action.READ)
    public ResponseEntity<byte[]> assets() {
        return csv(csvExportService.exportAssets(), "assets");
    }

    @GetMapping("/threats")
    @RequireModulePermission(module = AppModule.THREATS, action = Action.READ)
    public ResponseEntity<byte[]> threats() {
        return csv(csvExportService.exportThreats(), "threats");
    }

    @GetMapping("/controls")
    @RequireModulePermission(module = AppModule.CONTROLS, action = Action.READ)
    public ResponseEntity<byte[]> controls() {
        return csv(csvExportService.exportControls(), "controls");
    }

    @GetMapping("/risks")
    @RequireModulePermission(module = AppModule.RISKS, action = Action.READ)
    public ResponseEntity<byte[]> risks() {
        return csv(csvExportService.exportRisks(), "risks");
    }

    private ResponseEntity<byte[]> csv(String body, String name) {
        String filename = "%s-%s.csv".formatted(name, LocalDate.now());
        return ResponseEntity.ok()
                // "attachment" makes the browser download rather than render it.
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body.getBytes(StandardCharsets.UTF_8));
    }
}
