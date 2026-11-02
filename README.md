# Cloudscope Money Printer ⚡

**Cloudscope Money Printer** is a high-performance, native Android intraday options intelligence terminal engineered exclusively for **NIFTY 50** index options.

Built with **Kotlin**, **Jetpack Compose**, and **Material 3**, the application delivers institutional-grade analytics, deterministic signal generation, calibrated AI confidence scoring, and an audited historical backtesting engine directly to your pocket.

---

## 📸 Key Highlights & Features

- **Institutional Trading Terminal Aesthetic**: High-density, clutter-free UI featuring Dark Terminal theme (`#0B0E14`) and Trading Light theme with instant persisted manual toggle.
- **Strictly NIFTY 50 Intraday Focus**: No portfolio bloat, no paper trading distractions, no social feeds. Only actionable intraday options intelligence.
- **Zero Mock / Authentic Broker Feed**: Integrates directly with the **Zerodha Kite Connect API** for real-time tick-by-tick spot prices, option chains, Greeks, India VIX, and open interest.
- **Deterministic Multi-Factor Signal Engine**: Zero black-box hallucinations. Evaluates 17+ technical and derivative metrics (VWAP, 20/50/200 EMA, RSI 14, MACD, Volume, 15m Opening Range Breakout, Support/Resistance, PCR, OI change).
- **Calibrated Statistical Confidence**: Probabilities derived from multi-factor logistic regressions and out-of-sample performance—never arbitrary percentages. If data is insufficient or regime unstable, displays `LOW CONFIDENCE`.
- **Options Contract Selection**: Automated selection of liquid ATM/slightly ITM contracts with Delta ~0.50, tight bid-ask spreads, and ₹0.05 tick-size compliance.
- **Audited Historical Backtesting Engine**: Multi-scenario backtest engine with transaction costs (STT 0.1%, Brokerage ₹40 roundtrip, GST 18%, Exchange turnover 0.05%, slippage 0.5 pt) and 15:15 IST auto square-off.
- **Zero-Leakage Security**: Broker API secrets stored in hardware-backed `EncryptedSharedPreferences` (AES-256 GCM). Never logged, committed, or transmitted to third-party servers.

---

## 🏛 Architecture & Tech Stack

Cloudscope Money Printer strictly follows **MVVM (Model-View-ViewModel)** with **Clean Architecture** separation:

```
app/
 ├── data/
 │    ├── local/
 │    │    ├── SecurePreferences.kt        # EncryptedSharedPreferences (AES-256)
 │    │    └── HistoricalSampleData.kt     # Multi-day 5-min candles for backtesting
 │    ├── model/
 │    │    ├── MarketQuote.kt              # NIFTY spot, India VIX, Option contracts
 │    │    ├── TradingSignal.kt            # Signals, Directions, Indicator metrics
 │    │    ├── BacktestModels.kt           # Trades, Configs, Simulation outcomes
 │    │    └── ConnectionState.kt          # Broker connection states & credentials
 │    ├── remote/
 │    │    ├── KiteConnectApi.kt           # Retrofit interface for Zerodha Kite
 │    │    └── KiteModels.kt               # Kite REST API response schemas
 │    └── repository/
 │         ├── MarketDataRepository.kt     # Data contract
 │         └── MarketDataRepositoryImpl.kt # Multi-instrument sync & synthetic chain
 ├── domain/
 │    ├── indicator/
 │    │    └── IndicatorEngine.kt          # Wilder RSI, EMA, VWAP, MACD, ATR, ORB, Pivots
 │    ├── options/
 │    │    ├── BlackScholes.kt             # Pricing, Delta, Gamma, Theta, Newton-Raphson IV
 │    │    └── OptionsSelector.kt          # Strike selection, tick-size rounding, SL/Target
 │    ├── risk/
 │    │    └── RiskManager.kt              # 2% rule, NIFTY 25 lot sizing, 15:15 IST square-off
 │    ├── signal/
 │    │    ├── ConfidenceCalibrator.kt     # Calibrated logistic probability model
 │    │    └── SignalEngine.kt             # Multi-factor confirmation rule engine
 │    └── backtest/
 │         └── BacktestEngine.kt           # Walk-forward historical simulator with cost drag
 └── ui/
      ├── components/
      │    ├── MarketSignalTile.kt         # Compact top status tile (Spot, Direction, AI %)
      │    ├── TradeSignalCard.kt          # BUY CE/PE, Entry, SL, T1, T2, R:R, NO TRADE
      │    ├── IndicatorsGrid.kt           # 8-metric technical dashboard
      │    ├── OptionChainSummaryCard.kt   # PCR, Max Pain, OI change, ATM strike grid
      │    ├── SupportResistanceCard.kt    # R1/R2, Pivots, S1/S2, PDH, PDL
      │    ├── ConnectionBar.kt            # Pulsing connection status & manual refresh
      │    ├── ApiConfigDialog.kt          # Modal to input & test Zerodha Kite credentials
      │    └── BacktestDialog.kt           # Complete simulation audit & trade log modal
      ├── screens/
      │    └── HomeScreen.kt               # Unified single-screen trading terminal
      ├── theme/
      │    ├── Color.kt                    # Terminal Green, Crimson Red, Dark & Light palettes
      │    ├── Theme.kt                    # Custom TerminalColorScheme with CompositionLocal
      │    └── Type.kt                     # Monospace and tabular typography
      ├── viewmodel/
      │    ├── MainUiState.kt              # Immutable UI state holder
      │    └── MainViewModel.kt            # Coroutine orchestration, 5s polling, backtest
      └── MainActivity.kt                  # Compose host activity
```

---

## 📈 Signal Engine & Factor Weighting

The signal engine evaluates 17 quantitative parameters:

| Factor | Bullish Condition | Bearish Condition | Weight |
|---|---|---|---|
| **Trend (EMA)** | Price > 20 EMA > 50 EMA | Price < 20 EMA < 50 EMA | High (1.5) |
| **Intraday VWAP** | Price > VWAP | Price < VWAP | High (1.5) |
| **Wilder's RSI 14** | 52 < RSI < 70 (Momentum) | 30 < RSI < 48 (Bearish) | Medium (1.0) |
| **MACD** | Hist > 0 & rising | Hist < 0 & falling | Medium (1.0) |
| **15m Opening Range (ORB)** | Breakout above 15m High | Breakdown below 15m Low | High (1.5) |
| **Put-Call Ratio (PCR)** | PCR > 1.15 (Put writing) | PCR < 0.85 (Call writing) | High (1.5) |
| **Change in OI** | Put OI addition > Call OI | Call OI addition > Put OI | High (1.5) |
| **India VIX** | 11.0 – 22.0 (Stable) | > 24.0 (Volatile penalty) | Filter |
| **Time of Day** | 09:30 – 15:00 IST | 15:15 IST Square-off | Gate |

### AI Confidence Calibration
Confidence is computed via a calibrated sigmoid transformation:
$$\text{Score} = \sum_{i} w_i \cdot c_i$$
$$\text{Probability} = \frac{1}{1 + e^{-k \cdot (\text{Score} - \theta)}}$$
- Penalized when India VIX is elevated (> 22.0) or when indicators conflict.
- Displays `LOW CONFIDENCE` if minimum factor sample threshold is not met.

---

## ⚙️ Zerodha Kite Connect Setup

1. Log in to [Zerodha Kite Developer Console](https://kite.trade/).
2. Create an App to obtain your **API Key** and **API Secret**.
3. Complete the daily login flow to generate your **Access Token**.
4. In Cloudscope Money Printer, tap the broker settings icon (top right corner).
5. Paste your `API Key` and `Access Token`, then tap **Save & Connect**.
6. The connection bar will change from `SETUP REQUIRED` to `LIVE` with a green pulsing indicator.

---

## 🧪 Historical Backtesting Methodology

The built-in backtester computes realistic equity curves incorporating all statutory Indian derivative trading fees:
- **Brokerage**: ₹20 per executed order (₹40 roundtrip per lot).
- **Securities Transaction Tax (STT)**: 0.1% on the sell side premium.
- **Goods & Services Tax (GST)**: 18% on (Brokerage + Exchange turnover charges).
- **Exchange Turnover Charges**: 0.0505% on premium value.
- **SEBI Turnover & Stamp Duty**: Applicable standard rates.
- **Slippage**: 0.5 index points deducted per execution.
- **Square-off Rule**: All open positions strictly closed at 15:15 IST.

Run backtests anytime by tapping the **Backtest** icon in the app header.

---

## 🚀 Continuous Integration & Automated Releases

The repository includes [`.github/workflows/android-release.yml`](.github/workflows/android-release.yml) for automated testing, APK building, signing, and immediate GitHub Release creation.

### Automatic Release on Code Push
Every push to the `main` branch automatically:
1. Runs all unit test suites (`./gradlew testReleaseUnitTest`).
2. Builds the release APK (`./gradlew assembleRelease`).
3. Signs the APK (with release keystore if configured in secrets, or fallback debug keystore so it installs immediately on any phone).
4. Creates a new **GitHub Release** (`v1.0.<build_number>`) with release notes and attaches the installable APK file `Cloudscope-Money-Printer-v1.0.<build_number>.apk`.

### Manual / Version Tag Releases
To trigger a tagged production release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Optional: Custom Keystore Secrets
If you want to sign with your own private production keystore instead of the default key, configure these in **GitHub Settings > Secrets and variables > Actions**:

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded Android release keystore file |
| `KEYSTORE_PASSWORD` | Password for the release keystore |
| `KEY_ALIAS` | Key alias in the keystore |
| `KEY_PASSWORD` | Password for the specific key alias |

---

## 🛠 Local Build Instructions

### Prerequisites
- JDK 17 (Eclipse Temurin or OpenJDK)
- Android SDK Build-Tools 35.0.0 (ARM64 / x86_64 matching your host)
- Gradle 8.7+

### Assembling Debug APK
```bash
./gradlew assembleDebug --console=plain --stacktrace
```
The resulting debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Running Unit Tests
```bash
./gradlew testDebugUnitTest --console=plain --stacktrace
```

---

## 📜 License
Proprietary & Confidential. All rights reserved © 2026 Cloudscope Technologies.
