package com.docportal.docportal.dto;

import java.util.List;
import java.util.Map;

public record DashboardStatsDto(
        long totalDocuments,
        long myDocuments,
        long activeCategories,
        long storageUsedBytes,
        List<Map<String, Object>> documentsPerCategory,
        List<DocumentDto> recentDocuments,
        List<HistoryDto> recentActions
) {
}
