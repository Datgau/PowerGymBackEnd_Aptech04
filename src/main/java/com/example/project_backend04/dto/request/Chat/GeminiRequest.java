package com.example.project_backend04.dto.request.Chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for Gemini REST API
 * POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequest(
        List<Content> contents,
        @JsonProperty("system_instruction") Content systemInstruction,
        List<Tool> tools,
        @JsonProperty("tool_config") ToolConfig toolConfig
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(
            String role,
            List<Part> parts
    ) {
        /** System instruction không có role */
        public static Content system(String text) {
            return new Content(null, List.of(Part.text(text)));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(
            String text,
            @JsonProperty("function_call") FunctionCall functionCall,
            @JsonProperty("function_response") FunctionResponse functionResponse
    ) {
        public static Part text(String text) {
            return new Part(text, null, null);
        }

        public static Part functionResponse(String name, Object response) {
            return new Part(null, null, new FunctionResponse(name, Map.of("result", response)));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FunctionCall(
            String name,
            Map<String, Object> args
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FunctionResponse(
            String name,
            Map<String, Object> response
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Tool(
            @JsonProperty("function_declarations") List<FunctionDeclaration> functionDeclarations
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FunctionDeclaration(
            String name,
            String description,
            Map<String, Object> parameters
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolConfig(
            @JsonProperty("function_calling_config") FunctionCallingConfig functionCallingConfig
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FunctionCallingConfig(String mode) {}
}
