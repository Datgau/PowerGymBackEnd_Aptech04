package com.example.project_backend04.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("🔒 Mã OTP xác minh tài khoản");

        String verifyLink = "http://localhost:5173/verify-email?email=" + to;

        String content = """
<html>
<body style="margin:0; padding:0; background-color:#f4f6f8;">
    <div style="font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display:flex; justify-content:center; align-items:center; padding:40px 0;">
        <div style="background: linear-gradient(135deg, #1e90ff, #00c6ff); color:white; border-radius:15px; max-width:500px; width:100%; padding:30px; box-shadow:0 4px 20px rgba(0,0,0,0.1);">
            <h2 style="margin-top:0; text-align:center;">Xin chào!</h2>
            <p style="text-align:center;">Chúng tôi nhận được yêu cầu đăng nhập/tạo tài khoản từ bạn.</p>
            <p style="text-align:center; font-size:22px; font-weight:bold; margin:25px 0;">
                Mã OTP của bạn: <span style="font-size:28px; background:white; color:#1e90ff; padding:5px 15px; border-radius:8px;">""" + otp + """
</span>
            </p>
            <p style="text-align:center; color:white; opacity:0.85;">Mã có hiệu lực trong 5 phút.</p>
            <hr style="border:0; border-top:1px solid rgba(255,255,255,0.3); margin:25px 0;">
            <p style="text-align:center; font-size:12px; opacity:0.7; margin-top:15px;">
                Nếu bạn không yêu cầu mã OTP này, vui lòng bỏ qua email.
            </p>
        </div>
    </div>
</body>
</html>
""";

        helper.setText(content, true);
        mailSender.send(message);
    }



    // Gửi email đăng ký thành công
    public void sendSuccessRegisterEmail(String to, String username) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("🎉 Đăng ký tài khoản thành công!");

        String content = """
<html>
<body style="margin:0; padding:0; background-color:#f4f6f8;">
    <div style="font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display:flex; justify-content:center; align-items:center; padding:40px 0;">
        <div style="background:#ffffff; color:#333; border-radius:15px; max-width:500px; width:100%; padding:30px; box-shadow:0 4px 20px rgba(0,0,0,0.1);">
            <h2 style="text-align:center; color:#1e90ff;">Xin chào """ + username + """
 👋</h2>
            <p style="text-align:center;">Tài khoản của bạn đã được tạo thành công!</p>
            <p style="text-align:center; font-size:15px; color:#555;">
                Cảm ơn bạn đã đăng ký. Bạn có thể đăng nhập ngay bây giờ và bắt đầu sử dụng dịch vụ.
            </p>
            <div style="text-align:center; margin-top:25px;">
                <a href="https://yourwebsite.com/login" 
                   style="background-color:#1e90ff; color:white; text-decoration:none; padding:10px 25px; border-radius:8px;">
                   Đăng nhập ngay
                </a>
            </div>
            <hr style="border:0; border-top:1px solid #ddd; margin:25px 0;">
            <p style="text-align:center; font-size:12px; color:#888;">
                Nếu bạn không tạo tài khoản này, vui lòng bỏ qua email này.
            </p>
        </div>
    </div>
</body>
</html>
""";

        helper.setText(content, true);
        mailSender.send(message);
    }
}
