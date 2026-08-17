package uz.infosec.risk.service.excel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingRule;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the exported workbook is not just well-formed but <b>correct</b>:
 * its formulas are evaluated with POI's engine and compared against the values
 * our Java engine computed.
 *
 * <p>That is the real requirement. A file that opens but recalculates to
 * different numbers than the platform would be worse than no export at all -
 * two sources of truth disagreeing in a bank's risk report.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExcelWorkbookExportTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    ExcelWorkbookExportService exportService;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = login("admin", "admin");
    }

    private String login(String u, String p) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(u, p)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private JsonNode create(String url, String body) throws Exception {
        return json.readTree(mvc.perform(post(url)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    /** Builds the golden scenario and returns the exported workbook. */
    private XSSFWorkbook buildAndExport(String tag) throws Exception {
        long assetId = create("/api/assets", """
                {"name":"Актив %s","criticality":"Критичная"}""".formatted(tag)).get("id").asLong();
        long threatId = create("/api/threats", """
                {"description":"Угроза %s","discoverability":2,"repeatability":2,
                 "exploitability":4,"affectedUsers":3,"damage":2}""".formatted(tag)).get("id").asLong();
        long riskId = create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"Риск %s","owner":"Служба ИБ",
                 "treatmentMethod":"Снижение","measureStatus":"Задержка"}"""
                .formatted(assetId, threatId, tag)).get("id").asLong();

        attach(riskId, control("Внедрённый 20%% " + tag, "0.20"), "IMPLEMENTED");
        attach(riskId, control("Плановый 20%% " + tag, "0.20"), "PLANNED");
        attach(riskId, control("Плановый 50%% " + tag, "0.50"), "PLANNED");

        byte[] bytes = exportService.export().bytes();
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private long control(String name, String pct) throws Exception {
        return create("/api/controls", """
                {"name":"%s","treatmentMethod":"Снижение","reductionPct":%s,"implemented":false}"""
                .formatted(name, pct)).get("id").asLong();
    }

    private void attach(long riskId, long controlId, String type) throws Exception {
        mvc.perform(post("/api/risks/" + riskId + "/controls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"controlId":%d,"type":"%s"}""".formatted(controlId, type)))
                .andExpect(status().isOk());
    }

    /** Locates the row on Реестр рисков whose column A holds the given code. */
    private int rowOf(Sheet sheet, String code) {
        for (Row row : sheet) {
            Cell c = row.getCell(0);
            if (c != null && c.getCellType() == CellType.STRING
                    && code.equals(c.getStringCellValue())) {
                return row.getRowNum();
            }
        }
        throw new AssertionError("No row with code " + code);
    }

    private Cell at(Sheet sheet, int rowIdx, String col) {
        return sheet.getRow(rowIdx).getCell(CellReference.convertColStringToIndex(col));
    }

    @Test
    void workbookHasTheSameEightSheetsInTheSameOrder() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("sheets")) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(8);
            assertThat(wb.getSheetName(0)).isEqualTo("Ma'lumot - Tahdidlar modeli");
            assertThat(wb.getSheetName(1)).isEqualTo("Реестр ключевых ИА");
            assertThat(wb.getSheetName(2)).isEqualTo("Реестр угроз");
            assertThat(wb.getSheetName(3)).isEqualTo("Матрица рисков");
            assertThat(wb.getSheetName(4)).isEqualTo("Реестр рисков");
            assertThat(wb.getSheetName(5)).isEqualTo("Риск-контроль");
            assertThat(wb.getSheetName(6)).isEqualTo("Техническая страница");
            assertThat(wb.getSheetName(7)).isEqualTo("Перечень инфосистем Банка");
            // Hidden in the source file, hidden here.
            assertThat(wb.isSheetHidden(7)).isTrue();
        }
    }

    @Test
    void riskSheetKeepsTheOriginalSeventyFiveColumnHeaders() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("cols")) {
            Sheet sh = wb.getSheet("Реестр рисков");
            Row h = sh.getRow(0);

            assertThat(h.getCell(0).getStringCellValue()).isEqualTo("ID Риска");
            assertThat(h.getCell(7).getStringCellValue()).isEqualTo("Снижающие контроли");
            assertThat(h.getCell(8).getStringCellValue()).isEqualTo("Уровень риска");
            assertThat(h.getCell(12).getStringCellValue()).isEqualTo("Остаточный риск");
            // Helper block, same letters as the source workbook.
            assertThat(at(sh, 0, "AF").getStringCellValue()).isEqualTo("Рейтинг актива");
            assertThat(at(sh, 0, "AH").getStringCellValue()).isEqualTo("Счет угрозы");
            assertThat(at(sh, 0, "AW").getStringCellValue()).isEqualTo("Счет угрозы после снижения 7");
            assertThat(at(sh, 0, "BG").getStringCellValue()).isEqualTo("Счет угрозы после обработки 5");
            assertThat(at(sh, 0, "BU").getStringCellValue()).isEqualTo("Остаточный риск");
            assertThat(at(sh, 0, "BW").getStringCellValue()).isEqualTo("Риск после контролей");
        }
    }

    private List<String> merges(Sheet sheet) {
        return sheet.getMergedRegions().stream().map(CellRangeAddress::formatAsString).toList();
    }

    private String filterRef(Sheet sheet) {
        var filter = ((XSSFSheet) sheet).getCTWorksheet().getAutoFilter();
        return filter == null ? null : filter.getRef();
    }

    /**
     * The furniture a reader actually sees: merged headings, the columns the
     * source keeps out of sight, the frozen header row and the filters.
     *
     * <p>None of it changes a number, which is exactly why it needs a test -
     * a layout regression is invisible to every other assertion here, and the
     * whole point of the export is that it looks like the file people know.
     */
    @Test
    void sheetFurnitureMatchesTheSourceWorkbook() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("layout")) {
            Sheet model = wb.getSheet("Ma'lumot - Tahdidlar modeli");
            assertThat(merges(model)).containsExactlyInAnyOrder(
                    "B2:C2", "D2:F2", "D3:F3", "D4:F4", "D5:F5", "D6:F6", "D7:F7", "B10:E11");

            Sheet matrix = wb.getSheet("Матрица рисков");
            assertThat(merges(matrix)).containsExactlyInAnyOrder(
                    "C1:G1", "A2:A6", "I2:S6", "T2:AB6", "C8:G8",
                    "I10:P10", "I11:J11", "L11:M11", "O11:P11");

            Sheet tech = wb.getSheet("Техническая страница");
            assertThat(merges(tech)).containsExactlyInAnyOrder("A1:B1", "C1:D1");

            // The two control blocks are banner headings spanning their slots.
            Sheet risks = wb.getSheet("Реестр рисков");
            assertThat(merges(risks)).containsExactlyInAnyOrder("R1:X1", "Z1:AD1");

            // Hidden in the source: the value is exported, just not shown.
            assertThat(risks.isColumnHidden(colOf("G"))).as("Индикаторы риска").isTrue();
            assertThat(wb.getSheet("Реестр ключевых ИА").isColumnHidden(colOf("F")))
                    .as("Класс защищенности").isTrue();
            assertThat(wb.getSheet("Риск-контроль").isColumnHidden(colOf("E")))
                    .as("Описание контроля").isTrue();

            // Only the two long registers freeze, and only their header row -
            // freezing column A as well would hide the ID behind the pane.
            for (String name : new String[]{"Реестр угроз", "Реестр рисков"}) {
                PaneInformation pane = wb.getSheet(name).getPaneInformation();
                assertThat(pane).as("%s freezes its header", name).isNotNull();
                assertThat(pane.getHorizontalSplitPosition()).isEqualTo((short) 1);
                assertThat(pane.getVerticalSplitPosition()).isEqualTo((short) 0);
            }
            for (String name : new String[]{"Реестр ключевых ИА", "Риск-контроль",
                    "Перечень инфосистем Банка"}) {
                assertThat(wb.getSheet(name).getPaneInformation())
                        .as("%s is unfrozen in the source", name).isNull();
            }

            // Filters cover the reporting columns only, never the helper block,
            // and are sized to the rows actually written.
            int rows = risks.getLastRowNum() + 1;
            assertThat(filterRef(risks)).isEqualTo("A1:P" + rows);
            assertThat(filterRef(wb.getSheet("Реестр ключевых ИА"))).startsWith("A1:F");
            assertThat(filterRef(wb.getSheet("Перечень инфосистем Банка"))).startsWith("A1:L");
            assertThat(filterRef(wb.getSheet("Риск-контроль")))
                    .as("unfiltered in the source").isNull();
        }
    }

    private int colOf(String col) {
        return CellReference.convertColStringToIndex(col);
    }

    private String fillOf(ConditionalFormatting cf, int ruleIndex) {
        XSSFColor colour = (XSSFColor) ((XSSFConditionalFormattingRule) cf.getRule(ruleIndex))
                .getPatternFormatting().getFillBackgroundColorColor();
        return colour.getARGBHex();
    }

    /**
     * The risk and threat ladders keep the colours the source paints them in.
     *
     * <p>A risk register that is not colour-coded is read wrong: «Критический»
     * has to be visible at a glance in a printout, not found by reading.
     */
    @Test
    void riskAndThreatLaddersKeepTheirColours() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("colours")) {
            SheetConditionalFormatting risks =
                    wb.getSheet("Реестр рисков").getSheetConditionalFormatting();
            assertThat(risks.getNumConditionalFormattings()).isEqualTo(1);

            ConditionalFormatting levels = risks.getConditionalFormattingAt(0);
            // One rule set over both «Уровень риска» and «Остаточный риск».
            assertThat(levels.getFormattingRanges()).hasSize(2);
            assertThat(levels.getFormattingRanges()[0].formatAsString()).startsWith("I2");
            assertThat(levels.getFormattingRanges()[1].formatAsString()).startsWith("M2");

            assertThat(levels.getNumberOfRules()).isEqualTo(5);
            assertThat(levels.getRule(4).getFormula1()).isEqualTo("\"Критический\"");
            assertThat(fillOf(levels, 4)).isEqualTo("FFC00000");
            assertThat(fillOf(levels, 0)).as("Незначительный").isEqualTo("FF00B0F0");

            SheetConditionalFormatting threats =
                    wb.getSheet("Реестр угроз").getSheetConditionalFormatting();
            // Out-of-range DREAD inputs, then the threat-level ladder.
            assertThat(threats.getNumConditionalFormattings()).isEqualTo(2);
            ConditionalFormatting outOfRange = threats.getConditionalFormattingAt(0);
            assertThat(outOfRange.getRule(0).getComparisonOperation())
                    .isEqualTo(ComparisonOperator.LT);
            assertThat(outOfRange.getRule(1).getComparisonOperation())
                    .isEqualTo(ComparisonOperator.GT);
            assertThat(fillOf(outOfRange, 0)).isEqualTo("FFFF0000");
            assertThat(threats.getConditionalFormattingAt(1).getNumberOfRules()).isEqualTo(5);
        }
    }

    @Test
    void computedCellsAreFormulasNotFrozenValues() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("formulas")) {
            Sheet risks = wb.getSheet("Реестр рисков");
            int r = rowOf(risks, lastRiskCode(risks));

            for (String col : new String[]{"C", "E", "H", "I", "L", "M",
                    "AF", "AG", "AH", "AI", "AQ", "AW", "BC", "BG", "BH", "BU", "BV", "BW"}) {
                assertThat(at(risks, r, col).getCellType())
                        .as("column %s must stay a live formula", col)
                        .isEqualTo(CellType.FORMULA);
            }

            Sheet threats = wb.getSheet("Реестр угроз");
            assertThat(at(threats, 1, "Q").getCellFormula()).isEqualTo("SUM(L2:P2)");
            assertThat(at(threats, 1, "R").getCellFormula()).contains("IF(Q2<6,1,");
        }
    }

    private String lastRiskCode(Sheet risks) {
        String code = null;
        for (Row row : risks) {
            Cell c = row.getCell(0);
            if (c != null && c.getCellType() == CellType.STRING
                    && c.getStringCellValue().startsWith("R")
                    && !"ID Риска".equals(c.getStringCellValue())) {
                code = c.getStringCellValue();
            }
        }
        return code;
    }

    /**
     * THE test: recalculate the exported workbook and check it agrees with the
     * platform. 13 -> 10.4 after the implemented control, -> 4.16 after the two
     * planned ones, giving Средний now and Низкий residual on a rating-5 asset.
     */
    @Test
    void formulasRecalculateToTheSameNumbersAsTheEngine() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("recalc")) {
            Sheet risks = wb.getSheet("Реестр рисков");
            int r = rowOf(risks, lastRiskCode(risks));
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();

            // Asset rating comes back through the VLOOKUP into Реестр ключевых ИА,
            // which itself VLOOKUPs the Техническая страница. Two hops.
            assertThat(ev.evaluate(at(risks, r, "AF")).getNumberValue()).isEqualTo(5.0);

            // Threat score via VLOOKUP into Реестр угроз column Q (=SUM(L:P)).
            assertThat(ev.evaluate(at(risks, r, "AH")).getNumberValue()).isEqualTo(13.0);

            // The implemented chain: 13 x 0.8
            assertThat(ev.evaluate(at(risks, r, "AW")).getNumberValue()).isEqualTo(10.4);
            assertThat(ev.evaluate(at(risks, r, "BV")).getNumberValue()).isEqualTo(2.0);

            // The planned chain continues from 10.4: x0.8 -> 8.32, x0.5 -> 4.16
            assertThat(ev.evaluate(at(risks, r, "BG")).getNumberValue()).isEqualTo(4.16);
            assertThat(ev.evaluate(at(risks, r, "BH")).getNumberValue()).isEqualTo(1.0);

            // And the classification the two ratings imply, on a=5.
            assertThat(ev.evaluate(at(risks, r, "BW")).getStringValue()).isEqualTo("Средний");
            assertThat(ev.evaluate(at(risks, r, "BU")).getStringValue()).isEqualTo("Низкий");
            // The visible columns mirror the helpers.
            assertThat(ev.evaluate(at(risks, r, "I")).getStringValue()).isEqualTo("Средний");
            assertThat(ev.evaluate(at(risks, r, "M")).getStringValue()).isEqualTo("Низкий");
        }
    }

    /** The control names and percentages must resolve through Риск-контроль. */
    @Test
    void controlLookupsResolveAcrossSheets() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("lookup")) {
            Sheet risks = wb.getSheet("Реестр рисков");
            int r = rowOf(risks, lastRiskCode(risks));
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();

            // First implemented control: 20% reduction, and its name.
            assertThat(ev.evaluate(at(risks, r, "AJ")).getNumberValue()).isEqualTo(0.20);
            assertThat(ev.evaluate(at(risks, r, "BI")).getStringValue()).contains("Внедрённый 20%");

            // First two planned controls: 20% and 50%.
            assertThat(ev.evaluate(at(risks, r, "AX")).getNumberValue()).isEqualTo(0.20);
            assertThat(ev.evaluate(at(risks, r, "AY")).getNumberValue()).isEqualTo(0.50);

            // Unused slots resolve to 0, which leaves the chain unchanged.
            assertThat(ev.evaluate(at(risks, r, "AK")).getNumberValue()).isEqualTo(0.0);
        }
    }

    @Test
    void threatSheetRecalculatesDreadFromScratch() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("dread")) {
            Sheet sh = wb.getSheet("Реестр угроз");
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();

            int r = -1;
            for (Row row : sh) {
                Cell c = row.getCell(1);
                if (c != null && c.getCellType() == CellType.STRING
                        && c.getStringCellValue().startsWith("Угроза dread")) {
                    r = row.getRowNum();
                }
            }
            assertThat(r).isPositive();

            assertThat(ev.evaluate(at(sh, r, "Q")).getNumberValue()).isEqualTo(13.0);
            assertThat(ev.evaluate(at(sh, r, "R")).getNumberValue()).isEqualTo(3.0);
            assertThat(ev.evaluate(at(sh, r, "H")).getStringValue()).isEqualTo("Средний");
        }
    }

    @Test
    void dictionarySheetFeedsTheAssetRatingLookup() throws Exception {
        try (XSSFWorkbook wb = buildAndExport("dict")) {
            Sheet tech = wb.getSheet("Техническая страница");
            assertThat(tech.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Очень низкая");
            assertThat(tech.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(1.0);
            assertThat(tech.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Критичная");
            assertThat(tech.getRow(5).getCell(1).getNumericCellValue()).isEqualTo(5.0);

            Sheet assets = wb.getSheet("Реестр ключевых ИА");
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();
            int r = -1;
            for (Row row : assets) {
                Cell c = row.getCell(1);
                if (c != null && c.getCellType() == CellType.STRING
                        && c.getStringCellValue().startsWith("Актив dict")) {
                    r = row.getRowNum();
                }
            }
            assertThat(r).isPositive();
            assertThat(ev.evaluate(at(assets, r, "H")).getNumberValue()).isEqualTo(5.0);
        }
    }

    /**
     * Functions newer than Excel 2007 must be stored in the XML with an
     * {@code _xlfn.} prefix. Without it the workbook still opens and every
     * other test still passes - but Excel treats TEXTJOIN as an unknown
     * user-defined function and the column shows #NAME?.
     *
     * <p>This asserts on the raw sheet XML, because that is exactly the bytes
     * Excel parses. Reading the formula back through POI would hide the bug.
     */
    @Test
    void newerFunctionsCarryTheXlfnPrefixInTheStoredXml() throws Exception {
        buildAndExport("xlfn");
        byte[] bytes = exportService.export().bytes();

        String risksSheetXml = null;
        try (java.util.zip.ZipInputStream zip =
                     new java.util.zip.ZipInputStream(new ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("xl/worksheets/sheet5.xml")) {
                    risksSheetXml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        assertThat(risksSheetXml).as("Реестр рисков sheet XML").isNotNull();
        assertThat(risksSheetXml).contains("_xlfn.TEXTJOIN");
        // No bare occurrence anywhere: every TEXTJOIN must be prefixed.
        assertThat(risksSheetXml.split("TEXTJOIN", -1).length - 1)
                .as("every TEXTJOIN occurrence is prefixed")
                .isEqualTo(risksSheetXml.split("_xlfn\\.TEXTJOIN", -1).length - 1);
    }

    @Test
    void endpointReturnsAnXlsxAttachment() throws Exception {
        buildAndExport("http");

        var response = mvc.perform(get("/api/export/workbook")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition"))
                .contains("attachment").contains("filename*=UTF-8''");

        byte[] body = response.getContentAsByteArray();
        // "PK" - every .xlsx is a zip archive.
        assertThat(body[0]).isEqualTo((byte) 'P');
        assertThat(body[1]).isEqualTo((byte) 'K');
        assertThat(body.length).isGreaterThan(5000);
    }

    @Test
    void exportRequiresRiskReadPermission() throws Exception {
        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"wb-blocked","password":"secret123",
                                 "fullName":"Workbook Blocked","role":"USER"}"""))
                .andExpect(status().isCreated());

        String usersJson = mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        long id = -1;
        for (JsonNode u : json.readTree(usersJson)) {
            if ("wb-blocked".equals(u.get("username").asText())) {
                id = u.get("id").asLong();
            }
        }

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/users/" + id + "/permissions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions":[{"module":"RISKS","canCreate":false,"canRead":false,
                                  "canUpdate":false,"canDelete":false}]}"""))
                .andExpect(status().isOk());

        String token = login("wb-blocked", "secret123");
        mvc.perform(get("/api/export/workbook").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
