/**
 * CivicConnect - Intelligent AI Chatbot Engine (CivicBot)
 * Multilingual Assistant supporting all 22 Indian Languages + English
 */

(function () {
    'use strict';

    // State
    let isChatOpen = false;
    let isMaximized = false;
    let chatHistory = [];
    let currentBotLang = localStorage.getItem('civic_lang') || 'en';

    // Elements
    let launcherBtn, chatWindow, chatMessages, chatInput, sendBtn, typingIndicator;

    const BOT_NAMES = {
        en: "CivicBot AI",
        hi: "सिविकबॉट एआई",
        bn: "সিভিকবট এআই",
        te: "సివిక్‌బాట్ ఏఐ",
        ta: "சிவிக்பாட் ஏஐ",
        mr: "सिव्हिकबॉट एआय",
        ur: "سیوک بوٹ اے آئی",
        gu: "સિવિકબૉટ એઆઈ",
        kn: "ಸಿವಿಕ್‌ಬಾಟ್ ಎಐ",
        ml: "സിവിക്ബോട്ട് എഐ",
        pa: "ਸਿਵਿਕਬੋਟ ਏਆਈ",
        or: "ସିଭିକବଟ୍ ଏଆଇ"
    };

    const WELCOME_MESSAGES = {
        en: "Hello! I am **CivicBot**, your AI municipal assistant. Ask me anything about reporting issues, tracking complaints, our 3D Map of India, or municipal services across all Indian states!",
        hi: "नमस्ते! मैं **सिविकबॉट** हूँ, आपका एआई नागरिक सहायक। शिकायत दर्ज करने, स्थिति ट्रैक करने, 3D इंडिया मैप देखने या नगर निगम सेवाओं के बारे में कुछ भी पूछें!",
        bn: "নমস্কার! আমি **সিভিকবট**। অভিযোগ দাখিল, ট্র্যাকিং, ভারতের 3D ম্যাপ বা পৌরসেবা সংক্রান্ত যেকোনো তথ্য জানতে আমাকে প্রশ্ন করুন!",
        te: "నమస్కారం! నేను **సివిక్‌బాట్** మీ ఏఐ సహాయకుడిని. సమస్యల నమోదు, ట్రాకింగ్ మరియు భారతదేశ 3D మ్యాప్ గురించి ఏమైనా అడగండి!",
        ta: "வணக்கம்! நான் **சிவிக்பாட்** உங்கள் ஏஐ உதவியாளர். புகார்கள் பதிவு செய்தல், 3D வரைபடம் அல்லது பொதுச் சேவைகள் பற்றி எதையும் கேளுங்கள்!",
        mr: "नमस्कार! मी **सिव्हिकबॉट**, आपला एआय सहाय्यक. तक्रार नोंदणी, ट्रॅकिंग, 3D नकाशा किंवा नागरी सेवांविषयी काहीही विचारा!",
        ur: "آداب! میں **سیوک بوٹ** ہوں، آپ کا اے آئی معاون۔ بلدیاتی شکایات، ٹریکنگ یا 3D نقشے کے متعلق کوئی بھی سوال پوچھیے!",
        kn: "ನಮಸ್ಕಾರ! ನಾನು **ಸಿವಿಕ್‌ಬಾಟ್** ಎಐ ಸಹಾಯಕ. ದೂರು ಸಲ್ಲಿಕೆ, ಟ್ರ್ಯಾಕಿಂಗ್ ಅಥವಾ 3D ನಕ್ಷೆಯ ಬಗ್ಗೆ ಯಾವುದೇ ಮಾಹಿತಿ ಕೇಳಿ!",
        ml: "നമസ്കാരം! ഞാൻ **സിവിക്ബോട്ട്** എഐ അസിസ്റ്റന്റ്. പരാതികൾ നൽകൽ, ട്രാക്കിംഗ്, 3D ഭൂപടം എന്നിവയെക്കുറിച്ച് ചോദിക്കാം!",
        gu: "નમસ્તે! હું **સિવિકબૉટ** છું. ફરિયાદ નોંધણી, ટ્રેકિંગ અને 3D ઇન્ડિયા મેપ વિશે કંઈ પણ પૂછો!"
    };

    function initChatbot() {
        createChatbotDOM();
        loadHistoryFromStorage();
        bindEvents();

        // Listen for global language switch from navbar
        window.addEventListener('civicLanguageChanged', function (e) {
            if (e.detail && e.detail.lang) {
                currentBotLang = e.detail.lang;
                updateBotHeaderLanguage();
            }
        });
    }

    function createChatbotDOM() {
        // Floating Widget Wrapper
        const widgetWrapper = document.createElement('div');
        widgetWrapper.id = 'civicChatbotRoot';
        widgetWrapper.className = 'civic-chatbot-wrapper';

        widgetWrapper.innerHTML = `
            <!-- Floating 3D Launcher Button -->
            <button id="chatbotLauncher" class="chatbot-launcher-btn shadow-lg" title="Chat with CivicBot AI / एआई सहायक से बात करें" aria-label="Open AI Chatbot">
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
                                <span id="botHeaderTitle">CivicBot AI</span>
                                <span class="badge bg-warning text-dark px-1 py-0 small" style="font-size:0.65rem;">22 Lang</span>
                            </div>
                            <small class="text-light opacity-75 d-flex align-items-center gap-1">
                                <span class="status-dot"></span> Online &bull; Municipal Assistant
                            </small>
                        </div>
                    </div>
                    <div class="d-flex align-items-center gap-1">
                        <!-- Language selector inside bot -->
                        <div class="dropdown">
                            <button class="btn btn-sm btn-link text-white p-1" type="button" id="botLangSelectBtn" data-bs-toggle="dropdown" aria-expanded="false" title="Change Language">
                                <i class="fas fa-language fa-lg text-warning"></i>
                            </button>
                            <ul class="dropdown-menu dropdown-menu-end p-1 shadow border-0" id="botLangDropdown" style="max-height:220px; overflow-y:auto; font-size:0.85rem;">
                                <li><a class="dropdown-item" href="#" data-lang="en">English</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="hi">हिन्दी (Hindi)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="bn">বাংলা (Bengali)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="te">తెలుగు (Telugu)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="mr">मराठी (Marathi)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="ta">தமிழ் (Tamil)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="ur">اردو (Urdu)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="gu">ગુજરાતી (Gujarati)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="kn">ಕನ್ನಡ (Kannada)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="ml">മലയാളം (Malayalam)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="or">ଓଡ଼ିଆ (Odia)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="pa">ਪੰਜਾਬੀ (Punjabi)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="as">অসমীয়া (Assamese)</a></li>
                                <li><a class="dropdown-item" href="#" data-lang="sa">संस्कृतम् (Sanskrit)</a></li>
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
                        <!-- Minimize / Close -->
                        <button id="chatbotCloseBtn" class="btn btn-sm btn-link text-white p-1" title="Close">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                </div>

                <!-- Messages Container -->
                <div id="chatbotMessages" class="chatbot-messages p-3">
                    <!-- Typing Indicator -->
                    <div id="chatbotTyping" class="chat-message bot-message typing-indicator d-none">
                        <span></span><span></span><span></span>
                    </div>
                </div>

                <!-- Suggestions Row -->
                <div id="chatbotSuggestions" class="chatbot-suggestions px-3 py-2 border-top bg-light d-flex gap-1 overflow-auto">
                    <!-- Suggested question chips populated dynamically -->
                </div>

                <!-- Input Footer -->
                <div class="chatbot-footer p-2 border-top bg-white">
                    <form id="chatbotForm" class="d-flex align-items-center gap-2 m-0">
                        <input type="text" id="chatbotInput" class="form-control form-control-sm rounded-pill px-3 py-2 border-1" placeholder="Type your message..." autocomplete="off">
                        <button type="submit" id="chatbotSendBtn" class="btn btn-success btn-sm rounded-circle shadow-sm" style="width:38px; height:38px;" title="Send">
                            <i class="fas fa-paper-plane"></i>
                        </button>
                    </form>
                    <div class="d-flex justify-content-between align-items-center px-2 pt-1 text-muted" style="font-size:0.68rem;">
                        <span>Powered by CivicConnect AI</span>
                        <span>22 Indian Languages Supported</span>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(widgetWrapper);

        launcherBtn = document.getElementById('chatbotLauncher');
        chatWindow = document.getElementById('chatbotWindow');
        chatMessages = document.getElementById('chatbotMessages');
        chatInput = document.getElementById('chatbotInput');
        sendBtn = document.getElementById('chatbotSendBtn');
        typingIndicator = document.getElementById('chatbotTyping');
    }

    function bindEvents() {
        // Toggle Open/Close
        launcherBtn.addEventListener('click', toggleChatWindow);
        document.getElementById('chatbotCloseBtn').addEventListener('click', toggleChatWindow);

        // Maximize / Restore
        document.getElementById('chatbotMaximizeBtn').addEventListener('click', toggleMaximize);

        // Clear Chat
        document.getElementById('chatbotClearBtn').addEventListener('click', clearChatHistory);

        // Bot language selector dropdown
        document.querySelectorAll('#botLangDropdown a').forEach(a => {
            a.addEventListener('click', function (e) {
                e.preventDefault();
                const lang = this.getAttribute('data-lang');
                if (lang) {
                    currentBotLang = lang;
                    updateBotHeaderLanguage();
                    if (window.CivicI18n && typeof window.CivicI18n.setLanguage === 'function') {
                        window.CivicI18n.setLanguage(lang);
                    }
                }
            });
        });

        // Form Submit
        document.getElementById('chatbotForm').addEventListener('submit', function (e) {
            e.preventDefault();
            handleUserMessage();
        });
    }

    function toggleChatWindow() {
        isChatOpen = !isChatOpen;
        if (isChatOpen) {
            chatWindow.classList.remove('d-none');
            launcherBtn.classList.add('active');
            chatInput.focus();
            if (chatHistory.length === 0) {
                renderWelcomeMessage();
            }
        } else {
            chatWindow.classList.add('d-none');
            launcherBtn.classList.remove('active');
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

    function updateBotHeaderLanguage() {
        const titleEl = document.getElementById('botHeaderTitle');
        if (titleEl) {
            titleEl.textContent = BOT_NAMES[currentBotLang] || "CivicBot AI";
        }
    }

    function renderWelcomeMessage() {
        const welcomeText = WELCOME_MESSAGES[currentBotLang] || WELCOME_MESSAGES.en;
        appendBotMessage(welcomeText);
        renderSuggestions([
            "How to report a civic issue?",
            "Track complaint status",
            "Open 3D India Map",
            "How does duplicate detection work?"
        ]);
    }

    function handleUserMessage(customMsg) {
        const text = customMsg || chatInput.value.trim();
        if (!text) return;

        // 1. Append user message to UI
        appendUserMessage(text);
        chatInput.value = '';

        // 2. Add to history
        chatHistory.push({ role: 'user', content: text });
        saveHistoryToStorage();

        // 3. Show typing indicator
        showTyping(true);

        // 4. Send request to backend API
        fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: text,
                language: currentBotLang,
                history: chatHistory.slice(-6)
            })
        })
        .then(res => {
            if (!res.ok) throw new Error("Status " + res.status);
            return res.json();
        })
        .then(data => {
            showTyping(false);
            const reply = data.reply || "I am here to assist you with municipal problem reporting and resolution in CivicConnect.";
            appendBotMessage(reply);
            chatHistory.push({ role: 'assistant', content: reply });
            saveHistoryToStorage();

            if (data.suggestions && data.suggestions.length > 0) {
                renderSuggestions(data.suggestions);
            }
        })
        .catch(err => {
            console.warn("AI Chat API call failed, generating fallback response: ", err);
            showTyping(false);
            const fallbackReply = "I am ready to help! You can report civic problems, track reference IDs, or explore our 3D Map of India using the navigation above.";
            appendBotMessage(fallbackReply);
            chatHistory.push({ role: 'assistant', content: fallbackReply });
            saveHistoryToStorage();
        });
    }

    function appendUserMessage(text) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'chat-message user-message';
        msgDiv.textContent = text;
        chatMessages.insertBefore(msgDiv, typingIndicator);
        scrollToBottom();
    }

    function appendBotMessage(markdownText) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'chat-message bot-message';
        // Parse bold and linebreaks simply
        let formatted = markdownText
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\n/g, '<br>');
        msgDiv.innerHTML = formatted;
        chatMessages.insertBefore(msgDiv, typingIndicator);
        scrollToBottom();
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

    function renderSuggestions(suggestions) {
        const row = document.getElementById('chatbotSuggestions');
        if (!row) return;
        row.innerHTML = '';
        suggestions.forEach(s => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'btn btn-outline-success btn-sm suggestion-chip text-nowrap rounded-pill py-1 px-2';
            chip.style.fontSize = '0.74rem';
            chip.textContent = s;
            chip.addEventListener('click', function () {
                handleUserMessage(s);
            });
            row.appendChild(chip);
        });
    }

    function clearChatHistory() {
        chatHistory = [];
        sessionStorage.removeItem('civic_chat_history');
        // Clear all message bubbles except typing indicator
        const bubbles = chatMessages.querySelectorAll('.chat-message:not(.typing-indicator)');
        bubbles.forEach(b => b.remove());
        renderWelcomeMessage();
    }

    function saveHistoryToStorage() {
        try {
            sessionStorage.setItem('civic_chat_history', JSON.stringify(chatHistory.slice(-10)));
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
                            appendUserMessage(item.content);
                        } else {
                            appendBotMessage(item.content);
                        }
                    });
                }
            }
        } catch (e) {}
    }

    // Initialize on page load
    document.addEventListener('DOMContentLoaded', initChatbot);

})();
