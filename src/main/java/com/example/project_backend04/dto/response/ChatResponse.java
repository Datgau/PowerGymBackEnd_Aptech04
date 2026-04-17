package com.example.project_backend04.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Structured response from AI chat endpoint.
 * Contains AI text + full data objects for frontend to render as cards.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String text,
        List<Map<String, Object>> services,     // gym service cards with images
        List<Map<String, Object>> memberships,  // membership package cards
        List<Map<String, Object>> trainers      // trainer cards with avatar
) {
    public static ChatResponse textOnly(String text) {
        return new ChatResponse(text, null, null, null);
    }

    public static Builder builder(String text) {
        return new Builder(text);
    }

    public static class Builder {
        private final String text;
        private List<Map<String, Object>> services;
        private List<Map<String, Object>> memberships;
        private List<Map<String, Object>> trainers;

        Builder(String text) { this.text = text; }

        public Builder services(List<Map<String, Object>> v)    { this.services = v;    return this; }
        public Builder memberships(List<Map<String, Object>> v) { this.memberships = v; return this; }
        public Builder trainers(List<Map<String, Object>> v)    { this.trainers = v;    return this; }

        public ChatResponse build() {
            return new ChatResponse(text, services, memberships, trainers);
        }
    }
}
