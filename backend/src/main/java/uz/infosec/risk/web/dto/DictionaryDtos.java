package uz.infosec.risk.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.infosec.risk.domain.DictType;

import java.util.List;

public final class DictionaryDtos {

    private DictionaryDtos() {
    }

    /** id is null for a row the user just added in the UI. */
    public record DictionaryItemDto(
            Long id,
            @NotBlank @Size(max = 64) String label,
            @Min(1) @Max(5) Integer numericValue,
            int sortOrder) {
    }

    /** One dictionary with its display title, as the UI groups them. */
    public record DictionaryGroupDto(
            DictType dictType,
            String title,
            boolean numericRequired,
            List<DictionaryItemDto> items) {
    }

    /**
     * Replaces the contents of ONE dictionary. Scoping the write to a single
     * dict_type means a stale browser tab editing statuses can never wipe the
     * criticality list it was not even showing.
     */
    public record UpdateDictionaryRequest(
            @NotNull DictType dictType,
            @NotNull @Valid List<DictionaryItemDto> items) {
    }
}
