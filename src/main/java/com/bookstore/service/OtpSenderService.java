package com.bookstore.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

/**
 * Dịch vụ gửi tin nhắn SMS OTP thực tế đến mạng viễn thông (SIM di động người dùng).
 * Tích hợp Twilio API (toàn cầu/Việt Nam) & SpeedSMS API (Việt Nam).
 */
public class OtpSenderService {
    private static final Properties config = new Properties();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    static {
        loadConfig();
    }

    public static void loadConfig() {
        try {
            File file = new File("sms_config.properties");
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    config.load(is);
                }
            } else {
                try (InputStream is = OtpSenderService.class.getClassLoader().getResourceAsStream("sms_config.properties")) {
                    if (is != null) config.load(is);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không thể nạp file sms_config.properties: " + e.getMessage());
        }
    }

    /**
     * Gửi tin nhắn SMS thực tế đến số điện thoại di động của người dùng
     * @param rawPhone Số điện thoại người dùng nhập (ví dụ: 0912345678)
     * @param otpCode Mã OTP 6 chữ số
     * @return true nếu gửi thành công qua cổng dịch vụ viễn thông, false nếu thất bại hoặc chưa cấu hình
     */
    public static boolean sendRealSms(String rawPhone, String otpCode) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            return false;
        }

        loadConfig();
        String phone = normalizePhoneNumber(rawPhone.trim());
        String provider = config.getProperty("SMS_PROVIDER", "TWILIO").trim().toUpperCase();

        if ("TWILIO".equalsIgnoreCase(provider)) {
            return sendTwilioSms(phone, otpCode);
        } else if ("SPEEDSMS".equalsIgnoreCase(provider)) {
            return sendSpeedSms(phone, otpCode);
        }

        return false;
    }

    /**
     * Chuẩn hóa số điện thoại Việt Nam sang định dạng E.164 quốc tế (+84...)
     * Ví dụ: 0912345678 -> +84912345678
     */
    public static String normalizePhoneNumber(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("0")) {
            return "+84" + cleaned.substring(1);
        }
        if (cleaned.startsWith("84") && !cleaned.startsWith("+84")) {
            return "+" + cleaned;
        }
        if (!cleaned.startsWith("+")) {
            return "+84" + cleaned;
        }
        return cleaned;
    }

    /**
     * Kiểm tra xem hệ thống đã được cấu hình SMS Gateway thực tế hay chưa
     */
    public static boolean isConfigured() {
        String provider = config.getProperty("SMS_PROVIDER", "TWILIO").trim().toUpperCase();
        if ("TWILIO".equalsIgnoreCase(provider)) {
            String sid = config.getProperty("TWILIO_ACCOUNT_SID", "").trim();
            String token = config.getProperty("TWILIO_AUTH_TOKEN", "").trim();
            return !sid.isEmpty() && !sid.contains("your_account_sid") && !token.isEmpty() && !token.contains("your_auth_token");
        } else if ("SPEEDSMS".equalsIgnoreCase(provider)) {
            String token = config.getProperty("SPEEDSMS_ACCESS_TOKEN", "").trim();
            return !token.isEmpty() && !token.contains("your_speedsms");
        }
        return false;
    }

    private static boolean sendTwilioSms(String toPhone, String otpCode) {
        String accountSid = config.getProperty("TWILIO_ACCOUNT_SID", "").trim();
        String authToken = config.getProperty("TWILIO_AUTH_TOKEN", "").trim();
        String fromPhone = config.getProperty("TWILIO_PHONE_NUMBER", "").trim();

        if (accountSid.isEmpty() || accountSid.contains("your_account_sid") ||
            authToken.isEmpty() || authToken.contains("your_auth_token")) {
            System.out.println("===============================================================");
            System.out.println("ℹ️ [SMS GATEWAY CHƯA CẤU HÌNH KEY THỰC TẾ]");
            System.out.println("   Số điện thoại nhận: " + toPhone);
            System.out.println("   Mã OTP: " + otpCode);
            System.out.println("   👉 Để SMS tự động gửi thẳng đến SIM điện thoại của bạn:");
            System.out.println("      Hãy mở file 'sms_config.properties' và điền API Key Twilio / SpeedSMS.");
            System.out.println("===============================================================");
            return false;
        }

        try {
            String messageBody = "[Bookora] Ma xac thuc dang ky tai khoan cua ban la: " + otpCode + ". Ma co hieu luc trong 2 phut.";
            String formData = "To=" + URLEncoder.encode(toPhone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromPhone, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            String auth = accountSid + ":" + authToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("===============================================================");
                System.out.println("✅ [TWILIO SMS GATEWAY] ĐÃ GỬI TIN NHẮN THỰC TẾ THÀNH CÔNG!");
                System.out.println("   Đến số điện thoại: " + toPhone);
                System.out.println("   Mã OTP: " + otpCode);
                System.out.println("===============================================================");
                return true;
            } else {
                System.err.println("❌ [TWILIO SMS THẤT BẠI " + response.statusCode() + "]: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ [TWILIO SMS EXCEPTION]: " + e.getMessage());
            return false;
        }
    }

    private static boolean sendSpeedSms(String toPhone, String otpCode) {
        String accessToken = config.getProperty("SPEEDSMS_ACCESS_TOKEN", "").trim();
        if (accessToken.isEmpty() || accessToken.contains("your_speedsms")) {
            return false;
        }
        try {
            String phoneNoPlus = toPhone.startsWith("+") ? toPhone.substring(1) : toPhone;
            String content = "Ma xac thuc Bookora cua ban la: " + otpCode;
            String jsonBody = String.format("{\"to\":[\"%s\"],\"content\":\"%s\",\"sms_type\":2}", phoneNoPlus, content);

            String auth = Base64.getEncoder().encodeToString((accessToken + ":x").getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.speedsms.vn/index.php/sms/send"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("✅ [SPEEDSMS] Đã gửi SMS thực tế thành công đến: " + toPhone);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("❌ [SPEEDSMS EXCEPTION]: " + e.getMessage());
            return false;
        }
    }
}
