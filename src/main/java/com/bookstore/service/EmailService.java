package com.bookstore.service;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Dịch vụ gửi Email thực tế qua Gmail SMTP (SSL Port 465).
 * Không phụ thuộc thư viện ngoài, tương thích chuẩn mọi phiên bản Java.
 */
public class EmailService {
    private static final Properties config = new Properties();

    static {
        loadConfig();
    }

    public static void loadConfig() {
        try {
            File file = new File("email_config.properties");
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    config.load(is);
                }
            } else {
                try (InputStream is = EmailService.class.getClassLoader().getResourceAsStream("email_config.properties")) {
                    if (is != null) config.load(is);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không thể đọc email_config.properties: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra xem đã cấu hình tài khoản gửi Email thực tế chưa
     */
    public static boolean isConfigured() {
        loadConfig();
        String sender = config.getProperty("EMAIL_SENDER", "").trim();
        String pass = config.getProperty("EMAIL_APP_PASSWORD", "").trim();
        return !sender.isEmpty() && !sender.contains("your_gmail") && !pass.isEmpty() && !pass.contains("your_app_password");
    }

    /**
     * Gửi email mã OTP thực tế đến hòm thư người nhận
     * @param toEmail Địa chỉ email người nhận (ví dụ: nguyenoanhh0912@gmail.com)
     * @param otpCode Mã OTP gồm 6 chữ số
     * @param fullName Họ tên người nhận
     * @return true nếu gửi thành công vào hòm thư thật, false nếu thất bại hoặc chưa cấu hình
     */
    public static boolean sendOtpEmail(String toEmail, String otpCode, String fullName) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return false;
        }

        loadConfig();
        if (!isConfigured()) {
            System.out.println("===============================================================");
            System.out.println("ℹ️ [EMAIL THẬT CHƯA CẤU HÌNH TÀI KHOẢN GMAIL GỬI]");
            System.out.println("   Hòm thư người nhận: " + toEmail);
            System.out.println("   Mã OTP: " + otpCode);
            System.out.println("   👉 Để email gửi THẬT 100% đến hòm thư Gmail của bạn:");
            System.out.println("      Hãy mở file 'email_config.properties' và điền:");
            System.out.println("      EMAIL_SENDER=dia_chi_gmail_cua_ban@gmail.com");
            System.out.println("      EMAIL_APP_PASSWORD=16_ky_tu_mat_khau_ung_dung");
            System.out.println("      (Tạo mật khẩu ứng dụng tại: https://myaccount.google.com/apppasswords)");
            System.out.println("===============================================================");
            return false;
        }

        String host = config.getProperty("EMAIL_SMTP_HOST", "smtp.gmail.com").trim();
        int port = 465;
        try {
            port = Integer.parseInt(config.getProperty("EMAIL_SMTP_PORT", "465").trim());
        } catch (Exception ignored) {}

        String sender = config.getProperty("EMAIL_SENDER", "").trim();
        String appPassword = config.getProperty("EMAIL_APP_PASSWORD", "").trim().replace(" ", "");
        String fromName = config.getProperty("EMAIL_FROM_NAME", "Bookora - Tiệm Sách Tri Thức").trim();

        String subject = "[Bookora] Mã xác thực OTP đăng ký tài khoản: " + otpCode;
        String htmlBody = buildOtpEmailHtml(fullName, otpCode);

        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.setSoTimeout(15000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                // 1. Đọc chào mừng ban đầu từ Gmail
                String resp = reader.readLine();
                if (resp == null || !resp.startsWith("220")) {
                    System.err.println("❌ SMTP lỗi chào mừng: " + resp);
                    return false;
                }

                // 2. Gửi EHLO
                writer.print("EHLO localhost\r\n");
                writer.flush();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("250 ")) break;
                }

                // 3. Đăng nhập AUTH LOGIN
                writer.print("AUTH LOGIN\r\n");
                writer.flush();
                line = reader.readLine();
                if (line == null || !line.startsWith("334")) {
                    System.err.println("❌ SMTP lỗi AUTH LOGIN: " + line);
                    return false;
                }

                // 4. Gửi Username Base64
                writer.print(Base64.getEncoder().encodeToString(sender.getBytes(StandardCharsets.UTF_8)) + "\r\n");
                writer.flush();
                line = reader.readLine();
                if (line == null || !line.startsWith("334")) {
                    System.err.println("❌ SMTP lỗi Username: " + line);
                    return false;
                }

                // 5. Gửi Password Base64
                writer.print(Base64.getEncoder().encodeToString(appPassword.getBytes(StandardCharsets.UTF_8)) + "\r\n");
                writer.flush();
                line = reader.readLine();
                if (line == null || !line.startsWith("235")) {
                    System.err.println("❌ SMTP Đăng nhập thất bại (Kiểm tra lại Mật khẩu ứng dụng Gmail): " + line);
                    return false;
                }

                // 6. MAIL FROM
                writer.print("MAIL FROM:<" + sender + ">\r\n");
                writer.flush();
                line = reader.readLine();
                if (line == null || !line.startsWith("250")) {
                    System.err.println("❌ SMTP lỗi MAIL FROM: " + line);
                    return false;
                }

                // 7. RCPT TO
                writer.print("RCPT TO:<" + toEmail.trim() + ">\r\n");
                writer.flush();
                line = reader.readLine();
                if (line == null || !line.startsWith("250")) {
                    System.err.println("❌ SMTP lỗi RCPT TO: " + line);
                    return false;
                }

                // 8. DATA
                writer.print("DATA\r\n");
                writer.flush();
                line = reader.readLine();
                if (line == null || !line.startsWith("354")) {
                    System.err.println("❌ SMTP lỗi DATA: " + line);
                    return false;
                }

                // 9. Gửi Header & Nội dung Email
                String encodedSubject = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8)) + "?=";
                String encodedFromName = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(fromName.getBytes(StandardCharsets.UTF_8)) + "?=";

                StringBuilder msg = new StringBuilder();
                msg.append("From: ").append(encodedFromName).append(" <").append(sender).append(">\r\n");
                msg.append("To: <").append(toEmail.trim()).append(">\r\n");
                msg.append("Subject: ").append(encodedSubject).append("\r\n");
                msg.append("MIME-Version: 1.0\r\n");
                msg.append("Content-Type: text/html; charset=UTF-8\r\n");
                msg.append("Content-Transfer-Encoding: base64\r\n");
                msg.append("\r\n");
                msg.append(Base64.getEncoder().encodeToString(htmlBody.getBytes(StandardCharsets.UTF_8)));
                msg.append("\r\n.\r\n");

                writer.print(msg.toString());
                writer.flush();

                line = reader.readLine();
                if (line != null && line.startsWith("250")) {
                    System.out.println("===============================================================");
                    System.out.println("✅ [GMAIL SMTP THẬT] ĐÃ GỬI THÀNH CÔNG EMAIL THỰC TẾ!");
                    System.out.println("   Đến hòm thư: " + toEmail);
                    System.out.println("   Mã OTP: " + otpCode);
                    System.out.println("===============================================================");

                    writer.print("QUIT\r\n");
                    writer.flush();
                    return true;
                } else {
                    System.err.println("❌ SMTP lỗi gửi nội dung: " + line);
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [EMAIL SMTP EXCEPTION]: " + e.getMessage());
            return false;
        }
    }

    private static String buildOtpEmailHtml(String name, String otp) {
        String displayName = (name != null && !name.trim().isEmpty()) ? name : "Quý khách";
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif; background: #f8fafc; padding: 30px; margin: 0;'>"
                + "  <div style='max-width: 520px; margin: 0 auto; background: white; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;'>"
                + "    <div style='background: linear-gradient(135deg, #1e1b4b 0%, #312e81 100%); color: white; padding: 28px 24px; text-align: center;'>"
                + "      <h1 style='margin: 0; font-size: 24px; letter-spacing: 1px;'>📖 Bookora</h1>"
                + "      <p style='margin: 6px 0 0; font-size: 13px; color: #c7d2fe;'>Không Gian Tri Thức & Nghệ Thuật Sống</p>"
                + "    </div>"
                + "    <div style='padding: 32px 28px; color: #1e293b; line-height: 1.6;'>"
                + "      <h2 style='font-size: 18px; margin-top: 0; color: #1e1b4b;'>Xác thực đăng ký tài khoản</h2>"
                + "      <p>Xin chào <strong>" + displayName + "</strong>,</p>"
                + "      <p>Bạn vừa thực hiện đăng ký tài khoản tại <strong>Bookora</strong>. Dưới đây là mã xác thực OTP của bạn:</p>"
                + "      <div style='background: #f1f5f9; border: 2px dashed #6366f1; border-radius: 10px; text-align: center; padding: 18px; margin: 24px 0;'>"
                + "        <div style='font-size: 32px; font-weight: 800; letter-spacing: 10px; color: #4338ca;'>" + otp + "</div>"
                + "        <div style='font-size: 12px; color: #64748b; margin-top: 6px;'>Mã có hiệu lực trong <strong>2 phút (120 giây)</strong></div>"
                + "      </div>"
                + "      <p style='font-size: 13px; color: #64748b;'>⚠️ Vì lý do an toàn bảo mật, vui lòng không cung cấp mã này cho bất kỳ ai khác.</p>"
                + "      <p style='font-size: 13px; color: #64748b;'>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>"
                + "    </div>"
                + "    <div style='background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;'>"
                + "      © 2026 Bookora Bookstore. Mọi quyền được bảo lưu."
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("🔍 KIỂM TRA CẤU HÌNH GỬI EMAIL THỰC TẾ GMAIL SMTP");
        System.out.println("===============================================================");
        loadConfig();
        String sender = config.getProperty("EMAIL_SENDER", "").trim();
        String target = args.length > 0 ? args[0] : (sender.isEmpty() ? "nguyenoanhh0912@gmail.com" : sender);

        if (!isConfigured()) {
            System.out.println("❌ Chưa cấu hình EMAIL_SENDER hoặc EMAIL_APP_PASSWORD trong email_config.properties!");
            System.out.println("   Vui lòng mở file 'email_config.properties' và nhập Mật khẩu ứng dụng 16 ký tự.");
            return;
        }

        System.out.println("📧 Đang thử nghiệm gửi email OTP thật đến: " + target + " ...");
        boolean success = sendOtpEmail(target, "888999", "Khách Hàng Bookora");
        if (success) {
            System.out.println("🎉 CHÚC MỪNG! Email thực tế đã được gửi thành công đến " + target + "!");
            System.out.println("   Hãy mở hộp thư đến (Inbox hoặc Hộp thư rác/Spam) để xem mã OTP.");
        } else {
            System.out.println("❌ Gửi email thất bại! Vui lòng kiểm tra lại địa chỉ Gmail và Mật khẩu ứng dụng.");
        }
        System.out.println("===============================================================");
    }
}
