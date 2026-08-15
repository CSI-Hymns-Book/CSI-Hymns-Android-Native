package com.reyzie.hymns.data

enum class LegalDocumentKind {
    PRIVACY,
    TERMS;

    fun title(language: ConsentManager.LegalLanguage): String = when (this to language) {
        PRIVACY to ConsentManager.LegalLanguage.ENGLISH -> "Privacy Policy"
        PRIVACY to ConsentManager.LegalLanguage.KANNADA -> "ಗೌಪ್ಯತಾ ನೀತಿ"
        TERMS to ConsentManager.LegalLanguage.ENGLISH -> "Terms of Use"
        TERMS to ConsentManager.LegalLanguage.KANNADA -> "ಬಳಕೆಯ ನಿಯಮಗಳು"
        else -> "Legal"
    }
}

data class LegalSection(
    val id: String,
    val heading: String,
    val body: String
)

object LegalDocuments {
    const val LEGACY_PRIVACY_URL = "https://sites.google.com/view/csi-hymns-privacy-policy/home"

    fun sections(
        kind: LegalDocumentKind,
        language: ConsentManager.LegalLanguage
    ): List<LegalSection> = when (kind to language) {
        LegalDocumentKind.PRIVACY to ConsentManager.LegalLanguage.ENGLISH -> privacyEnglish
        LegalDocumentKind.PRIVACY to ConsentManager.LegalLanguage.KANNADA -> privacyKannada
        LegalDocumentKind.TERMS to ConsentManager.LegalLanguage.ENGLISH -> termsEnglish
        LegalDocumentKind.TERMS to ConsentManager.LegalLanguage.KANNADA -> termsKannada
        else -> emptyList()
    }

    val noticeEnglish: String
        get() = """
This notice is given under the Digital Personal Data Protection Act, 2023 and the Digital Personal Data Protection Rules, 2025, independently of other app information.

Data Fiduciary: ${ConsentManager.DATA_FIDUCIARY_NAME}, ${ConsentManager.DATA_FIDUCIARY_REGION}.
Contact for rights, withdrawal, and grievances: ${ConsentManager.GRIEVANCE_EMAIL}

Personal data we may process, and why:
• Account data (email, name, sign-in identifiers) — to create and maintain your optional CSI Hymns account and sync favourites and collections you choose to save.
• App preferences stored on device — to remember theme, instrument, and similar settings.
• Support tickets and a random device ID — only if you report a lyric or audio issue, so we can track and reply.
• Optional analytics (PostHog: app version, device model, Android version, in-app events) — only if you opt in, to improve stability and features. Not required to use the hymn book.
• Optional push notifications (Firebase Cloud Messaging: device push token) — only if you opt in, to send service messages. Not required to use the hymn book.

You may withdraw consent at any time in Settings → Privacy Centre, with the same ease as giving it. Withdrawal of optional analytics or notifications does not block hymn reading. Withdrawal of account-related consent signs you out and stops cloud sync.

You may request access, correction, erasure, grievance redressal, and nomination by emailing ${ConsentManager.GRIEVANCE_EMAIL}. You may complain to the Data Protection Board of India.
        """.trimIndent()

    val noticeKannada: String
        get() = """
ಈ ಸೂಚನೆಯನ್ನು ಡಿಜಿಟಲ್ ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶ ಸಂರಕ್ಷಣಾ ಅಧಿನಿಯಮ, 2023 ಮತ್ತು ಡಿಜಿಟಲ್ ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶ ಸಂರಕ್ಷಣಾ ನಿಯಮಗಳು, 2025 ರ ಅಡಿಯಲ್ಲಿ, ಇತರ ಆ್ಯಪ್ ಮಾಹಿತಿಯಿಂದ ಪ್ರತ್ಯೇಕವಾಗಿ ನೀಡಲಾಗಿದೆ.

ದತ್ತಾಂಶ ನಂಬಿಕೆದಾರರು: ${ConsentManager.DATA_FIDUCIARY_NAME}, ${ConsentManager.DATA_FIDUCIARY_REGION}.
ಹಕ್ಕುಗಳು, ಸಮ್ಮತಿ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆ ಮತ್ತು ದೂರುಗಳಿಗೆ: ${ConsentManager.GRIEVANCE_EMAIL}

ನಾವು ಸಂಸ್ಕರಿಸಬಹುದಾದ ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶ ಮತ್ತು ಉದ್ದೇಶ:
• ಖಾತೆ ದತ್ತಾಂಶ (ಇಮೇಲ್, ಹೆಸರು) — ಐಚ್ಛಿಕ ಖಾತೆ ಮತ್ತು ನೀವು ಉಳಿಸುವ ಮೆಚ್ಚಿನ/ಸಂಗ್ರಹಗಳ ಸಿಂಕ್‌ಗಾಗಿ.
• ಸಾಧನದಲ್ಲಿನ ಆದ್ಯತೆಗಳು — ಥೀಮ್ ಮತ್ತು ಸಂಗೀತ ಸೆಟ್ಟಿಂಗ್‌ಗಳಿಗಾಗಿ.
• ಬೆಂಬಲ ಟಿಕೆಟ್ ಮತ್ತು ಯಾದೃಚ್ಛಿಕ ಸಾಧನ ಗುರುತು — ನೀವು ದೋಷ ವರದಿ ಮಾಡಿದಾಗ ಮಾತ್ರ.
• ಐಚ್ಛಿಕ ವಿಶ್ಲೇಷಣೆ (PostHog) — ನೀವು ಒಪ್ಪಿದರೆ ಮಾತ್ರ. ಗೀತೆಗಳನ್ನು ಓದಲು ಅಗತ್ಯವಿಲ್ಲ.
• ಐಚ್ಛಿಕ ಪುಶ್ ಸೂಚನೆಗಳು (FCM) — ನೀವು ಒಪ್ಪಿದರೆ ಮಾತ್ರ.

ಸೆಟ್ಟಿಂಗ್‌ಗಳು → ಗೌಪ್ಯತಾ ಕೇಂದ್ರದಲ್ಲಿ ಯಾವಾಗ ಬೇಕಾದರೂ ಸಮ್ಮತಿಯನ್ನು ಹಿಂತೆಗೆದುಕೊಳ್ಳಬಹುದು. ನೀವು ಪ್ರವೇಶ, ತಿದ್ದುಪಡಿ, ಅಳಿಸುವಿಕೆ, ದೂರು ನಿವಾರಣೆ ಮತ್ತು ನಾಮನಿರ್ದೇಶನವನ್ನು ${ConsentManager.GRIEVANCE_EMAIL} ಗೆ ಇಮೇಲ್ ಮಾಡಿ ಕೇಳಬಹುದು. ಭಾರತದ ದತ್ತಾಂಶ ಸಂರಕ್ಷಣಾ ಮಂಡಳಿಗೆ ದೂರು ಸಲ್ಲಿಸಬಹುದು.
        """.trimIndent()

    private val privacyEnglish: List<LegalSection>
        get() = listOf(
            LegalSection("who", "1. Who we are", """
CSI Hymns is a bilingual hymn and keerthane reader. The Data Fiduciary for personal data processed through this Android app is ${ConsentManager.DATA_FIDUCIARY_NAME}, based in ${ConsentManager.DATA_FIDUCIARY_REGION}.

Contact: ${ConsentManager.GRIEVANCE_EMAIL}
This policy is version ${ConsentManager.CURRENT_POLICY_VERSION} (15 August 2026).
            """.trimIndent()),
            LegalSection("scope", "2. Scope and law", """
This policy explains how we process digital personal data of Data Principals in India under the Digital Personal Data Protection Act, 2023 (“DPDP Act”) and the Digital Personal Data Protection Rules, 2025.

Hymn lyrics, MIDI files, and Order of Service texts are not personal data. This policy covers personal data about you as a user.
            """.trimIndent()),
            LegalSection("data", "3. Personal data we process", """
Depending on how you use the app, we may process:

Account (only if you sign in): email address, display name, Google sign-in identifiers, and a Supabase user ID.

Cloud content you create (only if you sign in): favourite hymn numbers, custom collection names and song lists, Christmas carol drafts you submit as an authorised editor.

Support (only if you report an issue): ticket text, song number/title, app version, and a random device identifier generated on your phone (not your advertising ID).

Optional analytics (only with separate opt-in): app version, Android version, device model, screen names, and in-app events (for example song opened). We do not sell this data.

Optional notifications (only with separate opt-in): a push token via Firebase Cloud Messaging.

Local-only: theme, page-turn preference, MIDI instrument, on-device playback history. These stay on your device unless you sign in and sync collections.

We do not knowingly collect government ID numbers, precise location, contacts, photos, or payment card numbers in the hymn reader. Donations, if enabled, are processed by the payment provider you choose; we do not store card data on our servers.
            """.trimIndent()),
            LegalSection("purpose", "4. Purposes (purpose limitation)", """
We process personal data only for:
• providing the hymn book and optional account sync you request;
• authenticating you and securing your account;
• handling lyric/audio correction tickets you submit;
• sending optional service notifications you opt into;
• optional product analytics you opt into;
• complying with law and responding to Data Principal requests.

We do not use your personal data for targeted advertising, and we do not sell personal data.
            """.trimIndent()),
            LegalSection("consent", "5. Consent", """
Where consent is the legal basis, it is requested in clear language, is not pre-ticked, and is specific to each purpose. Using the hymn book does not require analytics or push notifications.

You may withdraw consent in Settings → Privacy Centre. Withdrawal is as easy as the original acceptance. Processing that already happened remains lawful. If you withdraw account-related consent, we sign you out and stop cloud sync; you can still read bundled hymns on device after you accept the current notice again if you wish to continue using the app.
            """.trimIndent()),
            LegalSection("processors", "6. Processors and sharing", """
We use service providers (Data Processors) only as needed:
• Supabase (Auth and database, region ap-south-1, India) for accounts and sync.
• PostHog for optional analytics.
• Firebase Cloud Messaging for optional push notifications.
• Google if you use Google sign-in.
• Atlassian Jira if you submit a support ticket.
• GitHub for publicly hosted MIDI/lyric files (not your account profile).

We do not share your account with churches or third-party marketers. Admin operators who maintain lyrics may see support tickets you send.
            """.trimIndent()),
            LegalSection("transfer", "7. Cross-border processing", """
Account data is stored in India (Supabase Mumbai). Some optional processors (analytics, push, Google sign-in, Jira) may process data outside India. Transfers are made only as permitted under the DPDP Act (including to countries not for the time being restricted by the Central Government).
            """.trimIndent()),
            LegalSection("retain", "8. Retention and security", """
Account and sync data are kept while your account exists. Support tickets are kept until the issue is closed and for a reasonable period to prevent repeat errors. Analytics events, if enabled, are kept only as long as needed to improve the app. After account deletion we erase or de-identify personal data we control, except where law requires retention.

We use TLS in transit, access controls, and row-level security on the database. No method of transmission is 100% secure.
            """.trimIndent()),
            LegalSection("rights", "9. Your rights", """
You may:
• access a summary of personal data we hold about you;
• request correction of inaccurate data;
• request erasure (including Delete Account in Settings);
• withdraw consent;
• nominate another person to exercise rights in the event of death or incapacity (email us with the nominee’s name and contact);
• seek grievance redressal from us, and complain to the Data Protection Board of India if unresolved.

We will respond within the time required by law. Email ${ConsentManager.GRIEVANCE_EMAIL}. Do not include unnecessary sensitive data in your request.
            """.trimIndent()),
            LegalSection("children", "10. Children", """
The app is a general hymn book. If you are under 18, a parent or lawful guardian must review this notice and provide verifiable consent before an account is created or optional analytics/notifications are enabled. We do not track children for advertising.
            """.trimIndent()),
            LegalSection("changes", "11. Changes", """
If we change processing in a material way, we will update this policy version and ask for a fresh consent where required. Continued use after a version change is not treated as consent.
            """.trimIndent())
        )

    private val termsEnglish: List<LegalSection>
        get() = listOf(
            LegalSection("agree", "1. Agreement", """
These Terms of Use govern CSI Hymns for Android. By accepting them you enter a licence to use the app for personal, congregational, and non-commercial worship. If you do not agree, do not use the app.

Version ${ConsentManager.CURRENT_POLICY_VERSION}. Contact: ${ConsentManager.GRIEVANCE_EMAIL}.
            """.trimIndent()),
            LegalSection("licence", "2. Hymn content", """
Lyrics, translations, MIDI accompaniments, and Order of Service texts may be owned by the Church of South India, composers, translators, or other rights holders. The app provides a convenient reader; it does not transfer copyright to you. Do not scrape, republish, or commercially exploit the corpus. Report suspected errors in-app rather than redistributing modified sheets as official CSI text.
            """.trimIndent()),
            LegalSection("account", "3. Accounts", """
An account is optional. You must provide accurate information, keep credentials confidential, and not impersonate others. We may suspend accounts that abuse reporting tools, attempt unauthorised admin access, or harm other users.
            """.trimIndent()),
            LegalSection("acceptable", "4. Acceptable use", """
You must not: reverse engineer the app for harm; probe or attack our systems; upload malware; submit false identity data; harass maintainers; or use the app to process others’ personal data unlawfully.
            """.trimIndent()),
            LegalSection("donations", "5. Donations", """
Optional donations help hosting costs. They are not required for hymn access, are not tax advice, and are processed by third-party gateways. Refunds follow the gateway’s rules.
            """.trimIndent()),
            LegalSection("disclaimer", "6. Disclaimer", """
The app is provided “as is” for worship convenience. We do not warrant uninterrupted MIDI streaming, complete lyric accuracy, or fitness for a particular liturgical purpose. Official church publications prevail in case of conflict.
            """.trimIndent()),
            LegalSection("liability", "7. Liability", """
To the extent permitted by Indian law, we are not liable for indirect or consequential loss arising from use of the app. Nothing in these terms limits liability that cannot be limited by law.
            """.trimIndent()),
            LegalSection("law", "8. Governing law", """
These terms are governed by the laws of India. Courts at Bengaluru, Karnataka shall have exclusive jurisdiction, subject to mandatory protections for consumers and Data Principals.
            """.trimIndent())
        )

    private val privacyKannada: List<LegalSection>
        get() = listOf(
            LegalSection("who", "೧. ನಾವು ಯಾರು", """
CSI Hymns ಒಂದು ದ್ವಿಭಾಷಾ ಗೀತೆ/ಕೀರ್ತನೆ ಓದುಗ. ಈ Android ಆ್ಯಪ್‌ನಲ್ಲಿ ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶವನ್ನು ಸಂಸ್ಕರಿಸುವ ದತ್ತಾಂಶ ನಂಬಿಕೆದಾರರು ${ConsentManager.DATA_FIDUCIARY_NAME}, ${ConsentManager.DATA_FIDUCIARY_REGION}.

ಸಂಪರ್ಕ: ${ConsentManager.GRIEVANCE_EMAIL}
ಆವೃತ್ತಿ ${ConsentManager.CURRENT_POLICY_VERSION} (೧೫ ಆಗಸ್ಟ್ ೨೦೨೬).
            """.trimIndent()),
            LegalSection("scope", "೨. ವ್ಯಾಪ್ತಿ ಮತ್ತು ಕಾನೂನು", "ಈ ನೀತಿ ಭಾರತದಲ್ಲಿ ಡಿಜಿಟಲ್ ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶ ಸಂರಕ್ಷಣಾ ಅಧಿನಿಯಮ, 2023 ಮತ್ತು 2025ರ ನಿಯಮಗಳ ಅಡಿಯಲ್ಲಿ ನಿಮ್ಮ ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶವನ್ನು ಹೇಗೆ ಸಂಸ್ಕರಿಸುತ್ತೇವೆ ಎಂಬುದನ್ನು ವಿವರಿಸುತ್ತದೆ. ಗೀತೆಗಳ ಪಠ್ಯ ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶವಲ್ಲ."),
            LegalSection("data", "೩. ಸಂಸ್ಕರಿಸುವ ದತ್ತಾಂಶ", """
ಖಾತೆ (ಸೈನ್ ಇನ್ ಮಾಡಿದರೆ): ಇಮೇಲ್, ಹೆಸರು, Google ಗುರುತು.
ಕ್ಲೌಡ್ ವಿಷಯ: ಮೆಚ್ಚಿನ ಗೀತೆಗಳು ಮತ್ತು ನಿಮ್ಮ ಸಂಗ್ರಹಗಳು.
ಬೆಂಬಲ: ನೀವು ವರದಿ ಮಾಡಿದ ಟಿಕೆಟ್ ಪಠ್ಯ ಮತ್ತು ಯಾದೃಚ್ಛಿಕ ಸಾಧನ ಗುರುತು.
ಐಚ್ಛಿಕ ವಿಶ್ಲೇಷಣೆ ಮತ್ತು ಪುಶ್ ಸೂಚನೆಗಳು: ಪ್ರತ್ಯೇಕ ಒಪ್ಪಿಗೆ ಇದ್ದಾಗ ಮಾತ್ರ.
ನಾವು ಜಾಹೀರಾತಿಗಾಗಿ ದತ್ತಾಂಶವನ್ನು ಮಾರಾಟ ಮಾಡುವುದಿಲ್ಲ.
            """.trimIndent()),
            LegalSection("purpose", "೪. ಉದ್ದೇಶಗಳು", "ಗೀತೆ ಪುಸ್ತಕ ಒದಗಿಸುವುದು, ಐಚ್ಛಿಕ ಖಾತೆ ಸಿಂಕ್, ಭದ್ರತೆ, ನೀವು ಕಳುಹಿಸಿದ ದೋಷ ವರದಿಗಳು, ನೀವು ಒಪ್ಪಿದ ವಿಶ್ಲೇಷಣೆ/ಸೂಚನೆಗಳು, ಮತ್ತು ಕಾನೂನು ಪಾಲನೆ."),
            LegalSection("consent", "೫. ಸಮ್ಮತಿ", "ಸಮ್ಮತಿ ಮುಂತಿಳಿಸದೆ ಗುರುತು ಹಾಕಲಾಗುವುದಿಲ್ಲ. ಪ್ರತಿ ಉದ್ದೇಶಕ್ಕೆ ಪ್ರತ್ಯೇಕ ಆಯ್ಕೆ. ಸೆಟ್ಟಿಂಗ್‌ಗಳು → ಗೌಪ್ಯತಾ ಕೇಂದ್ರದಲ್ಲಿ ಹಿಂತೆಗೆದುಕೊಳ್ಳಬಹುದು."),
            LegalSection("processors", "೬. ಸಂಸ್ಕಾರಕರು", "Supabase (ಭಾರತ), ಐಚ್ಛಿಕ PostHog ಮತ್ತು Firebase Cloud Messaging, Google ಸೈನ್-ಇನ್, ಬೆಂಬಲಕ್ಕೆ Jira. ಚರ್ಚುಗಳಿಗೆ ನಿಮ್ಮ ಖಾತೆಯನ್ನು ಹಂಚುವುದಿಲ್ಲ."),
            LegalSection("transfer", "೭. ಗಡಿ ದಾಟಿದ ಸಂಸ್ಕರಣೆ", "ಖಾತೆ ದತ್ತಾಂಶ ಭಾರತದಲ್ಲಿ (Mumbai) ಇರುತ್ತದೆ. ಕೆಲವು ಐಚ್ಛಿಕ ಸೇವೆಗಳು ಭಾರತದ ಹೊರಗೆ ಸಂಸ್ಕರಿಸಬಹುದು, DPDP ಅಧಿನಿಯಮದ ಅನುಮತಿಯಂತೆ."),
            LegalSection("retain", "೮. ಇಡುವ ಅವಧಿ ಮತ್ತು ಭದ್ರತೆ", "ಖಾತೆ ಇರುವವರೆಗೆ ಖಾತೆ ದತ್ತಾಂಶ. ಖಾತೆ ಅಳಿಸಿದ ನಂತರ ನಾವು ನಿಯಂತ್ರಿಸುವ ವೈಯಕ್ತಿಕ ದತ್ತಾಂಶವನ್ನು ಅಳಿಸುತ್ತೇವೆ ಅಥವಾ ಗುರುತು ತೆಗೆಯುತ್ತೇವೆ, ಕಾನೂನು ಬೇಡಿಕೆಯನ್ನು ಹೊರತುಪಡಿಸಿ."),
            LegalSection("rights", "೯. ನಿಮ್ಮ ಹಕ್ಕುಗಳು", "ಪ್ರವೇಶ, ತಿದ್ದುಪಡಿ, ಅಳಿಸುವಿಕೆ, ಸಮ್ಮತಿ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆ, ನಾಮನಿರ್ದೇಶನ, ದೂರು ನಿವಾರಣೆ, ಮತ್ತು ದತ್ತಾಂಶ ಸಂರಕ್ಷಣಾ ಮಂಡಳಿಗೆ ದೂರು. ${ConsentManager.GRIEVANCE_EMAIL} ಗೆ ಬರೆಯಿರಿ."),
            LegalSection("children", "೧೦. ಮಕ್ಕಳು", "೧೮ ವರ್ಷಕ್ಕಿಂತ ಕಡಿಮೆ ಇದ್ದರೆ, ಖಾತೆ ಅಥವಾ ಐಚ್ಛಿಕ ವಿಶ್ಲೇಷಣೆಗೆ ಪೋಷಕ/ಪಾಲಕರ ಸಮ್ಮತಿ ಬೇಕು."),
            LegalSection("changes", "೧೧. ಬದಲಾವಣೆಗಳು", "ಮುಖ್ಯ ಬದಲಾವಣೆಯಾದಾಗ ಆವೃತ್ತಿ ನವೀಕರಿಸಿ ಹೊಸ ಸಮ್ಮತಿ ಕೇಳಲಾಗುತ್ತದೆ. ಬಳಕೆಯನ್ನು ಮುಂದುವರಿಸುವುದು ಸಮ್ಮತಿಯಲ್ಲ.")
        )

    private val termsKannada: List<LegalSection>
        get() = listOf(
            LegalSection("agree", "೧. ಒಪ್ಪಂದ", "ಈ ನಿಯಮಗಳು CSI Hymns Android ಬಳಕೆಯನ್ನು ನಿಯಂತ್ರಿಸುತ್ತವೆ. ಒಪ್ಪಿದರೆ ವೈಯಕ್ತಿಕ ಮತ್ತು ಆರಾಧನಾ ಬಳಕೆಗೆ ಪರವಾನಗಿ. ಒಪ್ಪದಿದ್ದರೆ ಆ್ಯಪ್ ಬಳಸಬೇಡಿ. ಆವೃತ್ತಿ ${ConsentManager.CURRENT_POLICY_VERSION}."),
            LegalSection("licence", "೨. ಗೀತೆ ವಿಷಯ", "ಸಾಹಿತ್ಯ, ಅನುವಾದ, MIDI ಮತ್ತು ಆರಾಧನಾ ಕ್ರಮ ಹಕ್ಕುಸ್ವಾಮ್ಯದ ಮೇಲೆ ಇತರರಿಗೆ ಸೇರಿರಬಹುದು. ವಾಣಿಜ್ಯ ಮರುಪ್ರಕಟಣೆ ನಿಷಿದ್ಧ."),
            LegalSection("account", "೩. ಖಾತೆಗಳು", "ಖಾತೆ ಐಚ್ಛಿಕ. ಸತ್ಯವಾದ ಮಾಹಿತಿ ನೀಡಿ, ಗುಪ್ತಪದ ರಕ್ಷಿಸಿ. ದುರುಪಯೋಗವಾದರೆ ಖಾತೆ ನಿಲ್ಲಿಸಬಹುದು."),
            LegalSection("acceptable", "೪. ಸ್ವೀಕಾರಾರ್ಹ ಬಳಕೆ", "ಹಾನಿಕಾರಕ ರಿವರ್ಸ್ ಎಂಜಿನಿಯರಿಂಗ್, ದಾಳಿ, ಮಾಲ್‌ವೇರ್, ನಕಲಿ ಗುರುತು ಅಥವಾ ಇತರರ ದತ್ತಾಂಶದ ಅಕ್ರಮ ಸಂಸ್ಕರಣೆ ನಿಷಿದ್ಧ."),
            LegalSection("donations", "೫. ದೇಣಿಗೆ", "ದೇಣಿಗೆ ಐಚ್ಛಿಕ ಮತ್ತು ಹೋಸ್ಟಿಂಗ್ ವೆಚ್ಚಕ್ಕೆ ಸಹಾಯ. ಕಾರ್ಡ್ ದತ್ತಾಂಶವನ್ನು ನಾವು ಇಡುವುದಿಲ್ಲ."),
            LegalSection("disclaimer", "೬. ಹಕ್ಕುತ್ಯಾಗ", "ಆ್ಯಪ್ “ಇರುವಂತೆ” ಒದಗಿಸಲಾಗಿದೆ. ಅಧಿಕೃತ ಚರ್ಚ್ ಪ್ರಕಟಣೆಗಳೇ ಪ್ರಮುಖ."),
            LegalSection("liability", "೭. ಹೊಣೆ", "ಭಾರತೀಯ ಕಾನೂನು ಅನುಮತಿಸುವ ಮಟ್ಟಿಗೆ ಪರೋಕ್ಷ ನಷ್ಟಕ್ಕೆ ಹೊಣೆಯಲ್ಲ. ಕಾನೂನು ಮಿತಿಗೊಳಿಸಲಾಗದ ಹೊಣೆಯನ್ನು ಇವು ಕಡಿಮೆ ಮಾಡುವುದಿಲ್ಲ."),
            LegalSection("law", "೮. ಕಾನೂನು", "ಭಾರತದ ಕಾನೂನು ಅನ್ವಯ. ಬೆಂಗಳೂರು ನ್ಯಾಯಾಲಯಗಳಿಗೆ ವಿಶೇಷ ವ್ಯಾಪ್ತಿ, ಗ್ರಾಹಕ ಮತ್ತು ದತ್ತಾಂಶ ಹಕ್ಕುಗಳಿಗೆ ಒಳಪಟ್ಟು.")
        )
}
