package com.example.project_backend04.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response DTO from Gemini REST API.
 *
 * Gemini 2.5 Flash (thinking model) can return MULTIPLE parts in one response:
 *   parts[0] = {text: "...", thoughtSignature: "..."}  <- thinking text, ignore
 *   parts[1] = {functionCall: {...}}                   <- actual tool call
 *
 * Rule: if ANY part has a functionCall → treat as function call response.
 * Only return text if NO functionCall exists anywhere in parts.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<Candidate> candidates
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
            Content content,
            @JsonProperty("finish_reason") String finishReason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            String role,
            List<Part> parts
    ) {}

    /**
     * Part can contain text, functionCall, or both (with thoughtSignature).
     * thoughtSignature is ignored but must be declared for Jackson to parse correctly.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(
            String text,
            @JsonProperty("function_call") FunctionCall functionCall,
            @JsonProperty("thought_signature") String thoughtSignature  // ignored, but needed for parsing
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionCall(
            String name,
            Map<String, Object> args
    ) {}

    private List<Part> firstCandidateParts() {
        if (candidates == null || candidates.isEmpty()) return List.of();
        var content = candidates.get(0).content();
        if (content == null || content.parts() == null) return List.of();
        return content.parts();
    }

    /**
     * Returns true if ANY part contains a functionCall.
     * Takes priority over text — must be checked before getText().
     */
    public boolean hasFunctionCall() {
        List<Part> parts = firstCandidateParts();
        boolean result = parts.stream().anyMatch(p -> p.functionCall() != null);
        
        // Debug log
        if (!parts.isEmpty()) {
            System.out.println("DEBUG hasFunctionCall: parts.size=" + parts.size());
            for (int i = 0; i < parts.size(); i++) {
                Part p = parts.get(i);
                System.out.println("  Part[" + i + "]: text=" + (p.text() != null ? "yes" : "null") 
                    + ", functionCall=" + (p.functionCall() != null ? "yes" : "null")
                    + ", thoughtSignature=" + (p.thoughtSignature() != null ? "yes" : "null"));
            }
        }
        
        return result;
    }

    /**
     * Returns the first functionCall found across all parts.
     */
    public FunctionCall getFunctionCall() {
        return firstCandidateParts().stream()
                .filter(p -> p.functionCall() != null)
                .map(Part::functionCall)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns concatenated text from all text-only parts (no functionCall).
     * Only call this when hasFunctionCall() == false.
     */
    public String getText() {
        return firstCandidateParts().stream()
                .filter(p -> p.text() != null && !p.text().isBlank() && p.functionCall() == null)
                .map(Part::text)
                .reduce("", String::concat)
                .trim();
    }
}
