//package com.example.project_backend04.service;
//
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.MimeMessage;
//import lombok.RequiredArgsConstructor;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class EmailService {
//
//    private final JavaMailSender mailSender;
//
//    public void sendOtpEmail(String to, String otp) throws MessagingException {
//
//        MimeMessage message = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//        helper.setTo(to);
//        helper.setSubject(" Mã OTP xác minh email - PowerGym");
//
//        String stringContent = String.format("""
//<!DOCTYPE html>
//<html lang="vi">
//<head>
//    <meta charset="UTF-8">
//    <meta name="viewport" content="width=device-width, initial-scale=1.0">
//</head>
//<body style="margin:0; padding:0; background-color:#f5f7fa; font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
//
//    <table role="presentation" style="width:100%%; border-collapse:collapse; background-color:#f5f7fa;">
//        <tr>
//            <td style="padding:40px 20px;">
//
//                <table role="presentation" style="max-width:600px; margin:0 auto; background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.08);">
//
//                    <!-- Header với gradient -->
//                    <tr>
//                        <td style="background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding:40px 30px; text-align:center;">
//                            <h1 style="margin:0; color:#ffffff; font-size:32px; font-weight:700; letter-spacing:-0.5px;">
//                                💪 PowerGym
//                            </h1>
//                            <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.95); font-size:16px; font-weight:400;">
//                                Nền tảng quản lý phòng gym hàng đầu
//                            </p>
//                        </td>
//                    </tr>
//
//                    <!-- Content -->
//                    <tr>
//                        <td style="padding:50px 40px;">
//
//                            <h2 style="margin:0 0 20px 0; color:#1a202c; font-size:24px; font-weight:600; text-align:center;">
//                                Xác thực tài khoản của bạn
//                            </h2>
//
//                            <p style="margin:0 0 30px 0; color:#4a5568; font-size:16px; line-height:1.6; text-align:center;">
//                                Chúng tôi đã nhận được yêu cầu xác thực địa chỉ email của bạn. Vui lòng sử dụng mã OTP bên dưới để hoàn tất quá trình xác thực.
//                            </p>
//
//                            <!-- OTP Box -->
//                            <table role="presentation" style="width:100%%; border-collapse:collapse; margin:0 0 30px 0;">
//                                <tr>
//                                    <td style="text-align:center;">
//                                        <div style="background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius:12px; padding:3px; display:inline-block;">
//                                            <div style="background:#ffffff; border-radius:10px; padding:20px 40px;">
//                                                <p style="margin:0 0 8px 0; color:#4a5568; font-size:14px; font-weight:600; text-transform:uppercase; letter-spacing:1px;">
//                                                    Mã xác thực OTP
//                                                </p>
//                                                <p style="margin:0; color:#667eea; font-size:36px; font-weight:700; letter-spacing:8px; font-family:'Courier New', monospace;">
//                                                    %s
//                                                </p>
//                                            </div>
//                                        </div>
//                                    </td>
//                                </tr>
//                            </table>
//
//                            <!-- Warning Box -->
//                            <table role="presentation" style="width:100%%; border-collapse:collapse; background-color:#fff5f5; border-left:4px solid #f56565; border-radius:8px; padding:16px; margin:0 0 30px 0;">
//                                <tr>
//                                    <td>
//                                        <p style="margin:0; color:#742a2a; font-size:14px; line-height:1.6;">
//                                            ⏰ <strong>Lưu ý:</strong> Mã OTP này chỉ có hiệu lực trong <strong>5 phút</strong> kể từ khi được gửi. Vui lòng không chia sẻ mã này với bất kỳ ai.
//                                        </p>
//                                    </td>
//                                </tr>
//                            </table>
//
//                            <!-- Info Box -->
//                            <table role="presentation" style="width:100%%; border-collapse:collapse; background-color:#f7fafc; border-radius:8px; padding:20px; margin:0 0 20px 0;">
//                                <tr>
//                                    <td>
//                                        <p style="margin:0 0 12px 0; color:#2d3748; font-size:14px; font-weight:600;">
//                                            🔐 Bảo mật tài khoản
//                                        </p>
//                                        <p style="margin:0; color:#4a5568; font-size:14px; line-height:1.6;">
//                                            Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này và đảm bảo tài khoản của bạn được bảo mật.
//                                        </p>
//                                    </td>
//                                </tr>
//                            </table>
//
//                        </td>
//                    </tr>
//
//                    <!-- Footer -->
//                    <tr>
//                        <td style="background-color:#f7fafc; padding:30px 40px; border-top:1px solid #e2e8f0;">
//                            <p style="margin:0 0 12px 0; color:#718096; font-size:13px; text-align:center; line-height:1.6;">
//                                Email này được gửi tự động, vui lòng không trả lời.<br>
//                                Nếu bạn cần hỗ trợ, vui lòng liên hệ đội ngũ chăm sóc khách hàng của chúng tôi.
//                            </p>
//                            <p style="margin:0; color:#a0aec0; font-size:12px; text-align:center;">
//                                © 2024 PowerGym. All rights reserved.
//                            </p>
//                        </td>
//                    </tr>
//
//                </table>
//
//            </td>
//        </tr>
//    </table>
//
//</body>
//</html>
//""", otp);
//
//        helper.setText(stringContent, true);
//        mailSender.send(message);
//    }
//
//
//
//    // Gửi email đăng ký thành công
//    public void sendSuccessRegisterEmail(String to, String fullName) throws MessagingException {
//
//        MimeMessage message = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//        helper.setTo(to);
//        helper.setSubject("🎉 Đăng ký tài khoản thành công!");
//
//        String stringContent = String.format("""
//        <html>
//            <body style="margin:0; padding:0; background-color:#f4f6f8;">
//                <div style="font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display:flex; justify-content:center; align-items:center; padding:40px 0;">
//                    <div style="background:#ffffff; color:#333; border-radius:15px; max-width:500px; padding:30px; box-shadow:0 4px 20px rgba(0,0,0,0.1);">
//
//                        <h2 style="text-align:center; color:#1e90ff;">
//                            Chào %s 👋
//                        </h2>
//
//                        <p style="text-align:center; font-weight:bold;">
//                            Chào mừng bạn đến với <span style="color:#1e90ff;">PowerGym</span>!
//                        </p>
//
//                        <p style="text-align:center; font-size:15px; color:#555;">
//                            Tài khoản PowerGym của bạn đã được tạo thành công.<br>
//                            Bạn có thể đăng nhập ngay để bắt đầu quản lý và tập luyện hiệu quả hơn.
//                        </p>
//
//                        <div style="text-align:center; margin-top:25px;">
//                            <a href="https://yourwebsite.com/login"
//                               style="background-color:#1e90ff; color:white; text-decoration:none; padding:12px 28px; border-radius:8px; font-weight:bold;">
//                                Đăng nhập PowerGym
//                            </a>
//                        </div>
//
//                        <hr style="border:0; border-top:1px solid #ddd; margin:25px 0;">
//
//                        <p style="text-align:center; font-size:12px; color:#888;">
//                            Nếu bạn không tạo tài khoản PowerGym này, vui lòng bỏ qua email.<br>
//                            Cảm ơn bạn đã tin tưởng và đồng hành cùng <b>PowerGym</b> 💪
//                        </p>
//
//                    </div>
//                </div>
//            </body>
//        </html>
//        """, fullName);
//
//        helper.setText(stringContent, true);
//        mailSender.send(message);
//    }
//
//}

package com.example.project_backend04.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    private void sendEmail(String to, String subject, String htmlContent) throws IOException {

        Email from = new Email(fromEmail);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlContent);

        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        if (response.getStatusCode() >= 400) {
            throw new RuntimeException("SendGrid error: " + response.getBody());
        }
    }

    // ================= OTP EMAIL =================
    public void sendOtpEmail(String to, String otp) throws IOException {

        String stringContent = String.format("""
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0; padding:0; background-color:#f5f7fa; font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
    
    <table role="presentation" style="width:100%%; border-collapse:collapse; background-color:#f5f7fa;">
        <tr>
            <td style="padding:40px 20px;">
                
                <table role="presentation" style="max-width:600px; margin:0 auto; background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.08);">
                    
                    <tr>
                        <td style="background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding:40px 30px; text-align:center;">
                            <h1 style="margin:0; color:#ffffff; font-size:32px; font-weight:700;">
                                💪 PowerGym
                            </h1>
                            <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.95); font-size:16px;">
                                Nền tảng quản lý phòng gym hàng đầu
                            </p>
                        </td>
                    </tr>
                    
                    <tr>
                        <td style="padding:50px 40px;">
                            
                            <h2 style="text-align:center;">
                                Xác thực tài khoản của bạn
                            </h2>
                            
                            <p style="text-align:center;">
                                Vui lòng sử dụng mã OTP bên dưới để hoàn tất xác thực.
                            </p>
                            
                            <table role="presentation" style="width:100%%; margin:20px 0;">
                                <tr>
                                    <td style="text-align:center;">
                                        <div style="background:#ffffff; padding:20px 40px;">
                                            <p style="font-size:36px; font-weight:700; letter-spacing:8px;">
                                                %s
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                            
                            <p style="color:#742a2a; text-align:center;">
                                ⏰ Mã có hiệu lực trong 5 phút.
                            </p>
                            
                        </td>
                    </tr>
                    
                    <tr>
                        <td style="background-color:#f7fafc; padding:20px; text-align:center;">
                            © 2024 PowerGym. All rights reserved.
                        </td>
                    </tr>
                    
                </table>
                
            </td>
        </tr>
    </table>
    
</body>
</html>
""", otp);

        sendEmail(to, "Mã OTP xác minh email - PowerGym", stringContent);
    }

    // ================= SUCCESS REGISTER =================
    public void sendSuccessRegisterEmail(String to, String fullName) throws IOException {

        String stringContent = String.format("""
<html>
<body style="margin:0; padding:0; background-color:#f4f6f8;">
    <div style="display:flex; justify-content:center; padding:40px 0;">
        <div style="background:#ffffff; border-radius:15px; max-width:500px; padding:30px;">
            
            <h2 style="text-align:center; color:#1e90ff;">
                Chào %s 👋
            </h2>

            <p style="text-align:center;">
                Chào mừng bạn đến với <span style="color:#1e90ff;">PowerGym</span>!
            </p>

            <p style="text-align:center;">
                Tài khoản của bạn đã được tạo thành công.
            </p>

            <div style="text-align:center; margin-top:25px;">
                <a href="https://yourwebsite.com/login"
                   style="background-color:#1e90ff; color:white; padding:12px 28px; border-radius:8px;">
                    Đăng nhập PowerGym
                </a>
            </div>

        </div>
    </div>
</body>
</html>
""", fullName);

        sendEmail(to, "🎉 Đăng ký tài khoản thành công!", stringContent);
    }
}
