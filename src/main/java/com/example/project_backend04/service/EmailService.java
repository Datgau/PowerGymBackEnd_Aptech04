package com.example.project_backend04.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendOtpEmailAsync(String to, String otp) {
        try {
            sendOtpEmail(to, otp);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendSuccessRegisterEmailAsync(String to, String fullName) {
        try {
            sendSuccessRegisterEmail(to, fullName);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendPasswordEmailAsync(String to, String fullName, String password) {
        try {
            sendPasswordEmail(to, fullName, password);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendPaymentConfirmationEmailAsync(String to, String fullName, String transactionId, String serviceName, String amount, String paymentTime) {
        try {
            sendPaymentConfirmationEmail(to, fullName, transactionId, serviceName, amount, paymentTime);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email to {}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendCounterRegistrationEmailAsync(String to, String fullName, String registrationId, String serviceName, String amount) {
        try {
            sendCounterRegistrationEmail(to, fullName, registrationId, serviceName, amount);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send counter registration email to {}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }

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

    // ================= OTP EMAIL ================= (SYNC - for internal use)
    public void sendOtpEmail(String to, String otp) throws IOException {

        String stringContent = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            
            <body style="margin:0; padding:0; background:#f4f6f8; font-family:Segoe UI,Arial,sans-serif;">
            
            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8; padding:40px 0;">
                <tr>
                    <td align="center">
            
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.08);">
                                        <tr>
                                <td style="background:linear-gradient(135deg,#045668,#00b4ff); padding:40px; text-align:center;">
                                    <h1 style="margin:0; color:white; font-size:32px;">PowerGym</h1>
                                    <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.9); font-size:16px;">
                                        Account Verification
                                    </p>
                                </td>
                            </tr>
                                        <tr>
                                <td style="padding:45px 40px;">
            
                                    <h2 style="text-align:center; color:#1a202c;">
                                        Verify Your Account
                                    </h2>
            
                                    <p style="text-align:center; color:#4a5568; line-height:1.6;">
                                        Please use the OTP code below to complete your verification.
                                    </p>
            
                                    <!-- OTP BOX -->
                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
                                        <tr>
                                            <td align="center">
            
                                                <div style="background:#f8fafc;
            border:1px solid #e2e8f0;
            border-radius:12px;
            padding:25px 40px;
            display:inline-block;">
            
                                                    <p style="
            font-size:36px;
            font-weight:700;
            letter-spacing:10px;
            margin:0;
            color:#045668;
            font-family:Courier New,monospace;">
                                                        %s
                                                    </p>
            
                                                </div>
            
                                            </td>
                                        </tr>
                                    </table>
                                    <p style="text-align:center; color:#718096; font-size:14px;">
                                        This code will expire in <strong>5 minutes</strong>.
                                    </p>
            
                                    <p style="text-align:center; color:#a0aec0; font-size:13px; margin-top:20px;">
                                        If you did not request this code, please ignore this email.
                                    </p>
            
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
            </body>
            </html>
""", otp);

        sendEmail(to, "Email verify otp - PowerGym", stringContent);
    }

    // Reset Password
    public void sendResetPasswordEmail(String to, String resetLink) throws IOException {

        String htmlContent = String.format("""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>

        <body style="margin:0; padding:0; background:#f4f6f8; font-family:Segoe UI,Arial,sans-serif;">

        <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8; padding:40px 0;">
            <tr>
                <td align="center">

                    <table width="600" cellpadding="0" cellspacing="0"
                           style="background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.08);">
                        <tr>
                            <td style="background:linear-gradient(135deg,#045668,#00b4ff); padding:40px; text-align:center;">
                                <h1 style="margin:0; color:white; font-size:32px;">PowerGym</h1>
                                <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.9); font-size:16px;">
                                    Password Reset Request
                                </p>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding:45px 40px;">

                                <h2 style="text-align:center; color:#1a202c;">
                                    Reset Your Password
                                </h2>

                                <p style="text-align:center; color:#4a5568; line-height:1.6;">
                                    We received a request to reset your password.
                                    Click the button below to set a new password.
                                </p>
                                <table width="100%%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
                                    <tr>
                                        <td align="center">

                                            <a href="%s"
                                               style="
                                                background:linear-gradient(135deg,#045668,#00b4ff);
                                                color:white;
                                                padding:14px 28px;
                                                border-radius:10px;
                                                text-decoration:none;
                                                font-weight:600;
                                                display:inline-block;
                                                font-size:16px;">
                                                Reset Password
                                            </a>

                                        </td>
                                    </tr>
                                </table>

                                <p style="text-align:center; color:#718096; font-size:14px;">
                                    If the button doesn't work, copy and paste this link:
                                </p>

                                <p style="text-align:center; word-break:break-all; font-size:13px; color:#3182ce;">
                                    %s
                                </p>

                                <p style="text-align:center; color:#718096; font-size:14px; margin-top:20px;">
                                    This link will expire in <strong>15 minutes</strong>.
                                </p>

                                <p style="text-align:center; color:#a0aec0; font-size:13px; margin-top:20px;">
                                    If you did not request this, please ignore this email.
                                </p>

                            </td>
                        </tr>

                    </table>

                </td>
            </tr>
        </table>

        </body>
        </html>
        """, resetLink, resetLink);

        sendEmail(to, "Reset your password - PowerGym", htmlContent);
    }
    // ================= SUCCESS REGISTER ================= (SYNC - for internal use)
    public void sendSuccessRegisterEmail(String to, String fullName) throws IOException {

        String stringContent = String.format("""
                <!DOCTYPE html>
                       <html lang="en">
                       <head>
                           <meta charset="UTF-8">
                       </head>
                       <body style="margin:0; padding:0; background-color:#f4f6f8;">
                       <div style="display:flex; justify-content:center; padding:40px 0;">
                           <div style="background:#ffffff; border-radius:15px; max-width:500px; padding:30px;">
                               <h2 style="text-align:center; color:#1e90ff;">
                                   Hello %s
                               </h2>
                               <p style="text-align:center;">
                                   Welcome to <span style="color:#1e90ff;">PowerGym</span>!
                               </p>
                               <p style="text-align:center;">
                                   Your account has been successfully created.
                               </p>
                               <div style="text-align:center; margin-top:25px;">
                                   <a href="https://yourwebsite.com/login"
                                      style="background-color:#1e90ff; color:white; padding:12px 28px; border-radius:8px; text-decoration:none;">
                                       Login to PowerGym
                                   </a>
                               </div>
                           </div>
                       </div>
                       </body>
                       </html>
        """, fullName);
        sendEmail(to, " Register Successful!", stringContent);
    }

    // ================= PASSWORD EMAIL ================= (SYNC - for internal use)
    public void sendPasswordEmail(String to, String fullName, String password) throws IOException {

        String stringContent = String.format("""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0; padding:0; background:#f4f6f8; font-family:Segoe UI,Arial,sans-serif;">
<table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8; padding:40px 0;">
    <tr>
        <td align="center">
            <table width="600" cellpadding="0" cellspacing="0"
                   style="background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.08);">
                <tr>
                    <td style="background:linear-gradient(135deg,#045668,#00b4ff); padding:40px; text-align:center;">
                        <h1 style="margin:0; color:white; font-size:32px;">PowerGym</h1>
                        <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.9); font-size:16px;">
                            Your account has been created
                        </p>
                    </td>
                </tr>
                <tr>
                    <td style="padding:45px 40px;">
                        <h2 style="text-align:center; color:#1a202c; margin-bottom:10px;">
                            Hello %s!
                        </h2>
                        <p style="text-align:center; color:#4a5568; line-height:1.6;">
                            Your <strong>PowerGym</strong> account has been created by the administrator.<br>
                            Here are your login details:
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
                            <tr>
                                <td align="center">
                                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:12px; padding:25px;">
                                        <p style="margin:0 0 6px 0; font-size:13px; color:#718096;">
                                            Login Email
                                        </p>
                                        <p style="margin:0 0 18px 0; font-size:18px; font-weight:600; color:#2d3748;">
                                            %s
                                        </p>
                                        <p style="margin:0 0 6px 0; font-size:13px; color:#718096;">
                                            Temporary Password
                                        </p>
                                        <p style="margin:0; font-size:22px; font-weight:700; color:#045668; letter-spacing:2px; font-family:Courier New,monospace;">
                                            %s
                                        </p>
                                    </div>
                                </td>
                            </tr>
                        </table>
                        <table width="100%%" style="background:#fff5f5; border-left:4px solid #ff6b6b; border-radius:6px;">
                            <tr>
                                <td style="padding:14px;">
                                    <p style="margin:0; font-size:14px; color:#742a2a;">
                                        <strong>Important:</strong> Please change your password after your first login to secure your account.
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <div style="text-align:center; margin-top:35px;">
                            <a href="https://powergym-aptech.netlify.app/login"
                               style="background:linear-gradient(135deg,#045668,#00b4ff);
color:white;
text-decoration:none;
padding:14px 34px;
border-radius:8px;
font-weight:600;
display:inline-block;
font-size:15px;">
                                Login to PowerGym
                            </a>
                        </div>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>

</body>
</html>
""", fullName, to, password);

        sendEmail(to, "PowerGym login information", stringContent);
    }

    // ================= PAYMENT CONFIRMATION EMAIL ================= (SYNC - for internal use)
    public void sendPaymentConfirmationEmail(String to, String fullName, String transactionId, String serviceName, String amount, String paymentTime) throws IOException {

        String stringContent = String.format("""
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0; padding:0; background:#f4f6f8; font-family:Segoe UI,Arial,sans-serif;">
<table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8; padding:40px 0;">
    <tr>
        <td align="center">
            <table width="600" cellpadding="0" cellspacing="0"
                   style="background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.08);">
                <tr>
                    <td style="background:linear-gradient(135deg,#045668,#00b4ff); padding:40px; text-align:center;">
                        <h1 style="margin:0; color:white; font-size:32px;">PowerGym</h1>
                        <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.9); font-size:16px;">
                            Xác nhận thanh toán thành công
                        </p>
                    </td>
                </tr>
                <tr>
                    <td style="padding:45px 40px;">
                        <h2 style="text-align:center; color:#1a202c; margin-bottom:10px;">
                            Xin chào %s!
                        </h2>
                        <p style="text-align:left; color:#4a5568; line-height:1.8;">
                            PowerGym xin chân thành cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của chúng tôi.
                        </p>
                        <p style="text-align:left; color:#4a5568; line-height:1.8;">
                            Chúng tôi xác nhận rằng giao dịch thanh toán của bạn đã được thực hiện thành công với thông tin chi tiết như sau:
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
                            <tr>
                                <td>
                                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:12px; padding:25px;">
                                        <table width="100%%" cellpadding="8" cellspacing="0">
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0;">
                                                    <strong>Mã giao dịch:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0;">
                                                    %s
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Dịch vụ:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Số tiền:</strong>
                                                </td>
                                                <td style="color:#045668; font-size:18px; font-weight:700; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s VNĐ
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Thời gian thanh toán:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s
                                                </td>
                                            </tr>
                                        </table>
                                    </div>
                                </td>
                            </tr>
                        </table>
                        <table width="100%%" style="background:#f0fdf4; border-left:4px solid #10b981; border-radius:6px; margin:20px 0;">
                            <tr>
                                <td style="padding:14px;">
                                    <p style="margin:0; font-size:14px; color:#065f46;">
                                        ✓ Dịch vụ của bạn đã được kích hoạt và bạn có thể bắt đầu sử dụng ngay từ bây giờ.
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <p style="text-align:left; color:#4a5568; line-height:1.8; margin-top:25px;">
                            Nếu bạn cần hỗ trợ thêm hoặc có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi qua:
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:15px 0;">
                            <tr>
                                <td style="padding:8px 0;">
                                    <p style="margin:0; color:#4a5568; font-size:14px;">
                                        📧 Email: <a href="mailto:support@powergym.com" style="color:#045668; text-decoration:none;">support@powergym.com</a>
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:8px 0;">
                                    <p style="margin:0; color:#4a5568; font-size:14px;">
                                        📞 Hotline: <a href="tel:0789179033" style="color:#045668; text-decoration:none;">0789179033</a>
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <p style="text-align:left; color:#4a5568; line-height:1.8; margin-top:25px;">
                            Một lần nữa, cảm ơn bạn đã lựa chọn PowerGym. Chúc bạn có những trải nghiệm tuyệt vời và đạt được mục tiêu sức khỏe của mình!
                        </p>
                        <p style="text-align:left; color:#2d3748; font-weight:600; margin-top:30px;">
                            Trân trọng,<br>
                            <span style="color:#045668;">PowerGym Team</span>
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
""", fullName, transactionId, serviceName, amount, paymentTime);

        sendEmail(to, "Xác nhận thanh toán thành công - PowerGym", stringContent);
    }

    // ================= COUNTER REGISTRATION EMAIL ================= (SYNC - for internal use)
    public void sendCounterRegistrationEmail(String to, String fullName, String registrationId, String serviceName, String amount) throws IOException {

        String stringContent = String.format("""
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0; padding:0; background:#f4f6f8; font-family:Segoe UI,Arial,sans-serif;">
<table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8; padding:40px 0;">
    <tr>
        <td align="center">
            <table width="600" cellpadding="0" cellspacing="0"
                   style="background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.08);">
                <tr>
                    <td style="background:linear-gradient(135deg,#045668,#00b4ff); padding:40px; text-align:center;">
                        <h1 style="margin:0; color:white; font-size:32px;">PowerGym</h1>
                        <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.9); font-size:16px;">
                            Xác nhận đăng ký dịch vụ
                        </p>
                    </td>
                </tr>
                <tr>
                    <td style="padding:45px 40px;">
                        <h2 style="text-align:center; color:#1a202c; margin-bottom:10px;">
                            Xin chào %s!
                        </h2>
                        <p style="text-align:left; color:#4a5568; line-height:1.8;">
                            Cảm ơn bạn đã đăng ký sử dụng dịch vụ tại PowerGym.
                        </p>
                        <p style="text-align:left; color:#4a5568; line-height:1.8;">
                            Chúng tôi đã ghi nhận yêu cầu đăng ký của bạn với thông tin như sau:
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
                            <tr>
                                <td>
                                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:12px; padding:25px;">
                                        <table width="100%%" cellpadding="8" cellspacing="0">
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0;">
                                                    <strong>Mã đăng ký:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0;">
                                                    %s
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Dịch vụ:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Số tiền cần thanh toán:</strong>
                                                </td>
                                                <td style="color:#045668; font-size:18px; font-weight:700; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s VNĐ
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Hình thức thanh toán:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    Thanh toán tại quầy
                                                </td>
                                            </tr>
                                        </table>
                                    </div>
                                </td>
                            </tr>
                        </table>
                        <table width="100%%" style="background:#fff5f5; border-left:4px solid #ff6b6b; border-radius:6px; margin:20px 0;">
                            <tr>
                                <td style="padding:18px;">
                                    <p style="margin:0 0 8px 0; font-size:15px; color:#742a2a; font-weight:600;">
                                        🔔 Lưu ý quan trọng:
                                    </p>
                                    <p style="margin:0; font-size:14px; color:#742a2a; line-height:1.6;">
                                        Vui lòng đến trực tiếp phòng tập PowerGym để hoàn tất thanh toán trong vòng <strong>3 ngày</strong> kể từ thời điểm đăng ký.
                                    </p>
                                    <p style="margin:8px 0 0 0; font-size:14px; color:#742a2a; line-height:1.6;">
                                        Sau thời hạn trên, nếu bạn chưa hoàn tất thanh toán, hệ thống sẽ tự động hủy đăng ký của bạn.
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <p style="text-align:left; color:#4a5568; line-height:1.8; margin-top:20px;">
                            Chúng tôi khuyến khích bạn đến sớm để đảm bảo giữ được suất đăng ký và bắt đầu trải nghiệm dịch vụ một cách thuận lợi nhất.
                        </p>
                        <p style="text-align:left; color:#4a5568; line-height:1.8; margin-top:25px;">
                            Nếu bạn cần hỗ trợ hoặc có bất kỳ câu hỏi nào, vui lòng liên hệ:
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:15px 0;">
                            <tr>
                                <td style="padding:8px 0;">
                                    <p style="margin:0; color:#4a5568; font-size:14px;">
                                        📧 Email: <a href="mailto:support@powergym.com" style="color:#045668; text-decoration:none;">support@powergym.com</a>
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:8px 0;">
                                    <p style="margin:0; color:#4a5568; font-size:14px;">
                                        📞 Hotline: <a href="tel:0789179033" style="color:#045668; text-decoration:none;">0789179033</a>
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <p style="text-align:left; color:#4a5568; line-height:1.8; margin-top:25px;">
                            Rất mong được chào đón bạn tại PowerGym!
                        </p>
                        <p style="text-align:left; color:#2d3748; font-weight:600; margin-top:30px;">
                            Trân trọng,<br>
                            <span style="color:#045668;">PowerGym Team</span>
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
""", fullName, registrationId, serviceName, amount);

        sendEmail(to, "Xác nhận đăng ký dịch vụ - PowerGym", stringContent);
    }

    // ================= EMAIL CHANGE OTP ================= (SYNC - for internal use)
    public void sendOtpEmail(String to, String otp, String fullName) throws IOException {

        String stringContent = String.format("""
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0; padding:0; background:#f4f6f8; font-family:Segoe UI,Arial,sans-serif;">
<table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8; padding:40px 0;">
    <tr>
        <td align="center">
            <table width="600" cellpadding="0" cellspacing="0"
                   style="background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.08);">
                <tr>
                    <td style="background:linear-gradient(135deg,#045668,#00b4ff); padding:40px; text-align:center;">
                        <h1 style="margin:0; color:white; font-size:32px;">PowerGym</h1>
                        <p style="margin:10px 0 0 0; color:rgba(255,255,255,0.9); font-size:16px;">
                            Xác thực thay đổi email
                        </p>
                    </td>
                </tr>
                <tr>
                    <td style="padding:45px 40px;">
                        <h2 style="text-align:center; color:#1a202c; margin-bottom:10px;">
                            Xin chào %s!
                        </h2>
                        <p style="text-align:center; color:#4a5568; line-height:1.6;">
                            Bạn đã yêu cầu thay đổi địa chỉ email cho tài khoản PowerGym của mình.
                        </p>
                        <p style="text-align:center; color:#4a5568; line-height:1.6;">
                            Vui lòng sử dụng mã OTP bên dưới để xác thực email mới:
                        </p>
                        <!-- OTP BOX -->
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
                            <tr>
                                <td align="center">
                                    <div style="background:#f8fafc;
                                                border:2px solid #0066ff;
                                                border-radius:12px;
                                                padding:25px 40px;
                                                display:inline-block;">
                                        <p style="font-size:36px;
                                                   font-weight:700;
                                                   letter-spacing:10px;
                                                   margin:0;
                                                   color:#045668;
                                                   font-family:Courier New,monospace;">
                                            %s
                                        </p>
                                    </div>
                                </td>
                            </tr>
                        </table>
                        <table width="100%%" style="background:#fff5f5; border-left:4px solid #ff6b6b; border-radius:6px; margin:20px 0;">
                            <tr>
                                <td style="padding:14px;">
                                    <p style="margin:0; font-size:14px; color:#742a2a;">
                                        <strong>Lưu ý:</strong> Mã OTP này sẽ hết hạn sau <strong>10 phút</strong>.
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <p style="text-align:center; color:#718096; font-size:14px; margin-top:20px;">
                            Nếu bạn không yêu cầu thay đổi email, vui lòng bỏ qua email này và liên hệ với chúng tôi ngay lập tức.
                        </p>
                        <p style="text-align:center; color:#2d3748; font-weight:600; margin-top:30px;">
                            Trân trọng,<br>
                            <span style="color:#045668;">PowerGym Team</span>
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
""", fullName, otp);

        sendEmail(to, "Xác thực thay đổi email - PowerGym", stringContent);
    }
}
