package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Chat.*;
import com.example.project_backend04.dto.response.ChatResponse;
import com.example.project_backend04.dto.response.GeminiResponse;
import com.example.project_backend04.entity.*;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class GeminiChatService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}";

    private static final String SYSTEM_PROMPT = """
            Bạn là chuyên gia tư vấn gym thông minh của PowerGym.
            
            NHIỆM VỤ:
            1. Tìm hiểu nhu cầu khách hàng
            2. Tư vấn gói membership, dịch vụ, trainer phù hợp
            3. Hỗ trợ đặt lịch tập với trainer
            
            QUY TẮC SỬ DỤNG TOOLS:
            - Khi khách hỏi về gói tập/membership → gọi searchMembershipPackages
            - Khi khách hỏi về dịch vụ/lớp học → gọi searchGymServices
            - Khi khách hỏi về trainer/huấn luyện viên → gọi searchTrainers
            - Khi khách muốn đặt lịch → gọi createTrainerBooking (cần đủ: trainerId, bookingDate, startTime, endTime)
            
            SAU KHI GỌI TOOL:
            - Tóm tắt kết quả ngắn gọn, thân thiện bằng tiếng Việt
            - Đề xuất bước tiếp theo cho khách
            - Cần liệt kê lại toàn bộ dữ liệu và hiển thị card nếu có dịch vụ/trainer/membership nào phù hợp
            
            LƯU Ý:
            - Luôn trả lời tiếng Việt
            - Thân thiện, chuyên nghiệp
            - Không tự ý đặt lịch khi chưa đủ thông tin
            """;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    private final RestTemplate restTemplate;
    private final MembershipPackageRepository membershipPackageRepository;
    private final GymServiceRepository gymServiceRepository;
    private final UserRepository userRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final TrainerBookingRepository trainerBookingRepository;
    private final ObjectMapper objectMapper;
    private final Cache<String, List<GeminiRequest.Content>> sessionStore = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    // ==================== MAIN CHAT METHOD ====================

    public ChatResponse chat(String sessionId, String userMessage) {
        List<GeminiRequest.Content> history = sessionStore.get(sessionId, k -> new ArrayList<>());
        history.add(new GeminiRequest.Content("user", List.of(GeminiRequest.Part.text(userMessage))));

        // Collect rich data from tool calls to embed in response
        List<Map<String, Object>> foundServices = new ArrayList<>();
        List<Map<String, Object>> foundMemberships = new ArrayList<>();
        List<Map<String, Object>> foundTrainers = new ArrayList<>();

        // Agentic loop (max 5 iterations)
        for (int i = 0; i < 5; i++) {
            GeminiResponse response = callGemini(history);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                return ChatResponse.textOnly("Xin lỗi, không nhận được phản hồi từ AI. Vui lòng thử lại.");
            }

            log.info("Loop {}: hasFunctionCall={}", i, response.hasFunctionCall());

            if (response.hasFunctionCall()) {
                GeminiResponse.FunctionCall fc = response.getFunctionCall();
                log.info("Tool called: {}", fc.name());

                // Save clean function_call to history (no thoughtSignature)
                history.add(new GeminiRequest.Content("model",
                        List.of(new GeminiRequest.Part(null,
                                new GeminiRequest.FunctionCall(fc.name(), fc.args()), null))));

                // Execute tool — returns text summary for Gemini + collects rich data
                ToolResult result = executeToolWithData(fc.name(), fc.args());

                // Collect rich data by tool type
                switch (fc.name()) {
                    case "searchGymServices"        -> foundServices.addAll(result.data());
                    case "searchMembershipPackages" -> foundMemberships.addAll(result.data());
                    case "searchTrainers"           -> foundTrainers.addAll(result.data());
                }

                // Give Gemini the text summary to reason about
                history.add(new GeminiRequest.Content("user",
                        List.of(GeminiRequest.Part.functionResponse(fc.name(), result.summary()))));

            } else {
                // Final text response from Gemini
                String text = response.getText();
                if (text != null && !text.isBlank()) {
                    history.add(new GeminiRequest.Content("model",
                            List.of(GeminiRequest.Part.text(text))));
                }
                sessionStore.put(sessionId, history);

                String finalText = (text != null && !text.isBlank())
                        ? text : "Xin lỗi, không có phản hồi. Vui lòng thử lại.";

                return ChatResponse.builder(finalText)
                        .services(foundServices.isEmpty() ? null : foundServices)
                        .memberships(foundMemberships.isEmpty() ? null : foundMemberships)
                        .trainers(foundTrainers.isEmpty() ? null : foundTrainers)
                        .build();
            }
        }

        sessionStore.put(sessionId, history);
        return ChatResponse.textOnly("Xin lỗi, đã xảy ra lỗi xử lý. Vui lòng thử lại.");
    }

    // ==================== GEMINI API CALL ====================

    private GeminiResponse callGemini(List<GeminiRequest.Content> contents) {
        try {
            GeminiRequest request = new GeminiRequest(
                    contents,
                    GeminiRequest.Content.system(SYSTEM_PROMPT),
                    List.of(new GeminiRequest.Tool(buildFunctionDeclarations())),
                    new GeminiRequest.ToolConfig(new GeminiRequest.FunctionCallingConfig("AUTO"))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

            try {
                log.debug("Gemini request: {}", objectMapper.writeValueAsString(request));
            } catch (Exception ignored) {}

            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    GEMINI_API_URL, HttpMethod.POST, entity, String.class, model, apiKey);

            log.debug("Gemini raw response: {}", rawResponse.getBody());
            return objectMapper.readValue(rawResponse.getBody(), GeminiResponse.class);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Gemini HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi kết nối Gemini API: " + e.getMessage());
        }
    }

    // ==================== TOOL DISPATCH ====================

    /** Wraps tool result: text summary for Gemini + rich data for frontend */
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
            default -> new ToolResult("Tool không tồn tại: " + toolName, List.of());
        };
    }

    // ==================== TOOL IMPLEMENTATIONS ====================

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
                return new ToolResult("Không tìm thấy dịch vụ phù hợp.", List.of());
            }

            NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
            List<Map<String, Object>> data = new ArrayList<>();
            StringBuilder summary = new StringBuilder("Tìm thấy " + services.size() + " dịch vụ:\n");

            for (GymService s : services) {
                // Rich data for frontend cards
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

                // Text summary for Gemini
                summary.append(String.format("- %s: %s VNĐ/buổi, %d phút\n",
                        s.getName(),
                        s.getPrice() != null ? fmt.format(s.getPrice()) : "N/A",
                        s.getDuration() != null ? s.getDuration() : 0));
            }

            return new ToolResult(summary.toString(), data);
        } catch (Exception e) {
            log.error("Error in searchGymServices", e);
            return new ToolResult("Lỗi khi tìm kiếm dịch vụ.", List.of());
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
                return new ToolResult("Không tìm thấy gói membership phù hợp.", List.of());
            }

            NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
            List<Map<String, Object>> data = new ArrayList<>();
            StringBuilder summary = new StringBuilder("Tìm thấy " + packages.size() + " gói membership:\n");

            for (MembershipPackage p : packages) {
                // Rich data for frontend cards
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

                // Text summary for Gemini
                summary.append(String.format("- %s: %s VNĐ, %d ngày%s\n",
                        p.getName(),
                        fmt.format(p.getPrice()),
                        p.getDuration(),
                        p.getDiscount() != null && p.getDiscount() > 0 ? " (giảm " + p.getDiscount() + "%)" : ""));
            }

            return new ToolResult(summary.toString(), data);
        } catch (Exception e) {
            log.error("Error in searchMembershipPackages", e);
            return new ToolResult("Lỗi khi tìm kiếm gói membership.", List.of());
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
                return new ToolResult("Không tìm thấy trainer phù hợp.", List.of());
            }

            List<Map<String, Object>> data = new ArrayList<>();
            StringBuilder summary = new StringBuilder("Tìm thấy " + trainers.size() + " trainer:\n");

            for (User trainer : trainers) {
                // Rich data for frontend cards
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

                // Text summary for Gemini
                String specStr = specs.stream()
                        .map(ts -> ts.getSpecialty().getDisplayName())
                        .collect(Collectors.joining(", "));
                summary.append(String.format("- %s (ID: %d): %d năm kinh nghiệm, chuyên môn: %s\n",
                        trainer.getFullName(), trainer.getId(),
                        trainer.getTotalExperienceYears() != null ? trainer.getTotalExperienceYears() : 0,
                        specStr));
            }

            return new ToolResult(summary.toString(), data);
        } catch (Exception e) {
            log.error("Error in searchTrainers", e);
            return new ToolResult("Lỗi khi tìm kiếm trainer.", List.of());
        }
    }

    private String createTrainerBooking(Map<String, Object> args) {
        try {
            Long trainerId = args.get("trainerId") != null ? ((Number) args.get("trainerId")).longValue() : null;
            String bookingDateStr = (String) args.get("bookingDate");
            String startTimeStr = (String) args.get("startTime");
            String endTimeStr = (String) args.get("endTime");
            String notes = (String) args.get("notes");

            if (trainerId == null) return "Vui lòng cung cấp ID của trainer.";
            if (bookingDateStr == null) return "Vui lòng cung cấp ngày đặt lịch.";
            if (startTimeStr == null || endTimeStr == null) return "Vui lòng cung cấp thời gian bắt đầu và kết thúc.";

            User trainer = userRepository.findById(trainerId).orElse(null);
            if (trainer == null) return "Không tìm thấy trainer với ID " + trainerId + ".";
            if (!trainer.isTrainer() || !trainer.getIsActive()) return "Trainer này hiện không khả dụng.";

            LocalDate bookingDate = LocalDate.parse(bookingDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            if (bookingDate.isBefore(LocalDate.now())) return "Ngày đặt lịch không thể là ngày trong quá khứ.";
            if (!startTime.isBefore(endTime)) return "Thời gian bắt đầu phải trước thời gian kết thúc.";

            List<TrainerBooking> conflicts = trainerBookingRepository.findConflictingBookingsForTrainer(
                    trainerId, bookingDate, startTime, endTime,
                    List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            if (!conflicts.isEmpty()) return "Trainer đã có lịch trong khung giờ này. Vui lòng chọn thời gian khác.";

            return String.format(
                    "✅ Thông tin hợp lệ! Trainer: %s, Ngày: %s, Giờ: %s-%s. " +
                    "Để hoàn tất, vui lòng đăng nhập và xác nhận booking.",
                    trainer.getFullName(),
                    bookingDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    endTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        } catch (Exception e) {
            log.error("Error in createTrainerBooking", e);
            return "Lỗi khi xử lý đặt lịch: " + e.getMessage();
        }
    }

    // ==================== FUNCTION DECLARATIONS ====================

    private List<GeminiRequest.FunctionDeclaration> buildFunctionDeclarations() {
        return List.of(
                new GeminiRequest.FunctionDeclaration(
                        "searchMembershipPackages",
                        "Tìm kiếm gói membership theo từ khóa, thời hạn, hoặc giá tối đa.",
                        Map.of("type", "object", "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "Từ khóa tên gói"),
                                "duration", Map.of("type", "integer", "description", "Thời hạn (ngày)"),
                                "maxPrice", Map.of("type", "number", "description", "Giá tối đa (VNĐ)")
                        ))),
                new GeminiRequest.FunctionDeclaration(
                        "searchGymServices",
                        "Tìm kiếm dịch vụ gym theo tên hoặc danh mục.",
                        Map.of("type", "object", "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "Từ khóa tên dịch vụ"),
                                "categoryName", Map.of("type", "string", "description", "Tên danh mục")
                        ))),
                new GeminiRequest.FunctionDeclaration(
                        "searchTrainers",
                        "Tìm kiếm trainer theo chuyên môn.",
                        Map.of("type", "object", "properties", Map.of(
                                "specialtyName", Map.of("type", "string", "description", "Tên chuyên môn")
                        ))),
                new GeminiRequest.FunctionDeclaration(
                        "createTrainerBooking",
                        "Kiểm tra và xác nhận đặt lịch với trainer.",
                        Map.of("type", "object",
                                "required", List.of("trainerId", "bookingDate", "startTime", "endTime"),
                                "properties", Map.of(
                                        "trainerId", Map.of("type", "integer", "description", "ID trainer"),
                                        "bookingDate", Map.of("type", "string", "description", "Ngày (YYYY-MM-DD)"),
                                        "startTime", Map.of("type", "string", "description", "Giờ bắt đầu (HH:mm)"),
                                        "endTime", Map.of("type", "string", "description", "Giờ kết thúc (HH:mm)"),
                                        "notes", Map.of("type", "string", "description", "Ghi chú")
                                )))
        );
    }
}
