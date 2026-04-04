package com.example.project_backend04.config;

import com.example.project_backend04.dto.request.Chat.*;
import com.example.project_backend04.entity.MembershipPackage;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.entity.TrainerSpecialty;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.repository.MembershipPackageRepository;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.repository.TrainerSpecialtyRepository;
import com.example.project_backend04.repository.TrainerBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiToolsConfig {

    private final MembershipPackageRepository membershipPackageRepository;
    private final GymServiceRepository gymServiceRepository;
    private final UserRepository userRepository;
    private final TrainerSpecialtyRepository trainerSpecialtyRepository;
    private final TrainerBookingRepository trainerBookingRepository;


    @Bean
    @Description("Tìm kiếm các gói membership của gym theo từ khóa, thời hạn, hoặc giá tối đa. " +
            "Sử dụng tool này khi khách hàng hỏi về gói thành viên, gói tập, hoặc membership.")
    public Function<SearchMembershipRequest, String> searchMembershipPackagesTool() {
        return request -> {
            try {
                log.info("Searching membership packages with keyword: {}, duration: {}, maxPrice: {}",
                        request.keyword(), request.duration(), request.maxPrice());
                List<MembershipPackage> packages = membershipPackageRepository.findByIsActiveTrue();

                if (request.keyword() != null && !request.keyword().trim().isEmpty()) {
                    String keyword = request.keyword().toLowerCase();
                    packages = packages.stream()
                            .filter(p -> p.getName().toLowerCase().contains(keyword))
                            .collect(Collectors.toList());
                }
                if (request.duration() != null) {
                    packages = packages.stream()
                            .filter(p -> p.getDuration().equals(request.duration()))
                            .collect(Collectors.toList());
                }

                // Filter by maxPrice (price <= maxPrice)
                if (request.maxPrice() != null) {
                    packages = packages.stream()
                            .filter(p -> p.getPrice().compareTo(request.maxPrice()) <= 0)
                            .collect(Collectors.toList());
                }

                // Handle empty results
                if (packages.isEmpty()) {
                    return "Không tìm thấy gói membership phù hợp. Bạn có thể thử tìm với từ khóa khác hoặc tăng ngân sách.";
                }

                // Format results
                StringBuilder result = new StringBuilder("Kết quả tìm kiếm gói membership:\n\n");
                NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

                for (int i = 0; i < packages.size(); i++) {
                    MembershipPackage pkg = packages.get(i);
                    result.append(String.format("%d. %s (ID: %d)\n", i + 1, pkg.getName(), pkg.getId()));
                    result.append(String.format("   - Giá: %s VNĐ", currencyFormat.format(pkg.getPrice())));

                    if (pkg.getDiscount() != null && pkg.getDiscount() > 0) {
                        result.append(String.format(" (Giảm %d%%)", pkg.getDiscount()));
                    }
                    result.append("\n");

                    result.append(String.format("   - Thời hạn: %d ngày\n", pkg.getDuration()));

                    if (pkg.getFeatures() != null && !pkg.getFeatures().isEmpty()) {
                        result.append("   - Ưu đãi:\n");
                        for (String feature : pkg.getFeatures()) {
                            result.append(String.format("     • %s\n", feature));
                        }
                    }

                    result.append("\n");
                }

                return result.toString();

            } catch (Exception e) {
                log.error("Database error in searchMembershipPackagesTool", e);
                return "Xin lỗi, đã có lỗi khi tìm kiếm gói membership. Vui lòng thử lại sau.";
            }
        };
    }

    /**
     * AI Tool for searching gym services
     * Filters by keyword and categoryName
     */
    @Bean
    @Description("Tìm kiếm các dịch vụ gym (lớp học, dịch vụ) theo tên hoặc danh mục. " +
            "Sử dụng tool này khi khách hàng hỏi về các lớp học, dịch vụ như yoga, boxing, personal training.")
    public Function<SearchGymServiceRequest, String> searchGymServiceTool() {
        return request -> {
            try {
                log.info("Searching gym services with keyword: {}, categoryName: {}",
                        request.keyword(), request.categoryName());

                // Query active services with category
                List<GymService> services = gymServiceRepository.findByIsActiveTrueWithImages();

                // Filter by keyword (case-insensitive contains match on name or description)
                if (request.keyword() != null && !request.keyword().trim().isEmpty()) {
                    String keyword = request.keyword().toLowerCase();
                    services = services.stream()
                            .filter(s -> s.getName().toLowerCase().contains(keyword) ||
                                    (s.getDescription() != null && s.getDescription().toLowerCase().contains(keyword)))
                            .collect(Collectors.toList());
                }

                // Filter by categoryName (exact match on category.name)
                if (request.categoryName() != null && !request.categoryName().trim().isEmpty()) {
                    services = services.stream()
                            .filter(s -> s.getCategory() != null &&
                                    s.getCategory().getName().equalsIgnoreCase(request.categoryName()))
                            .collect(Collectors.toList());
                }

                // Handle empty results
                if (services.isEmpty()) {
                    return "Không tìm thấy dịch vụ phù hợp. Bạn có thể xem danh sách đầy đủ các dịch vụ của chúng tôi.";
                }

                // Format results
                StringBuilder result = new StringBuilder("Kết quả tìm kiếm dịch vụ gym:\n\n");
                NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

                for (int i = 0; i < services.size(); i++) {
                    GymService service = services.get(i);
                    result.append(String.format("%d. %s (ID: %d)\n", i + 1, service.getName(), service.getId()));

                    if (service.getDescription() != null && !service.getDescription().isEmpty()) {
                        result.append(String.format("   - Mô tả: %s\n", service.getDescription()));
                    }

                    if (service.getPrice() != null) {
                        result.append(String.format("   - Giá: %s VNĐ/buổi\n",
                                currencyFormat.format(service.getPrice())));
                    }

                    if (service.getDuration() != null) {
                        result.append(String.format("   - Thời lượng: %d phút\n", service.getDuration()));
                    }

                    if (service.getMaxParticipants() != null) {
                        result.append(String.format("   - Sĩ số tối đa: %d người\n", service.getMaxParticipants()));
                    }

                    result.append("\n");
                }

                return result.toString();

            } catch (Exception e) {
                log.error("Database error in searchGymServiceTool", e);
                return "Xin lỗi, đã có lỗi khi tìm kiếm dịch vụ gym. Vui lòng thử lại sau.";
            }
        };
    }

    /**
     * AI Tool for searching trainers
     * Filters by specialtyName
     */
    @Bean
    @Description("Tìm kiếm trainer theo chuyên môn. " +
            "Sử dụng tool này khi khách hàng hỏi về huấn luyện viên, PT, hoặc trainer.")
    public Function<SearchTrainerRequest, String> searchTrainerTool() {
        return request -> {
            try {
                log.info("Searching trainers with specialtyName: {}", request.specialtyName());

                // Get all active trainer specialties
                List<TrainerSpecialty> trainerSpecialties = trainerSpecialtyRepository.findAllActiveTrainerSpecialties();

                // Filter by specialtyName if provided
                if (request.specialtyName() != null && !request.specialtyName().trim().isEmpty()) {
                    String specialtyName = request.specialtyName().toLowerCase();
                    trainerSpecialties = trainerSpecialties.stream()
                            .filter(ts -> ts.getSpecialty() != null &&
                                    (ts.getSpecialty().getName().toLowerCase().contains(specialtyName) ||
                                            ts.getSpecialty().getDisplayName().toLowerCase().contains(specialtyName)))
                            .collect(Collectors.toList());
                }

                // Group by trainer to avoid duplicates
                List<User> trainers = trainerSpecialties.stream()
                        .map(TrainerSpecialty::getUser)
                        .distinct()
                        .sorted((t1, t2) -> {
                            Integer exp1 = t1.getTotalExperienceYears() != null ? t1.getTotalExperienceYears() : 0;
                            Integer exp2 = t2.getTotalExperienceYears() != null ? t2.getTotalExperienceYears() : 0;
                            return exp2.compareTo(exp1); // Sort by experience descending
                        })
                        .collect(Collectors.toList());

                // Handle empty results
                if (trainers.isEmpty()) {
                    return "Không tìm thấy trainer phù hợp. Bạn có thể xem danh sách tất cả trainer của chúng tôi.";
                }

                // Format results
                StringBuilder result = new StringBuilder("Kết quả tìm kiếm trainer:\n\n");

                for (int i = 0; i < trainers.size(); i++) {
                    User trainer = trainers.get(i);
                    result.append(String.format("%d. %s (ID: %d)\n", i + 1, trainer.getFullName(), trainer.getId()));

                    if (trainer.getTotalExperienceYears() != null) {
                        result.append(String.format("   - Kinh nghiệm: %d năm\n", trainer.getTotalExperienceYears()));
                    }

                    // Get trainer's specialties
                    List<TrainerSpecialty> specialties = trainerSpecialties.stream()
                            .filter(ts -> ts.getUser().getId().equals(trainer.getId()))
                            .collect(Collectors.toList());

                    if (!specialties.isEmpty()) {
                        result.append("   - Chuyên môn: ");
                        String specialtiesStr = specialties.stream()
                                .map(ts -> {
                                    String name = ts.getSpecialty().getDisplayName();
                                    if (ts.getExperienceYears() != null) {
                                        return name + " (" + ts.getExperienceYears() + " năm)";
                                    }
                                    return name;
                                })
                                .collect(Collectors.joining(", "));
                        result.append(specialtiesStr).append("\n");
                    }

                    if (trainer.getBio() != null && !trainer.getBio().isEmpty()) {
                        result.append(String.format("   - Giới thiệu: %s\n", trainer.getBio()));
                    }

                    result.append("\n");
                }

                return result.toString();

            } catch (Exception e) {
                log.error("Database error in searchTrainerTool", e);
                return "Xin lỗi, đã có lỗi khi tìm kiếm trainer. Vui lòng thử lại sau.";
            }
        };
    }

    @Bean
    @Description("Tạo lịch đặt buổi tập với trainer. " +
            "QUAN TRỌNG: Các tham số trainerId, bookingDate, startTime, endTime là BẮT BUỘC. " +
            "Bạn PHẢI hỏi khách hàng cung cấp đầy đủ các thông tin này trước khi gọi tool. " +
            "Sử dụng tool này khi khách hàng muốn đặt lịch với trainer.")
    public Function<CreateBookingRequest, String> createTrainerBookingTool() {
        return request -> {
            try {
                log.info("Creating trainer booking for trainerId: {}, date: {}, time: {}-{}",
                        request.trainerId(), request.bookingDate(), request.startTime(), request.endTime());

                // Validate required parameters
                if (request.trainerId() == null) {
                    return "Vui lòng cung cấp ID của trainer bạn muốn đặt lịch.";
                }

                if (request.bookingDate() == null) {
                    return "Vui lòng cung cấp ngày đặt lịch.";
                }

                if (request.startTime() == null || request.endTime() == null) {
                    return "Vui lòng cung cấp thời gian bắt đầu và kết thúc.";
                }

                // Validate trainer exists
                User trainer = userRepository.findById(request.trainerId()).orElse(null);
                if (trainer == null) {
                    return "Không tìm thấy trainer với ID này. Vui lòng kiểm tra lại ID trainer.";
                }

                if (!trainer.isTrainer() || !trainer.getIsActive()) {
                    return "Trainer này hiện không khả dụng.";
                }

                // Validate bookingDate is not in the past
                if (request.bookingDate().isBefore(LocalDate.now())) {
                    return "Ngày đặt lịch không thể là ngày trong quá khứ. Vui lòng chọn ngày từ hôm nay trở đi.";
                }

                // Validate time range
                if (!request.isValidTimeRange()) {
                    return "Thời gian bắt đầu phải trước thời gian kết thúc.";
                }

                if (!request.isMinimumDuration()) {
                    return "Thời lượng buổi tập tối thiểu là 30 phút.";
                }

                if (!request.isMaximumDuration()) {
                    return "Thời lượng buổi tập tối đa là 8 giờ.";
                }

                // Check for time conflicts with existing bookings
                List<TrainerBooking> conflicts = trainerBookingRepository.findConflictingBookingsForTrainer(
                        request.trainerId(),
                        request.bookingDate(),
                        request.startTime(),
                        request.endTime(),
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
                );

                if (!conflicts.isEmpty()) {
                    return "Trainer đã có lịch trong khung giờ này. Vui lòng chọn thời gian khác hoặc chọn trainer khác.";
                }
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

                return String.format("""
                         Thông tin đặt lịch hợp lệ!
                        
                        Trainer: %s (ID: %d)
                        Ngày: %s
                        Thời gian: %s - %s
                        %s
                        
                        Để hoàn tất đặt lịch, bạn cần đăng nhập vào hệ thống và xác nhận booking này.
                        Sau khi đăng nhập, bạn có thể tạo booking thông qua trang "Đặt lịch trainer" hoặc liên hệ với chúng tôi để được hỗ trợ.
                        """,
                        trainer.getFullName(),
                        trainer.getId(),
                        request.bookingDate().format(dateFormatter),
                        request.startTime().format(timeFormatter),
                        request.endTime().format(timeFormatter),
                        request.notes() != null ? "Ghi chú: " + request.notes() : ""
                );

            } catch (Exception e) {
                log.error("Error in createTrainerBookingTool", e);
                return "Xin lỗi, đã có lỗi khi xử lý yêu cầu đặt lịch. Vui lòng thử lại sau.";
            }
        };
    }

    /**
     * ChatClient bean configured with system prompt and default functions
     * This is the main AI chatbot client
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        Bạn là chuyên gia tư vấn gym thông minh của PowerGym.
                        
                        NHIỆM VỤ CỦA BẠN:
                        1. Tìm hiểu nhu cầu của khách hàng
                        2. Tư vấn các gói membership, dịch vụ, và trainer phù hợp
                        3. Hỗ trợ đặt lịch tập với trainer
                        
                        QUY TRÌNH TƯ VẤN:
                        
                        Bước 1: KHI KHÁCH HỎI VỀ GÓI MEMBERSHIP
                        - Dùng searchMembershipPackagesTool để tìm kiếm
                        - Giới thiệu top 3-5 gói phù hợp nhất
                        - Làm nổi bật ưu đãi và tính năng đặc biệt
                        - Format giá tiền với dấu phẩy và đơn vị VNĐ
                        
                        Bước 2: KHI KHÁCH HỎI VỀ DỊCH VỤ GYM
                        - Dùng searchGymServiceTool để tìm kiếm
                        - Giới thiệu các lớp học và dịch vụ phù hợp
                        - Nêu rõ thời lượng, giá cả, và sĩ số
                        
                        Bước 3: KHI KHÁCH HỎI VỀ TRAINER
                        - Dùng searchTrainerTool để tìm kiếm
                        - Giới thiệu trainer theo chuyên môn
                        - Làm nổi bật kinh nghiệm và chứng chỉ
                        
                        Bước 4: KHI KHÁCH MUỐN ĐẶT LỊCH
                        - HỎI ĐẦY ĐỦ thông tin: trainerId, bookingDate (YYYY-MM-DD), startTime (HH:mm), endTime (HH:mm)
                        - Chỉ gọi createTrainerBookingTool KHI ĐÃ CÓ ĐẦY ĐỦ thông tin
                        - Xác nhận lại thông tin với khách trước khi tạo booking
                        
                        LƯU Ý QUAN TRỌNG:
                        - Luôn trả lời bằng tiếng Việt tự nhiên và thân thiện
                        - Format giá tiền với dấu phẩy và đơn vị VNĐ
                        - Khi có nhiều lựa chọn, giới thiệu top 3-5 options phù hợp nhất
                        - Khi đặt lịch, PHẢI hỏi đủ: trainerId, bookingDate, startTime, endTime
                        - Không bao giờ tự ý đặt lịch mà chưa có đầy đủ thông tin
                        - Luôn xác nhận lại thông tin với khách hàng trước khi thực hiện
                        """)
                .defaultFunctions(
                        "searchMembershipPackagesTool",
                        "searchGymServiceTool",
                        "searchTrainerTool",
                        "createTrainerBookingTool"
                )
                .build();
    }
}
