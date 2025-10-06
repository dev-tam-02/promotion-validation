package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a time window within a day (e.g., 09:00-17:00)
 * Time stored as strings in HH:mm format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeOfDayWindow {
    private String start;  // Format: "HH:mm"
    private String end;    // Format: "HH:mm"
}
