# UI & UX Product Guidelines

Guidelines for building native-fidelity, accessible, and high-performance Compose Multiplatform interfaces.
For architectural decision workflows, execute **`skills/kmp-product-engineer/SKILL.md`**.

---

## 1. Platform Muscle Memory & Fidelity

- **iOS Edge Swipe-Back**: Ensure navigation stacks allow interactive edge swipe-to-pop gestures without gesture conflicts from nested sliders or carousels.
- **Dynamic Island & Safe Areas**: Always derive container padding from `Scaffold` or `WindowInsets.safeDrawing`. Never hardcode top insets (e.g. `48.dp`).
- **Android 15+ Edge-to-Edge**: The app renders edge-to-edge by default. Apply `.imePadding()` to bottom action buttons and text input fields to smoothly animate above the keyboard.
- **Touch Targets**: Minimum 48dp on Android and 44pt on iOS for all clickable elements (`Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)`).

---

## 2. State Design Standards

1. **No Boolean Soup**: Every feature screen must define a single sealed interface `ScreenUiState` in `presentation/screens/<feature>/`:
   ```kotlin
   sealed interface FeatureUiState {
       data object Loading : FeatureUiState
       data object Empty : FeatureUiState
       data class Content(val data: List<ItemUiModel>, val isRefreshing: Boolean = false) : FeatureUiState
       data class Error(val message: String, val canRetry: Boolean = true) : FeatureUiState
   }
   ```
2. **Dual-Overload Screen Pattern**:
   - Screen with ViewModel: `@Composable fun FeatureScreen(viewModel: FeatureViewModel, onNavigate: (Route) -> Unit)`
   - Pure stateless screen: `@Composable fun FeatureScreen(uiState: FeatureUiState, onUiEvent: (FeatureUiEvent) -> Unit, ...)`
   - The pure overload enables instantaneous Compose `@Preview` and headless verification via **`skills/verify-ui/SKILL.md`**.

---

## 3. Performance & Strong Skipping

- Pass unique and stable `key = { it.id }` to `items()` inside `LazyColumn` and `LazyRow`.
- Defer high-frequency state reads (scroll positions, drag offsets) to layout or draw lambdas: `Modifier.offset { IntOffset(0, scrollState.value) }`.
- Localize all user-facing copy in `composeResources/values/strings.xml` using typographic apostrophes (`’`).
