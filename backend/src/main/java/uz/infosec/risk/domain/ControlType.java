package uz.infosec.risk.domain;

/**
 * Whether a control is already in place for a given risk, or merely planned.
 *
 * <p>Maps to "Внедрен? Да / Нет" on the workbook's Риск-контроль sheet, and
 * decides which reduction chain a control joins:
 * IMPLEMENTED -> current score, IMPLEMENTED + PLANNED -> residual score.
 */
public enum ControlType {
    IMPLEMENTED,
    PLANNED
}
