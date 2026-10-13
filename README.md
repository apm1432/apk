# CourseStream · TG CourseStream Android App
> **Akshayclasses** Telegram Course Streaming, Video Player & Offline Learning Android Application

Connecting directly to the live backend server:
**`https://sick-nikaniki-shubham8208989-6d7c863a.koyeb.app/`**
Synchronized with the Go source code from:
**`https://github.com/AnataVortex7/TG-coursestreamBot`**

---

## 🌟 Key Features (महत्त्वाची वैशिष्ट्ये)

### 1. 🚫 Zero Fake Data (सर्व ओरिजिनल डेटा)
- **सर्व फेक पॅलेट्स आणि डमी कोर्सेस पूर्णपणे काढले आहेत**:
  - Direct Stream Tester कार्ड आणि फेक सॅम्पल कोर्सेस काढून टाकले आहेत.
  - ॲप थेट तुमच्या **Koyeb सर्व्हरवरून (`/courses`, `/course/:id`)** ओरिजिनल डेटा, कोर्सेस, फोल्डर्स आणि फाइल्स फेच करते.

### 2. 🔑 Real Authentication (खरे लॉगिन पर्याय)
- **📲 Telegram OTP Login**:
  - तुमचा रजिस्टर्ड मोबाईल नंबर किंवा ईमेल प्रविष्ट करा.
  - सर्व्हर `POST /login/otp/send` द्वारे Telegram Bot वर 4-अंकी OTP पाठवतो.
  - 4-अंकी कोड टाकून `Verify & Sign In` केल्यावर Session Cookie (`ak_session`) सुरक्षितपणे ॲप आणि WebView मध्ये सेव्ह होते.
- **🔑 Password Login**:
  - युझरनेम/ईमेल आणि पासवर्ड टाकून त्वरित लॉगिन करा (`POST /login/password`).
- **🛡️ Admin Studio Login**:
  - Admin युझर आणि पासवर्ड टाकून Admin Panel चे संपूर्ण ॲक्सेस अनलॉक करा (`POST /api/admin/login`).

### 3. 🎬 Real Video & Audio Player (लेक्चर प्लेअर)
- **Live Stream Playback**:
  - कोर्सेसमधील लेक्चर्स `/watch/:token` द्वारे प्ले होतात.
  - Go बॅकएंडमधील **`player.html` चा संपूर्ण ओरिजिनल हाय-स्पीड प्लेअर** WebView मध्ये लोड होतो, ज्यात:
    - कस्टम प्ले/पॉज, व्हॉल्यूम, स्पीड कंट्रोल (0.5x ते 2x).
    - डबल टॅप सीक (10 सेकंद मागे/पुढे).
    - युझरचा ईमेल/मोबाईल वॉटरमार्क.
    - सीक बार आणि रेझ्युम वॉचिंग.
- **Native vs Web Player Toggle**:
  - प्लेअर स्क्रीनवर युझर एका क्लिकवर **Web Player** किंवा **Native Android VideoView Player** मध्ये स्विच करू शकतात.

### 4. 📄 100% Working PDF Viewer (ऑफलाइन व ऑनलाइन)
- **Native Android `PdfRenderer` इंजिन**:
  - याआधी गुगल डॉक्स व्ह्यूअर सेशन कुकी नसल्याने एरर देत होता.
  - आता ॲप **Android SDK चा Native `PdfRenderer`** वापरून PDF चे पेजेस हाय-रिझोल्युशन बिटमॅपमध्ये रेंडर करते.
  - **वैशिष्ट्ये**:
    - मागील/पुढील पेज (Previous/Next Page).
    - पेज नंबर काउंटर (उदा. Page 1 of 24).
    - झूम इन / झूम आउट (Pinch to Zoom).
    - **Open in External App**: सिस्टीम PDF व्ह्यूअर (Drive / Adobe) मध्ये उघडण्यासाठी बटण.

### 5. 🖼️ Real Image Viewer (इमेज व्ह्यूअर)
- ऑनलाइन फोटो थेट ऑथेंटिकेटेड कुकीजसह डाऊनलोड होऊन स्मूथ पिंच-टू-झूम (Pinch-to-zoom & Pan) सह उघडतात.
- गॅलरी किंवा एक्सटर्नल फोटो ॲपमध्ये उघडण्यासाठी FileProvider सपोर्ट.

### 6. 📥 Real Offline Downloads & Playback (ऑफलाइन डाऊनलोड)
- **OkHttp Chunked Download Manager**:
  - व्हिडिओ, ऑडिओ, PDF आणि इमेज बॅकग्राउंडमध्ये डाऊनलोड होतात.
  - लाइव्ह प्रोग्रेस बार (0% ते 100%) आणि डाऊनलोड साईझ दाखवतो.
- **इंटरनेटशिवाय १००% ऑफलाइन प्लेबॅक**:
  - डाऊनलोड पूर्ण झाल्यावर फाईल डिव्हाइसच्या सुरक्षित स्टोरेजमध्ये सेव्ह होते (`Room Database` द्वारे ट्रॅक केली जाते).
  - ऑफलाइन लायब्ररी टॅब (`Offline` Tab) मध्ये जाऊन **कोणत्याही इंटरनेटशिवाय व्हिडिओ प्ले करा, PDF वाचा किंवा फोटो बघा**.

---

## 📱 App Navigation (ॲप नेव्हिगेशन)

1. **📚 Courses Tab**:
   - सर्व लाइव्ह कोर्सेस (Enrolled आणि Available).
   - सर्च बार आणि कॅटेगरी फिल्टर्स.
   - कोर्सवर टॅप केल्यावर फोल्डर आणि लेक्चर्सची संपूर्ण यादी.
2. **📥 Offline Tab**:
   - डिव्हाइसवर डाऊनलोड केलेल्या सर्व फाइल्स.
   - डिव्हाइस स्टोरेज वापर (MB Used & GB Free).
   - इंटरनेट बंद असतानाही थेट प्ले/ओपन बटण.
3. **🛠️ Admin Studio Tab**:
   - ॲडमिन क्रेडेंशियल्ससह प्रोटेक्टेड पॅनेल.
   - टेलिग्राम चॅनेल सिंक (`Trigger Telegram Sync`).
   - सर्व्हर आकडेवारी (टोटल युझर्स, ॲक्टिव्ह स्ट्रीम्स, कोर्सेस, स्टोरेज).

---

## 🛠️ Testing & Verification (ॲप कसे टेस्ट करावे)

1. **पायरी १: ॲप उघडा**
   - ॲप उघडल्यावर वरती **Koyeb Live** हिरवा इंडिकेटर दिसेल.
2. **पायरी २: लॉगिन करा**
   - वरच्या उजव्या बाजूला **Sign In** वर टॅप करा.
   - **Telegram OTP** टॅब निवडा -> तुमचा Telegram-linked ईमेल किंवा मोबाईल नंबर टाका -> **Request OTP** दाबा.
   - तुमच्या Telegram Bot मध्ये आलेला 4-अंकी OTP टाका आणि **Verify OTP & Sign In** करा.
   - *(पर्यायी: युझरनेम/पासवर्डने किंवा ॲडमिन टॅबमध्ये जाऊन ॲडमिन लॉगिन करू शकता)*.
3. **पायरी ३: कोर्सेस पहा आणि स्ट्रीम करा**
   - लॉगिन झाल्यावर **Sync from Server** आयकॉन दाबा.
   - तुमचे सर्व कोर्सेस लोड होतील.
   - कोणत्याही कोर्सवर टॅप करा आणि **Watch** (व्हिडिओ), **Open PDF** (नोट्स) किंवा **View Image** दाबा.
4. **पायरी ४: ऑफलाइन टेस्ट करा**
   - कोणत्याही लेक्चर समोरील **Download** आयकॉन दाबा.
   - प्रोग्रेस १००% झाल्यावर **Offline** टॅबमध्ये जा.
   - इंटरनेट नसतानाही **Play** दाबा — लेक्चर थेट डिव्हाइसवरून प्ले होईल!

---

## 🏗️ Technical Stack
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **UI Framework**: Jetpack Compose (Material 3 Dark Cyber Theme)
- **Local Database**: Room DB (Courses, Lectures, DownloadTasks, WatchHistory)
- **Networking**: OkHttp3 with persistent CookieJar & HTTP/2
- **PDF Engine**: Android Native `PdfRenderer` + Compose GraphicsLayer
- **Video Engine**: Dual Engine (WebView `player.html` + Native Android `VideoView`)
- **Backend**: Go / Gin-Gonic (`TG-coursestreamBot` on Koyeb)
