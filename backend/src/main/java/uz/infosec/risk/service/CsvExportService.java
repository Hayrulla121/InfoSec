package uz.infosec.risk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.*;
import uz.infosec.risk.repository.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * CSV export of the registries, so management reporting still works after the
 * workbook is retired.
 *
 * <p>Two deliberate choices for Excel compatibility:
 * <ul>
 *   <li><b>UTF-8 BOM</b> - without it, Excel on Windows opens a UTF-8 CSV as
 *       cp1251 and every Cyrillic column turns into mojibake.</li>
 *   <li><b>Semicolon separator</b> - in locales where the decimal separator is
 *       a comma (ru/uz among them), Excel expects ';' as the list separator.
 *       A comma-separated file lands entirely in column A.</li>
 * </ul>
 */
@Service
public class CsvExportService {

    /** Byte-order mark. Tells Excel "this really is UTF-8". */
    public static final String BOM = "﻿";
    private static final char SEPARATOR = ';';

    private final AssetRepository assetRepository;
    private final ThreatRepository threatRepository;
    private final ControlRepository controlRepository;
    private final RiskRepository riskRepository;

    public CsvExportService(AssetRepository assetRepository,
                            ThreatRepository threatRepository,
                            ControlRepository controlRepository,
                            RiskRepository riskRepository) {
        this.assetRepository = assetRepository;
        this.threatRepository = threatRepository;
        this.controlRepository = controlRepository;
        this.riskRepository = riskRepository;
    }

    @Transactional(readOnly = true)
    public String exportAssets() {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "ID", "Axborot tizimining nomi", "Axborot tizimining ko'lami",
                "Qayta ishlanadigan axborot toifasi", "Axborot tizimining muhimlik darajasi",
                "Reyting", "Класс защищенности", "Создан", "Изменён");
        for (Asset a : assetRepository.findAll()) {
            row(sb, a.getCode(), a.getName(), a.getScope(), a.getInfoCategory(),
                    a.getCriticality(), String.valueOf(a.getCriticalityRating()),
                    a.getSecurityClass(), a.getCreatedBy(), a.getUpdatedBy());
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportThreats() {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "#", "Угрозы", "Обнаружение", "Повторение", "Эксплуатирование",
                "Масштаб", "Ущерб", "Сумма", "Рейтинг", "Уровень угрозы");
        for (Threat t : threatRepository.findAll()) {
            row(sb, t.getCode(), t.getDescription(),
                    String.valueOf(t.getDiscoverability()), String.valueOf(t.getRepeatability()),
                    String.valueOf(t.getExploitability()), String.valueOf(t.getAffectedUsers()),
                    String.valueOf(t.getDamage()), String.valueOf(t.getTotalScore()),
                    String.valueOf(t.getRating()), t.getLevelLabel());
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportControls() {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "ID Контроля", "Название контроля", "Описание контроля",
                "Метод управления риском", "Процент снижения риска", "Внедрен?");
        for (Control c : controlRepository.findAll()) {
            row(sb, c.getCode(), c.getName(), c.getDescription(), c.getTreatmentMethod(),
                    decimal(c.getReductionPct()), c.isImplemented() ? "Да" : "Нет");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportRisks() {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "ID Риска", "ID Связанного ИА", "Название ИА", "ID Связанной угрозы",
                "Описание угрозы", "Наименование риска", "Индикаторы риска",
                "Снижающие контроли", "Уровень риска", "Владелец риска",
                "Метод управления риском", "Запланированные мероприятия", "Остаточный риск",
                "Статус мероприятий", "Финальная дата внедрения мероприятий", "Комментарий");

        for (Risk r : riskRepository.findAll()) {
            Asset a = r.getAsset();
            Threat t = r.getThreat();
            row(sb, r.getCode(), a.getCode(), a.getName(), t.getCode(), t.getDescription(),
                    r.getName(), r.getIndicators(),
                    // Rebuilds Excel's TEXTJOIN columns H and L.
                    joinControls(r, ControlType.IMPLEMENTED),
                    r.getCurrentRiskLabel(),
                    r.getOwner(), r.getTreatmentMethod(),
                    joinControls(r, ControlType.PLANNED),
                    r.getResidualRiskLabel(),
                    r.getMeasureStatus(),
                    r.getImplementationDeadline() == null ? null
                            : r.getImplementationDeadline().toString(),
                    r.getComment());
        }
        return sb.toString();
    }

    private String joinControls(Risk risk, ControlType type) {
        return risk.getControls().stream()
                .filter(rc -> rc.getControlType() == type)
                .map(rc -> rc.getControl().getName())
                .collect(Collectors.joining(", "));
    }

    private String decimal(BigDecimal value) {
        // Comma decimal mark, as Excel expects in these locales.
        return value == null ? "" : value.toPlainString().replace('.', ',');
    }

    private void row(StringBuilder sb, String... values) {
        // Arrays.stream, NOT List.of(values).stream(): List.of rejects null
        // elements outright, and nullable columns (description, comment,
        // deadline) are entirely normal here.
        sb.append(Arrays.stream(values)
                .map(this::escape)
                .collect(Collectors.joining(String.valueOf(SEPARATOR))));
        // CRLF: the line ending every spreadsheet program accepts.
        sb.append("\r\n");
    }

    /**
     * RFC 4180 quoting: wrap in double quotes if the value contains a
     * separator, a quote or a newline, and double any embedded quotes.
     * Threat descriptions routinely contain commas, so this is not optional.
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.indexOf(SEPARATOR) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
