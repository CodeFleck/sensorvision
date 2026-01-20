package io.indcloud.model;

/**
 * Types of LLM-powered features available in the platform.
 */
public enum LLMFeatureType {
    ANOMALY_EXPLANATION("anomaly_explanation", "Explain anomalies and suggest fixes"),
    NATURAL_LANGUAGE_QUERY("nl_query", "Query data using natural language"),
    REPORT_GENERATION("report_generation", "Generate reports from data"),
    ROOT_CAUSE_ANALYSIS("root_cause", "Analyze root causes of issues"),
    ALERT_SUMMARY("alert_summary", "Summarize alerts and events"),
    PREDICTIVE_INSIGHT("predictive_insight", "Provide predictive insights"),
    WIDGET_ASSISTANT("widget_assistant", "Widget creation via natural language");

    private final String code;
    private final String description;

    LLMFeatureType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
