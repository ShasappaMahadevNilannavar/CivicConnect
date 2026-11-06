package com.civicconnect.service;

import com.civicconnect.dto.ChatRequest;
import com.civicconnect.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Value("${ai.api.key:}")
    private String configuredApiKey;

    private HttpClient httpClient;

    private synchronized HttpClient getHttpClient() {
        if (httpClient == null) {
            try {
                httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
            } catch (Exception e) {
                log.warn("Could not initialize HttpClient: {}", e.getMessage());
            }
        }
        return httpClient;
    }

    public ChatResponse processChat(ChatRequest request) {
        String userMsg = request.getMessage() != null ? request.getMessage().trim() : "";
        String lang = request.getLanguage() != null && !request.getLanguage().isBlank() ? request.getLanguage() : "en";

        String apiKey = getEffectiveApiKey();

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String aiReply = callGeminiApi(apiKey, userMsg, lang, request.getHistory());
                if (aiReply != null && !aiReply.isBlank()) {
                    List<String> suggestions = generateSuggestions(userMsg, lang);
                    return new ChatResponse(aiReply, suggestions, lang, true);
                }
            } catch (Exception e) {
                log.warn("External AI call failed, falling back to local civic engine: {}", e.getMessage());
            }
        }

        // Reliable Built-in Multilingual Civic & India Knowledge Engine
        String localReply = generateLocalResponse(userMsg, lang);
        List<String> suggestions = generateSuggestions(userMsg, lang);
        return new ChatResponse(localReply, suggestions, lang, false);
    }

    private String getEffectiveApiKey() {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey;
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        return System.getenv("AI_API_KEY");
    }

    private String callGeminiApi(String apiKey, String message, String lang, List<Map<String, String>> history) throws Exception {
        HttpClient client = getHttpClient();
        if (client == null) return null;

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        String systemPrompt = "You are CivicBot, an intelligent and friendly AI assistant for CivicConnect (India's Community Problem Reporting & Resolution Platform). "
                + "You assist citizens and administrators with municipal issues (potholes, streetlights, garbage, water leaks, drainage, duplicate detection, verification). "
                + "You also possess deep knowledge about India: its 28 States, 8 Union Territories, capitals, geography, culture, civic governance, and all 22 official languages. "
                + "Answer politely, concisely, and helpfully. The user's active language code is '" + lang + "'. Always reply directly in this requested language.";

        StringBuilder contentsJson = new StringBuilder();
        contentsJson.append("{\"role\":\"user\",\"parts\":[{\"text\":").append(escapeJson(systemPrompt + "\nUser Question: " + message)).append("}]}");

        String requestBody = "{\"contents\":[" + contentsJson + "]}";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            // Parse response text
            int textIdx = body.indexOf("\"text\": \"");
            if (textIdx != -1) {
                int start = textIdx + 9;
                int end = body.indexOf("\"", start);
                while (end != -1 && body.charAt(end - 1) == '\\') {
                    end = body.indexOf("\"", end + 1);
                }
                if (end != -1) {
                    String raw = body.substring(start, end);
                    return raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                }
            }
        } else {
            log.warn("Gemini API returned status {}: {}", response.statusCode(), response.body());
        }
        return null;
    }

    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private String generateLocalResponse(String message, String lang) {
        String msg = message.toLowerCase();

        // 1. Greetings
        if (msg.contains("hi") || msg.contains("hello") || msg.contains("namaste") || msg.contains("hey") || msg.contains("namaskar")) {
            return getGreetingResponse(lang);
        }

        // 2. How to file / report complaint
        if (msg.contains("how to report") || msg.contains("file") || msg.contains("submit") || msg.contains("new complaint") || msg.contains("complain") || msg.contains("shikayat") || msg.contains("pothole") || msg.contains("garbage") || msg.contains("light")) {
            return getReportingGuide(lang);
        }

        // 3. Tracking & Statuses
        if (msg.contains("track") || msg.contains("status") || msg.contains("reference") || msg.contains("under review") || msg.contains("progress")) {
            return getTrackingGuide(lang);
        }

        // 4. Duplicate Detection
        if (msg.contains("duplicate") || msg.contains("similarity") || msg.contains("algorithm") || msg.contains("jaro") || msg.contains("haversine")) {
            return getDuplicateGuide(lang);
        }

        // 5. Verification & Dispute
        if (msg.contains("verify") || msg.contains("dispute") || msg.contains("feedback") || msg.contains("resolution") || msg.contains("resolved")) {
            return getVerificationGuide(lang);
        }

        // 6. India 3D Map / States
        if (msg.contains("map") || msg.contains("3d") || msg.contains("state") || msg.contains("capital") || msg.contains("india") || msg.contains("tourist") || msg.contains("population")) {
            return getIndiaMapGuide(lang);
        }

        // 7. Admin capabilities
        if (msg.contains("admin") || msg.contains("department") || msg.contains("assign") || msg.contains("priority")) {
            return getAdminGuide(lang);
        }

        // Default Helpful Response
        return getDefaultHelp(lang);
    }

    private String getGreetingResponse(String lang) {
        switch (lang) {
            case "hi": return "नमस्ते! मैं सिविकबॉट हूँ, आपका सिविककनेक्ट सहायक। मैं नागरिक शिकायतों को दर्ज करने, ट्रैक करने तथा भारत के राज्यों एवं सेवाओं के बारे में आपकी सहायता कर सकता हूँ। आप क्या जानना चाहते हैं?";
            case "bn": return "নমস্কার! আমি সিভিকবট, সিভিককানেক্টের ভার্চুয়াল সহকারী। অভিযোগ দাখিল, ট্র্যাকিং এবং ভারতের রাজ্য ও পৌরসেবা সম্পর্কে আমি সাহায্য করতে পারি। আপনি কী জানতে চান?";
            case "te": return "నమస్కారం! నేను సివిక్‌బాట్, మీ సహాయకుడిని. సమస్యల నమోదు, ట్రాకింగ్ మరియు భారతదేశ రాష్ట్రాల వివరాల గురించి మీకు సహాయం చేయడానికి సిద్ధంగా ఉన్నాను.";
            case "ta": return "வணக்கம்! நான் சிவிக்பாட், உங்கள் உதவி உதவியாளர். புகார்கள் பதிவு செய்தல், கண்காணித்தல் மற்றும் இந்தியாவின் மாநிலங்கள் பற்றி உங்களுக்கு உதவ முடியும்.";
            case "mr": return "नमस्कार! मी सिव्हिकबॉट, आपला सहाय्यक. नागरी तक्रारी नोंदवणे, ट्रॅक करणे आणि भारताच्या राज्यांविषयी माहिती मिळवण्यासाठी मी सदैव तत्पर आहे.";
            case "ur": return "آداب! میں سوک بوٹ ہوں، آپ کا ورچوئل معاون۔ بلدیاتی شکایات کے اندراج، ٹریکنگ اور بھارت کی ریاستوں کے بارے میں میں آپ کی رہنمائی کر سکتا ہوں۔";
            case "kn": return "ನಮಸ್ಕಾರ! ನಾನು ಸಿವಿಕ್‌ಬಾಟ್, ನಿಮ್ಮ ನೆರವು ಸಹಾಯಕ. ನಾಗರಿಕ ದೂರು ದಾಖಲಿಸುವುದು, ಟ್ರ್ಯಾಕಿಂಗ್ ಹಾಗೂ ಭಾರತದ ರಾಜ್ಯಗಳ ಬಗ್ಗೆ ಮಾಹಿತಿ ನೀಡಲು ಸಿದ್ಧನಾಗಿದ್ದೇನೆ.";
            default: return "Hello! I am CivicBot, your CivicConnect AI Assistant. I can help you report civic issues, track complaint statuses, understand municipal workflows, or explore India's 3D states and geography. How can I help you today?";
        }
    }

    private String getReportingGuide(String lang) {
        switch (lang) {
            case "hi": return "शिकायत दर्ज करने के लिए:\n1. 'शिकायत दर्ज करें' बटन पर क्लिक करें।\n2. श्रेणी चुनें (सड़क, लाइट, कचरा, पानी आदि)।\n3. विस्तृत विवरण दें और स्थल की तस्वीर अपलोड करें।\n4. ओपनस्ट्रीटमैप पर समस्या का सटीक स्थान पिन करें।\n5. 'सबमिट करें' पर क्लिक करने पर आपको एक विशिष्ट संदर्भ आईडी (उदा. CC-20261106-A101) मिलेगी।";
            case "bn": return "অভিযোগ জানানোর সহজ নিয়ম:\n১. 'অভিযোগ জানান' বাটনে ক্লিক করুন।\n২. সমস্যা বিভাগ বাছুন (রাস্তা, বাতি, আবর্জনা ইত্যাদি)।\n৩. ছবি আপলোড করুন এবং ম্যাপে পিন করুন।\n৪. জমা দিলে একটি রেফারেন্স নম্বর পাবেন যা দিয়ে অগ্রগতি দেখতে পারবেন।";
            default: return "To file a civic complaint:\n1. Click 'Report Issue' in the top navigation.\n2. Select category (Road Damage, Streetlight, Garbage, Water Leakage, Drainage).\n3. Provide description and upload photos (up to 5 images, max 10MB).\n4. Pin exact GPS location on the interactive map.\n5. Click Submit to receive your unique Reference ID (e.g. CC-20261106-A101) to track progress!";
        }
    }

    private String getTrackingGuide(String lang) {
        switch (lang) {
            case "hi": return "शिकायत की स्थिति ट्रैक करने के लिए 'मेरी शिकायतें' में जाएं या मुख्य पृष्ठ पर अपनी संदर्भ आईडी दर्ज करें। स्थितियां:\n• SUBMITTED (दर्ज)\n• UNDER_REVIEW (समीक्षाधीन)\n• APPROVED (स्वीकृत)\n• IN_PROGRESS (प्रगति पर)\n• RESOLVED_PENDING_VERIFICATION (सत्यापन लंबित)\n• VERIFIED_RESOLVED (सत्यापित समाधान)।";
            default: return "You can track your issue under 'My Complaints' or using your Reference ID on the home page. The 6-stage lifecycle is:\n1. SUBMITTED -> 2. UNDER_REVIEW -> 3. APPROVED -> 4. IN_PROGRESS -> 5. RESOLVED_PENDING_VERIFICATION -> 6. VERIFIED_RESOLVED.\nImmutable timestamps are recorded for every state change.";
        }
    }

    private String getDuplicateGuide(String lang) {
        switch (lang) {
            case "hi": return "सिविककनेक्ट का डुप्लिकेट इंजन जारो-विंकलर अल्गोरिदम (विवरण समानता) तथा हेवरसाइन फॉर्मूला (200 मीटर के भीतर भौगोलिक निकटता) का उपयोग करता है। संभावित डुप्लिकेट प्रशासक को समीक्षा के लिए दिखाए जाते हैं, किसी भी नागरिक की रिपोर्ट स्वतः नष्ट नहीं होती।";
            default: return "CivicConnect detects duplicates using a hybrid engine:\n1. Jaro-Winkler String Similarity for titles & descriptions (>= 85% match).\n2. Haversine Great-Circle Distance for geo-coordinates (within 200m radius).\nMatched candidates are flagged for admin review, ensuring no citizen voice is deleted or silently ignored.";
        }
    }

    private String getVerificationGuide(String lang) {
        switch (lang) {
            case "hi": return "नागरिक सत्यापन: जब नगर निगम मरम्मत पूरी कर फोटो प्रमाण अपलोड करता है, तो शिकायत 'सत्यापन लंबित' स्थिति में जाती है। नागरिक समाधान की पुष्टि (Confirm) कर सकते हैं या असंतोष होने पर विवाद (Dispute) दर्ज कर सकते हैं।";
            default: return "Citizen Verification Loop:\nWhen administrators complete field repairs, they upload photographic proof and mark the issue 'RESOLVED_PENDING_VERIFICATION'. You can then view the evidence and click 'Confirm Resolved' or 'Dispute Resolution' with feedback. A complaint is never closed without citizen confirmation!";
        }
    }

    private String getIndiaMapGuide(String lang) {
        switch (lang) {
            case "hi": return "हमारे नए '3D इंडिया मैप' पर जाएँ! वहाँ आप भारत के सभी 28 राज्यों और 8 केंद्र शासित प्रदेशों को 3D इलाके, ऊंचाई, राजधानियों, प्रमुख शहरों, संस्कृति, जनसंख्या और पर्यटन स्थलों के साथ घूम और ज़ूम कर सकते हैं।";
            default: return "Explore our interactive 3D Map of India under '3D India Map' in the navigation bar! It features realistic 3D terrain elevation, state boundary extrusion, dynamic lighting, city markers, and complete profiles for all 28 States & 8 Union Territories with demographic and cultural data.";
        }
    }

    private String getAdminGuide(String lang) {
        switch (lang) {
            case "hi": return "व्यवस्थापक (Admin) पोर्टल में लाइव चार्ट्स, विभाग आवंटन, प्राथमिकता निर्धारण (LOW, MEDIUM, HIGH, CRITICAL), डुप्लिकेट समीक्षा और समाधान साक्ष्य अपलोड करने की सुविधा उपलब्ध है।";
            default: return "Administrators have access to:\n1. Live triage queue with status & priority controls.\n2. Department routing (Roads, Sanitation, Electrical, Water Board).\n3. Duplicate review center.\n4. After-repair photo evidence upload.\n5. Complete immutable audit trails.";
        }
    }

    private String getDefaultHelp(String lang) {
        switch (lang) {
            case "hi": return "मैं आपकी कैसे सहायता करूँ? आप पूछ सकते हैं:\n• शिकायत कैसे दर्ज करें?\n• समस्या को ट्रैक कैसे करें?\n• 3D इंडिया मैप कैसे देखें?\n• डुप्लिकेट डिटेक्शन कैसे काम करता है?";
            default: return "I'm here to help! You can ask me:\n• How do I report a pothole or streetlight?\n• How to track my complaint status?\n• How does the 3D Map of India work?\n• What is the citizen resolution verification loop?\n• Or ask about any Indian state or governance service!";
        }
    }

    private List<String> generateSuggestions(String message, String lang) {
        String msg = message.toLowerCase();
        if (msg.contains("map") || msg.contains("state")) {
            return Arrays.asList("Show 3D Map of India", "List all 28 States", "How to report issue in my state?", "What is CivicConnect?");
        }
        if (msg.contains("report") || msg.contains("file")) {
            return Arrays.asList("How to track status?", "What photo formats are accepted?", "How to pin GPS location?", "Can I report anonymously?");
        }
        return Arrays.asList("How to report a civic problem?", "How to track complaint?", "Open 3D India Map", "How does duplicate detection work?");
    }
}
