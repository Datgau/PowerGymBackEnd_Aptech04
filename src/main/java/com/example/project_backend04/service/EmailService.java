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
                                PowerGym
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
                                Mã có hiệu lực trong 5 phút.
                            </p>
                            
                        </td>
                    </tr>
                    
                    <tr>
                        <td style="background-color:#00b4ff; padding:20px; text-align:center;">
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
                        Chào %s
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
        sendEmail(to, " Đăng ký tài khoản thành công!", stringContent);
    }

    // ================= PASSWORD EMAIL =================
    public void sendPasswordEmail(String to, String fullName, String password) throws IOException {

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
                                PowerGym
                            </h1>
                            <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.95); font-size:16px;">
                                Tài khoản của bạn đã được tạo
                            </p>
                        </td>
                    </tr>
                    
                    <tr>
                        <td style="padding:50px 40px;">
                            
                            <h2 style="text-align:center; color:#1a202c;">
                                Chào %s!
                            </h2>
                            
                            <p style="text-align:center; color:#4a5568;">
                                Tài khoản PowerGym của bạn đã được tạo thành công bởi quản trị viên.<br>
                                Dưới đây là thông tin đăng nhập của bạn:
                            </p>
                            
                            <table role="presentation" style="width:100%%; margin:30px 0;">
                                <tr>
                                    <td style="text-align:center;">
                                        <div style="background:#00b4ff; border-radius:12px; padding:20px;">
                                            <p style="margin:0 0 8px 0; color:#4a5568; font-size:14px;">
                                                Email đăng nhập
                                            </p>
                                            <p style="margin:0 0 20px 0; color:#2d3748; font-size:18px; font-weight:600;">
                                                %s
                                            </p>
                                            <p style="margin:0 0 8px 0; color:#4a5568; font-size:14px;">
                                                Mật khẩu tạm thời
                                            </p>
                                            <p style="margin:0; color:#667eea; font-size:24px; font-weight:700; letter-spacing:2px; font-family:'Courier New', monospace;">
                                                %s
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                            
                            <table role="presentation" style="width:100%%; background-color:#fff5f5; border-left:4px solid #f56565; border-radius:8px; padding:16px; margin:0 0 20px 0;">
                                <tr>
                                    <td>
                                        <p style="margin:0; color:#742a2a; font-size:14px;">
                                             <strong>Quan trọng:</strong> Vui lòng đổi mật khẩu ngay sau khi đăng nhập lần đầu để bảo mật tài khoản.
                                        </p>
                                    </td>
                                </tr>
                            </table>

                            <div style="text-align:center; margin-top:30px;">
                                <a href="https://powergym-aptech.netlify.app/login"
                                   style="background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color:white; text-decoration:none; padding:14px 32px; border-radius:8px; font-weight:600; display:inline-block;">
                                    Đăng nhập ngay
                                </a>
                            </div>
                            
                        </td>
                    </tr>
                    
                    <tr>
                        <td style="background-color:#00b4ff; padding:20px; text-align:center; border-top:1px solid #e2e8f0;">
                            <p style="margin:0; color:#718096; font-size:13px;">
                                © 2024 PowerGym. All rights reserved.
                            </p>
                        </td>
                    </tr>
                    
                </table>
                
            </td>
        </tr>
    </table>
    
</body>
</html>
""", fullName, to, password);

        sendEmail(to, "🔑 Thông tin đăng nhập PowerGym", stringContent);
    }
}
