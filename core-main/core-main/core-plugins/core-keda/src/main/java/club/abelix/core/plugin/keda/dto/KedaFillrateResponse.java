package club.abelix.core.plugin.keda.dto;

import java.util.Map;

public record KedaFillrateResponse(Map<String, Map<String, Integer>> values) {
}
