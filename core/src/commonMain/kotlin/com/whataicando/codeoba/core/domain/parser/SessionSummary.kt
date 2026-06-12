package com.whataicando.codeoba.core.domain.parser

import kotlinx.serialization.Serializable

@Serializable
data class SessionSummary(
    val keyActions: List<String>,
    val errors: List<String>,
    val performanceCharts: List<PerformanceChartPoint>
)

@Serializable
data class PerformanceChartPoint(
    val label: String,
    val value: Double
)
