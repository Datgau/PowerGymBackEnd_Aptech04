package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.ChatResponse;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIChatService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    private static final String SYSTEM_PROMPT = """
            You are PowerGym's intelligent gym consultant - friendly, enthusiastic, and professional.
            
            MAIN RESPONSIBILITIES:
            1. Understand customer needs and fitness goals
            2. Recommend suitable membership packages, services, and trainers
            3. Provide detailed registration and service usage instructions
            4. Assist with trainer booking appointments
            
            TOOL USAGE RULES:
            - When customer asks about membership packages -> call searchMembershipPackages
            - When customer asks about services/classes/PT -> call searchGymServices
            - When customer asks about trainers -> call searchTrainers
            - When customer wants to book -> call createTrainerBooking (requires: trainerId, bookingDate, startTime, endTime)
            
            REGISTRATION INSTRUCTIONS (IMPORTANT):
            
            [MEMBERSHIP] MEMBERSHIP PACKAGE REGISTRATION:
            1. Click "Register Package" button on the membership card
            2. Choose payment method (MoMo or Bank Transfer)
            3. Complete payment following instructions
            4. Membership will be activated immediately after successful payment
            
            [SERVICE] SERVICE REGISTRATION (PT, Yoga, Boxing...):
            1. Click "Register Now" button on the service card
            2. Choose suitable trainer (or let system auto-select)
            3. Select desired date and time
            4. Enter promo code if available
            5. Choose payment method and complete
            6. Wait for trainer confirmation
            
            [TRAINER] BOOK WITH TRAINER:
            1. Click "Book Now" button on trainer card
            2. Select service to register
            3. Choose date and time
            4. Complete payment
            5. Wait for trainer confirmation
            
            [NOTES] IMPORTANT NOTES:
            - Login required to register services
            - Membership must be activated before registering services
            - Promo codes available for discounts
            - Trainer may decline if busy, you can choose another trainer
            
            RESPONSE GUIDELINES:
            - Always remember previous conversation context
            - When customer asks follow-up questions, reference previous information
            - After calling tools, summarize results briefly and guide next steps
            - When displaying cards (services/memberships/trainers), always explain registration process
            - Ask for more information if customer needs are unclear
            
            COMMUNICATION STYLE:
            - Friendly and enthusiastic like a friend
            - Professional with fitness expertise
            - Concise and easy to understand
            - Always encourage and motivate customers
            """;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate;
    private final MembershipPackageRepository membershipPackageRepository;
    private final GymServiceRepository gymServiceRepository;
    private final UserRepository userRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final TrainerBookingRepository trainerBookingRepository;
    private final ObjectMapper objectMapper;
    
    private final Cache<String, List<Map<String, Object>>> sessionStore = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    // ==================== MAIN CHAT METHOD ====================

    public ChatResponse chat(String sessionId, String userMessage) {
        List<Map<String, Object>> history = sessionStore.get(sessionId, k -> new ArrayList<>());
        
        log.info("Session {}: Current history size = {}, New message: {}", 
                sessionId, history.size(), userMessage);
        
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);

        List<Map<String, Object>> foundServices = new ArrayList<>();
        List<Map<String, Object>> foundMemberships = new ArrayList<>();
        List<Map<String, Object>> foundTrainers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            JsonNode response = callOpenAI(history);

            if (response == null) {
                return ChatResponse.textOnly("Sorry, no response from AI. Please try again.");
            }

            JsonNode message = response.path("choices").path(0).path("message");
            JsonNode toolCalls = message.path("tool_calls");

            if (!toolCalls.isMissingNode() && toolCalls.isArray() && toolCalls.size() > 0) {

                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", message.path("content").asText(""));
                assistantMsg.put("tool_calls", objectMapper.convertValue(toolCalls, List.class));
                history.add(assistantMsg);

                // Execute each tool call
                for (JsonNode toolCall : toolCalls) {
                    String toolCallId = toolCall.path("id").asText();
                    String functionName = toolCall.path("function").path("name").asText();
                    String argumentsStr = toolCall.path("function").path("arguments").asText();
                    
                    log.info("Tool called: {}", functionName);
                    
                    Map<String, Object> args;
                    try {
                        args = objectMapper.readValue(argumentsStr, Map.class);
                    } catch (Exception e) {
                        args = new HashMap<>();
                    }

                    // Execute tool
                    ToolResult result = executeToolWithData(functionName, args);

                    // Collect rich data
                    switch (functionName) {
                        case "searchGymServices"        -> foundServices.addAll(result.data());
                        case "searchMembershipPackages" -> foundMemberships.addAll(result.data());
                        case "searchTrainers"           -> foundTrainers.addAll(result.data());
                    }

                    // Add tool response to history
                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", toolCallId);
                    toolMsg.put("content", result.summary());
                    history.add(toolMsg);
                }

            } else {
                // Final text response
                String text = message.path("content").asText("");
                if (!text.isBlank()) {
                    Map<String, Object> assistantMsg = new HashMap<>();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", text);
                    history.add(assistantMsg);
                }
                
                sessionStore.put(sessionId, history);
                log.info("Session {}: Saved history with {} messages", sessionId, history.size());

                String finalText = !text.isBlank() ? text : "Sorry, no response. Please try again.";

                return ChatResponse.builder(finalText)
                        .services(foundServices.isEmpty() ? null : foundServices)
                        .memberships(foundMemberships.isEmpty() ? null : foundMemberships)
                        .trainers(foundTrainers.isEmpty() ? null : foundTrainers)
                        .build();
            }
        }

        sessionStore.put(sessionId, history);
        log.info("Session {}: Max iterations reached, saved history with {} messages", sessionId, history.size());
        return ChatResponse.textOnly("Sorry, processing error occurred. Please try again.");
    }

    private JsonNode callOpenAI(List<Map<String, Object>> messages) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", model);
            
            ArrayNode messagesArray = objectMapper.createArrayNode();
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messagesArray.add(systemMsg);
            
            for (Map<String, Object> msg : messages) {
                messagesArray.add(objectMapper.valueToTree(msg));
            }
            request.set("messages", messagesArray);
            
            request.set("tools", objectMapper.valueToTree(buildTools()));
            request.put("tool_choice", "auto");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

            log.debug("OpenAI request: {}", objectMapper.writeValueAsString(request));

            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    OPENAI_API_URL, HttpMethod.POST, entity, String.class);

            log.debug("OpenAI raw response: {}", rawResponse.getBody());
            return objectMapper.readTree(rawResponse.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("OpenAI HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi kết nối OpenAI API: " + e.getMessage());
        }
    }

    private record ToolResult(String summary, List<Map<String, Object>> data) {}

    private ToolResult executeToolWithData(String toolName, Map<String, Object> args) {
        return switch (toolName) {
            case "searchGymServices"        -> searchGymServices(args);
            case "searchMembershipPackages" -> searchMembershipPackages(args);
            case "searchTrainers"           -> searchTrainers(args);
            case "createTrainerBooking"     -> {
                String result = createTrainerBooking(args);
                yield new ToolResult(result, List.of());
            }
            default -> new ToolResult("Tool does not exist: " + toolName, List.of());
        };
    }
    
    private ToolResult searchGymServices(Map<String, Object> args) {
        try {
            String keyword = (String) args.get("keyword");
            String categoryName = (String) args.get("categoryName");

            List<GymService> services = gymServiceRepository.findByIsActiveTrueWithImages();

            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.toLowerCase();
                services = services.stream()
                        .filter(s -> s.getName().toLowerCase().contains(kw) ||
                                (s.getDescription() != null && s.getDescription().toLowerCase().contains(kw)))
                        .collect(Collectors.toList());
            }
            if (categoryName != null && !categoryName.isBlank()) {
                services = services.stream()
                        .filter(s -> s.getCategory() != null &&
                                s.getCategory().getName().equalsIgnoreCase(categoryName))
                        .collect(Collectors.toList());
            }

            if (services.isEmpty()) {
                return new ToolResult("No matching services found.", List.of());
            }

            NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
            List<Map<String, Object>> data = new ArrayList<>();
            StringBuilder summary = new StringBuilder("Found " + services.size() + " services:\n");

            for (GymService s : services) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", s.getId());
                item.put("name", s.getName());
                item.put("description", s.getDescription());
                item.put("price", s.getPrice());
                item.put("priceFormatted", s.getPrice() != null ? fmt.format(s.getPrice()) + " VNĐ/buổi" : null);
                item.put("duration", s.getDuration());
                item.put("maxParticipants", s.getMaxParticipants());
                item.put("category", s.getCategory() != null ? s.getCategory().getName() : null);
                List<String> images = s.getImages() == null ? List.of() :
                        s.getImages().stream()
                                .sorted(Comparator.comparingInt(GymServiceImage::getSortOrder))
                                .map(GymServiceImage::getImageUrl)
                                .collect(Collectors.toList());
                item.put("images", images);
                item.put("thumbnail", images.isEmpty() ? null : images.get(0));
                data.add(item);

                summary.append(String.format("- %s: %s VND/session, %d minutes\n",
                        s.getName(),
                        s.getPrice() != null ? fmt.format(s.getPrice()) : "N/A",
                        s.getDuration() != null ? s.getDuration() : 0));
            }

            return new ToolResult(summary.toString(), data);
        } catch (Exception e) {
            log.error("Error in searchGymServices", e);
            return new ToolResult("Error searching for services.", List.of());
        }
    }

    private ToolResult searchMembershipPackages(Map<String, Object> args) {
        try {
            String keyword = (String) args.get("keyword");
            Integer duration = args.get("duration") != null ? ((Number) args.get("duration")).intValue() : null;
            BigDecimal maxPrice = args.get("maxPrice") != null
                    ? BigDecimal.valueOf(((Number) args.get("maxPrice")).doubleValue()) : null;

            List<MembershipPackage> packages = membershipPackageRepository.findByIsActiveTrue();

            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.toLowerCase();
                packages = packages.stream()
                        .filter(p -> p.getName().toLowerCase().contains(kw))
                        .collect(Collectors.toList());
            }
            if (duration != null) {
                packages = packages.stream()
                        .filter(p -> p.getDuration().equals(duration))
                        .collect(Collectors.toList());
            }
            if (maxPrice != null) {
                packages = packages.stream()
                        .filter(p -> p.getPrice().compareTo(maxPrice) <= 0)
                        .collect(Collectors.toList());
            }

            if (packages.isEmpty()) {
                return new ToolResult("No matching membership packages found.", List.of());
            }

            NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
            List<Map<String, Object>> data = new ArrayList<>();
            StringBuilder summary = new StringBuilder("Found " + packages.size() + " membership packages:\n");

            for (MembershipPackage p : packages) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getName());
                item.put("description", p.getDescription());
                item.put("price", p.getPrice());
                item.put("priceFormatted", p.getPrice() != null ? fmt.format(p.getPrice()) + " VNĐ" : null);
                item.put("originalPrice", p.getOriginalPrice());
                item.put("originalPriceFormatted", p.getOriginalPrice() != null ? fmt.format(p.getOriginalPrice()) + " VNĐ" : null);
                item.put("duration", p.getDuration());
                item.put("discount", p.getDiscount());
                item.put("isPopular", p.getIsPopular());
                item.put("color", p.getColor());
                item.put("features", p.getFeatures() != null ? p.getFeatures() : List.of());
                data.add(item);

                summary.append(String.format("- %s: %s VND, %d days%s\n",
                        p.getName(),
                        fmt.format(p.getPrice()),
                        p.getDuration(),
                        p.getDiscount() != null && p.getDiscount() > 0 ? " (" + p.getDiscount() + "% off)" : ""));
            }

            return new ToolResult(summary.toString(), data);
        } catch (Exception e) {
            log.error("Error in searchMembershipPackages", e);
            return new ToolResult("Error searching for membership packages.", List.of());
        }
    }

    private ToolResult searchTrainers(Map<String, Object> args) {
        try {
            String specialtyName = (String) args.get("specialtyName");

            List<TrainerSpecialty> trainerSpecialties = trainerSpecialtyRepository.findAllActiveTrainerSpecialties();

            if (specialtyName != null && !specialtyName.isBlank()) {
                String sn = specialtyName.toLowerCase();
                trainerSpecialties = trainerSpecialties.stream()
                        .filter(ts -> ts.getSpecialty() != null &&
                                (ts.getSpecialty().getName().toLowerCase().contains(sn) ||
                                        ts.getSpecialty().getDisplayName().toLowerCase().contains(sn)))
                        .collect(Collectors.toList());
            }

            List<User> trainers = trainerSpecialties.stream()
                    .map(TrainerSpecialty::getUser)
                    .distinct()
                    .sorted(Comparator.comparingInt(t ->
                            -(t.getTotalExperienceYears() != null ? t.getTotalExperienceYears() : 0)))
                    .collect(Collectors.toList());

            if (trainers.isEmpty()) {
                return new ToolResult("No matching trainers found.", List.of());
            }

            List<Map<String, Object>> data = new ArrayList<>();
            StringBuilder summary = new StringBuilder("Found " + trainers.size() + " trainers:\n");

            for (User trainer : trainers) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", trainer.getId());
                item.put("fullName", trainer.getFullName());
                item.put("bio", trainer.getBio());
                item.put("avatar", trainer.getAvatar());
                item.put("totalExperienceYears", trainer.getTotalExperienceYears());

                List<TrainerSpecialty> specs = trainerSpecialties.stream()
                        .filter(ts -> ts.getUser().getId().equals(trainer.getId()))
                        .collect(Collectors.toList());

                List<Map<String, Object>> specialtyList = specs.stream().map(ts -> {
                    Map<String, Object> spec = new LinkedHashMap<>();
                    spec.put("name", ts.getSpecialty().getDisplayName());
                    spec.put("experienceYears", ts.getExperienceYears());
                    return spec;
                }).collect(Collectors.toList());
                item.put("specialties", specialtyList);
                data.add(item);

                String specStr = specs.stream()
                        .map(ts -> ts.getSpecialty().getDisplayName())
                        .collect(Collectors.joining(", "));
                summary.append(String.format("- %s (ID: %d): %d years experience, specialties: %s\n",
                        trainer.getFullName(), trainer.getId(),
                        trainer.getTotalExperienceYears() != null ? trainer.getTotalExperienceYears() : 0,
                        specStr));
            }

            return new ToolResult(summary.toString(), data);
        } catch (Exception e) {
            log.error("Error in searchTrainers", e);
            return new ToolResult("Error searching for trainers.", List.of());
        }
    }

    private String createTrainerBooking(Map<String, Object> args) {
        try {
            Long trainerId = args.get("trainerId") != null ? ((Number) args.get("trainerId")).longValue() : null;
            String bookingDateStr = (String) args.get("bookingDate");
            String startTimeStr = (String) args.get("startTime");
            String endTimeStr = (String) args.get("endTime");

            if (trainerId == null) return "Please provide trainer ID.";
            if (bookingDateStr == null) return "Please provide booking date.";
            if (startTimeStr == null || endTimeStr == null) return "Please provide start and end time.";

            User trainer = userRepository.findById(trainerId).orElse(null);
            if (trainer == null) return "Trainer with ID " + trainerId + " not found.";
            if (!trainer.isTrainer() || !trainer.getIsActive()) return "This trainer is currently unavailable.";

            LocalDate bookingDate = LocalDate.parse(bookingDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            if (bookingDate.isBefore(LocalDate.now())) return "Booking date cannot be in the past.";
            if (!startTime.isBefore(endTime)) return "Start time must be before end time.";

            List<TrainerBooking> conflicts = trainerBookingRepository.findConflictingBookingsForTrainer(
                    trainerId, bookingDate, startTime, endTime,
                    List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            if (!conflicts.isEmpty()) return "Trainer is already booked during this time slot. Please choose another time.";

            return String.format(
                    "Valid information! Trainer: %s, Date: %s, Time: %s-%s. " +
                    "To complete, please login and confirm the booking.",
                    trainer.getFullName(),
                    bookingDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    endTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        } catch (Exception e) {
            log.error("Error in createTrainerBooking", e);
            return "Error processing booking: " + e.getMessage();
        }
    }

    private List<Map<String, Object>> buildTools() {
        return List.of(
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "searchMembershipPackages",
                                "description", "Search for membership packages by keyword, duration, or maximum price.",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "keyword", Map.of("type", "string", "description", "Package name keyword"),
                                                "duration", Map.of("type", "integer", "description", "Duration in days"),
                                                "maxPrice", Map.of("type", "number", "description", "Maximum price in VND")
                                        )
                                )
                        )
                ),
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "searchGymServices",
                                "description", "Search for gym services by name or category.",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "keyword", Map.of("type", "string", "description", "Service name keyword"),
                                                "categoryName", Map.of("type", "string", "description", "Category name")
                                        )
                                )
                        )
                ),
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "searchTrainers",
                                "description", "Search for trainers by specialty.",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "specialtyName", Map.of("type", "string", "description", "Specialty name")
                                        )
                                )
                        )
                ),
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "createTrainerBooking",
                                "description", "Validate and confirm trainer booking.",
                                "parameters", Map.of(
                                        "type", "object",
                                        "required", List.of("trainerId", "bookingDate", "startTime", "endTime"),
                                        "properties", Map.of(
                                                "trainerId", Map.of("type", "integer", "description", "Trainer ID"),
                                                "bookingDate", Map.of("type", "string", "description", "Date (YYYY-MM-DD)"),
                                                "startTime", Map.of("type", "string", "description", "Start time (HH:mm)"),
                                                "endTime", Map.of("type", "string", "description", "End time (HH:mm)"),
                                                "notes", Map.of("type", "string", "description", "Notes")
                                        )
                                )
                        )
                )
        );
    }
}
