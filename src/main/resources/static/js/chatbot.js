/**
 * CivicConnect - Advanced Multilingual Municipal AI Chatbot (CivicAI)
 * Features:
 * 1. 17 Languages + Auto Detect
 * 2. Automatic script and language detection
 * 3. 9-Step Interactive Guided Complaint Flow
 * 4. Voice Speech-to-Text (Microphone) & Text-to-Speech (Speaker)
 * 5. Real Database Integration & Reference ID Generation
 * 6. Live Complaint Tracking
 * 7. Dark / Light Theme & Responsive Design
 */

(function () {
    'use strict';

    // State
    let isChatOpen = false;
    let isMaximized = false;
    let isDarkTheme = localStorage.getItem('civic_chatbot_theme') === 'dark';
    let currentBotLang = localStorage.getItem('civic_chatbot_lang') || 'auto';
    let detectedLang = 'en';
    let chatHistory = [];
    let isListening = false;
    let speechRecognition = null;

    // 9-Step Complaint Flow State
    let complaintFlow = {
        active: false,
        step: 0,
        title: '',
        category: '',
        location: '',
        latitude: null,
        longitude: null,
        description: '',
        imageBase64: null,
        imageFileName: ''
    };

    // DOM References
    let launcherBtn, chatWindow, chatMessages, chatInput, sendBtn, micBtn, typingIndicator, themeToggleBtn;

    const LANGUAGE_CONFIG = {
        auto: { label: "🌐 Auto Detect", bcp47: "en-IN", name: "CivicAI Assistant" },
        en: { label: "English", bcp47: "en-IN", name: "CivicAI Assistant" },
        kn: { label: "ಕನ್ನಡ (Kannada)", bcp47: "kn-IN", name: "ಸಿವಿಕ್‌ಕನೆಕ್ಟ್ ಎಐ" },
        hi: { label: "हिन्दी (Hindi)", bcp47: "hi-IN", name: "सिविकएआई सहायक" },
        te: { label: "తెలుగు (Telugu)", bcp47: "te-IN", name: "సివిక్‌ఏఐ సహాయకుడు" },
        ta: { label: "தமிழ் (Tamil)", bcp47: "ta-IN", name: "சிவிக்ஏஐ உதவியாளர்" },
        ml: { label: "മലയാളം (Malayalam)", bcp47: "ml-IN", name: "സിവിക്എഐ അസിസ്റ്റന്റ്" },
        mr: { label: "मराठी (Marathi)", bcp47: "mr-IN", name: "सिव्हिकएआय सहाय्यक" },
        bn: { label: "বাংলা (Bengali)", bcp47: "bn-IN", name: "সিভিকএআই সহকারী" },
        gu: { label: "ગુજરાતી (Gujarati)", bcp47: "gu-IN", name: "સિવિકએઆઈ આસિસ્ટન્ટ" },
        pa: { label: "ਪੰਜਾਬੀ (Punjabi)", bcp47: "pa-IN", name: "ਸਿਵਿਕਏਆਈ ਸਹਾਇਕ" },
        or: { label: "ଓଡ଼ିଆ (Odia)", bcp47: "or-IN", name: "ସିଭିକଏଆଇ ସହାୟକ" },
        ur: { label: "اردو (Urdu)", bcp47: "ur-IN", name: "سیوک اے آئی معاون" },
        as: { label: "অসমীয়া (Assamese)", bcp47: "as-IN", name: "চিভিকএআই সহায়ক" },
        es: { label: "Español (Spanish)", bcp47: "es-ES", name: "CivicAI Español" },
        fr: { label: "Français (French)", bcp47: "fr-FR", name: "CivicAI Français" },
        ar: { label: "العربية (Arabic)", bcp47: "ar-SA", name: "سيفيك إي آي" },
        zh: { label: "中文 (Chinese)", bcp47: "zh-CN", name: "CivicAI 市政助手" }
    };

    const WELCOME_MESSAGES = {
        en: "Hello! I am **CivicAI**, your municipal assistant. I can help you report civic issues step-by-step, track complaints with your Reference ID, explain statuses, or answer questions about municipal services across India.",
        kn: "ನಮಸ್ಕಾರ! ನಾನು **ಸಿವಿಕ್‌ಎಐ** ಮುನ್ಸಿಪಲ್ ಸಹಾಯಕ. ರಸ್ತೆ ಗುಂಡಿ, ಬೀದಿ ದೀಪ, ಕಸ, ನೀರು ಸರಬರಾಜು ಸಮಸ್ಯೆಗಳನ್ನು ಹಂತ-ಹಂತವಾಗಿ ದಾಖಲಿಸಲು, ದೂರು ಟ್ರ್ಯಾಕ್ ಮಾಡಲು ಅಥವಾ ಮಾಹಿತಿ ಪಡೆಯಲು ನಾನು ಸಹಾಯ ಮಾಡುತ್ತೇನೆ.",
        hi: "नमस्ते! मैं **सिविकएआई** हूँ। सड़क के गड्ढे, बंद स्ट्रीटलाइट, कचरा या जलभराव की शिकायतें चरणबद्ध तरीके से दर्ज करने, संदर्भ आईडी से स्थिति ट्रैक करने या नगर निगम सेवाओं की जानकारी के लिए मैं तैयार हूँ।",
        te: "నమస్కారం! నేను **సివిక్‌ఏఐ** సహాయకుడిని. రోడ్డు గుంతలు, వీధి దీపాలు, చెత్త మరియు నీటి సమస్యలను సులభంగా నమోదు చేయడానికి మరియు ట్రాక్ చేయడానికి నేను సిద్ధంగా ఉన్నాను.",
        ta: "வணக்கம்! நான் **சிவிக்ஏஐ** உதவியாளர். சாலைப் பள்ளங்கள், தெருவிளக்குகள், கழிவு மேலாண்மை புகார்களைப் பதிவு செய்யவும் கண்காணிக்கவும் நான் உதவுகிறேன்.",
        ml: "നമസ്കാരം! ഞാൻ **സിവിക്എഐ** അസിസ്റ്റന്റ്. റോഡ്, സ്ട്രീറ്റ് ലൈറ്റ്, മാലിന്യ പ്രശ്നങ്ങൾ എളുപ്പത്തിൽ രജിസ്റ്റർ ചെയ്യാനും ട്രാക്ക് ചെയ്യാനും ചോദിക്കാം.",
        mr: "नमस्कार! मी **सिव्हिकएआय** सहाय्यक. खड्डे, पथदिवे, कचरा समस्यांची तक्रार नोंदवण्यासाठी आणि स्थिती ट्रॅक करण्यासाठी मी सदैव तत्पर आहे.",
        bn: "নমস্কার! আমি **সিভিকএআই** সহকারী। রাস্তাঘাট, আলো ও বর্জ্য সংক্রান্ত পৌর সমস্যা রিপোর্ট এবং ট্র্যাক করতে আমাকে প্রশ্ন করুন।",
        gu: "નમસ્તે! હું **સિવિકએઆઈ** છું. રસ્તાના ખાડા, સ્ટ્રીટલાઈટ, કચરાની સમસ્યા નોંધવા અને ટ્રેક કરવા હું મદદ કરી શકું છું.",
        pa: "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ! ਮੈਂ **ਸਿਵਿਕਏਆਈ** ਸਹਾਇਕ ਹਾਂ। ਸੜਕਾਂ, ਲਾਈਟਾਂ ਅਤੇ ਕੂੜੇ ਨਾਲ ਸੰਬੰਧਿਤ ਸਮੱਸਿਆਵਾਂ ਦਰਜ ਕਰਨ ਲਈ ਤੁਹਾਡੀ ਮਦದ ਕਰਾਂਗਾ।",
        or: "ନମସ୍କାର! ମୁଁ **ସିଭିକଏଆଇ** ସହାୟକ। ପୌରପାଳିକା ସମସ୍ୟା ସମ୍ବନ୍ଧୀୟ ଅଭିଯୋଗ ଦାଖଲ ଏବଂ ଟ୍ରାକିଂ ପାଇଁ ମୁଁ ଉପଲବ୍ଧ।",
        ur: "آداب! میں **سیوک اے آئی** معاون ہوں۔ میونسپل شکایات کے اندراج اور ٹریکنگ میں میں آپ کی رہنمائی کر سکتا ہوں۔",
        as: "নমস্কাৰ! মই **চিভিকএআই** সহায়ক। পৌৰসভাৰ সমস্যাসমূহ দাখিল আৰু ট্ৰেক কৰাত মই সহায় কৰিম।",
        es: "¡Hola! Soy **CivicAI**, tu asistente municipal. Puedo ayudarte a reportar baches, alumbrado, basura y rastrear tus quejas con tu ID de referencia.",
        fr: "Bonjour ! Je suis **CivicAI**, votre assistant municipal. Je peux vous guider pour déposer une réclamation et suivre son avancement en direct.",
        ar: "مرحباً! أنا **سيفيك إي آي**، مساعدك البلدي الذكي. يمكنني مساعدتك في الإبلاغ عن المشكلات وتتبع الشكاوى عبر رقم المرجع.",
        zh: "您好！我是 **CivicAI** 市政智能助手。我可以协助您分步提交市政报修投诉，或通过参考编号跟踪处理进度。"
    };

    function initChatbot() {
        createChatbotDOM();
        applyTheme();
        bindEvents();
        setupSpeechRecognition();
        loadHistoryFromStorage();
    }

    function createChatbotDOM() {
        const root = document.createElement('div');
        root.id = 'civicChatbotRoot';
        root.className = 'civic-chatbot-wrapper';

        // Build Language Dropdown Items
        let langOptionsHtml = '';
        for (const [code, info] of Object.entries(LANGUAGE_CONFIG)) {
            const activeClass = (code === currentBotLang) ? 'active fw-bold' : '';
            langOptionsHtml += `<li><a class="dropdown-item ${activeClass}" href="#" data-lang="${code}">${info.label}</a></li>`;
        }

        root.innerHTML = `
            <!-- Floating 3D Launcher Button -->
            <button id="chatbotLauncher" class="chatbot-launcher-btn shadow-lg" title="Chat with CivicAI Assistant" aria-label="Open AI Municipal Chatbot">
                <div class="launcher-3d-sphere">
                    <i class="fas fa-robot robot-icon"></i>
                </div>
                <span class="launcher-pulse"></span>
                <span class="launcher-badge">AI</span>
            </button>

            <!-- Modern Chat Window -->
            <div id="chatbotWindow" class="chatbot-window shadow-lg d-none">
                <!-- Header -->
                <div class="chatbot-header d-flex justify-content-between align-items-center p-3 text-white">
                    <div class="d-flex align-items-center gap-2">
                        <div class="bot-avatar-circle">
                            <i class="fas fa-robot"></i>
                        </div>
                        <div>
                            <div class="fw-bold d-flex align-items-center gap-1">
                                <span id="botHeaderTitle">CivicAI Assistant</span>
                                <span class="badge bg-warning text-dark px-1 py-0 small" style="font-size:0.65rem;" id="botActiveLangBadge">AUTO</span>
                            </div>
                            <small class="text-light opacity-75 d-flex align-items-center gap-1">
                                <span class="status-dot"></span> Online &bull; Municipal Redressal
                            </small>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-1">
                        <!-- Theme Toggle Button -->
                        <button id="chatbotThemeBtn" class="theme-toggle-btn" title="Toggle Dark/Light Theme">
                            <i class="${isDarkTheme ? 'fas fa-sun text-warning' : 'fas fa-moon'}"></i>
                        </button>

                        <!-- Language Selector Dropdown -->
                        <div class="dropdown">
                            <button class="btn btn-sm btn-link text-white p-1" type="button" id="botLangSelectBtn" data-bs-toggle="dropdown" aria-expanded="false" title="Select Chatbot Language">
                                <i class="fas fa-language fa-lg text-warning"></i>
                            </button>
                            <ul class="dropdown-menu dropdown-menu-end p-1 shadow border-0" id="botLangDropdown" style="max-height:280px; overflow-y:auto; font-size:0.85rem;">
                                ${langOptionsHtml}
                            </ul>
                        </div>

                        <!-- Maximize / Restore -->
                        <button id="chatbotMaximizeBtn" class="btn btn-sm btn-link text-white p-1" title="Maximize/Restore">
                            <i class="far fa-window-maximize"></i>
                        </button>
                        <!-- Clear Chat -->
                        <button id="chatbotClearBtn" class="btn btn-sm btn-link text-white p-1" title="Clear Chat History">
                            <i class="fas fa-trash-alt"></i>
                        </button>
                        <!-- Close -->
                        <button id="chatbotCloseBtn" class="btn btn-sm btn-link text-white p-1" title="Close">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                </div>

                <!-- Messages Area -->
                <div id="chatbotMessages" class="chatbot-messages p-3">
                    <!-- Typing Indicator -->
                    <div id="chatbotTyping" class="chat-message bot-message typing-indicator d-none">
                        <span></span><span></span><span></span>
                    </div>
                </div>

                <!-- Suggestions Row -->
                <div id="chatbotSuggestions" class="chatbot-suggestions px-3 py-2 border-top bg-light d-flex gap-1 overflow-auto">
                    <!-- Chips inserted dynamically -->
                </div>

                <!-- Footer with Input & Voice Mic -->
                <div class="chatbot-footer p-2 border-top bg-white">
                    <form id="chatbotForm" class="d-flex align-items-center gap-2 m-0">
                        <button type="button" id="chatbotMicBtn" class="mic-btn" title="Speak in your language (Voice to Text)">
                            <i class="fas fa-microphone"></i>
                        </button>
                        <input type="text" id="chatbotInput" class="form-control form-control-sm rounded-pill px-3 py-2 border-1" placeholder="Type or click mic to speak..." autocomplete="off">
                        <button type="submit" id="chatbotSendBtn" class="btn btn-success btn-sm rounded-circle shadow-sm" style="width:38px; height:38px; flex-shrink:0;" title="Send Message">
                            <i class="fas fa-paper-plane"></i>
                        </button>
                    </form>
                    <div class="d-flex justify-content-between align-items-center px-2 pt-1 text-muted" style="font-size:0.68rem;">
                        <span id="botFooterStatus">CivicAI &bull; Voice & Multilingual</span>
                        <span>17 Languages &bull; STT & TTS</span>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(root);

        launcherBtn = document.getElementById('chatbotLauncher');
        chatWindow = document.getElementById('chatbotWindow');
        chatMessages = document.getElementById('chatbotMessages');
        chatInput = document.getElementById('chatbotInput');
        sendBtn = document.getElementById('chatbotSendBtn');
        micBtn = document.getElementById('chatbotMicBtn');
        typingIndicator = document.getElementById('chatbotTyping');
        themeToggleBtn = document.getElementById('chatbotThemeBtn');
    }

    function applyTheme() {
        if (isDarkTheme) {
            chatWindow.classList.add('dark-theme');
            themeToggleBtn.innerHTML = '<i class="fas fa-sun text-warning"></i>';
        } else {
            chatWindow.classList.remove('dark-theme');
            themeToggleBtn.innerHTML = '<i class="fas fa-moon"></i>';
        }
    }

    function bindEvents() {
        // Open/Close
        launcherBtn.addEventListener('click', toggleChatWindow);
        document.getElementById('chatbotCloseBtn').addEventListener('click', toggleChatWindow);

        // Maximize
        document.getElementById('chatbotMaximizeBtn').addEventListener('click', toggleMaximize);

        // Clear Chat
        document.getElementById('chatbotClearBtn').addEventListener('click', resetChat);

        // Dark/Light Theme Toggle
        themeToggleBtn.addEventListener('click', function () {
            isDarkTheme = !isDarkTheme;
            localStorage.setItem('civic_chatbot_theme', isDarkTheme ? 'dark' : 'light');
            applyTheme();
        });

        // Language Dropdown Selection
        document.querySelectorAll('#botLangDropdown a').forEach(item => {
            item.addEventListener('click', function (e) {
                e.preventDefault();
                const selectedCode = this.getAttribute('data-lang');
                if (selectedCode) {
                    setBotLanguage(selectedCode);
                }
            });
        });

        // Form Submit
        document.getElementById('chatbotForm').addEventListener('submit', function (e) {
            e.preventDefault();
            handleUserInput();
        });

        // Microphone Button
        micBtn.addEventListener('click', toggleSpeechRecognition);
    }

    function setBotLanguage(langCode) {
        currentBotLang = langCode;
        localStorage.setItem('civic_chatbot_lang', langCode);

        // Update dropdown active state
        document.querySelectorAll('#botLangDropdown a').forEach(a => {
            if (a.getAttribute('data-lang') === langCode) {
                a.classList.add('active', 'fw-bold');
            } else {
                a.classList.remove('active', 'fw-bold');
            }
        });

        const activeBadge = document.getElementById('botActiveLangBadge');
        if (activeBadge) {
            activeBadge.textContent = langCode.toUpperCase();
        }

        const effectiveLang = (langCode === 'auto') ? (detectedLang || 'en') : langCode;
        const info = LANGUAGE_CONFIG[effectiveLang] || LANGUAGE_CONFIG.en;
        const botTitle = document.getElementById('botHeaderTitle');
        if (botTitle) botTitle.textContent = info.name;

        // Welcome or notify
        if (chatHistory.length === 0) {
            renderWelcome();
        } else {
            appendBotMessage(`🌐 Language set to **${LANGUAGE_CONFIG[langCode] ? LANGUAGE_CONFIG[langCode].label : langCode}**.`);
        }
    }

    function detectScript(text) {
        if (!text) return 'en';
        if (/[\u0C80-\u0CFF]/.test(text)) return 'kn'; // Kannada
        if (/[\u0C00-\u0C7F]/.test(text)) return 'te'; // Telugu
        if (/[\u0B80-\u0BFF]/.test(text)) return 'ta'; // Tamil
        if (/[\u0D00-\u0D7F]/.test(text)) return 'ml'; // Malayalam
        if (/[\u0900-\u097F]/.test(text)) return 'hi'; // Hindi / Marathi
        if (/[\u0980-\u09FF]/.test(text)) return 'bn'; // Bengali / Assamese
        if (/[\u0A80-\u0AFF]/.test(text)) return 'gu'; // Gujarati
        if (/[\u0A00-\u0A7F]/.test(text)) return 'pa'; // Punjabi
        if (/[\u0B00-\u0B7F]/.test(text)) return 'or'; // Odia
        if (/[\u0600-\u06FF]/.test(text)) return 'ur'; // Urdu / Arabic
        if (/[\u4E00-\u9FFF]/.test(text)) return 'zh'; // Chinese

        const lower = text.toLowerCase();
        if (lower.includes('hola') || lower.includes('queja') || lower.includes('gracias')) return 'es';
        if (lower.includes('bonjour') || lower.includes('merci') || lower.includes('plainte')) return 'fr';

        return 'en';
    }

    function toggleChatWindow() {
        isChatOpen = !isChatOpen;
        if (isChatOpen) {
            chatWindow.classList.remove('d-none');
            launcherBtn.classList.add('active');
            chatInput.focus();
            if (chatHistory.length === 0) {
                renderWelcome();
            }
        } else {
            chatWindow.classList.add('d-none');
            launcherBtn.classList.remove('active');
            stopSpeechRecognition();
        }
    }

    function toggleMaximize() {
        isMaximized = !isMaximized;
        chatWindow.classList.toggle('maximized', isMaximized);
        const icon = document.querySelector('#chatbotMaximizeBtn i');
        if (icon) {
            icon.className = isMaximized ? 'far fa-window-restore' : 'far fa-window-maximize';
        }
    }

    function renderWelcome() {
        const effectiveLang = (currentBotLang === 'auto') ? (detectedLang || 'en') : currentBotLang;
        const welcomeText = WELCOME_MESSAGES[effectiveLang] || WELCOME_MESSAGES.en;
        appendBotMessage(welcomeText);
        renderDefaultSuggestions();
    }

    function renderDefaultSuggestions() {
        const effectiveLang = (currentBotLang === 'auto') ? (detectedLang || 'en') : currentBotLang;
        let chips = [];

        switch (effectiveLang) {
            case 'kn':
                chips = ["ಹೊಸ ದೂರು ದಾಖಲಿಸಿ", "ದೂರು ಟ್ರ್ಯಾಕ್ ಮಾಡಿ", "ಸ್ಥಿತಿಗಳ ವಿವರಣೆ", "3D ನಕ್ಷೆ"];
                break;
            case 'hi':
                chips = ["नई शिकायत दर्ज करें", "शिकायत ट्रैक करें", "स्थितियों की व्याख्या", "3D नक्शा"];
                break;
            case 'te':
                chips = ["కొత్త సమస్యను నమోదు చేయండి", "ట్రాక్ చేయండి", "స్థితుల వివరణ", "3D పటం"];
                break;
            case 'ta':
                chips = ["புதிய புகார் அளிக்கவும்", "கண்காணிக்கவும்", "விளக்கம்", "3D வரைபடம்"];
                break;
            case 'es':
                chips = ["Reportar un problema", "Rastrear queja", "Estados", "Mapa 3D"];
                break;
            case 'fr':
                chips = ["Signaler un problème", "Suivre ma réclamation", "Statuts", "Carte 3D"];
                break;
            default:
                chips = ["Report a Civic Problem", "Track Complaint Status", "Explain Complaint Statuses", "Open 3D India Map"];
        }

        renderSuggestions(chips);
    }

    function handleUserInput(customText) {
        const text = customText || chatInput.value.trim();
        if (!text) return;

        appendUserMessage(text);
        chatInput.value = '';

        // Auto language detection if enabled
        if (currentBotLang === 'auto') {
            const detected = detectScript(text);
            if (detected && detected !== detectedLang) {
                detectedLang = detected;
                const badge = document.getElementById('botActiveLangBadge');
                if (badge) badge.textContent = detected.toUpperCase();
            }
        }

        // Check if user is in the 9-Step Complaint Flow
        if (complaintFlow.active) {
            handleComplaintStepInput(text);
            return;
        }

        // Check if user wants to start the Guided Complaint Flow
        const lower = text.toLowerCase();
        if (lower.includes('report') || lower.includes('file') || lower.includes('new complaint') || lower.includes('शिकायत') || lower.includes('ದೂರು') || lower.includes('problem')) {
            startGuidedComplaintFlow(text);
            return;
        }

        // Check if user input is a Reference ID (e.g. CC-20261107-XXXXX)
        const refMatch = text.match(/CC-\d{8}-[A-Za-z0-9]+/i);
        if (refMatch) {
            fetchLiveTracking(refMatch[0]);
            return;
        }

        // Standard AI / Q&A Call
        sendToBackendChat(text);
    }

    // ========================================================
    // 9-STEP COMPLAINT GUIDANCE WORKFLOW
    // ========================================================
    function startGuidedComplaintFlow(initialProblem) {
        complaintFlow = {
            active: true,
            step: 1,
            title: '',
            category: '',
            location: '',
            latitude: null,
            longitude: null,
            description: '',
            imageBase64: null,
            imageFileName: ''
        };

        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;

        let msg = "📝 **Step 1 of 8: What civic problem are you facing?**\n\nPlease describe the issue (e.g., *'Large pothole on 4th Cross Road'* or *'Broken streetlight near Central Park'*).";
        if (effectiveLang === 'kn') {
            msg = "📝 **ಹಂತ 1 (8 ರಲ್ಲಿ): ನೀವು ಎದುರಿಸುತ್ತಿರುವ ನಾಗರಿಕ ಸಮಸ್ಯೆ ಏನು?**\n\nದಯವಿಟ್ಟು ಸಮಸ್ಯೆಯನ್ನು ತಿಳಿಸಿ (ಉದಾ: *'4ನೇ ಮುಖ್ಯರಸ್ತೆಯಲ್ಲಿ ದೊಡ್ಡ ಗುಂಡಿ ಬಿದ್ದಿದೆ'* ಅಥವಾ *'ಬೀದಿ ದೀಪ ಕೆಲಸ ಮಾಡುತ್ತಿಲ್ಲ'*).";
        } else if (effectiveLang === 'hi') {
            msg = "📝 **चरण 1 (8 में से): आप किस नागरिक समस्या का सामना कर रहे हैं?**\n\nकृपया समस्या का संक्षेप में उल्लेख करें (उदा: *'मुख्य मार्ग पर बड़ा गड्ढा'* या *'स्ट्रीटलाइट बंद है'*).";
        }

        // If user already typed a specific problem, advance to Step 2 directly!
        if (initialProblem && initialProblem.length > 5 && !initialProblem.toLowerCase().includes('report') && !initialProblem.toLowerCase().includes('file')) {
            complaintFlow.title = initialProblem;
            complaintFlow.description = initialProblem;
            complaintFlow.step = 2;
            promptCategorySelection();
            return;
        }

        appendBotMessage(msg);
        renderSuggestions(["Pothole on road", "Streetlight broken", "Garbage overflow", "Water pipe leak", "Drainage block"]);
    }

    function handleComplaintStepInput(userInput) {
        switch (complaintFlow.step) {
            case 1:
                // Step 1: Capture Title / Problem
                complaintFlow.title = userInput;
                complaintFlow.description = userInput;
                complaintFlow.step = 2;
                promptCategorySelection();
                break;

            case 2:
                // Step 2: Category Selected
                complaintFlow.category = userInput;
                complaintFlow.step = 3;
                promptLocationInput();
                break;

            case 3:
                // Step 3: Location Received
                complaintFlow.location = userInput;
                complaintFlow.step = 4;
                promptDetailedDescription();
                break;

            case 4:
                // Step 4: Detailed Description Received
                if (userInput && userInput.trim() !== '.') {
                    complaintFlow.description = userInput;
                }
                complaintFlow.step = 5;
                promptPhotoUpload();
                break;

            case 5:
                // Step 5 handled by file picker / skip button
                break;

            case 6:
            case 7:
                // Confirmation step handled by Confirm/Cancel buttons
                if (userInput.toLowerCase().includes('confirm') || userInput.toLowerCase().includes('yes') || userInput.toLowerCase().includes('submit')) {
                    submitComplaintToBackend();
                } else if (userInput.toLowerCase().includes('cancel')) {
                    cancelComplaintFlow();
                }
                break;
        }
    }

    function promptCategorySelection() {
        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;

        let msg = "🏷️ **Step 2 of 8: Please select the complaint category:**";
        if (effectiveLang === 'kn') msg = "🏷️ **ಹಂತ 2: ದೂರಿನ ವರ್ಗವನ್ನು ಆಯ್ಕೆಮಾಡಿ:**";
        if (effectiveLang === 'hi') msg = "🏷️ **चरण 2: कृपया शिकायत की श्रेणी चुनें:**";

        appendBotMessage(msg);

        // Render interactive category chips inside chat
        const card = document.createElement('div');
        card.className = 'complaint-flow-card';
        card.innerHTML = `
            <div class="small fw-bold text-muted mb-1">Click a Category:</div>
            <div class="category-chips-grid">
                <button type="button" class="category-chip-btn" data-cat="Road Damage"><i class="fas fa-road text-primary"></i> Road Damage</button>
                <button type="button" class="category-chip-btn" data-cat="Streetlight"><i class="fas fa-lightbulb text-warning"></i> Streetlight</button>
                <button type="button" class="category-chip-btn" data-cat="Garbage"><i class="fas fa-trash text-danger"></i> Garbage</button>
                <button type="button" class="category-chip-btn" data-cat="Water Leakage"><i class="fas fa-tint text-info"></i> Water Leakage</button>
                <button type="button" class="category-chip-btn" data-cat="Drainage"><i class="fas fa-water text-secondary"></i> Drainage</button>
                <button type="button" class="category-chip-btn" data-cat="Public Safety"><i class="fas fa-shield-alt text-success"></i> Public Safety</button>
                <button type="button" class="category-chip-btn" data-cat="Other"><i class="fas fa-ellipsis-h text-dark"></i> Other</button>
            </div>
        `;

        card.querySelectorAll('.category-chip-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const cat = this.getAttribute('data-cat');
                appendUserMessage(cat);
                complaintFlow.category = cat;
                complaintFlow.step = 3;
                promptLocationInput();
            });
        });

        chatMessages.insertBefore(card, typingIndicator);
        scrollToBottom();
    }

    function promptLocationInput() {
        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;

        let msg = "📍 **Step 3 of 8: Where is this problem located?**\n\nYou can type the street address or tap the GPS button below:";
        if (effectiveLang === 'kn') msg = "📍 **ಹಂತ 3: ಈ ಸಮಸ್ಯೆಯ ಸ್ಥಳ ಯಾವುದು?**\n\nವಿಳಾಸವನ್ನು ಟೈಪ್ ಮಾಡಿ ಅಥವಾ ಕೆಳಗಿನ GPS ಬಟನ್ ಒತ್ತಿರಿ:";
        if (effectiveLang === 'hi') msg = "📍 **चरण 3: यह समस्या कहाँ स्थित है?**\n\nस्थान का नाम टाइप करें या नीचे दिया गया GPS बटन दबाएँ:";

        appendBotMessage(msg);

        const card = document.createElement('div');
        card.className = 'complaint-flow-card d-flex align-items-center justify-content-between';
        card.innerHTML = `
            <span class="small text-muted"><i class="fas fa-map-marker-alt text-danger"></i> Get exact location</span>
            <button type="button" id="botGpsBtn" class="btn btn-sm btn-outline-success rounded-pill px-3 py-1">
                <i class="fas fa-crosshairs me-1"></i> Use My GPS Location
            </button>
        `;

        card.querySelector('#botGpsBtn').addEventListener('click', function () {
            if (navigator.geolocation) {
                this.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Locating...';
                this.disabled = true;
                navigator.geolocation.getCurrentPosition(
                    pos => {
                        complaintFlow.latitude = pos.coords.latitude;
                        complaintFlow.longitude = pos.coords.longitude;
                        complaintFlow.location = `GPS: ${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)}`;
                        appendUserMessage(`📍 Located via GPS: ${complaintFlow.location}`);
                        complaintFlow.step = 4;
                        promptDetailedDescription();
                    },
                    err => {
                        this.disabled = false;
                        this.innerHTML = '<i class="fas fa-crosshairs me-1"></i> Retry GPS';
                        appendBotMessage("⚠️ Could not fetch GPS automatically. Please type your area, street, or landmark.");
                    },
                    { timeout: 10000, enableHighAccuracy: true }
                );
            } else {
                appendBotMessage("⚠️ Geolocation is not supported by your browser. Please type your location.");
            }
        });

        chatMessages.insertBefore(card, typingIndicator);
        scrollToBottom();
    }

    function promptDetailedDescription() {
        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;

        let msg = "📝 **Step 4 of 8: Please provide any additional details or description.**\n\n(Or type *'Same'* to keep your initial summary).";
        if (effectiveLang === 'kn') msg = "📝 **ಹಂತ 4: ದೂರಿನ ಬಗ್ಗೆ ಹೆಚ್ಚುವರಿ ವಿವರಗಳನ್ನು ಒದಗಿಸಿ.**\n\n(ಅಥವಾ ಮೊದಲ ವಿವರಣೆಯನ್ನೇ ಇರಿಸಲು *'Same'* ಎಂದು ಟೈಪ್ ಮಾಡಿ).";
        if (effectiveLang === 'hi') msg = "📝 **चरण 4: कृपया अतिरिक्त विवरण प्रदान करें।**\n\n(या प्रारंभिक विवरण रखने के लिए *'Same'* टाइप करें).";

        appendBotMessage(msg);
        renderSuggestions(["Problem is getting worse daily", "Urgent hazard for vehicles", "Same as title"]);
    }

    function promptPhotoUpload() {
        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;

        let msg = "📷 **Step 5 of 8: Would you like to upload a photo of the problem?**\n\nUploading a photo speeds up municipal inspection!";
        if (effectiveLang === 'kn') msg = "📷 **ಹಂತ 5: ಸಮಸ್ಯೆಯ ಫೋಟೋವನ್ನು ಅಪ್ಲೋಡ್ ಮಾಡಲು ಬಯಸುವಿರಾ?**\n\nಫೋಟೋ ಲಗತ್ತಿಸುವುದರಿಂದ ದುರಸ್ತಿ ವೇಗಗೊಳ್ಳುತ್ತದೆ!";
        if (effectiveLang === 'hi') msg = "📷 **चरण 5: क्या आप समस्या की तस्वीर अपलोड करना चाहते हैं?**\n\nफोटो जोड़ने से कार्य त्वरित गति से होता है!";

        appendBotMessage(msg);

        const card = document.createElement('div');
        card.className = 'complaint-flow-card text-center';
        card.innerHTML = `
            <div class="mb-2">
                <input type="file" id="botPhotoInput" accept="image/jpeg,image/png,image/webp" class="d-none">
                <button type="button" id="botChoosePhotoBtn" class="btn btn-sm btn-primary rounded-pill px-3 py-1 me-2">
                    <i class="fas fa-camera me-1"></i> Choose Photo
                </button>
                <button type="button" id="botSkipPhotoBtn" class="btn btn-sm btn-outline-secondary rounded-pill px-3 py-1">
                    <i class="fas fa-forward me-1"></i> Skip Photo
                </button>
            </div>
            <div id="botPhotoPreviewContainer" class="d-none mt-2">
                <img id="botPhotoPreview" src="" alt="Complaint Photo" style="max-height:100px; border-radius:8px; border:1px solid #ddd;">
                <div class="small text-success mt-1"><i class="fas fa-check-circle"></i> Photo attached</div>
            </div>
        `;

        const photoInput = card.querySelector('#botPhotoInput');
        const chooseBtn = card.querySelector('#botChoosePhotoBtn');
        const skipBtn = card.querySelector('#botSkipPhotoBtn');
        const previewContainer = card.querySelector('#botPhotoPreviewContainer');
        const previewImg = card.querySelector('#botPhotoPreview');

        chooseBtn.addEventListener('click', () => photoInput.click());

        photoInput.addEventListener('change', function () {
            if (this.files && this.files[0]) {
                const file = this.files[0];
                complaintFlow.imageFileName = file.name;
                const reader = new FileReader();
                reader.onload = function (e) {
                    complaintFlow.imageBase64 = e.target.result;
                    previewImg.src = e.target.result;
                    previewContainer.classList.remove('d-none');
                    chooseBtn.innerHTML = '<i class="fas fa-check"></i> Photo Selected';
                    setTimeout(() => {
                        complaintFlow.step = 6;
                        renderConfirmationCard();
                    }, 800);
                };
                reader.readAsDataURL(file);
            }
        });

        skipBtn.addEventListener('click', function () {
            appendUserMessage("⏭️ Skipped Photo");
            complaintFlow.step = 6;
            renderConfirmationCard();
        });

        chatMessages.insertBefore(card, typingIndicator);
        scrollToBottom();
    }

    function renderConfirmationCard() {
        complaintFlow.step = 7;
        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;

        let msg = "📋 **Step 6 & 7: Please review your complaint details before submission:**";
        if (effectiveLang === 'kn') msg = "📋 **ಹಂತ 6 & 7: ಸಲ್ಲಿಕೆಗೆ ಮುನ್ನ ದೂರಿನ ವಿವರಗಳನ್ನು ಪರಿಶೀಲಿಸಿ:**";
        if (effectiveLang === 'hi') msg = "📋 **चरण 6 & 7: कृपया सबमिट करने से पहले विवरण की पुष्टि करें:**";

        appendBotMessage(msg);

        const card = document.createElement('div');
        card.className = 'complaint-flow-card';
        card.innerHTML = `
            <table class="confirm-summary-table">
                <tr><td class="label-col">Title:</td><td class="fw-bold">${escapeHtml(complaintFlow.title)}</td></tr>
                <tr><td class="label-col">Category:</td><td><span class="badge bg-success">${escapeHtml(complaintFlow.category || 'General')}</span></td></tr>
                <tr><td class="label-col">Location:</td><td>${escapeHtml(complaintFlow.location || 'Municipal Area')}</td></tr>
                <tr><td class="label-col">Details:</td><td>${escapeHtml(complaintFlow.description)}</td></tr>
                <tr><td class="label-col">Photo:</td><td>${complaintFlow.imageBase64 ? '<span class="text-success"><i class="fas fa-image"></i> Attached (' + escapeHtml(complaintFlow.imageFileName) + ')</span>' : '<span class="text-muted">None</span>'}</td></tr>
            </table>
            <div class="d-flex justify-content-end gap-2 mt-2 pt-2 border-top">
                <button type="button" id="botCancelComplaintBtn" class="btn btn-sm btn-outline-danger rounded-pill px-3">
                    <i class="fas fa-times me-1"></i> Cancel
                </button>
                <button type="button" id="botSubmitComplaintBtn" class="btn btn-sm btn-success rounded-pill px-4 shadow-sm">
                    <i class="fas fa-check-circle me-1"></i> Confirm & Submit
                </button>
            </div>
        `;

        card.querySelector('#botCancelComplaintBtn').addEventListener('click', cancelComplaintFlow);
        card.querySelector('#botSubmitComplaintBtn').addEventListener('click', submitComplaintToBackend);

        chatMessages.insertBefore(card, typingIndicator);
        scrollToBottom();
    }

    function cancelComplaintFlow() {
        complaintFlow = { active: false, step: 0 };
        appendBotMessage("❌ Complaint registration cancelled. How else can I assist you?");
        renderDefaultSuggestions();
    }

    function submitComplaintToBackend() {
        showTyping(true);

        const payload = {
            title: complaintFlow.title,
            description: complaintFlow.description,
            category: complaintFlow.category,
            locationDescription: complaintFlow.location,
            latitude: complaintFlow.latitude,
            longitude: complaintFlow.longitude,
            imageBase64: complaintFlow.imageBase64,
            imageFileName: complaintFlow.imageFileName
        };

        fetch('/api/chat/submit-complaint', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(res => {
            if (!res.ok) throw new Error("HTTP error " + res.status);
            return res.json();
        })
        .then(data => {
            showTyping(false);
            complaintFlow.active = false;
            complaintFlow.step = 0;

            if (data.success && data.referenceId) {
                renderComplaintSuccessCard(data);
            } else {
                appendBotMessage("⚠️ There was an issue recording your complaint: " + (data.error || "Please try again later."));
                renderDefaultSuggestions();
            }
        })
        .catch(err => {
            showTyping(false);
            console.error("Complaint submission error:", err);
            appendBotMessage("⚠️ Could not connect to the municipal server. Please check your network or try again.");
            renderDefaultSuggestions();
        });
    }

    function renderComplaintSuccessCard(data) {
        const refId = data.referenceId;
        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;

        let congratulations = "🎉 **Complaint Successfully Submitted!**";
        if (effectiveLang === 'kn') congratulations = "🎉 **ದೂರನ್ನು ಯಶಸ್ವಿಯಾಗಿ ಸಲ್ಲಿಸಲಾಗಿದೆ!**";
        if (effectiveLang === 'hi') congratulations = "🎉 **आपकी शिकायत सफलतापूर्वक दर्ज कर ली गई है!**";

        appendBotMessage(congratulations);

        const card = document.createElement('div');
        card.className = 'complaint-success-box my-2';
        card.innerHTML = `
            <div class="fw-bold text-success mb-1"><i class="fas fa-check-circle fa-lg"></i> Official Municipal Record Created</div>
            <div class="small text-muted mb-1">Your Tracking Reference ID:</div>
            <div class="tracking-ref-badge">${escapeHtml(refId)}</div>
            <div class="small text-muted mt-2">
                <strong>Status:</strong> <span class="badge bg-primary">${escapeHtml(data.status || 'SUBMITTED')}</span> &bull; 
                <strong>Category:</strong> ${escapeHtml(data.category || 'General')}
            </div>
            <div class="d-flex justify-content-center gap-2 mt-3">
                <button type="button" class="btn btn-sm btn-outline-success track-this-btn rounded-pill px-3">
                    <i class="fas fa-search me-1"></i> Track Now
                </button>
                <a href="/citizen/dashboard" class="btn btn-sm btn-success rounded-pill px-3">
                    <i class="fas fa-tachometer-alt me-1"></i> My Dashboard
                </a>
            </div>
        `;

        card.querySelector('.track-this-btn').addEventListener('click', () => {
            fetchLiveTracking(refId);
        });

        chatMessages.insertBefore(card, typingIndicator);
        scrollToBottom();

        renderSuggestions(["Report another issue", "Explain complaint statuses", "Open 3D India Map"]);
    }

    // ========================================================
    // LIVE COMPLAINT TRACKING
    // ========================================================
    function fetchLiveTracking(refId) {
        showTyping(true);

        fetch(`/api/chat/track/${encodeURIComponent(refId.trim().toUpperCase())}`)
            .then(res => res.json())
            .then(data => {
                showTyping(false);
                if (data.found) {
                    renderTrackingResultCard(data);
                } else {
                    appendBotMessage(`❌ No complaint found for Reference ID **${escapeHtml(refId)}**. Please check the code and try again.`);
                    renderDefaultSuggestions();
                }
            })
            .catch(err => {
                showTyping(false);
                // Fallback to chat endpoint
                sendToBackendChat(`Track ${refId}`);
            });
    }

    function renderTrackingResultCard(c) {
        const statusColors = {
            SUBMITTED: 'bg-secondary',
            UNDER_REVIEW: 'bg-info text-dark',
            APPROVED: 'bg-primary',
            IN_PROGRESS: 'bg-warning text-dark',
            RESOLVED_PENDING_VERIFICATION: 'bg-info text-dark',
            VERIFIED_RESOLVED: 'bg-success',
            REJECTED: 'bg-danger'
        };

        const badgeClass = statusColors[c.status] || 'bg-secondary';

        const card = document.createElement('div');
        card.className = 'complaint-flow-card border-start border-4 border-success my-2';
        card.innerHTML = `
            <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="fw-bold font-monospace text-success">${escapeHtml(c.referenceId)}</span>
                <span class="badge ${badgeClass}">${escapeHtml(c.status)}</span>
            </div>
            <div class="fw-bold mb-1">${escapeHtml(c.title)}</div>
            <div class="small text-muted mb-2">${escapeHtml(c.description || '')}</div>
            <div class="small border-top pt-2">
                <div><strong>Category:</strong> ${escapeHtml(c.category)}</div>
                <div><strong>Location:</strong> ${escapeHtml(c.locationDescription || 'N/A')}</div>
                <div><strong>Logged on:</strong> ${escapeHtml(c.createdAt)}</div>
            </div>
        `;

        chatMessages.insertBefore(card, typingIndicator);
        scrollToBottom();
        renderSuggestions(["Report another issue", "Explain complaint statuses", "Open 3D India Map"]);
    }

    // ========================================================
    // BACKEND AI Q&A CALL
    // ========================================================
    function sendToBackendChat(userText) {
        showTyping(true);

        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;

        fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: userText,
                language: effectiveLang,
                history: chatHistory.slice(-6)
            })
        })
        .then(res => {
            if (!res.ok) throw new Error("Status " + res.status);
            return res.json();
        })
        .then(data => {
            showTyping(false);
            const reply = data.reply || "I am here to assist you with municipal redressal and civic services.";
            appendBotMessage(reply);
            chatHistory.push({ role: 'assistant', content: reply });
            saveHistoryToStorage();

            if (data.suggestions && data.suggestions.length > 0) {
                renderSuggestions(data.suggestions);
            } else {
                renderDefaultSuggestions();
            }
        })
        .catch(err => {
            showTyping(false);
            console.warn("AI Chat API call error, using local reply: ", err);
            const fallbackReply = "I am ready to help! You can report civic problems, track reference IDs, or explore our 3D Map of India using the navigation bar.";
            appendBotMessage(fallbackReply);
            chatHistory.push({ role: 'assistant', content: fallbackReply });
            saveHistoryToStorage();
            renderDefaultSuggestions();
        });
    }

    // ========================================================
    // SPEECH RECOGNITION (STT) & TEXT-TO-SPEECH (TTS)
    // ========================================================
    function setupSpeechRecognition() {
        const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRec) {
            console.info("SpeechRecognition is not supported in this browser.");
            return;
        }

        try {
            speechRecognition = new SpeechRec();
            speechRecognition.continuous = false;
            speechRecognition.interimResults = false;

            speechRecognition.onstart = function () {
                isListening = true;
                micBtn.classList.add('recording');
                micBtn.title = "Listening... Speak now";
                chatInput.placeholder = "Listening... speak now...";
            };

            speechRecognition.onresult = function (event) {
                if (event.results && event.results[0] && event.results[0][0]) {
                    const spokenText = event.results[0][0].transcript;
                    chatInput.value = spokenText;
                    stopSpeechRecognition();
                    handleUserInput(spokenText);
                }
            };

            speechRecognition.onerror = function (event) {
                console.warn("Speech recognition error:", event.error);
                stopSpeechRecognition();
            };

            speechRecognition.onend = function () {
                stopSpeechRecognition();
            };
        } catch (e) {
            console.warn("Could not initialize SpeechRecognition:", e);
        }
    }

    function toggleSpeechRecognition() {
        if (!speechRecognition) {
            appendBotMessage("⚠️ Voice recognition is not supported by your current browser. You can type your message in the chat box.");
            return;
        }

        if (isListening) {
            stopSpeechRecognition();
        } else {
            startSpeechRecognition();
        }
    }

    function startSpeechRecognition() {
        if (!speechRecognition) return;
        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;
        const config = LANGUAGE_CONFIG[effectiveLang] || LANGUAGE_CONFIG.en;
        speechRecognition.lang = config.bcp47 || "en-IN";

        try {
            speechRecognition.start();
        } catch (e) {
            console.warn("Failed to start speech recognition:", e);
            stopSpeechRecognition();
        }
    }

    function stopSpeechRecognition() {
        isListening = false;
        if (micBtn) {
            micBtn.classList.remove('recording');
            micBtn.title = "Speak in your language (Voice to Text)";
        }
        if (chatInput) {
            chatInput.placeholder = "Type or click mic to speak...";
        }
        if (speechRecognition) {
            try { speechRecognition.stop(); } catch (e) {}
        }
    }

    function speakText(text, btnElement) {
        if (!('speechSynthesis' in window)) {
            alert("Speech synthesis is not supported in your browser.");
            return;
        }

        window.speechSynthesis.cancel(); // Stop any ongoing speech

        if (btnElement && btnElement.classList.contains('speaking')) {
            btnElement.classList.remove('speaking');
            return;
        }

        const cleanText = text.replace(/[*_#`]/g, '').trim();
        const utterance = new SpeechSynthesisUtterance(cleanText);

        const effectiveLang = (currentBotLang === 'auto') ? detectedLang : currentBotLang;
        const config = LANGUAGE_CONFIG[effectiveLang] || LANGUAGE_CONFIG.en;
        utterance.lang = config.bcp47 || "en-IN";
        utterance.rate = 1.0;

        if (btnElement) {
            btnElement.classList.add('speaking');
            utterance.onend = () => btnElement.classList.remove('speaking');
            utterance.onerror = () => btnElement.classList.remove('speaking');
        }

        window.speechSynthesis.speak(utterance);
    }

    // ========================================================
    // UI HELPERS & MESSAGE RENDERING
    // ========================================================
    function appendUserMessage(text) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'chat-message user-message';
        msgDiv.textContent = text;
        chatMessages.insertBefore(msgDiv, typingIndicator);
        chatHistory.push({ role: 'user', content: text });
        saveHistoryToStorage();
        scrollToBottom();
    }

    function appendBotMessage(markdownText) {
        const wrapper = document.createElement('div');
        wrapper.className = 'bot-message-wrapper';

        const msgDiv = document.createElement('div');
        msgDiv.className = 'chat-message bot-message';

        // Format basic markdown
        let formatted = markdownText
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/`([^`]+)`/g, '<code class="bg-light px-1 rounded">$1</code>')
            .replace(/\n/g, '<br>');

        msgDiv.innerHTML = formatted;

        // Action buttons (Speaker TTS & Copy)
        const actionsDiv = document.createElement('div');
        actionsDiv.className = 'bot-msg-actions';

        const speakBtn = document.createElement('button');
        speakBtn.type = 'button';
        speakBtn.className = 'bot-action-btn bot-speak-btn';
        speakBtn.title = "Read response aloud";
        speakBtn.innerHTML = '<i class="fas fa-volume-up"></i> Listen';
        speakBtn.addEventListener('click', function () {
            speakText(msgDiv.innerText, this);
        });

        const copyBtn = document.createElement('button');
        copyBtn.type = 'button';
        copyBtn.className = 'bot-action-btn';
        copyBtn.title = "Copy message";
        copyBtn.innerHTML = '<i class="far fa-copy"></i> Copy';
        copyBtn.addEventListener('click', function () {
            navigator.clipboard.writeText(msgDiv.innerText).then(() => {
                this.innerHTML = '<i class="fas fa-check text-success"></i> Copied';
                setTimeout(() => { this.innerHTML = '<i class="far fa-copy"></i> Copy'; }, 2000);
            });
        });

        actionsDiv.appendChild(speakBtn);
        actionsDiv.appendChild(copyBtn);

        wrapper.appendChild(msgDiv);
        wrapper.appendChild(actionsDiv);

        chatMessages.insertBefore(wrapper, typingIndicator);
        scrollToBottom();
    }

    function renderSuggestions(suggestions) {
        const row = document.getElementById('chatbotSuggestions');
        if (!row) return;
        row.innerHTML = '';
        suggestions.forEach(s => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'suggestion-chip';
            chip.textContent = s;
            chip.addEventListener('click', function () {
                handleUserInput(s);
            });
            row.appendChild(chip);
        });
    }

    function showTyping(show) {
        if (show) {
            typingIndicator.classList.remove('d-none');
        } else {
            typingIndicator.classList.add('d-none');
        }
        scrollToBottom();
    }

    function scrollToBottom() {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function resetChat() {
        complaintFlow = { active: false, step: 0 };
        chatHistory = [];
        sessionStorage.removeItem('civic_chat_history');
        const elements = chatMessages.querySelectorAll('.chat-message:not(.typing-indicator), .bot-message-wrapper, .complaint-flow-card, .complaint-success-box');
        elements.forEach(el => el.remove());
        renderWelcome();
    }

    function saveHistoryToStorage() {
        try {
            sessionStorage.setItem('civic_chat_history', JSON.stringify(chatHistory.slice(-15)));
        } catch (e) {}
    }

    function loadHistoryFromStorage() {
        try {
            const raw = sessionStorage.getItem('civic_chat_history');
            if (raw) {
                const parsed = JSON.parse(raw);
                if (Array.isArray(parsed) && parsed.length > 0) {
                    chatHistory = parsed;
                    chatHistory.forEach(item => {
                        if (item.role === 'user') {
                            const msgDiv = document.createElement('div');
                            msgDiv.className = 'chat-message user-message';
                            msgDiv.textContent = item.content;
                            chatMessages.insertBefore(msgDiv, typingIndicator);
                        } else {
                            appendBotMessage(item.content);
                        }
                    });
                    scrollToBottom();
                }
            }
        } catch (e) {}
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return String(text).replace(/[&<>"']/g, m => map[m]);
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initChatbot);
    } else {
        initChatbot();
    }

})();
