package com.civicconnect.service;

import com.civicconnect.dto.ChatRequest;
import com.civicconnect.dto.ChatResponse;
import com.civicconnect.model.Complaint;
import com.civicconnect.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final Pattern REF_ID_PATTERN = Pattern.compile("CC-\\d{8}-[A-Z0-9]+", Pattern.CASE_INSENSITIVE);

    @Value("${ai.api.key:}")
    private String configuredApiKey;

    private final ComplaintRepository complaintRepository;
    private HttpClient httpClient;

    public ChatService(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

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
        String lang = request.getLanguage() != null && !request.getLanguage().isBlank() ? request.getLanguage().toLowerCase() : "en";

        // Auto language fallback
        if ("auto".equals(lang)) {
            lang = detectLanguageFromText(userMsg);
        }

        // 1. Direct Reference ID lookup check
        Matcher refMatcher = REF_ID_PATTERN.matcher(userMsg.toUpperCase());
        if (refMatcher.find()) {
            String refId = refMatcher.group();
            Optional<Complaint> found = complaintRepository.findByReferenceId(refId);
            if (found.isPresent()) {
                Complaint c = found.get();
                String trackingReply = formatComplaintTrackingReply(c, lang);
                List<String> suggestions = List.of("Report another issue", "How does resolution work?", "Open 3D India Map");
                return new ChatResponse(trackingReply, suggestions, lang, false);
            } else {
                String notFoundReply = formatComplaintNotFoundReply(refId, lang);
                List<String> suggestions = List.of("How to report a civic issue?", "Check reference ID format", "Help");
                return new ChatResponse(notFoundReply, suggestions, lang, false);
            }
        }

        // 2. Call Gemini AI API if configured
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

        // 3. Reliable Built-in Multilingual Civic Knowledge Engine
        String localReply = generateLocalResponse(userMsg, lang);
        List<String> suggestions = generateSuggestions(userMsg, lang);
        return new ChatResponse(localReply, suggestions, lang, false);
    }

    public String detectLanguageFromText(String text) {
        if (text == null || text.isBlank()) return "en";

        for (char c : text.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.KANNADA) return "kn";
            if (block == Character.UnicodeBlock.DEVANAGARI) return "hi"; // Hindi / Marathi
            if (block == Character.UnicodeBlock.TELUGU) return "te";
            if (block == Character.UnicodeBlock.TAMIL) return "ta";
            if (block == Character.UnicodeBlock.MALAYALAM) return "ml";
            if (block == Character.UnicodeBlock.BENGALI) return "bn";
            if (block == Character.UnicodeBlock.GUJARATI) return "gu";
            if (block == Character.UnicodeBlock.GURMUKHI) return "pa";
            if (block == Character.UnicodeBlock.ORIYA) return "or";
            if (block == Character.UnicodeBlock.ARABIC) return "ar";
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) return "zh";
        }

        String lower = text.toLowerCase();
        if (lower.contains("hola") || lower.contains("gracias") || lower.contains("por favor") || lower.contains("quejas")) return "es";
        if (lower.contains("bonjour") || lower.contains("merci") || lower.contains("plainte")) return "fr";

        return "en";
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

        String systemPrompt = "You are CivicAI Bot, an intelligent municipal assistant for CivicConnect. "
                + "You assist citizens with reporting potholes, streetlights, garbage, water leaks, and drainage issues. "
                + "You explain civic statuses, provide instructions, and answer questions about India's geography and governance. "
                + "The user's requested language is code: '" + lang + "'. Respond courteously, concisely, and completely in that language.";

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

    private String formatComplaintTrackingReply(Complaint c, String lang) {
        String dt = c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "Recent";
        String status = c.getStatus() != null ? c.getStatus().name() : "SUBMITTED";

        switch (lang) {
            case "kn":
                return "📋 **ದೂರು ಟ್ರ್ಯಾಕಿಂಗ್ ವಿವರಗಳು:**\n"
                        + "• **ರೆಫರೆನ್ಸ್ ಐಡಿ:** `" + c.getReferenceId() + "`\n"
                        + "• **ಶೀರ್ಷಿಕೆ:** " + c.getTitle() + "\n"
                        + "• **ವರ್ಗ:** " + (c.getCategory() != null ? c.getCategory().getName() : "ಸಾಮಾನ್ಯ") + "\n"
                        + "• **ಪ್ರಸ್ತುತ ಸ್ಥಿತಿ:** `" + status + "`\n"
                        + "• **ದಾಖಲಾದ ದಿನಾಂಕ:** " + dt + "\n"
                        + "• **ಸ್ಥಳ:** " + (c.getLocationDescription() != null ? c.getLocationDescription() : "ಒದಗಿಸಲಾಗಿದೆ") + "\n\n"
                        + "ನಿಮ್ಮ ದೂರನ್ನು ನಗರಪಾಲಿಕೆ ಸಂಬಂಧಿತ ಇಲಾಖೆಗೆ ರವಾನಿಸಲಾಗಿದೆ.";
            case "hi":
                return "📋 **शिकायत ट्रैकिंग विवरण:**\n"
                        + "• **संदर्भ आईडी:** `" + c.getReferenceId() + "`\n"
                        + "• **शीर्षक:** " + c.getTitle() + "\n"
                        + "• **श्रेणी:** " + (c.getCategory() != null ? c.getCategory().getName() : "सामान्य") + "\n"
                        + "• **वर्तमान स्थिति:** `" + status + "`\n"
                        + "• **दर्ज दिनांक:** " + dt + "\n"
                        + "• **स्थान:** " + (c.getLocationDescription() != null ? c.getLocationDescription() : "दर्ज स्थल") + "\n\n"
                        + "आपकी शिकायत संबंधित नगर निगम विभाग द्वारा प्रक्रियाधीन है।";
            case "te":
                return "📋 **ఫిర్యాదు ట్రాకింగ్ వివరాలు:**\n"
                        + "• **రిఫరెన్స్ ID:** `" + c.getReferenceId() + "`\n"
                        + "• **శీర్షిక:** " + c.getTitle() + "\n"
                        + "• **వర్గం:** " + (c.getCategory() != null ? c.getCategory().getName() : "సాధారణం") + "\n"
                        + "• **ప్రస్తుత స్థితి:** `" + status + "`\n"
                        + "• **తేదీ:** " + dt + "\n\n"
                        + "మీ సమస్యను పురపాలక విభాగం పరిశీలిస్తోంది.";
            case "ta":
                return "📋 **புகார் கண்காணிப்பு விவரங்கள்:**\n"
                        + "• **குறிப்பு எண்:** `" + c.getReferenceId() + "`\n"
                        + "• **தலைப்பு:** " + c.getTitle() + "\n"
                        + "• **பிரிவு:** " + (c.getCategory() != null ? c.getCategory().getName() : "பொது") + "\n"
                        + "• **தற்போதைய நிலை:** `" + status + "`\n"
                        + "• **தேதி:** " + dt + "\n\n"
                        + "உங்கள் புகார் நகராட்சி துறையால் நடவடிக்கை எடுக்கப்பட்டு வருகிறது.";
            default:
                return "📋 **Complaint Tracking Details:**\n"
                        + "• **Reference ID:** `" + c.getReferenceId() + "`\n"
                        + "• **Title:** " + c.getTitle() + "\n"
                        + "• **Category:** " + (c.getCategory() != null ? c.getCategory().getName() : "General") + "\n"
                        + "• **Current Status:** `" + status + "`\n"
                        + "• **Submitted On:** " + dt + "\n"
                        + "• **Location:** " + (c.getLocationDescription() != null ? c.getLocationDescription() : "Provided on map") + "\n\n"
                        + "Your complaint is actively tracked in the municipal system.";
        }
    }

    private String formatComplaintNotFoundReply(String refId, String lang) {
        switch (lang) {
            case "kn": return "ಕ್ಷಮಿಸಿ, `" + refId + "` ರೆಫರೆನ್ಸ್ ಐಡಿಯೊಂದಿಗೆ ಯಾವುದೇ ದೂರು ಕಂಡುಬಂದಿಲ್ಲ. ದಯವಿಟ್ಟು ಐಡಿಯನ್ನು ಸರಿಯಾಗಿ ಪರಿಶೀಲಿಸಿ.";
            case "hi": return "क्षमा करें, संदर्भ आईडी `" + refId + "` के लिए कोई शिकायत नहीं मिली। कृपया संदर्भ संख्या की पुष्टि करें।";
            default: return "Sorry, no complaint was found matching Reference ID `" + refId + "`. Please check the number or report a new issue.";
        }
    }

    private String generateLocalResponse(String message, String lang) {
        String msg = message.toLowerCase();

        // 1. Status explanations
        if (msg.contains("status") || msg.contains("under review") || msg.contains("in progress") || msg.contains("verified") || msg.contains("स्थिति") || msg.contains("ಸ್ಥಿತಿ")) {
            return getStatusExplanation(lang);
        }

        // 2. Greetings
        if (msg.contains("hi") || msg.contains("hello") || msg.contains("namaste") || msg.contains("namaskara") || msg.contains("hey") || msg.contains("hola") || msg.contains("bonjour") || msg.contains("salam")) {
            return getGreetingResponse(lang);
        }

        // 3. How to report / complaint flow
        if (msg.contains("report") || msg.contains("file") || msg.contains("submit") || msg.contains("complaint") || msg.contains("pothole") || msg.contains("garbage") || msg.contains("light") || msg.contains("water") || msg.contains("drain") || msg.contains("दुरुस्त") || msg.contains("ದೂರು")) {
            return getReportingGuide(lang);
        }

        // 4. Tracking guide
        if (msg.contains("track") || msg.contains("reference") || msg.contains("check") || msg.contains("ಟ್ರ್ಯಾಕ್") || msg.contains("ट्रैक")) {
            return getTrackingGuide(lang);
        }

        // 5. CivicAI / About Platform
        if (msg.contains("civicai") || msg.contains("civicconnect") || msg.contains("about") || msg.contains("who are you") || msg.contains("what is")) {
            return getAboutGuide(lang);
        }

        // 6. India 3D Map
        if (msg.contains("map") || msg.contains("3d") || msg.contains("india") || msg.contains("state")) {
            return getIndiaMapGuide(lang);
        }

        return getDefaultHelp(lang);
    }

    private String getStatusExplanation(String lang) {
        switch (lang) {
            case "kn":
                return "📊 **ದೂರು ಸ್ಥಿತಿಗಳ ವಿವರಣೆ:**\n"
                        + "1. **SUBMITTED (ಸಲ್ಲಿಸಲಾಗಿದೆ):** ದೂರು ದಾಖಲಾಗಿದೆ.\n"
                        + "2. **UNDER_REVIEW (ಪರಿಶೀಲನೆಯಲ್ಲಿದೆ):** ಅಧಿಕಾರಿಗಳು ಪರಿಶೀಲಿಸುತ್ತಿದ್ದಾರೆ.\n"
                        + "3. **APPROVED (ಅನುಮೋದಿಸಲಾಗಿದೆ):** ದುರಸ್ತಿ ಕಾರ್ಯಕ್ಕೆ ಅನುಮೋದನೆ ದೊರೆತಿದೆ.\n"
                        + "4. **IN_PROGRESS (ಪ್ರಗತಿಯಲ್ಲಿದೆ):** ಸ್ಥಳದಲ್ಲಿ ಕಾಮಗಾರಿ ನಡೆಯುತ್ತಿದೆ.\n"
                        + "5. **RESOLVED_PENDING_VERIFICATION (ಪರಿಹಾರ - ಪರಿಶೀಲನೆ ಬಾಕಿ):** ಕಾಮಗಾರಿ ಮುಗಿದು ಫೋಟೋ ಅಪ್ಲೋಡ್ ಆಗಿದೆ.\n"
                        + "6. **VERIFIED_RESOLVED (ದೃಢೀಕರಿಸಲಾಗಿದೆ):** ನಾಗರಿಕರಿಂದ ದೃಢೀಕರಿಸಲ್ಪಟ್ಟಿದೆ.";
            case "hi":
                return "📊 **शिकायत स्थितियों की व्याख्या:**\n"
                        + "1. **SUBMITTED (दर्ज):** शिकायत सफलतापूर्वक दर्ज की गई।\n"
                        + "2. **UNDER_REVIEW (समीक्षाधीन):** संबंधित विभाग द्वारा सत्यापन किया जा रहा है।\n"
                        + "3. **APPROVED (स्वीकृत):** कार्य आदेश जारी किया गया।\n"
                        + "4. **IN_PROGRESS (प्रगति पर):** स्थल पर कार्य चल रहा है।\n"
                        + "5. **RESOLVED_PENDING_VERIFICATION (समाधान सत्यापन लंबित):** कार्य पूर्ण कर फोटो प्रमाण अपलोड किया गया।\n"
                        + "6. **VERIFIED_RESOLVED (सत्यापित समाधान):** नागरिक द्वारा पुष्टि के बाद समाधान बंद।";
            case "es":
                return "📊 **Estados de las Quejas en CivicConnect:**\n"
                        + "1. **SUBMITTED:** Queja recibida en el sistema.\n"
                        + "2. **UNDER_REVIEW:** En revisión por el equipo municipal.\n"
                        + "3. **APPROVED:** Aprobada para reparación.\n"
                        + "4. **IN_PROGRESS:** Trabajo en curso sobre el terreno.\n"
                        + "5. **RESOLVED_PENDING_VERIFICATION:** Trabajo completado con fotos.\n"
                        + "6. **VERIFIED_RESOLVED:** Verificado y cerrado por el ciudadano.";
            case "fr":
                return "📊 **Explication des Statuts de Réclamation:**\n"
                        + "1. **SUBMITTED:** Déposée avec succès.\n"
                        + "2. **UNDER_REVIEW:** En cours d'examen par la municipalité.\n"
                        + "3. **APPROVED:** Validée pour intervention.\n"
                        + "4. **IN_PROGRESS:** Travaux en cours sur place.\n"
                        + "5. **RESOLVED_PENDING_VERIFICATION:** Réparé, en attente de validation.\n"
                        + "6. **VERIFIED_RESOLVED:** Clôturé après confirmation du citoyen.";
            default:
                return "📊 **CivicConnect Complaint Status Stages:**\n"
                        + "1. **SUBMITTED:** Issue successfully logged into the municipal database.\n"
                        + "2. **UNDER_REVIEW:** City engineers verify category and duplicate checks.\n"
                        + "3. **APPROVED:** Work order assigned to the relevant department.\n"
                        + "4. **IN_PROGRESS:** Field crews dispatched for on-site repairs.\n"
                        + "5. **RESOLVED_PENDING_VERIFICATION:** Repair complete with photo evidence uploaded.\n"
                        + "6. **VERIFIED_RESOLVED:** Citizen confirmed satisfactory resolution.";
        }
    }

    private String getGreetingResponse(String lang) {
        switch (lang) {
            case "kn": return "ನಮಸ್ಕಾರ! ನಾನು **ಸಿವಿಕ್‌ಬಾಟ್**, ಸಿವಿಕ್‌ಕನೆಕ್ಟ್‌ನ AI ಸಹಾಯಕ. ನೀವು ನಗರದ ಯಾವುದೇ ಸಮಸ್ಯೆಗಳನ್ನು (ರಸ್ತೆ, ದೀಪ, ಕಸ, ನೀರು ಸರಬರಾಜು) ದೂರು ದಾಖಲಿಸಲು ಅಥವಾ ಟ್ರ್ಯಾಕ್ ಮಾಡಲು ನಾನು ಸಹಾಯ ಮಾಡುತ್ತೇನೆ. ನೀವು ದೂರನ್ನು ದಾಖಲಿಸಲು ಬಯಸುವಿರಾ?";
            case "hi": return "नमस्ते! मैं **सिविकबॉट** हूँ, आपका एआई सहायक। मैं गड्ढों, स्ट्रीटलाइट्स, कचरा और जल निकासी से संबंधित शिकायतों को दर्ज करने और ट्रैक करने में आपकी सहायता कर सकता हूँ। क्या आप नई शिकायत दर्ज करना चाहते हैं?";
            case "te": return "నమస్కారం! నేను **సివిక్‌బాట్**, మీ ఏఐ సహాయకుడిని. రోడ్డు గుంతలు, వీధి దీపాలు, చెత్త మరియు మురుగునీటి సమస్యలను నమోదు చేయడానికి మరియు ట్రాక్ చేయడానికి నేను మీకు సహాయపడతాను.";
            case "ta": return "வணக்கம்! நான் **சிவிக்பாட்** ஏஐ உதவியாளர். சாலைப் பள்ளங்கள், தெருவிளக்குகள், கழிவுகள் மற்றும் குடிநீர் தொடர்பான புகார்களைப் பதிவு செய்யவும் கண்காணிக்கவும் நான் உதவுகிறேன்.";
            case "mr": return "नमस्कार! मी **सिव्हिकबॉट** एआय सहाय्यक. खड्डे, पथदिवे, कचरा आणि पाणीपुरवठा संबंधित तक्रारी नोंदवण्यासाठी आणि ट्रॅक करण्यासाठी मी सदैव तयार आहे.";
            case "bn": return "নমস্কার! আমি **সিভিকবট** এআই সহকারী। রাস্তাঘাট, বাতি, বর্জ্য ও নর্দমা সংক্রান্ত পৌর সমস্যা রিপোর্ট এবং ট্র্যাক করতে আমি সাহায্য করতে প্রস্তুত।";
            case "ml": return "നമസ്കാരം! ഞാൻ **സിവിക്ബോട്ട്** എഐ അസിസ്റ്റന്റ്. റോഡ് തകരാറുകൾ, സ്ട്രീറ്റ് ലൈറ്റ്, മാലിന്യം എന്നിവ സംബന്ധിച്ച പരാതികൾ നൽകാനും പരിശോധിക്കാനും ഞാൻ സഹായിക്കാം.";
            case "gu": return "નમસ્તે! હું **સિવિકબૉટ** એઆઈ આસિસ્ટન્ટ છું. રસ્તાના ખાડા, સ્ટ્રીટલાઇટ, કચરો અને પાણીની સમસ્યાઓની ફરિયાદ નોંધવા અને ટ્રેક કરવા હું તૈયાર છું.";
            case "pa": return "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ! ਮੈਂ **ਸਿਵਿਕਬੋਟ** ਏਆਈ ਸਹਾਇਕ ਹਾਂ। ਸੜਕਾਂ, ਲਾਈਟਾਂ ਅਤੇ ਕੂੜੇ ਨਾਲ ਸੰਬੰਧਿਤ ਸਮੱਸਿਆਵਾਂ ਦਰਜ ਕਰਨ ਲਈ ਮੈਂ ਤੁਹਾਡੀ ਮਦਦ ਕਰ ਸਕਦਾ ਹਾਂ।";
            case "ur": return "آداب! میں **سیوک بوٹ** اے آئی معاون ہوں۔ بلدیاتی شکایات درج کرنے اور ٹریک کرنے میں میں آپ کی مدد کے لیے حاضر ہوں۔";
            case "or": return "ନମସ୍କାର! ମୁଁ **ସିଭିକବଟ୍** ଏଆଇ ସହାୟକ। ରାସ୍ତା ଖାଲ, ଷ୍ଟ୍ରିଟ୍ ଲାଇଟ୍ ଏବଂ ଅଳିଆ ସମ୍ବନ୍ଧୀୟ ଅଭିଯୋਗ ଦାଖଲ କରିବାକୁ ମୁଁ ସାହାଯ୍ୟ କରିବି।";
            case "as": return "নমস্কাৰ! মই **চিভিকবট** এআই সহায়ক। পৌৰসভাৰ সমস্যাসমূহ দাখিল আৰু ট্ৰেক কৰাত মই সহায় কৰিব পাৰোঁ।";
            case "es": return "¡Hola! Soy **CivicBot**, tu asistente de inteligencia artificial para CivicConnect. Puedo ayudarte a reportar baches, alumbrado público, basura o rastrear tus quejas existentes.";
            case "fr": return "Bonjour ! Je suis **CivicBot**, votre assistant d'IA municipal. Je peux vous aider à signaler un problème urbain ou à suivre une réclamation existante.";
            case "ar": return "مرحباً! أنا **سيفيك بوت**، مساعدك الذكي لبلدية سيفيك كونيكت. يمكنني مساعدتك في الإبلاغ عن مشكلات الطرق والإنارة وتتبع طلباتك.";
            case "zh": return "您好！我是 **CivicBot**，您的市政 AI 助手。我可以帮助您提交道路、路灯、垃圾等问题报告，或查询跟踪现有投诉进度。";
            default: return "Hello! I am **CivicBot**, your AI municipal assistant for CivicConnect. I can help you report civic issues step-by-step, track existing complaints with your Reference ID, or answer any municipal questions. How can I help you today?";
        }
    }

    private String getReportingGuide(String lang) {
        switch (lang) {
            case "kn":
                return "📝 **ದೂರು ದಾಖಲಿಸುವ ಹಂತಗಳು:**\n"
                        + "1. ಸಮಸ್ಯೆಯನ್ನು ವಿವರಿಸಿ (ಉದಾ: ರಸ್ತೆ ಗುಂಡಿ, ಬೀದಿ ದೀಪ ದುರಸ್ತಿ).\n"
                        + "2. ವರ್ಗವನ್ನು ಆಯ್ಕೆಮಾಡಿ (Road, Streetlight, Garbage, Water, Drainage).\n"
                        + "3. ನಿಮ್ಮ ಸ್ಥಳ ಅಥವಾ GPS ನಮೂದಿಸಿ.\n"
                        + "4. ಫೋಟೋ ಲಗತ್ತಿಸಿ (ಐಚ್ಛಿಕ).\n"
                        + "5. ವಿವರಗಳನ್ನು ದೃಢೀಕರಿಸಿ ಸಲ್ಲಿಸಿ!\n\n"
                        + "ನೀವು ನೇರವಾಗಿ ಈ ಚಾಟ್‌ಬಾಟ್‌ನಲ್ಲೇ ಹಂತ-ಹಂತವಾಗಿ ದೂರು ದಾಖಲಿಸಲು ಕೆಳಗಿನ **'ಹೊಸ ದೂರು ದಾಖಲಿಸಿ'** ಬಟನ್ ಕ್ಲಿಕ್ ಮಾಡಬಹುದು!";
            case "hi":
                return "📝 **शिकायत दर्ज करने के चरण:**\n"
                        + "1. अपनी समस्या बताएं (सड़क गड्ढा, बंद स्ट्रीटलाइट, कचरा आदि)।\n"
                        + "2. श्रेणी चुनें (Road Damage, Streetlight, Garbage, Water Leakage, Drainage)।\n"
                        + "3. स्थान या GPS प्रदान करें।\n"
                        + "4. फोटो अपलोड करें (वैकल्पिक)।\n"
                        + "5. विवरण की पुष्टि कर सबमिट करें!\n\n"
                        + "आप इसी चैटबॉट में सीधे शिकायत दर्ज करने के लिए नीचे **'नई शिकायत दर्ज करें'** चुन सकते हैं!";
            default:
                return "📝 **Step-by-Step Complaint Reporting:**\n"
                        + "1. State your issue (e.g. pothole on 5th Cross, broken streetlight).\n"
                        + "2. Select category (Road Damage, Streetlight, Garbage, Water, Drainage).\n"
                        + "3. Provide location or tap '📍 GPS Location'.\n"
                        + "4. Add a detailed description & photo (optional).\n"
                        + "5. Review and tap **Confirm & Submit** to receive your unique Reference ID!\n\n"
                        + "You can start right now by clicking **'Report a Problem'** below!";
        }
    }

    private String getTrackingGuide(String lang) {
        switch (lang) {
            case "kn": return "🔍 ನಿಮ್ಮ ದೂರನ್ನು ಟ್ರ್ಯಾಕ್ ಮಾಡಲು ನಿಮ್ಮ ರೆಫರೆನ್ಸ್ ಐಡಿ (ಉದಾ: `CC-20261107-101`) ಯನ್ನು ಇಲ್ಲಿ ಟೈಪ್ ಮಾಡಿ. ನಾನು ತಕ್ಷಣ ಪ್ರಸ್ತುತ ಸ್ಥಿತಿಯನ್ನು ಪರಿಶೀಲಿಸಿ ನಿಮಗೆ ತಿಳಿಸುತ್ತೇನೆ!";
            case "hi": return "🔍 अपनी शिकायत ट्रैक करने के लिए अपनी संदर्भ आईडी (जैसे: `CC-20261107-101`) यहाँ टाइप करें। मैं तुरंत डेटाबेस से स्थिति बताऊँगा!";
            default: return "🔍 To track a complaint, simply type your Reference ID (e.g. `CC-20261107-XXXXX`) right here in the chat, and I will fetch its live status instantly!";
        }
    }

    private String getAboutGuide(String lang) {
        switch (lang) {
            case "kn": return "🏛️ **CivicConnect (CivicAI)** ಎಂಬುದು ಭಾರತದ ನಾಗರಿಕರಿಗೆ ಪುರಸಭೆಯ ಸಮಸ್ಯೆಗಳನ್ನು ವರದಿ ಮಾಡಲು, ಪಾರದರ್ಶಕವಾಗಿ ಟ್ರ್ಯಾಕ್ ಮಾಡಲು ಮತ್ತು ಪರಿಹಾರವನ್ನು ದೃಢೀಕರಿಸಲು ನಿರ್ಮಿಸಲಾದ ಸುಧಾರಿತ ವೇದಿಕೆಯಾಗಿದೆ.";
            case "hi": return "🏛️ **CivicConnect (CivicAI)** भारत के नागरिकों के लिए समुदाय की समस्याओं (सड़क, प्रकाश, कचरा, जल) को दर्ज करने, पारदर्शी ट्रैकिंग और समाधान पुष्टि के लिए एक आधुनिक नागरिक मंच है।";
            default: return "🏛️ **CivicConnect (CivicAI)** is India's next-generation municipal grievance redressal platform featuring AI deduplication, interactive 3D mapping, citizen verification, and support for all 22 scheduled Indian languages plus international languages.";
        }
    }

    private String getIndiaMapGuide(String lang) {
        switch (lang) {
            case "kn": return "🗺️ ನಮ್ಮ **3D ಇಂಟರ್ಯಾಕ್ಟಿವ್ ಭಾರತ ನಕ್ಷೆ** ಮೂಲಕ ನೀವು ಭಾರತದ 28 ರಾಜ್ಯಗಳು ಮತ್ತು 8 ಕೇಂದ್ರಾಡಳಿತ ಪ್ರದೇಶಗಳನ್ನು 3D ಭೂಪ್ರದೇಶ ಮತ್ತು ಉಪಗ್ರಹ ವೀಕ್ಷಣೆಯೊಂದಿಗೆ ಅನ್ವೇಷಿಸಬಹುದು!";
            case "hi": return "🗺️ हमारे **3D इंटरैक्टिव भारत मानचित्र** के साथ आप भारत के 28 राज्यों और 8 केंद्र शासित प्रदेशों को 3D इलाके और उपग्रह दृश्य के साथ देख सकते हैं!";
            default: return "🗺️ Our **Interactive 3D Map of India** features realistic 3D terrain elevation, state boundaries, demographic profiles, and municipal statistics across all 28 States and 8 Union Territories. You can open it anytime from the top navigation bar!";
        }
    }

    private String getDefaultHelp(String lang) {
        switch (lang) {
            case "kn": return "ನಾನು ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ? ನೀವು:\n• 'ಹೊಸ ದೂರು ದಾಖಲಿಸಿ'\n• ರೆಫರೆನ್ಸ್ ಐಡಿ ನೀಡಿ ಸ್ಥಿತಿ ಪರಿಶೀಲಿಸಿ (ಉದಾ: CC-202611...)\n• ದೂರು ಸ್ಥಿತಿಗಳ ವಿವರಣೆ ಪಡೆಯಿರಿ\n• 3D ಭಾರತ ನಕ್ಷೆ ನೋಡಿ";
            case "hi": return "मैं आपकी क्या सहायता कर सकता हूँ?\n• नई शिकायत दर्ज करें\n• संदर्भ संख्या से स्थिति ट्रैक करें\n• नगर निगम प्रक्रियाओं की जानकारी लें\n• 3D भारत का नक्शा देखें";
            default: return "I am ready to assist you! You can:\n• Report a new municipal issue step-by-step\n• Track your complaint by entering your Reference ID (e.g., CC-20261107-...)\n• Ask about complaint statuses and resolution verification\n• Explore the 3D Map of India!";
        }
    }

    private List<String> generateSuggestions(String message, String lang) {
        switch (lang) {
            case "kn":
                return List.of("ಹೊಸ ದೂರು ದಾಖಲಿಸಿ", "ದೂರು ಸ್ಥಿತಿ ಟ್ರ್ಯಾಕ್ ಮಾಡಿ", "ಸ್ಥಿತಿಗಳ ವಿವರಣೆ", "3D ನಕ್ಷೆ");
            case "hi":
                return List.of("नई शिकायत दर्ज करें", "शिकायत ट्रैक करें", "स्थितियों की व्याख्या", "3D भारत नक्शा");
            case "te":
                return List.of("కొత్త ఫిర్యాదు చేయండి", "స్థితిని ట్రాక్ చేయండి", "3D భారతదేశ పటం", "సహాయం");
            case "ta":
                return List.of("புதிய புகார் அளிக்கவும்", "நிலையைக் கண்காணிக்கவும்", "3D இந்திய வரைபடம்", "உதவி");
            case "es":
                return List.of("Reportar un problema", "Rastrear queja", "Estados de queja", "Mapa 3D");
            case "fr":
                return List.of("Signaler un problème", "Suivre une réclamation", "Explication statuts", "Carte 3D");
            default:
                return List.of("Report a Civic Problem", "Track Complaint Status", "Explain Complaint Statuses", "Open 3D India Map");
        }
    }
}
