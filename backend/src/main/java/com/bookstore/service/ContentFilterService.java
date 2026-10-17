package com.bookstore.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dịch vụ kiểm tra và lọc tự động nội dung đánh giá sách
 * Chặn từ ngữ xúc phạm, chửi tục, công kích, lừa đảo, đe dọa.
 */
public class ContentFilterService {

    public static class FilterResult {
        private final boolean blocked;
        private final String matchedKeyword;
        private final String reason;

        public FilterResult(boolean blocked, String matchedKeyword, String reason) {
            this.blocked = blocked;
            this.matchedKeyword = matchedKeyword;
            this.reason = reason;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getMatchedKeyword() {
            return matchedKeyword;
        }

        public String getReason() {
            return reason;
        }
    }

    // Danh sách từ/cụm từ bị cấm (có thể mở rộng linh hoạt)
    private static final List<String> BLACKLIST_PHRASES = Arrays.asList(
        // Từ tục tĩu / chửi bới tiếng Việt
        "đm", "dmm", "đmm", "đcm", "dcm", "dkm", "đkm", "clm", "vcl", "vl", "vkl",
        "địt", "dit", "đm", "đ.m", "d.m", "cặc", "cac", "lồn", "lon", "buồi", "buoi",
        "đái", "ỉa", "cứt", "cut",
        // Từ ngữ công kích, lăng mạ
        "đồ chó", "do cho", "óc chó", "oc cho", "đồ ngu", "do ngu", "mất dạy", "mat day",
        "khốn nạn", "khon nan", "thối nát", "thoi nat", "mẹ kiếp", "me kiep", "thằng lằn",
        "đồ lợn", "do lon", "chửi", "chui", "xúc phạm", "xuc pham", "hãm", "ham lol",
        "bán hàng giả", "lừa đảo", "lua dao", "bán đồ giả", "bán đồ rác", "shop lừa", "rác rưởi",
        "đồ gian thương", "gian thuong", "tẩy chay", "đe dọa", "tao giết", "giết người",
        // Tiếng Anh xúc phạm / quấy rối
        "fuck", "bitch", "bastard", "shit", "asshole", "scam", "scammer", "fraud"
    );

    /**
     * Kiểm tra nội dung đánh giá xem có chứa từ cấm hoặc công kích hay không.
     */
    public static FilterResult checkContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new FilterResult(false, null, null);
        }

        String rawLower = content.toLowerCase().trim();
        String normalizedText = removeDiacritics(rawLower);

        // Chuẩn hóa loại bỏ các ký tự đặc biệt gián đoạn (vd: "đ.m", "d-m", "d m")
        String cleanedRaw = rawLower.replaceAll("[^\\p{L}\\p{N}\\s]", "");
        String cleanedNormalized = normalizedText.replaceAll("[^a-z0-9\\s]", "");

        for (String keyword : BLACKLIST_PHRASES) {
            String keywordLower = keyword.toLowerCase();
            String keywordNormalized = removeDiacritics(keywordLower);

            // 1. Kiểm tra khớp cụm từ chính xác hoặc regex từ nguyên vẹn
            if (isKeywordPresent(rawLower, keywordLower) ||
                isKeywordPresent(normalizedText, keywordNormalized) ||
                isKeywordPresent(cleanedRaw, keywordLower) ||
                isKeywordPresent(cleanedNormalized, keywordNormalized)) {
                
                return new FilterResult(
                    true, 
                    keyword, 
                    "Nội dung đánh giá chứa từ ngữ không phù hợp. Vui lòng chỉnh sửa và gửi lại."
                );
            }
        }

        return new FilterResult(false, null, null);
    }

    private static boolean isKeywordPresent(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) return false;
        
        // Nếu từ khóa có nhiều hơn 1 từ (cụm từ), kiểm tra contains
        if (keyword.contains(" ")) {
            return text.contains(keyword);
        }
        
        // Đối với từ đơn ngắn (vd: "vl", "dm", "đm", "dit", "cac", "lon"), dùng Word Boundary Regex để tránh chặn nhầm từ hợp lệ (vd: "cảm ơn", "sách")
        String patternString;
        if (keyword.length() <= 3) {
            patternString = "(?i)(?:^|\\s|\\p{Punct})" + Pattern.quote(keyword) + "(?:$|\\s|\\p{Punct})";
        } else {
            patternString = "(?i)" + Pattern.quote(keyword);
        }

        try {
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(text);
            return matcher.find();
        } catch (Exception e) {
            return text.contains(keyword);
        }
    }

    /**
     * Loại bỏ dấu tiếng Việt để so sánh mở rộng
     */
    public static String removeDiacritics(String str) {
        if (str == null) return "";
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(nfdNormalizedString).replaceAll("");
        return result.replace('đ', 'd').replace('Đ', 'D');
    }
}
