# RhythmLens

**RhythmLens** is an Android and server-side system for digitizing paper ECGs, restoring missing signal fragments, running automated ECG analysis, and supporting doctor–patient review workflows.

The project is designed around a complete processing pipeline:

```text
paper ECG image → mobile upload → server processing → digital 12-lead ECG → signal completion → AI analysis → doctor review → synthetic ECG image → comparison over time
```

> RhythmLens is a research and educational project. Automated ECG analysis is intended to support review and must not replace clinical interpretation by a qualified physician.

---

## Key Features

### Android application

- Authentication and registration with two roles: **patient** and **doctor**.
- Patient invitation codes and doctor–patient linking.
- ECG upload from camera, gallery, or digital file.
- Local storage with Room for users, patients, ECG records, signals, predictions, and doctor conclusions.
- Home dashboard with the selected patient, latest ECG, processing status, and compact statistics.
- ECG history with compact status indicators for processing, success, and errors.
- Doctor patient list with selection by invitation code.
- ECG detail screen with AI results, doctor conclusion, signal metadata, and 12-lead visualization.
- Interactive ECG charts with separate coloring for digitized and reconstructed fragments.
- Signal display modes: full final signal or only digitized fragments.
- ECG comparison screen with per-lead alignment and overlaid waveforms.
- Synthetic ECG image viewer with regeneration, saving, and sharing.
- Russian on-device translation and short explanations for ECG analysis classes.
- Light and dark themes with local preference persistence.

### Backend

- FastAPI backend with PostgreSQL storage.
- User registration, authentication, and role-aware access control.
- Doctor–patient relationship management by invitation code.
- ECG record storage with processing status and metadata.
- Paper ECG digitization pipeline integration.
- Signal completion pipeline integration for missing leads and incomplete 10-second fragments.
- Automated ECG analysis result storage.
- Doctor conclusion API for synchronized clinical comments.
- 12-lead signal API with segment-level origin metadata.
- Synthetic ECG image generation through a separate service.
- Docker-based deployment for backend and processing services.

---

## ECG Processing Pipeline

1. **Upload**
   
   A patient or doctor uploads an ECG image from the mobile application. Doctors can upload ECGs for the currently selected patient.

2. **Digitization**
   
   The server extracts a digital ECG signal from the paper ECG image and stores the result as structured lead data.

3. **Signal completion**
   
   Missing leads and incomplete fragments are reconstructed. The final signal keeps original digitized fragments where available and fills gaps with reconstructed data.

4. **Automated analysis**
   
   The processed ECG is analyzed by an AI module. The application displays the most probable classes, translated labels, and short explanations.

5. **Doctor review**
   
   A doctor can write and update a conclusion. The patient can view the conclusion after synchronization.

6. **Synthetic ECG image**
   
   The backend can generate a standardized ECG image from the final digital signal. The generated image can be viewed, saved, shared, or regenerated from the Android app.

7. **Comparison**
   
   Two ECG records from the same patient can be compared. Signals are aligned per lead and displayed as overlaid charts.

---

## Technology Stack

### Android

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Architecture:** Clean Architecture + MVI-style presentation layer
- **Dependency injection:** Hilt
- **Local database:** Room
- **Networking:** Retrofit + Kotlin Serialization
- **Navigation:** type-safe Navigation Compose
- **Camera and media input:** CameraX / Android media pickers
- **Charts:** custom Compose ECG rendering components
- **State handling:** Kotlin Coroutines and Flow

### Backend

- **API:** FastAPI
- **Database:** PostgreSQL
- **Runtime:** Python
- **Deployment:** Docker Compose
- **Synthetic ECG rendering:** ECG-Image-Kit-based service
- **Processing modules:** ECG digitization, signal completion, and automated ECG analysis services

---

## Related Repositories

RhythmLens integrates and builds upon separate ECG processing components:

- [Open-ECG-Digitizer](https://github.com/Akadza/Open-ECG-Digitizer) — paper ECG image digitization.
- [ECG-completion](https://github.com/Akadza/ECG-completion) — restoration and completion of ECG signals.
- [ECGify / ECG Founder](https://github.com/Akadza/ECGify) — earlier ECG application work and project foundation.

---

## Project Structure

```text
RhythmLens/
├── app/                         # Android application
│   └── src/main/java/...         # Kotlin source code
│       ├── data/                 # Room, Retrofit, repositories
│       ├── domain/               # Domain models, repositories, use cases
│       ├── ui/                   # Compose screens, navigation, theme
│       └── util/                 # Utility classes
│
├── server/                       # Backend and processing services
│   ├── app/                      # FastAPI application and routes
│   └── modules/                  # ECG processing and synthetic image services
│
└── README.md
```

---

## Main Screens

- **Home** — selected patient, latest ECG, upload entry point, processing state.
- **History** — ECG record list with status indicators and analysis summary.
- **Patients** — doctor-only patient linking and patient selection.
- **Profile** — account data, patient invite code, selected patient, theme switch, logout.
- **ECG Detail** — AI analysis, doctor conclusion, signal metadata, and 12-lead charts.
- **ECG Comparison** — aligned comparison of two ECG records.
- **Synthetic ECG** — generated ECG image preview, regeneration, saving, and sharing.

---

## Running the Android App

1. Clone the repository:

```bash
git clone https://github.com/Akadza/RhythmLens.git
cd RhythmLens
```

2. Open the project in Android Studio.

3. Configure the backend base URL in the Android network configuration if required.

4. Build and run:

```bash
./gradlew assembleDebug
```

Android 8.0+ is recommended.

---

## Running the Backend

From the server directory:

```bash
cd server
docker compose up -d --build
```

Depending on the deployment environment, update database credentials, service URLs, and Android API base URL before running the full pipeline.

---

## Current Status

The project currently supports the complete end-to-end workflow: account creation, doctor–patient linking, ECG upload, digitization, signal completion, automated analysis, doctor conclusion, synthetic image generation, and ECG comparison.

Further improvements may include extended report export, more advanced clinical validation, and production-grade deployment hardening.

---

## License

No license file is currently specified in this repository.
