package club.abelix.api.base.report.util;

import club.abelix.commons.language.Language;
import club.abelix.network.type.report.ReportReason;
import lombok.experimental.UtilityClass;

import java.util.Arrays;

/**
 * @author pro0xy
 * <p>Created on 06.11.2023 at 08:34</p>
 */

@UtilityClass
public class ReportUtil {
    public static ReportReason getReportByReason(String reason) {
        return Arrays.stream(ReportReason.values())
                .filter(reportReason -> Arrays.stream(reportReason.getAlias())
                        .anyMatch(alias -> alias.contains(reason.toLowerCase())))
                .findFirst()
                .orElse(null);
    }
}
