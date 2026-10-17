package com.bookstore.service;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Tiện ích kiểm tra định dạng và tính hợp lệ của địa chỉ Email.
 * Ngăn chặn tuyệt đối việc gửi OTP tới các email sai định dạng, gõ nhầm đuôi (.colm, .con...)
 * hoặc tên miền không tồn tại, tránh việc Gmail báo lỗi trả về hòm thư người gửi.
 */
public class EmailValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,24}$"
    );

    // Danh sách các đuôi tên miền (TLD) gõ nhầm phổ biến
    private static final Set<String> INVALID_TLDS = new HashSet<>(Arrays.asList(
            "colm", "con", "comm", "cmo", "cpm", "coom", "cm", "c0m",
            "gmai", "gamil", "gmial", "gmaill", "yaho", "yahooo", "hotmial"
    ));

    // Danh sách các tên miền gõ sai nhà cung cấp email phổ biến
    private static final Set<String> TYPO_DOMAINS = new HashSet<>(Arrays.asList(
            "gmai.com", "gamil.com", "gmial.com", "gmaill.com", "gmail.con", "gmail.colm", "gmail.cm",
            "yaho.com", "yahooo.com", "yahoo.con", "yahoo.colm",
            "hotmial.com", "hotmaill.com", "outlok.com", "outllok.com"
    ));

    // Danh sách các tên miền phổ biến luôn hợp lệ (bỏ qua DNS để tối ưu tốc độ)
    private static final Set<String> TRUSTED_DOMAINS = new HashSet<>(Arrays.asList(
            "gmail.com", "yahoo.com", "yahoo.com.vn", "hotmail.com", "outlook.com",
            "icloud.com", "bookora.vn", "hust.edu.vn", "vnu.edu.vn", "uit.edu.vn", "fpt.edu.vn"
    ));

    // Cache kết quả tra cứu DNS domain để phản hồi cực nhanh
    private static final Map<String, Boolean> domainCache = new ConcurrentHashMap<>();

    /**
     * Kiểm tra cú pháp và định dạng email cơ bản
     */
    public static boolean isFormatValid(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        if (trimmed.isEmpty()) return false;
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) return false;

        int atIdx = trimmed.lastIndexOf('@');
        if (atIdx <= 0 || atIdx >= trimmed.length() - 1) return false;

        String domain = trimmed.substring(atIdx + 1).toLowerCase();
        int dotIdx = domain.lastIndexOf('.');
        if (dotIdx <= 0 || dotIdx >= domain.length() - 1) return false;

        String tld = domain.substring(dotIdx + 1);
        if (INVALID_TLDS.contains(tld)) {
            return false;
        }

        if (TYPO_DOMAINS.contains(domain)) {
            return false;
        }

        return true;
    }

    /**
     * Kiểm tra tính khả dụng thực tế của tên miền email qua DNS MX/A
     */
    public static boolean isDomainReachable(String email) {
        if (!isFormatValid(email)) return false;

        String trimmed = email.trim();
        String domain = trimmed.substring(trimmed.lastIndexOf('@') + 1).toLowerCase();

        if (TRUSTED_DOMAINS.contains(domain)) {
            return true;
        }

        return domainCache.computeIfAbsent(domain, d -> {
            try {
                Hashtable<String, String> env = new Hashtable<>();
                env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
                env.put("com.sun.jndi.dns.timeout.initial", "1500");
                env.put("com.sun.jndi.dns.timeout.retries", "1");
                DirContext ctx = new InitialDirContext(env);

                // Ưu tiên kiểm tra bản ghi Mail Exchange (MX)
                Attributes attrs = ctx.getAttributes(d, new String[]{"MX"});
                if (attrs != null && attrs.get("MX") != null && attrs.get("MX").size() > 0) {
                    return true;
                }

                // Dự phòng kiểm tra bản ghi A
                attrs = ctx.getAttributes(d, new String[]{"A"});
                return attrs != null && attrs.get("A") != null && attrs.get("A").size() > 0;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Kiểm tra toàn diện email: định dạng cú pháp + tồn tại tên miền nhận thư
     */
    public static boolean isValid(String email) {
        return isFormatValid(email) && isDomainReachable(email);
    }
}
