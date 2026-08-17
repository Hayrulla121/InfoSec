package uz.infosec.risk.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Generates the workbook's human-readable identifiers: КИА1, У1, C1, R1, ИС1.
 *
 * <p>Rule from the spec: max existing number + 1. Note this is deliberately
 * "max + 1", not "count + 1" - deleting КИА3 out of three assets must not make
 * the next one КИА3 again and collide.
 *
 * <p>Under concurrent creates two requests could compute the same number; the
 * UNIQUE constraint on every code column turns that into a 409 rather than
 * duplicate data. For this application's traffic that is the right trade
 * against serialising every insert.
 */
@Service
public class CodeGenerator {

    public String next(String prefix, List<String> existingCodes) {
        int max = 0;
        for (String code : existingCodes) {
            if (code == null || !code.startsWith(prefix)) {
                continue;
            }
            String suffix = code.substring(prefix.length());
            // Skip anything hand-edited that is not purely numeric.
            if (suffix.isEmpty() || !suffix.chars().allMatch(Character::isDigit)) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                // Suffix longer than an int; cannot be the maximum we care about.
            }
        }
        return prefix + (max + 1);
    }
}
