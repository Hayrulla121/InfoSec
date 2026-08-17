package uz.infosec.risk.service.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingRule;
import org.apache.poi.xssf.usermodel.XSSFFontFormatting;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.*;
import uz.infosec.risk.repository.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static uz.infosec.risk.service.excel.SheetLayout.*;

/**
 * Rebuilds the original Excel workbook from the database, sheet for sheet and
 * formula for formula.
 *
 * <p><b>Why live formulas rather than plain values.</b> The department still
 * reports to management in Excel, and a workbook of frozen numbers cannot be
 * audited - a reviewer cannot click a cell and see how a risk level was
 * reached. Writing the real formulas keeps that audit trail, and lets the file
 * behave exactly like the one people already know.
 *
 * <p><b>The trade-off this creates.</b> An exported workbook recalculates on
 * its own. Edit a DREAD score in Excel and that copy will disagree with the
 * platform. The export is a <i>report</i>, not a second system of record; the
 * database remains authoritative.
 *
 * <p><b>Ranges are sized to the data.</b> The source file hard-codes lookup
 * ranges ($A$2:$R$196 and friends), which silently break once a registry grows
 * past them. Here every range is computed from the actual row count, so that
 * failure mode is gone.
 */
@Service
public class ExcelWorkbookExportService {

    /**
     * The generated file plus anything the Excel format could not represent.
     * Warnings are surfaced to the user rather than silently swallowed.
     */
    public record WorkbookExport(byte[] bytes, List<String> warnings) {
    }

    /**
     * TEXTJOIN, spelled the way the xlsx format requires.
     *
     * <p>Functions added after Excel 2007 must be stored with an {@code _xlfn.}
     * prefix. Written as a bare {@code TEXTJOIN(...)} the file still opens, but
     * Excel treats the name as an unknown user-defined function and every cell
     * shows #NAME?. The source workbook stores {@code _xlfn.TEXTJOIN} for the
     * same reason.
     */
    private static final String TEXTJOIN = "_xlfn.TEXTJOIN(\", \", TRUE, %s:%s)";

    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;
    private final ControlRepository controlRepository;
    private final RiskRepository riskRepository;
    private final InfoSystemRepository infoSystemRepository;
    private final DictionaryItemRepository dictionaryRepository;

    public ExcelWorkbookExportService(AssetRepository assetRepository,
                                      ThreatRepository threatRepository,
                                      ControlRepository controlRepository,
                                      RiskRepository riskRepository,
                                      InfoSystemRepository infoSystemRepository,
                                      DictionaryItemRepository dictionaryRepository) {
        this.assetRepository = assetRepository;
        this.threatRepository = threatRepository;
        this.controlRepository = controlRepository;
        this.riskRepository = riskRepository;
        this.infoSystemRepository = infoSystemRepository;
        this.dictionaryRepository = dictionaryRepository;
    }

    @Transactional(readOnly = true)
    public WorkbookExport export() {
        List<String> warnings = new ArrayList<>();

        List<Asset> assets = sorted(assetRepository.findAll(), Asset::getCode, Asset::getId);
        List<Threat> threats = sorted(threatRepository.findAll(), Threat::getCode, Threat::getId);
        List<Control> controls = sorted(controlRepository.findAll(), Control::getCode, Control::getId);
        List<Risk> risks = sorted(riskRepository.findAll(), Risk::getCode, Risk::getId);
        List<InfoSystem> systems = sorted(infoSystemRepository.findAll(),
                InfoSystem::getCode, InfoSystem::getId);

        // The Риск-контроль sheet is one row per (risk, control) link, exactly
        // as in the source file.
        List<RiskControl> links = new ArrayList<>();
        for (Risk risk : risks) {
            List<RiskControl> ordered = new ArrayList<>(risk.getControls());
            ordered.sort(Comparator.comparingInt(RiskControl::getApplyOrder)
                    .thenComparing(RiskControl::getId));
            links.addAll(ordered);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            WorkbookStyles st = new WorkbookStyles(wb);

            // Sheet order matches the source workbook.
            writeThreatModel(wb, st);
            writeAssets(wb, st, assets);
            writeThreats(wb, st, threats);
            writeMatrix(wb, st, risks.size());
            writeRisks(wb, st, risks, assets.size(), threats.size(), links.size(), warnings);
            writeControls(wb, st, links, controls, risks.size());
            writeTech(wb, st);
            writeInfoSystems(wb, st, systems);

            // Tells Excel to recompute every formula the moment the file opens.
            // Without it the cells show blank, because POI writes formulas but
            // never their cached results.
            wb.setForceFormulaRecalculation(true);

            wb.write(out);
            return new WorkbookExport(out.toByteArray(), warnings);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build the Excel workbook", e);
        }
    }

    // =====================================================================
    // 1. Ma'lumot - Tahdidlar modeli
    // =====================================================================

    private void writeThreatModel(XSSFWorkbook wb, WorkbookStyles st) {
        XSSFSheet sh = wb.createSheet(S_THREAT_MODEL);

        set(sh, 1, "B", "DREAD tahdidlarni baholash modeli", st.title);
        set(sh, 1, "D", "Izoh", st.header);
        set(sh, 1, "G", "0 ball", st.header);
        set(sh, 1, "H", "5 ball", st.header);
        set(sh, 1, "I", "Tavsiyalar", st.header);

        String[][] criteria = {
                {"Discoverability", "Aniqlash imkoniyati",
                        "Hujum qiluvchi tomonidan himoyadagi zaiflikni aniqlashning osonlik darajasi. "
                                + "Qasddan bo‘lmagan harakatlarda esa, subyekt tahdidni yuzaga keltiruvchi "
                                + "harakatni bilgan holda yoki tasodifan amalga oshirish ehtimolini ifodalaydi.",
                        "Murakkab", "Oson",
                        "Agar tahdid tabiiy omillar ta’sirida yuzaga kelsa, mazkur mezon bo‘yicha 0 ball qo‘yiladi."},
                {"Repeatability", "Takrorlash imkoniyati",
                        "Hujum qiluvchi yoki subyektning tahdidni amalga oshirishga olib keladigan "
                                + "harakatni takroran bajarish imkoniyatining osonlik darajasi.",
                        "Murakkab", "Oson",
                        "Agar tahdid tabiiy omillar ta’sirida yuzaga kelsa, mazkur mezon bo‘yicha ball "
                                + "hodisaning yuzaga kelish ehtimoli yoki sodir bo‘lish chastotasidan kelib chiqib belgilanadi."},
                {"Exploitability", "Ekspluatatsiya qilish (zaiflikdan foydalanish)",
                        "Mazkur tahdid bilan bog‘liq himoya jarayonlari va tizimlaridagi zaifliklardan "
                                + "foydalanish imkoniyatining osonlik darajasi.",
                        "Murakkab", "Oson",
                        "Agar tahdid tabiiy omillar bilan bog‘liq bo‘lsa, ushbu mezon bo‘yicha 5 ball qo‘yiladi."},
                {"Affected users", "Ta’sir ko‘radigan foydalanuvchilar",
                        "Mazkur tahdid amalga oshirilganda bevosita yoki bilvosita ta’sirga uchraydigan "
                                + "xodimlar, mijozlar va fuqarolar soni.",
                        "Kamroq", "Ko‘proq", "-"},
                {"Damage", "Zarar darajasi",
                        "Mazkur tahdid amalga oshirilishi natijasida yuzaga kelishi kutilayotgan moddiy, "
                                + "moliyaviy yoki boshqa nomoddiy zarar.",
                        "Kamroq", "Ko‘proq", "-"},
        };

        // The title spans B:C and every Izoh cell spans D:F, as in the source.
        merge(sh, "B2:C2");
        merge(sh, "D2:F2");

        int r = 2;
        for (String[] c : criteria) {
            set(sh, r, "B", c[0], st.text);
            set(sh, r, "C", c[1], st.wrapped);
            set(sh, r, "D", c[2], st.wrapped);
            set(sh, r, "G", c[3], st.centered);
            set(sh, r, "H", c[4], st.centered);
            set(sh, r, "I", c[5], st.wrapped);
            sh.getRow(r).setHeightInPoints(58);
            merge(sh, "D%d:F%d".formatted(r + 1, r + 1));
            r++;
        }

        set(sh, 9, "B",
                "Har bir ko‘rsatkich 0 dan 5 ballgacha bo‘lgan qiymatga ega bo‘lishi mumkin. "
                        + "Tahdidning yakuniy darajasini aniqlash uchun barcha ko‘rsatkichlar bo‘yicha "
                        + "ballar yig‘indisi hisoblanadi va tahdid darajasi quyidagi jadvalga muvofiq belgilanadi.",
                st.note);
        merge(sh, "B10:E11");
        sh.getRow(9).setHeightInPoints(34);

        set(sh, 12, "B", "Ko‘rsatkichlar yig‘indisi", st.header);
        set(sh, 12, "C", "Tahdid darajasi", st.header);
        String[][] levels = {{"0~5", "Ahamiyatsiz"}, {"6~10", "Past"}, {"11~15", "O‘rta"},
                {"16~20", "Yuqori"}, {"21~25", "Juda yuqori"}};
        r = 13;
        for (String[] lv : levels) {
            set(sh, r, "B", lv[0], st.centered);
            set(sh, r, "C", lv[1], st.text);
            r++;
        }

        width(sh, "B", 22);
        width(sh, "C", 34);
        width(sh, "D", 70);
        width(sh, "G", 11);
        width(sh, "H", 11);
        width(sh, "I", 52);
    }

    // =====================================================================
    // 2. Реестр ключевых ИА
    // =====================================================================

    private void writeAssets(XSSFWorkbook wb, WorkbookStyles st, List<Asset> assets) {
        XSSFSheet sh = wb.createSheet(S_ASSETS);

        header(sh, st, 0, new String[][]{
                {"A", "ID"}, {"B", "Axborot tizimining nomi"}, {"C", "Axborot tizimining ko‘lami"},
                {"D", "Qayta ishlanadigan axborot toifasi"}, {"E", "Axborot tizimining muhimlik darajasi"},
                {"F", "Класс защищенности информационной системы"}, {"H", "Reyting"}});

        int r = 1;
        for (Asset a : assets) {
            set(sh, r, "A", a.getCode(), st.text);
            set(sh, r, "B", a.getName(), st.wrapped);
            set(sh, r, "C", a.getScope(), st.wrapped);
            set(sh, r, "D", a.getInfoCategory(), st.wrapped);
            set(sh, r, "E", a.getCriticality(), st.text);
            set(sh, r, "F", a.getSecurityClass(), st.centered);
            // The VLOOKUP the source file uses, pointed at the dictionary sheet.
            formula(sh, r, "H",
                    "IF(E%d=\"\",\"\",VLOOKUP(E%d,'%s'!$A$2:$B$6,2,FALSE))"
                            .formatted(r + 1, r + 1, S_TECH), st.number);
            r++;
        }

        width(sh, "A", 10);
        width(sh, "B", 46);
        width(sh, "C", 26);
        width(sh, "D", 30);
        width(sh, "E", 24);
        width(sh, "F", 20);
        width(sh, "H", 10);
        // Класс защищенности is hidden in the source workbook - the value is
        // kept, just not shown - and the filter covers A:F, as it does there.
        hide(sh, "F");
        autoFilter(sh, "A", "F", r);
    }

    // =====================================================================
    // 3. Реестр угроз
    // =====================================================================

    private void writeThreats(XSSFWorkbook wb, WorkbookStyles st, List<Threat> threats) {
        XSSFSheet sh = wb.createSheet(S_THREATS);

        header(sh, st, 0, new String[][]{
                {"A", "#"}, {"B", "Угрозы"}, {"C", "Обнаружение"}, {"D", "Повторение"},
                {"E", "Эксплуатирование"}, {"F", "Масштаб"}, {"G", "Ущерб"}, {"H", "Уровень угрозы"},
                {"L", "О"}, {"M", "П"}, {"N", "Э"}, {"O", "М"}, {"P", "У"},
                {"Q", "Сумм"}, {"R", "Счет"}});

        int r = 1;
        for (Threat t : threats) {
            int x = r + 1; // 1-based Excel row
            set(sh, r, "A", t.getCode(), st.text);
            set(sh, r, "B", t.getDescription(), st.wrapped);
            set(sh, r, "C", t.getDiscoverability(), st.number);
            set(sh, r, "D", t.getRepeatability(), st.number);
            set(sh, r, "E", t.getExploitability(), st.number);
            set(sh, r, "F", t.getAffectedUsers(), st.number);
            set(sh, r, "G", t.getDamage(), st.number);

            formula(sh, r, "H",
                    "IF(R%d=1,\"Незначительный\",IF(R%d=2,\"Низкий\",IF(R%d=3,\"Средний\","
                            .formatted(x, x, x)
                            + "IF(R%d=4,\"Высокий\",IF(R%d=5,\"Очень высокий\",\"\")))))".formatted(x, x),
                    st.text);

            // Helper columns L..P clamp each criterion to 0..5, exactly as the
            // source does, then Q sums and R maps to a rating.
            String[] src = {"C", "D", "E", "F", "G"};
            String[] dst = {"L", "M", "N", "O", "P"};
            for (int i = 0; i < 5; i++) {
                formula(sh, r, dst[i],
                        "IF(%s%d=\"\",0,IF(%s%d<6,%s%d,5))".formatted(src[i], x, src[i], x, src[i], x),
                        st.number);
            }
            formula(sh, r, "Q", "SUM(L%d:P%d)".formatted(x, x), st.number);
            formula(sh, r, "R",
                    "IF(Q%d<6,1,IF(Q%d<11,2,IF(Q%d<16,3,IF(Q%d<21,4,5))))".formatted(x, x, x, x),
                    st.number);
            r++;
        }

        width(sh, "A", 8);
        width(sh, "B", 66);
        for (String c : new String[]{"C", "D", "E", "F", "G"}) {
            width(sh, c, 14);
        }
        width(sh, "H", 18);
        sh.createFreezePane(0, 1);
        applyThreatFormatting(sh, r);
    }

    /**
     * The two conditional-format blocks the source sheet carries: out-of-range
     * DREAD inputs painted red, and the threat level painted on its own ladder.
     */
    private void applyThreatFormatting(XSSFSheet sh, int lastRow) {
        XSSFSheetConditionalFormatting cf = sh.getSheetConditionalFormatting();
        int end = Math.max(lastRow, 2);

        // A criterion outside 0..5 is a data-entry error. The helper columns
        // clamp it before it reaches a score, so without this the mistake would
        // be silently absorbed rather than shown.
        cf.addConditionalFormatting(
                new CellRangeAddress[]{CellRangeAddress.valueOf("C2:G" + end)},
                new XSSFConditionalFormattingRule[]{
                        threshold(cf, ComparisonOperator.LT, "0"),
                        threshold(cf, ComparisonOperator.GT, "5")});

        cf.addConditionalFormatting(
                new CellRangeAddress[]{CellRangeAddress.valueOf("H2:H" + end)},
                new XSSFConditionalFormattingRule[]{
                        paint(cf, "Очень высокий", rgb(CLR_CRITICAL), true),
                        paint(cf, "Высокий", rgb(CLR_HIGH), false),
                        paint(cf, "Средний", rgb(CLR_THREAT_MEDIUM), false),
                        paint(cf, "Низкий", theme(THREAT_LOW_THEME, THREAT_LOW_TINT), false),
                        paint(cf, "Незначительный", rgb(CLR_THREAT_NEGLIGIBLE), true)});
    }

    // =====================================================================
    // 4. Матрица рисков
    // =====================================================================

    private void writeMatrix(XSSFWorkbook wb, WorkbookStyles st, int riskCount) {
        XSSFSheet sh = wb.createSheet(S_MATRIX);

        set(sh, 0, "C", "Har bir toifadagi xavflar soni", st.title);
        merge(sh, "C1:G1");
        set(sh, 1, "A", "Критичность актива", st.centered);
        merge(sh, "A2:A6");

        String[] cols = {"C", "D", "E", "F", "G"};
        // Rows descend 5..1; row 2 holds asset rating 5.
        for (int i = 0; i < 5; i++) {
            int r = 1 + i;
            int assetRating = 5 - i;
            set(sh, r, "B", assetRating, st.centered);
            for (int j = 0; j < 5; j++) {
                String countif = ("COUNTIFS('%s'!$%s:$%s,$B%d,'%s'!$%s:$%s,%s$7)")
                        .formatted(S_RISKS, C_ASSET_RATING, C_ASSET_RATING, r + 1,
                                S_RISKS, C_RATING_AFTER_CTRL, C_RATING_AFTER_CTRL, cols[j]);
                // Blank instead of 0 for an empty cell, as the source does.
                formula(sh, r, cols[j], "IF(%s=0,\"\",%s)".formatted(countif, countif), st.centered);
            }
        }
        for (int j = 0; j < 5; j++) {
            set(sh, 6, cols[j], j + 1, st.centered);
        }
        set(sh, 7, "C", "Tahdid darajasi", st.centered);
        merge(sh, "C8:G8");

        set(sh, 1, "I", """
                Xavflarni sifat jihatidan baholash tamoyillari

                1. Agar aktivning muhimlik darajasi yuqori yoki undan yuqori bo‘lsa va tahdid darajasi ham \
                yuqori bo‘lsa, yoxud aksincha, tahdid darajasi yuqori yoki undan yuqori bo‘lsa hamda \
                aktivning muhimlik darajasi yuqori bo‘lsa, xavf har doim kritik deb baholanadi;

                2. Muhimlik darajasi yuqori bo‘lgan aktivga nisbatan xavf ahamiyatsiz darajada bo‘lishi mumkin emas;

                3. Juda yuqori darajadagi tahdid natijasida yuzaga keladigan xavf o‘rta darajadan past bo‘lishi mumkin emas;

                4. Ahamiyatsiz darajadagi tahdid past darajadan yuqori bo‘lgan xavfni keltirib chiqarmaydi;

                5. Tahdidning o‘rta darajasi va aktivning o‘rta muhimlik darajasi o‘rta darajadagi xavfni yuzaga keltiradi.""",
                st.note);
        merge(sh, "I2:S6");

        set(sh, 9, "I", "Легенда", st.title);
        merge(sh, "I10:P10");
        set(sh, 10, "I", "Aktivning muhimligi", st.header);
        merge(sh, "I11:J11");
        set(sh, 10, "L", "Tahdid darajasi", st.header);
        merge(sh, "L11:M11");
        set(sh, 10, "O", "Risk darajasi", st.header);
        merge(sh, "O11:P11");

        String[] assetLv = {"Juda past", "Past", "O‘rta", "Yuqori", "Kritik"};
        String[] threatLv = {"Ahamiyatsiz", "Past", "O‘rta", "Yuqori", "Juda yuqori"};
        String[] riskLv = {"Ahamiyatsiz", "Past", "O‘rta", "Yuqori", "Kritik"};
        for (int i = 0; i < 5; i++) {
            set(sh, 11 + i, "I", i + 1, st.centered);
            set(sh, 11 + i, "J", assetLv[i], st.text);
            set(sh, 11 + i, "L", i + 1, st.centered);
            set(sh, 11 + i, "M", threatLv[i], st.text);
            // O stays empty: the source numbers only the two input ladders and
            // leaves the risk column as labels under its merged O:P heading.
            set(sh, 11 + i, "P", riskLv[i], st.text);
        }

        set(sh, 1, "T", """
                Algoritm:

                if ( a*t >= 20 ) r = 5
                else if (( a=1 & t<3) | ( t=1 & a<4 )) r = 1
                else if ( t>2 & a*t>=10 ) r = 4
                else if (( t<4 & t*a>3 & t*a<6) | ( t=3 & a<3 )) r = 2
                else r = 3

                Bu yerda: a — aktivning muhimlik darajasi, t — tahdid darajasi, r — xavf darajasi.""",
                st.note);
        merge(sh, "T2:AB6");

        width(sh, "A", 6);
        width(sh, "B", 6);
        for (String c : cols) {
            width(sh, c, 9);
        }
        width(sh, "I", 18);
        width(sh, "J", 16);
        width(sh, "T", 60);
        // Referenced so an empty register is still an obvious zero, not a gap.
        if (riskCount == 0) {
            set(sh, 8, "C", "Реестр рисков пуст", st.note);
        }
    }

    // =====================================================================
    // 5. Реестр рисков - the big one
    // =====================================================================

    private void writeRisks(XSSFWorkbook wb, WorkbookStyles st, List<Risk> risks,
                            int assetCount, int threatCount, int linkCount,
                            List<String> warnings) {
        XSSFSheet sh = wb.createSheet(S_RISKS);

        header(sh, st, 0, new String[][]{
                {"A", "ID Риска"}, {"B", "ID Связанного ИА"}, {"C", "Название ИА"},
                {"D", "ID Связанной угрозы"}, {"E", "Описание угрозы"}, {"F", "Наименование риска"},
                {"G", "Индикаторы риска"}, {"H", "Снижающие контроли"}, {"I", "Уровень риска"},
                {"J", "Владелец риска"}, {"K", "Метод управления риском"},
                {"L", "Запланированные мероприятия"}, {"M", "Остаточный риск"},
                {"N", "Статус мероприятий"}, {"O", "Финальная дата внедрения мероприятий"},
                {"P", "Комментарий"},
                {"R", "Внедренные контроли"}, {"Z", "Запланированные контроли"},
                {"AF", "Рейтинг актива"}, {"AG", "Рейтинг угрозы"}, {"AH", "Счет угрозы"},
                {"AI", "Риск"},
                {"AJ", "Снижение 1"}, {"AK", "Снижение 2"}, {"AL", "Снижение 3"}, {"AM", "Снижение 4"},
                {"AN", "Снижение 5"}, {"AO", "Снижение 6"}, {"AP", "Снижение 7"},
                {"AQ", "Счет угрозы после снижения 1"}, {"AR", "Счет угрозы после снижения 2"},
                {"AS", "Счет угрозы после снижения 3"}, {"AT", "Счет угрозы после снижения 4"},
                {"AU", "Счет угрозы после снижения 5"}, {"AV", "Счет угрозы после снижения 6"},
                {"AW", "Счет угрозы после снижения 7"},
                {"AX", "План 1"}, {"AY", "План 2"}, {"AZ", "План 3"}, {"BA", "План 4"}, {"BB", "План 5"},
                {"BC", "Счет угрозы после обработки 1"}, {"BD", "Счет угрозы после обработки 2"},
                {"BE", "Счет угрозы после обработки 3"}, {"BF", "Счет угрозы после обработки 4"},
                {"BG", "Счет угрозы после обработки 5"},
                {"BH", "Рейтинг угрозы после обработки"},
                {"BI", "Название 1"}, {"BJ", "Название 2"}, {"BK", "Название 3"}, {"BL", "Название 4"},
                {"BM", "Название 5"}, {"BN", "Название 6"}, {"BO", "Название 7"},
                {"BP", "Название план 1"}, {"BQ", "Название план 2"}, {"BR", "Название план 3"},
                {"BS", "Название план 4"}, {"BT", "Название план 5"},
                {"BU", "Остаточный риск"}, {"BV", "Рейтинг угрозы после контролей"},
                {"BW", "Риск после контролей"}});

        // The two control blocks are banner headings spanning their slots.
        merge(sh, "R1:X1");
        merge(sh, "Z1:AD1");

        // Lookup ranges sized to real data, with a little headroom for manual
        // edits - never the source file's hard-coded 196/300/454.
        int assetsEnd = Math.max(assetCount + 1, 2);
        int threatsEnd = Math.max(threatCount + 1, 2);
        int linksEnd = Math.max(linkCount + 1, 2);

        int r = 1;
        for (Risk risk : risks) {
            int x = r + 1;

            set(sh, r, "A", risk.getCode(), st.text);
            set(sh, r, "B", risk.getAsset().getCode(), st.text);
            formula(sh, r, "C", "IF(B%d=\"\",\"\",VLOOKUP(B%d,'%s'!$A$2:$B$%d,2,FALSE))"
                    .formatted(x, x, S_ASSETS, assetsEnd), st.wrapped);
            set(sh, r, "D", risk.getThreat().getCode(), st.text);
            formula(sh, r, "E", "IF(D%d=\"\",\"\",VLOOKUP(D%d,'%s'!$A$2:$B$%d,2,FALSE))"
                    .formatted(x, x, S_THREATS, threatsEnd), st.wrapped);
            set(sh, r, "F", risk.getName(), st.wrapped);
            set(sh, r, "G", risk.getIndicators(), st.wrapped);

            formula(sh, r, "H", TEXTJOIN.formatted("BI" + x, "BO" + x), st.wrapped);
            formula(sh, r, "I", "IF(BW%d=\"\",\"\",BW%d)".formatted(x, x), st.text);
            set(sh, r, "J", risk.getOwner(), st.text);
            set(sh, r, "K", risk.getTreatmentMethod(), st.text);
            formula(sh, r, "L", TEXTJOIN.formatted("BP" + x, "BT" + x), st.wrapped);
            formula(sh, r, "M", "IF(BU%d=\"\",\"\",BU%d)".formatted(x, x), st.text);
            set(sh, r, "N", risk.getMeasureStatus(), st.text);
            if (risk.getImplementationDeadline() != null) {
                Cell c = cell(sh, r, "O");
                c.setCellValue(java.sql.Date.valueOf(risk.getImplementationDeadline()));
                c.setCellStyle(st.date);
            }
            set(sh, r, "P", risk.getComment(), st.wrapped);

            // Control IDs are written as VALUES. The source derives them with a
            // dynamic-array FILTER over the Риск-контроль sheet; we already hold
            // the authoritative links, and a literal is both simpler and immune
            // to the #SPILL! errors a dynamic array hits when a neighbouring
            // cell is occupied.
            List<RiskControl> impl = ofType(risk, ControlType.IMPLEMENTED);
            List<RiskControl> plan = ofType(risk, ControlType.PLANNED);

            if (impl.size() > MAX_IMPLEMENTED) {
                warnings.add("%s: %d внедрённых контролей, в Excel помещается %d"
                        .formatted(risk.getCode(), impl.size(), MAX_IMPLEMENTED));
            }
            if (plan.size() > MAX_PLANNED) {
                warnings.add("%s: %d запланированных контролей, в Excel помещается %d"
                        .formatted(risk.getCode(), plan.size(), MAX_PLANNED));
            }

            writeControlBlock(sh, st, r, x, impl, IMPLEMENTED_ID_COLS, REDUCTION_COLS,
                    IMPL_NAME_COLS, linksEnd);
            writeControlBlock(sh, st, r, x, plan, PLANNED_ID_COLS, PLAN_REDUCTION_COLS,
                    PLAN_NAME_COLS, linksEnd);

            formula(sh, r, C_ASSET_RATING, "IF(B%d=\"\",\"\",VLOOKUP(B%d,'%s'!$A$2:$H$%d,8,FALSE))"
                    .formatted(x, x, S_ASSETS, assetsEnd), st.number);
            formula(sh, r, C_THREAT_SCORE, "IF($D%d=\"\",\"\",VLOOKUP($D%d,'%s'!$A$2:$R$%d,17,FALSE))"
                    .formatted(x, x, S_THREATS, threatsEnd), st.number);

            // Rating after the implemented chain (AW), and the risk it implies.
            formula(sh, r, C_THREAT_RATING,
                    ratingFormula("AW" + x, "AH" + x), st.number);
            formula(sh, r, C_RISK,
                    classifyFormula("AF" + x, "AG" + x), st.text);

            // Implemented chain: score = score - score * pct, seven times.
            formula(sh, r, AFTER_IMPL_COLS[0],
                    "IF($AH%d=\"\",\"\",AH%d-AH%d*%s%d)".formatted(x, x, x, REDUCTION_COLS[0], x),
                    st.number);
            for (int i = 1; i < AFTER_IMPL_COLS.length; i++) {
                String prev = AFTER_IMPL_COLS[i - 1];
                formula(sh, r, AFTER_IMPL_COLS[i],
                        "IF($AH%d=\"\",\"\",%s%d-%s%d*%s%d)"
                                .formatted(x, prev, x, prev, x, REDUCTION_COLS[i], x), st.number);
            }

            // Planned chain continues from AW, exactly as the source does.
            formula(sh, r, AFTER_PLAN_COLS[0],
                    "IF($AH%d=\"\",\"\",AW%d-AW%d*%s%d)".formatted(x, x, x, PLAN_REDUCTION_COLS[0], x),
                    st.number);
            for (int i = 1; i < AFTER_PLAN_COLS.length; i++) {
                String prev = AFTER_PLAN_COLS[i - 1];
                formula(sh, r, AFTER_PLAN_COLS[i],
                        "IF($AH%d=\"\",\"\",%s%d-%s%d*%s%d)"
                                .formatted(x, prev, x, prev, x, PLAN_REDUCTION_COLS[i], x), st.number);
            }

            formula(sh, r, C_RATING_AFTER_PLAN, ratingFormula("BG" + x, "AH" + x), st.number);
            formula(sh, r, C_RESIDUAL_RISK, classifyFormula("AF" + x, "BH" + x), st.text);
            formula(sh, r, C_RATING_AFTER_CTRL, ratingFormula("AW" + x, "AH" + x), st.number);
            formula(sh, r, C_RISK_AFTER_CTRL, classifyFormula("AF" + x, "BV" + x), st.text);

            r++;
        }

        width(sh, "A", 9);
        width(sh, "B", 13);
        width(sh, "C", 38);
        width(sh, "D", 13);
        width(sh, "E", 48);
        width(sh, "F", 46);
        width(sh, "G", 30);
        width(sh, "H", 52);
        width(sh, "I", 16);
        width(sh, "J", 24);
        width(sh, "K", 22);
        width(sh, "L", 46);
        width(sh, "M", 17);
        width(sh, "N", 20);
        width(sh, "O", 19);
        width(sh, "P", 38);
        // Индикаторы риска is hidden in the source, and only the reporting
        // columns A:P are filtered - never the helper block.
        hide(sh, C_INDICATORS);
        sh.createFreezePane(0, 1);
        autoFilter(sh, "A", "P", r);
        applyRiskLevelFormatting(sh, r);
    }

    /**
     * Paints «Уровень риска» and «Остаточный риск» on the five-step ladder.
     *
     * <p>Both columns share one rule set in the source workbook, so they are
     * added as a single two-region block rather than twice over.
     */
    private void applyRiskLevelFormatting(XSSFSheet sh, int lastRow) {
        XSSFSheetConditionalFormatting cf = sh.getSheetConditionalFormatting();
        int end = Math.max(lastRow, 2);

        cf.addConditionalFormatting(
                new CellRangeAddress[]{
                        CellRangeAddress.valueOf("%s2:%s%d".formatted(C_RISK_LEVEL, C_RISK_LEVEL, end)),
                        CellRangeAddress.valueOf("%s2:%s%d".formatted(C_RESIDUAL, C_RESIDUAL, end))},
                new XSSFConditionalFormattingRule[]{
                        paint(cf, "Незначительный", rgb(CLR_NEGLIGIBLE), true),
                        paint(cf, "Низкий", rgb(CLR_LOW), false),
                        paint(cf, "Средний", rgb(CLR_MEDIUM), false),
                        paint(cf, "Высокий", rgb(CLR_HIGH), false),
                        paint(cf, "Критический", rgb(CLR_CRITICAL), true)});
    }

    /** Writes one control block: the IDs, their reduction lookups and their names. */
    private void writeControlBlock(XSSFSheet sh, WorkbookStyles st, int r, int x,
                                   List<RiskControl> links, String[] idCols,
                                   String[] reductionCols, String[] nameCols, int linksEnd) {
        for (int i = 0; i < idCols.length; i++) {
            if (i < links.size()) {
                set(sh, r, idCols[i], links.get(i).getControl().getCode(), st.text);
            }
            // The lookups are written for every slot, filled or not: an empty
            // slot resolves to 0 (or ""), which is what keeps the chain valid
            // and lets a user add a control by hand later.
            formula(sh, r, reductionCols[i],
                    "IF(%s%d=\"\",0,VLOOKUP(%s%d,'%s'!$C$2:$G$%d,5,FALSE))"
                            .formatted(idCols[i], x, idCols[i], x, S_CONTROLS, linksEnd), st.percent);
            formula(sh, r, nameCols[i],
                    "IF(%s%d=\"\",\"\",VLOOKUP(%s%d,'%s'!$C$2:$D$%d,2,FALSE))"
                            .formatted(idCols[i], x, idCols[i], x, S_CONTROLS, linksEnd), st.wrapped);
        }
    }

    private List<RiskControl> ofType(Risk risk, ControlType type) {
        return risk.getControls().stream()
                .filter(rc -> rc.getControlType() == type)
                .sorted(Comparator.comparingInt(RiskControl::getApplyOrder)
                        .thenComparing(RiskControl::getId))
                .toList();
    }

    // =====================================================================
    // 6. Риск-контроль
    // =====================================================================

    private void writeControls(XSSFWorkbook wb, WorkbookStyles st, List<RiskControl> links,
                               List<Control> catalog, int riskCount) {
        XSSFSheet sh = wb.createSheet(S_CONTROLS);

        header(sh, st, 0, new String[][]{
                {"A", "ID Риска"}, {"B", "Наименование риска"}, {"C", "ID Контроля"},
                {"D", "Название контроля"}, {"E", "Описание контроля"},
                {"F", "Метод управления риском"}, {"G", "Процент снижения риска"}, {"H", "Внедрен?"}});

        int risksEnd = Math.max(riskCount + 1, 2);
        int r = 1;
        for (RiskControl link : links) {
            int x = r + 1;
            Control c = link.getControl();
            set(sh, r, "A", link.getRisk().getCode(), st.text);
            formula(sh, r, "B", "IF(A%d=\"\",\"\",VLOOKUP(A%d,'%s'!$A$2:$F$%d,6,FALSE))"
                    .formatted(x, x, S_RISKS, risksEnd), st.wrapped);
            set(sh, r, "C", c.getCode(), st.text);
            set(sh, r, "D", c.getName(), st.wrapped);
            set(sh, r, "E", c.getDescription(), st.wrapped);
            set(sh, r, "F", c.getTreatmentMethod(), st.text);
            setNumber(sh, r, "G", c.getReductionPct().doubleValue(), st.percent);
            set(sh, r, "H", link.getControlType() == ControlType.IMPLEMENTED ? "Да" : "Нет",
                    st.centered);
            r++;
        }

        // Catalog entries not yet attached to any risk would otherwise vanish
        // from the export. They are appended without a risk ID so nothing is lost.
        List<Long> used = links.stream().map(l -> l.getControl().getId()).distinct().toList();
        for (Control c : catalog) {
            if (used.contains(c.getId())) {
                continue;
            }
            set(sh, r, "C", c.getCode(), st.text);
            set(sh, r, "D", c.getName(), st.wrapped);
            set(sh, r, "E", c.getDescription(), st.wrapped);
            set(sh, r, "F", c.getTreatmentMethod(), st.text);
            setNumber(sh, r, "G", c.getReductionPct().doubleValue(), st.percent);
            set(sh, r, "H", c.isImplemented() ? "Да" : "Нет", st.centered);
            r++;
        }

        width(sh, "A", 11);
        width(sh, "B", 42);
        width(sh, "C", 12);
        width(sh, "D", 58);
        width(sh, "E", 46);
        width(sh, "F", 22);
        width(sh, "G", 20);
        width(sh, "H", 11);
        // Описание контроля is hidden in the source workbook.
        hide(sh, "E");
    }

    // =====================================================================
    // 7. Техническая страница
    // =====================================================================

    private void writeTech(XSSFWorkbook wb, WorkbookStyles st) {
        XSSFSheet sh = wb.createSheet(S_TECH);

        set(sh, 0, "A", "Значимость актива", st.header);
        set(sh, 0, "C", "Уровень угрозы", st.header);
        set(sh, 0, "E", "Метод управления риском", st.header);
        set(sh, 0, "F", "Статус мероприятий", st.header);
        // The two headings that sit over a label + value pair span both.
        merge(sh, "A1:B1");
        merge(sh, "C1:D1");

        writeDict(sh, st, DictType.ASSET_CRITICALITY, "A", "B");
        writeDict(sh, st, DictType.THREAT_LEVEL, "C", "D");
        writeDict(sh, st, DictType.TREATMENT_METHOD, "E", null);
        writeDict(sh, st, DictType.MEASURE_STATUS, "F", null);

        width(sh, "A", 20);
        width(sh, "B", 8);
        width(sh, "C", 20);
        width(sh, "D", 8);
        width(sh, "E", 26);
        width(sh, "F", 26);
    }

    private void writeDict(XSSFSheet sh, WorkbookStyles st, DictType type,
                           String labelCol, String valueCol) {
        List<DictionaryItem> items = dictionaryRepository.findByDictTypeOrderBySortOrderAsc(type);
        int r = 1;
        for (DictionaryItem item : items) {
            set(sh, r, labelCol, item.getLabel(), st.text);
            if (valueCol != null && item.getNumericValue() != null) {
                setNumber(sh, r, valueCol, item.getNumericValue(), st.centered);
            }
            r++;
        }
    }

    // =====================================================================
    // 8. Перечень инфосистем Банка
    // =====================================================================

    private void writeInfoSystems(XSSFWorkbook wb, WorkbookStyles st, List<InfoSystem> systems) {
        XSSFSheet sh = wb.createSheet(S_INFO_SYSTEMS);

        header(sh, st, 0, new String[][]{
                {"A", "ID"}, {"B", "Название ресурса"}, {"C", "Описание ресурса"},
                {"D", "Размещение"}, {"E", "Использование"}, {"F", "Формат"},
                {"G", "Уровень конфиденциальности"}, {"H", "Целостность"}, {"I", "Доступность"},
                {"J", "Частота обновления данных"}, {"K", "Пользователи"}, {"L", "Владелец"}});

        int r = 1;
        for (InfoSystem s : systems) {
            set(sh, r, "A", s.getCode(), st.text);
            set(sh, r, "B", s.getName(), st.wrapped);
            set(sh, r, "C", s.getDescription(), st.wrapped);
            set(sh, r, "D", s.getHosting(), st.wrapped);
            set(sh, r, "E", s.getUsagePurpose(), st.wrapped);
            set(sh, r, "F", s.getDataFormat(), st.centered);
            set(sh, r, "G", s.getConfidentiality(), st.centered);
            set(sh, r, "H", s.getIntegrity(), st.centered);
            set(sh, r, "I", s.getAvailability(), st.centered);
            set(sh, r, "J", s.getUpdateFrequency(), st.text);
            set(sh, r, "K", s.getUsersInfo(), st.wrapped);
            set(sh, r, "L", s.getOwner(), st.wrapped);
            r++;
        }

        width(sh, "A", 9);
        width(sh, "B", 34);
        width(sh, "C", 44);
        width(sh, "D", 40);
        width(sh, "E", 34);
        width(sh, "K", 44);
        width(sh, "L", 34);
        autoFilter(sh, "A", "L", r);
        // Hidden in the source workbook; kept hidden so the export matches.
        wb.setSheetHidden(wb.getSheetIndex(sh), true);
    }

    // =====================================================================
    // small helpers
    // =====================================================================

    private void header(XSSFSheet sh, WorkbookStyles st, int rowIdx, String[][] cells) {
        for (String[] c : cells) {
            set(sh, rowIdx, c[0], c[1], st.header);
        }
        sh.getRow(rowIdx).setHeightInPoints(30);
    }

    private Cell cell(XSSFSheet sh, int rowIdx, String col) {
        Row row = sh.getRow(rowIdx);
        if (row == null) {
            row = sh.createRow(rowIdx);
        }
        int colIdx = CellReference.convertColStringToIndex(col);
        Cell cell = row.getCell(colIdx);
        return cell == null ? row.createCell(colIdx) : cell;
    }

    private void set(XSSFSheet sh, int rowIdx, String col, String value, CellStyle style) {
        Cell c = cell(sh, rowIdx, col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    private void set(XSSFSheet sh, int rowIdx, String col, int value, CellStyle style) {
        setNumber(sh, rowIdx, col, value, style);
    }

    private void setNumber(XSSFSheet sh, int rowIdx, String col, double value, CellStyle style) {
        Cell c = cell(sh, rowIdx, col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void formula(XSSFSheet sh, int rowIdx, String col, String formula, CellStyle style) {
        Cell c = cell(sh, rowIdx, col);
        c.setCellFormula(formula);
        c.setCellStyle(style);
    }

    private void width(XSSFSheet sh, String col, int chars) {
        sh.setColumnWidth(CellReference.convertColStringToIndex(col), chars * 256);
    }

    private void merge(XSSFSheet sh, String ref) {
        sh.addMergedRegion(CellRangeAddress.valueOf(ref));
    }

    private void hide(XSSFSheet sh, String col) {
        sh.setColumnHidden(CellReference.convertColStringToIndex(col), true);
    }

    /**
     * The header-row filter, over the reporting columns only.
     *
     * <p>Sized to the rows actually written. The source workbook's filters were
     * left at whatever the register held when they were last set - 30 rows on a
     * 542-row sheet - so a filtered view there silently hides real data.
     */
    private void autoFilter(XSSFSheet sh, String firstCol, String lastCol, int rowsWritten) {
        sh.setAutoFilter(CellRangeAddress.valueOf(
                "%s1:%s%d".formatted(firstCol, lastCol, Math.max(rowsWritten, 1))));
    }

    // =====================================================================
    // conditional formatting
    // =====================================================================

    // The dxf colours of the source workbook, so a printed export is the same
    // document people already read.
    private static final String CLR_CRITICAL = "C00000";
    private static final String CLR_HIGH = "FFC000";
    private static final String CLR_MEDIUM = "FFFF00";
    private static final String CLR_LOW = "92D050";
    private static final String CLR_NEGLIGIBLE = "00B0F0";
    private static final String CLR_ERROR = "FF0000";
    /** Реестр угроз paints its own ladder in slightly different shades. */
    private static final String CLR_THREAT_MEDIUM = "FCF600";
    private static final String CLR_THREAT_NEGLIGIBLE = "0070C0";
    /** «Низкий» there is a theme colour, not a literal - accent6 at 60% tint. */
    private static final int THREAT_LOW_THEME = 9;
    private static final double THREAT_LOW_TINT = 0.59996337778862885;
    /**
     * White text on the darkest fills. The source stores this as theme 0, which
     * resolves to white; written literally here because the dxf font element
     * only carries an explicit colour.
     */
    private static final String CLR_LIGHT_TEXT = "FFFFFF";

    /** "This cell equals that label" -> paint it. */
    private XSSFConditionalFormattingRule paint(XSSFSheetConditionalFormatting cf, String label,
                                                XSSFColor fill, boolean lightText) {
        XSSFConditionalFormattingRule rule = cf.createConditionalFormattingRule(
                ComparisonOperator.EQUAL, "\"%s\"".formatted(label));
        // Excel stores a dxf fill as the pattern's *background*; setting the
        // foreground instead produces a rule that highlights nothing.
        rule.createPatternFormatting().setFillBackgroundColor(fill);
        if (lightText) {
            lightText(rule);
        }
        return rule;
    }

    /** An out-of-range number, painted as the error it is. */
    private XSSFConditionalFormattingRule threshold(XSSFSheetConditionalFormatting cf,
                                                    byte operator, String bound) {
        XSSFConditionalFormattingRule rule = cf.createConditionalFormattingRule(operator, bound);
        rule.createPatternFormatting().setFillBackgroundColor(rgb(CLR_ERROR));
        lightText(rule);
        return rule;
    }

    /**
     * White lettering, for fills too dark to read black text on.
     *
     * <p>The colour is set twice deliberately. POI stores only the three colour
     * bytes when it first creates a dxf font element, which yields a six-digit
     * value where the format calls for aRGB; restating it on the element that
     * now exists writes all four.
     */
    private void lightText(XSSFConditionalFormattingRule rule) {
        XSSFFontFormatting font = rule.createFontFormatting();
        font.setFontColor(rgb(CLR_LIGHT_TEXT));
        font.getFontColor().setRGB(argb(CLR_LIGHT_TEXT));
    }

    private static XSSFColor rgb(String hex) {
        return new XSSFColor(argb(hex), null);
    }

    /**
     * A colour as the four bytes Excel stores.
     *
     * <p>The leading opaque alpha byte matters: the attribute is aRGB, and a
     * three-byte value produces a six-digit string that stricter readers reject
     * as a malformed stylesheet.
     */
    private static byte[] argb(String hex) {
        return new byte[]{
                (byte) 0xFF,
                (byte) Integer.parseInt(hex.substring(0, 2), 16),
                (byte) Integer.parseInt(hex.substring(2, 4), 16),
                (byte) Integer.parseInt(hex.substring(4, 6), 16)};
    }

    /** A colour carried by the workbook theme, shaded by a tint. */
    private static XSSFColor theme(int index, double tint) {
        XSSFColor color = new XSSFColor((IndexedColorMap) null);
        color.setTheme(index);
        color.setTint(tint);
        return color;
    }

    private <T> List<T> sorted(List<T> items,
                               java.util.function.Function<T, String> code,
                               java.util.function.Function<T, Long> id) {
        List<T> copy = new ArrayList<>(items);
        // Natural order by the numeric part of the code (КИА2 before КИА10),
        // falling back to id when a code was hand-edited.
        copy.sort(Comparator.comparingInt((T t) -> numericSuffix(code.apply(t)))
                .thenComparing(id::apply));
        return copy;
    }

    private int numericSuffix(String code) {
        if (code == null) {
            return Integer.MAX_VALUE;
        }
        int i = 0;
        while (i < code.length() && !Character.isDigit(code.charAt(i))) {
            i++;
        }
        try {
            return i < code.length() ? Integer.parseInt(code.substring(i)) : Integer.MAX_VALUE;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
