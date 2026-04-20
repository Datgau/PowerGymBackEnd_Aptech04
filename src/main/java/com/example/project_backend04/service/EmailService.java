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
                            Payment Confirmation Successful
                        </p>
                    </td>
                </tr>
                <tr>
                    <td style="padding:45px 40px;">
                        <h2 style="text-align:center; color:#1a202c; margin-bottom:10px;">
                            Hello %s!
                        </h2>
                        <p style="text-align:left; color:#4a5568; line-height:1.8;">
                            PowerGym sincerely thanks you for trusting and using our services.
                        </p>
                        <p style="text-align:left; color:#4a5568; line-height:1.8;">
                            We confirm that your payment transaction has been successfully completed with the following details:
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
                            <tr>
                                <td>
                                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:12px; padding:25px;">
                                        <table width="100%%" cellpadding="8" cellspacing="0">
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0;">
                                                    <strong>Transaction ID:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0;">
                                                    %s
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Service:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Amount:</strong>
                                                </td>
                                                <td style="color:#045668; font-size:18px; font-weight:700; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s VND
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Payment Time:</strong>
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
                                        ✓ Your service has been activated and you can start using it right away.
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <p style="text-align:left; color:#4a5568; line-height:1.8; margin-top:25px;">
                            If you need further assistance or have any questions, please contact us via:
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
                            Once again, thank you for choosing PowerGym. We wish you wonderful experiences and success in achieving your health goals!
                        </p>
                        <p style="text-align:left; color:#2d3748; font-weight:600; margin-top:30px;">
                            Sincerely,<br>
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

        sendEmail(to, "Payment Confirmation Successful - PowerGym", stringContent);
    }

    public void sendCounterRegistrationEmail(String to, String fullName, String registrationId, String serviceName, String amount) throws IOException {

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
                            Service Registration Confirmation
                        </p>
                    </td>
                </tr>
                <tr>
                    <td style="padding:45px 40px;">
                        <h2 style="text-align:center; color:#1a202c; margin-bottom:10px;">
                            Hello %s!
                        </h2>
                        <p style="text-align:left; color:#4a5568; line-height:1.8;">
                            Thank you for registering to use PowerGym services.
                        </p>
                        <p style="text-align:left; color:#4a5568; line-height:1.8;">
                            We have recorded your registration request with the following details:
                        </p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
                            <tr>
                                <td>
                                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:12px; padding:25px;">
                                        <table width="100%%" cellpadding="8" cellspacing="0">
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0;">
                                                    <strong>Registration ID:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0;">
                                                    %s
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Service:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Amount to be paid:</strong>
                                                </td>
                                                <td style="color:#045668; font-size:18px; font-weight:700; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    %s VND
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="color:#718096; font-size:14px; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    <strong>Payment Method:</strong>
                                                </td>
                                                <td style="color:#2d3748; font-size:14px; text-align:right; padding:8px 0; border-top:1px solid #e2e8f0;">
                                                    Payment at counter
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
                                        🔔 Important Notice:
                                    </p>
                                    <p style="margin:0; font-size:14px; color:#742a2a; line-height:1.6;">
                                        Please visit PowerGym directly to complete the payment within <strong>3 days</strong> from the time of registration.
                                    </p>
                                    <p style="margin:8px 0 0 0; font-size:14px; color:#742a2a; line-height:1.6;">
                                        After this period, if you have not completed the payment, the system will automatically cancel your registration.
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <p style="text-align:left; color:#4a5568; line-height:1.8; margin-top:20px;">
                            We encourage you to come early to secure your registration slot and start experiencing the service conveniently.
                        </p>
                        <p style="text-align:left; color:#4a5568; line-height:1.8; margin-top:25px;">
                            If you need support or have any questions, please contact:
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
                        <p style="text-align:left; color:#2d3748; font-weight:600; margin-top:30px;">
                            Sincerely,<br>
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

        sendEmail(to, "Service Registration Confirmation - PowerGym", stringContent);
    }

    public void sendOtpEmail(String to, String otp, String fullName) throws IOException {

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
                            Email Change Verification
                        </p>
                    </td>
                </tr>
                <tr>
                    <td style="padding:45px 40px;">
                        <h2 style="text-align:center; color:#1a202c; margin-bottom:10px;">
                            Hello %s!
                        </h2>
                        <p style="text-align:center; color:#4a5568; line-height:1.6;">
                            You have requested to change the email address for your PowerGym account.
                        </p>
                        <p style="text-align:center; color:#4a5568; line-height:1.6;">
                            Please use the OTP code below to verify your new email:
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
                                        <strong>Note:</strong> This OTP will expire after <strong>10 minutes</strong>.
                                    </p>
                                </td>
                            </tr>
                        </table>
                        <p style="text-align:center; color:#718096; font-size:14px; margin-top:20px;">
                            If you did not request an email change, please ignore this email and contact us immediately.
                        </p>
                        <p style="text-align:center; color:#2d3748; font-weight:600; margin-top:30px;">
                            Sincerely,<br>
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

        sendEmail(to, "Email Change Verification - PowerGym", stringContent);
    }
}
