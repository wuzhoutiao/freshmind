package com.example.freshmind;

import android.graphics.Color;
import android.graphics.Paint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.freshmind.data.InventoryDatabase;
import com.example.freshmind.data.DebugInventorySeeder;
import com.example.freshmind.data.InventoryStorage;
import com.example.freshmind.data.RecipeDatabase;
import com.example.freshmind.model.Ingredient;
import com.example.freshmind.model.InventoryItem;
import com.example.freshmind.model.recipe.Quantity;
import com.example.freshmind.model.recipe.Recipe;
import com.example.freshmind.service.ExpiryReminderService;
import com.example.freshmind.service.InventoryService;
import com.example.freshmind.service.RecipeService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main screen controller for FreshMind. It manages inventory, expiry reminders,
 * recipe recommendations, and navigation between the app's screens.
 */
public class MainActivity extends AppCompatActivity {

    private static final int SCREEN_DASHBOARD = R.layout.layout_dashboard;
    private static final int SCREEN_INVENTORY = R.layout.layout_inventory;
    private static final int SCREEN_RECIPES = R.layout.layout_recipe_recommendations;
    private static final int SCREEN_ADD_INGREDIENTS = R.layout.layout_add_ingredients;
    private static final int SCREEN_CUSTOM_INGREDIENT_ENTRY = R.layout.layout_custom_ingredient_entry;
    private static final int SCREEN_INGREDIENT_ENTRY = R.layout.layout_ingredient_entry;
    private static final int SCREEN_INGREDIENT_DETAIL = R.layout.layout_ingredient_detail;
    private static final int SCREEN_RECIPE_DETAIL = R.layout.layout_recipe_detail;

    private static final String DEFAULT_QUANTITY = "1";
    private static final String DEFAULT_CATEGORY = "Others";
    private static final String INVENTORY_CATEGORY_VEGETABLES = "Vegetables";
    private static final String INVENTORY_CATEGORY_FRUITS = "Fruits";
    private static final String INVENTORY_CATEGORY_MEAT = "Meat";
    private static final int DASHBOARD_REMINDER_DAYS = 7;
    private static final String INPUT_DATE_PATTERN = "dd/MM/yyyy";
    private static final String CATALOG_SHELF_LIFE_PREFS = "catalog_shelf_life_overrides";
    private static final String CATALOG_SHELF_LIFE_KEY_PREFIX = "ingredient_";
    private static final float DATE_PICKER_TEXT_SIZE_SP = 16f;
    private static final String[] MONTH_PICKER_VALUES = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private InventoryStorage inventoryStorage;
    private InventoryService inventoryService;
    private ExpiryReminderService expiryReminderService;
    private RecipeService recipeService;

    private FrameLayout rootContainer;
    private final Deque<ScreenState> screenBackStack = new ArrayDeque<>();
    private final Map<String, Integer> recipeImageResourceIds = new HashMap<>();
    private List<Recipe> currentRecommendedRecipes = new ArrayList<>();
    private int currentScreen = SCREEN_DASHBOARD;
    private Recipe currentRecipe;
    private InventoryItem currentInventoryItem;
    private Ingredient selectedCatalogIngredient;
    private String selectedCategory = null;
    private String pendingCustomIngredientName = "";
    private String recipeSearchQuery = "";
    private boolean recipeSearchPending;
    private boolean editingInventoryItem;
    private int systemBottomInset;
    private int keyboardBottomInset;

    /**
     * Initializes services, loads saved data, configures window insets, and opens the dashboard.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(
                this,
                SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                ),
                SystemBarStyle.light(getColor(R.color.white), getColor(R.color.white))
        );
        setContentView(R.layout.activity_main);

        loadCatalogShelfLifeOverrides();
        inventoryStorage = new InventoryStorage(this);
        inventoryService = new InventoryService(DebugInventorySeeder.loadItemsForAppStart(inventoryStorage));
        expiryReminderService = new ExpiryReminderService();
        recipeService = new RecipeService();

        rootContainer = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            systemBottomInset = systemBars.bottom;
            keyboardBottomInset = Math.max(0, ime.bottom - systemBars.bottom);
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            applyBottomNavInsets(v);
            applyKeyboardScrollInsets(v);
            return insets;
        });

        showScreen(SCREEN_DASHBOARD);
        rootContainer.post(() -> preloadRecipeImages(RecipeDatabase.getRecipes()));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!navigateBack()) {
                    finish();
                }
            }
        });
    }

    /**
     * Displays a screen layout without adding a custom state object.
     */
    private void showScreen(int layoutResId) {
        renderScreen(layoutResId, 0);
    }

    /**
     * Restores a previous screen state before rendering it.
     */
    private void showScreen(ScreenState screenState) {
        restoreScreenState(screenState);
        renderScreen(screenState.layoutResId, screenState.scrollY);
    }

    /**
     * Inflates the requested screen and binds the matching UI behavior.
     */
    private void renderScreen(int layoutResId, int scrollY) {
        currentScreen = layoutResId;
        boolean hasCurrentScreen = rootContainer.getChildCount() > 0;
        View screen = LayoutInflater.from(this).inflate(layoutResId, rootContainer, false);
        if (hasCurrentScreen) {
            screen.setVisibility(View.INVISIBLE);
        }
        rootContainer.addView(screen);

        bindBottomNav(screen);
        applyKeyboardScrollInsets(screen);

        if (layoutResId == SCREEN_DASHBOARD) {
            bindDashboardScreen(screen);
        } else if (layoutResId == SCREEN_INVENTORY) {
            bindInventoryScreen(screen);
        } else if (layoutResId == SCREEN_RECIPES) {
            bindRecipeRecommendationsScreen(screen);
        } else if (layoutResId == SCREEN_ADD_INGREDIENTS) {
            bindAddIngredientsScreen(screen);
        } else if (layoutResId == SCREEN_CUSTOM_INGREDIENT_ENTRY) {
            bindCustomIngredientEntryScreen(screen);
        } else if (layoutResId == SCREEN_INGREDIENT_ENTRY) {
            bindIngredientEntryScreen(screen);
        } else if (layoutResId == SCREEN_RECIPE_DETAIL) {
            bindRecipeDetailScreen(screen, currentRecipe);
        } else if (layoutResId == SCREEN_INGREDIENT_DETAIL) {
            bindIngredientDetailScreen(screen, currentInventoryItem);
        }

        revealScreenWhenReady(screen, scrollY, hasCurrentScreen);
    }

    /**
     * Connects the bottom navigation buttons and updates the selected tab style.
     */
    private void bindBottomNav(View screen) {
        applyBottomNavInsets(screen);

        View navDashboard = screen.findViewById(R.id.navDashboard);
        View navInventory = screen.findViewById(R.id.navInventory);
        View navRecipes = screen.findViewById(R.id.navRecipes);

        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> switchRootScreen(SCREEN_DASHBOARD));
        }
        if (navInventory != null) {
            navInventory.setOnClickListener(v -> switchRootScreen(SCREEN_INVENTORY));
        }
        if (navRecipes != null) {
            navRecipes.setOnClickListener(v -> switchRootScreen(SCREEN_RECIPES));
        }

        int activeTab = getActiveNavTab();
        updateNavItem(
                screen,
                R.id.navDashboardIcon,
                R.id.navDashboardText,
                R.drawable.ic_home_active,
                R.drawable.ic_home,
                activeTab == SCREEN_DASHBOARD
        );
        updateNavItem(
                screen,
                R.id.navInventoryIcon,
                R.id.navInventoryText,
                R.drawable.ic_inventory_active,
                R.drawable.ic_inventory,
                activeTab == SCREEN_INVENTORY
        );
        updateNavItem(
                screen,
                R.id.navRecipesIcon,
                R.id.navRecipesText,
                R.drawable.ic_recipes_active,
                R.drawable.ic_recipes,
                activeTab == SCREEN_RECIPES
        );
    }

    /**
     * Applies system bottom inset spacing so the bottom navigation stays above gesture bars.
     */
    private void applyBottomNavInsets(View container) {
        View bottomNav = container.findViewById(R.id.bottomNav);
        if (bottomNav == null) {
            return;
        }

        int baseHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);
        ViewGroup.LayoutParams layoutParams = bottomNav.getLayoutParams();
        if (layoutParams != null && layoutParams.height != baseHeight + systemBottomInset) {
            layoutParams.height = baseHeight + systemBottomInset;
            bottomNav.setLayoutParams(layoutParams);
        }
        bottomNav.setPadding(
                bottomNav.getPaddingLeft(),
                bottomNav.getPaddingTop(),
                bottomNav.getPaddingRight(),
                systemBottomInset
        );
    }

    private void applyKeyboardScrollInsets(View container) {
        int contentGap = getResources().getDimensionPixelSize(R.dimen.content_bottom_nav_gap);
        int keyboardGap = keyboardBottomInset > 0 ? keyboardBottomInset + dpToPx(24) : 0;
        applyKeyboardScrollInsetsRecursive(container, contentGap + keyboardGap);
    }

    private void applyKeyboardScrollInsetsRecursive(View view, int bottomPadding) {
        if (view instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) view;
            if (scrollView.getChildCount() > 0) {
                View content = scrollView.getChildAt(0);
                content.setPadding(
                        content.getPaddingLeft(),
                        content.getPaddingTop(),
                        content.getPaddingRight(),
                        bottomPadding
                );
            }
            return;
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            applyKeyboardScrollInsetsRecursive(viewGroup.getChildAt(i), bottomPadding);
        }
    }

    private int getActiveNavTab() {
        if (currentScreen == SCREEN_RECIPES || currentScreen == SCREEN_RECIPE_DETAIL) {
            return SCREEN_RECIPES;
        }
        if (currentScreen == SCREEN_INVENTORY
                || currentScreen == SCREEN_ADD_INGREDIENTS
                || currentScreen == SCREEN_CUSTOM_INGREDIENT_ENTRY
                || currentScreen == SCREEN_INGREDIENT_ENTRY
                || currentScreen == SCREEN_INGREDIENT_DETAIL) {
            return SCREEN_INVENTORY;
        }
        return SCREEN_DASHBOARD;
    }

    private void updateNavItem(
            View screen,
            int iconId,
            int textId,
            int activeIconResId,
            int inactiveIconResId,
            boolean active
    ) {
        ImageView icon = screen.findViewById(iconId);
        TextView text = screen.findViewById(textId);
        int color = getColor(active ? R.color.fresh_green : R.color.black);

        if (icon != null) {
            icon.setImageResource(active ? activeIconResId : inactiveIconResId);
        }
        if (text != null) {
            text.setTextColor(color);
        }
    }

    /**
     * Loads dashboard reminders, recipe recommendations, and quick add/search actions.
     */
    private void bindDashboardScreen(View screen) {
        refreshInventoryStatuses();
        renderDashboardReminders(screen);
        renderDashboardRecommendations(screen);
        bindDashboardRecipeSearch(screen);

        View addIngredientsButton = screen.findViewById(R.id.addIngredientsButton);
        if (addIngredientsButton != null) {
            addIngredientsButton.setOnClickListener(v -> {
                editingInventoryItem = false;
                currentInventoryItem = null;
                openScreen(SCREEN_ADD_INGREDIENTS);
            });
        }
    }

    private void bindDashboardRecipeSearch(View screen) {
        EditText searchInput = screen.findViewById(R.id.dashboardRecipeSearchInput);
        if (searchInput == null) {
            return;
        }

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean searchAction = actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE;
            boolean enterPressed = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (searchAction || enterPressed) {
                openRecipeRecommendationsWithQuery(v.getText().toString());
                return true;
            }
            return false;
        });
    }

    /**
     * Opens the recipe recommendations screen using the dashboard search query.
     */
    private void openRecipeRecommendationsWithQuery(String query) {
        recipeSearchQuery = query == null ? "" : query.trim();
        recipeSearchPending = true;
        openScreen(SCREEN_RECIPES);
    }

    /**
     * Opens a child screen and stores the current screen so the back button can return to it.
     */
    private void openScreen(int layoutResId) {
        screenBackStack.push(captureCurrentScreenState());
        showScreen(layoutResId);
    }

    /**
     * Switches between root tabs and clears child screen history.
     */
    private void switchRootScreen(int layoutResId) {
        screenBackStack.clear();
        showScreen(layoutResId);
    }

    /**
     * Restores the previous screen from the custom in-app back stack.
     */
    private boolean navigateBack() {
        if (screenBackStack.isEmpty()) {
            return false;
        }
        showScreen(screenBackStack.pop());
        return true;
    }

    private void navigateBackOrRoot() {
        if (navigateBack()) {
            return;
        }
        switchRootScreen(getActiveNavTab());
    }

    /**
     * Captures the current screen, scroll position, and selected item state for back navigation.
     */
    private ScreenState captureCurrentScreenState() {
        return new ScreenState(
                currentScreen,
                getCurrentScrollY(),
                currentRecipe,
                currentInventoryItem,
                selectedCatalogIngredient,
                selectedCategory,
                recipeSearchQuery,
                recipeSearchPending,
                editingInventoryItem
        );
    }

    /**
     * Restores selected item and search state when returning to a previous screen.
     */
    private void restoreScreenState(ScreenState screenState) {
        currentRecipe = screenState.currentRecipe;
        currentInventoryItem = screenState.currentInventoryItem;
        selectedCatalogIngredient = screenState.selectedCatalogIngredient;
        selectedCategory = screenState.selectedCategory;
        recipeSearchQuery = screenState.recipeSearchQuery;
        recipeSearchPending = screenState.recipeSearchPending;
        editingInventoryItem = screenState.editingInventoryItem;
    }

    private int getCurrentScrollY() {
        ScrollView scrollView = findFirstScrollView(rootContainer);
        return scrollView == null ? 0 : scrollView.getScrollY();
    }

    private void revealScreenWhenReady(View screen, int scrollY, boolean removePreviousScreens) {
        ScrollView scrollView = findFirstScrollView(screen);
        if (scrollY <= 0 && !removePreviousScreens) {
            return;
        }

        screen.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (screen.getViewTreeObserver().isAlive()) {
                    screen.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                if (scrollView != null && scrollY > 0) {
                    scrollView.scrollTo(0, scrollY);
                }
                if (removePreviousScreens) {
                    removeOtherScreens(screen);
                    screen.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
    }

    private void removeOtherScreens(View screenToKeep) {
        for (int i = rootContainer.getChildCount() - 1; i >= 0; i--) {
            View child = rootContainer.getChildAt(i);
            if (child != screenToKeep) {
                rootContainer.removeViewAt(i);
            }
        }
    }

    private ScrollView findFirstScrollView(View view) {
        if (view instanceof ScrollView) {
            return (ScrollView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            ScrollView childScrollView = findFirstScrollView(viewGroup.getChildAt(i));
            if (childScrollView != null) {
                return childScrollView;
            }
        }
        return null;
    }

    /**
     * Renders recommended recipes on the dashboard based on the current inventory.
     */
    private void renderDashboardRecommendations(View screen) {
        LinearLayout recipeContainer = screen.findViewById(R.id.dashboardRecipeContainer);
        if (recipeContainer == null) {
            return;
        }

        recipeContainer.removeAllViews();

        List<InventoryItem> inventory = inventoryService.getAllItems();
        List<Recipe> allRecipes = RecipeDatabase.getRecipes();
        List<Recipe> recommendedRecipes = recipeService.recommendRecipes(inventory, allRecipes);
        if (recommendedRecipes.isEmpty()) {
            recommendedRecipes = allRecipes.subList(0, Math.min(5, allRecipes.size()));
        }

        int limit = Math.min(recommendedRecipes.size(), 5);
        for (int i = 0; i < limit; i++) {
            Recipe recipe = recommendedRecipes.get(i);
            View card = createRecipeCard(recipe);
            card.setBackgroundResource(R.drawable.bg_recipe_card_border);
            card.setElevation(0f);
            card.setOnClickListener(v -> {
                currentRecipe = recipe;
                openScreen(SCREEN_RECIPE_DETAIL);
            });
            recipeContainer.addView(card);

            if (i < limit - 1) {
                View spacer = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        dpToPx(14),
                        LinearLayout.LayoutParams.MATCH_PARENT
                );
                spacer.setLayoutParams(params);
                recipeContainer.addView(spacer);
            }
        }
    }

    /**
     * Displays inventory items grouped by category and wires the add ingredient action.
     */
    private void bindInventoryScreen(View screen) {
        refreshInventoryStatuses();

        LinearLayout listContainer = screen.findViewById(R.id.inventoryItemsContainer);
        if (listContainer != null) {
            listContainer.removeAllViews();
            List<InventoryItem> items = inventoryService.getItemsSortedByExpiryDate();

            if (items.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No ingredients in inventory yet.");
                empty.setTextSize(14f);
                empty.setTextColor(getColor(R.color.fresh_text));
                listContainer.addView(empty);
            } else {
                addInventoryCategorySection(listContainer, INVENTORY_CATEGORY_VEGETABLES, items, true);
                addInventoryCategorySection(listContainer, INVENTORY_CATEGORY_FRUITS, items, false);
                addInventoryCategorySection(listContainer, INVENTORY_CATEGORY_MEAT, items, false);
                addInventoryCategorySection(listContainer, DEFAULT_CATEGORY, items, false);
            }
        }

        View addFab = screen.findViewById(R.id.inventoryAddFab);
        if (addFab != null) {
            addFab.setOnClickListener(v -> {
                editingInventoryItem = false;
                currentInventoryItem = null;
                openScreen(SCREEN_ADD_INGREDIENTS);
            });
        }
    }

    private void addInventoryCategorySection(LinearLayout listContainer,
                                             String displayCategory,
                                             List<InventoryItem> allItems,
                                             boolean firstSection) {
        listContainer.addView(createInventoryCategoryHeader(displayCategory, firstSection));

        boolean hasItems = false;
        for (InventoryItem item : allItems) {
            if (!displayCategory.equals(getInventoryDisplayCategory(item))) {
                continue;
            }
            hasItems = true;
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_inventory_text, listContainer, false);
            bindInventoryRow(row, item);
            row.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                currentInventoryItem = item;
                openScreen(SCREEN_INGREDIENT_DETAIL);
            });
            listContainer.addView(row);
        }

        if (!hasItems) {
            TextView empty = createInventoryCategoryEmptyText(displayCategory);
            listContainer.addView(empty);
        }
    }

    private void bindInventoryRow(View row, InventoryItem item) {
        TextView name = row.findViewById(R.id.inventoryRowName);
        TextView summary = row.findViewById(R.id.inventoryRowSummary);
        TextView statusTag = row.findViewById(R.id.inventoryRowStatusTag);
        View statusStrip = row.findViewById(R.id.inventoryRowStatusStrip);

        int daysUntilExpiry = expiryReminderService.getDaysUntilExpiry(item);
        int statusColor = daysUntilExpiry < 0
                ? getColor(R.color.fresh_expired)
                : getInventoryStatusAccentColor(daysUntilExpiry);
        if (name != null) {
            name.setText(item.getIngredientName());
        }
        if (summary != null) {
            summary.setText(formatInventoryExpirySummary(daysUntilExpiry));
        }
        if (statusTag != null) {
            statusTag.setText(formatDaysLeft(daysUntilExpiry));
            if (daysUntilExpiry < 0) {
                statusTag.setBackgroundResource(R.drawable.bg_tag_expired);
                statusTag.setTextColor(getColor(R.color.fresh_expired));
            } else if (daysUntilExpiry <= ExpiryReminderService.NEAR_EXPIRY_THRESHOLD_DAYS) {
                statusTag.setBackgroundResource(R.drawable.bg_tag_red);
                statusTag.setTextColor(getColor(R.color.fresh_red));
            } else {
                statusTag.setBackgroundResource(R.drawable.bg_tag_blue);
                statusTag.setTextColor(getColor(R.color.fresh_blue));
            }
        }
        if (statusStrip != null) {
            statusStrip.setBackground(createInventoryStatusStripBackground(statusColor));
        }
        row.setContentDescription("View details for " + item.getIngredientName() + ". "
                + formatInventoryExpirySummary(daysUntilExpiry));
    }

    private GradientDrawable createInventoryStatusStripBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dpToPx(2));
        return drawable;
    }

    private int getInventoryStatusAccentColor(int daysUntilExpiry) {
        if (daysUntilExpiry <= ExpiryReminderService.NEAR_EXPIRY_THRESHOLD_DAYS) {
            return getColor(R.color.fresh_red);
        }
        return getColor(R.color.fresh_blue);
    }

    private TextView createInventoryCategoryHeader(String displayCategory, boolean firstSection) {
        TextView header = new TextView(this);
        header.setText(displayCategory);
        header.setTextColor(getColor(R.color.fresh_blue));
        header.setTextSize(18f);
        header.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_bold), Typeface.NORMAL);
        header.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = firstSection ? 0 : dpToPx(20);
        header.setLayoutParams(params);
        return header;
    }

    private TextView createInventoryCategoryEmptyText(String displayCategory) {
        TextView empty = new TextView(this);
        empty.setText("No " + displayCategory.toLowerCase(Locale.US) + " items yet.");
        empty.setTextColor(getColor(R.color.fresh_tag_blue));
        empty.setTextSize(13f);
        empty.setPadding(0, dpToPx(8), 0, 0);
        return empty;
    }

    /**
     * Binds the ingredient search screen for catalog selection or manual ingredient entry.
     */
    private void bindAddIngredientsScreen(View screen) {
        bindBackButton(screen, R.id.addIngredientsBackButton);

        EditText searchInput = screen.findViewById(R.id.ingredientSearchInput);
        LinearLayout suggestionsContainer = screen.findViewById(R.id.ingredientSuggestionsContainer);
        View searchDropdown = screen.findViewById(R.id.ingredientSearchDropdown);
        LinearLayout searchResultsContainer = screen.findViewById(R.id.ingredientSearchResultsContainer);
        View defaultIngredientsScroll = screen.findViewById(R.id.defaultIngredientsScroll);
        TextView manualAdd = screen.findViewById(R.id.manualAddIngredientText);

        if (suggestionsContainer != null) {
            renderDefaultIngredientSuggestions(suggestionsContainer);
        }

        if (searchInput != null && searchDropdown != null && searchResultsContainer != null) {
            searchInput.setOnClickListener(v -> {
                searchDropdown.setVisibility(View.VISIBLE);
                renderIngredientSearchResults(searchResultsContainer, searchInput.getText().toString());
            });
            searchInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    searchDropdown.setVisibility(View.VISIBLE);
                    renderIngredientSearchResults(searchResultsContainer, searchInput.getText().toString());
                } else {
                    searchDropdown.setVisibility(View.GONE);
                }
            });
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchDropdown.setVisibility(View.VISIBLE);
                    renderIngredientSearchResults(searchResultsContainer, s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            View.OnTouchListener dismissSearchDropdown = (v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideIngredientSearchDropdown(searchInput, searchDropdown);
                }
                return false;
            };
            screen.setFocusableInTouchMode(true);
            screen.setOnTouchListener(dismissSearchDropdown);
            if (defaultIngredientsScroll != null) {
                defaultIngredientsScroll.setOnTouchListener(dismissSearchDropdown);
            }
        }

        if (manualAdd != null) {
            manualAdd.setOnClickListener(v -> {
                String ingredientName = searchInput == null ? "" : searchInput.getText().toString();
                openCustomIngredientEntry(ingredientName);
            });
        }
    }

    /**
     * Binds the custom ingredient form for creating or editing inventory items.
     */
    private void bindCustomIngredientEntryScreen(View screen) {
        bindBackButton(screen, R.id.customIngredientBackButton);

        TextView title = screen.findViewById(R.id.customIngredientTitle);
        EditText nameInput = screen.findViewById(R.id.customIngredientNameInput);
        CheckBox knownExpiryCheckbox = screen.findViewById(R.id.customKnownExpiryCheckbox);
        EditText expiryInput = screen.findViewById(R.id.customExpiryDateInput);
        View expiryDatePickerButton = screen.findViewById(R.id.customExpiryDatePickerButton);
        CheckBox knownShelfLifeCheckbox = screen.findViewById(R.id.customKnownShelfLifeCheckbox);
        EditText purchaseInput = screen.findViewById(R.id.customPurchaseDateInput);
        View purchaseDatePickerButton = screen.findViewById(R.id.customPurchaseDatePickerButton);
        EditText shelfLifeInput = screen.findViewById(R.id.customShelfLifeInput);
        View expiryMethodCard = screen.findViewById(R.id.customExpiryMethodCard);
        View shelfLifeMethodCard = screen.findViewById(R.id.customShelfLifeMethodCard);
        TextView expiryPreviewText = screen.findViewById(R.id.customExpiryPreviewText);
        TextView saveButton = screen.findViewById(R.id.customSaveButton);

        bindCategoryChoice(screen, R.id.categoryVegetables, "Vegetables");
        bindCategoryChoice(screen, R.id.categoryFruits, "Fruits");
        bindCategoryChoice(screen, R.id.categoryMeat, "Meat");
        bindCategoryChoice(screen, R.id.categoryOthers, DEFAULT_CATEGORY);

        if (editingInventoryItem && currentInventoryItem != null) {
            selectedCategory = currentInventoryItem.getCategory();
            if (title != null) {
                title.setText("Edit Ingredient");
            }
            if (nameInput != null) {
                nameInput.setText(currentInventoryItem.getIngredientName());
            }
            if (purchaseInput != null) {
                purchaseInput.setText(formatDateForInput(currentInventoryItem.getPurchaseDate()));
            }
            if (expiryInput != null) {
                expiryInput.setText(formatDateForInput(currentInventoryItem.getExpiryDate()));
            }
            if (knownExpiryCheckbox != null) {
                knownExpiryCheckbox.setChecked(true);
            }
        } else {
            if (nameInput != null && !pendingCustomIngredientName.isEmpty()) {
                nameInput.setText(pendingCustomIngredientName);
                nameInput.setSelection(nameInput.getText().length());
            }
            if (purchaseInput != null) {
                purchaseInput.setText(getTodayInputString());
            }
            if (knownExpiryCheckbox != null) {
                knownExpiryCheckbox.setChecked(false);
            }
            selectedCategory = null;
        }
        pendingCustomIngredientName = "";
        bindMethodInput(expiryInput, knownExpiryCheckbox);
        bindMethodInput(purchaseInput, knownShelfLifeCheckbox);
        bindDatePickerButton(expiryDatePickerButton, expiryInput, knownExpiryCheckbox);
        bindDatePickerButton(purchaseDatePickerButton, purchaseInput, knownShelfLifeCheckbox);
        bindMethodInput(shelfLifeInput, knownShelfLifeCheckbox);

        updateCategorySelection(screen);
        updateCustomExpiryMethodUi(
                knownExpiryCheckbox,
                expiryInput,
                knownShelfLifeCheckbox,
                purchaseInput,
                shelfLifeInput,
                expiryMethodCard,
                shelfLifeMethodCard,
                expiryPreviewText
        );

        if (knownExpiryCheckbox != null) {
            knownExpiryCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && knownShelfLifeCheckbox != null && knownShelfLifeCheckbox.isChecked()) {
                    knownShelfLifeCheckbox.setChecked(false);
                }
                updateCustomExpiryMethodUi(
                        knownExpiryCheckbox,
                        expiryInput,
                        knownShelfLifeCheckbox,
                        purchaseInput,
                        shelfLifeInput,
                        expiryMethodCard,
                        shelfLifeMethodCard,
                        expiryPreviewText
                );
            });
        }
        if (knownShelfLifeCheckbox != null) {
            knownShelfLifeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && knownExpiryCheckbox != null && knownExpiryCheckbox.isChecked()) {
                    knownExpiryCheckbox.setChecked(false);
                }
                updateCustomExpiryMethodUi(
                        knownExpiryCheckbox,
                        expiryInput,
                        knownShelfLifeCheckbox,
                        purchaseInput,
                        shelfLifeInput,
                        expiryMethodCard,
                        shelfLifeMethodCard,
                        expiryPreviewText
                );
            });
        }
        if (expiryMethodCard != null && knownExpiryCheckbox != null) {
            expiryMethodCard.setOnClickListener(v -> knownExpiryCheckbox.setChecked(true));
        }
        if (shelfLifeMethodCard != null && knownShelfLifeCheckbox != null) {
            shelfLifeMethodCard.setOnClickListener(v -> knownShelfLifeCheckbox.setChecked(true));
        }
        TextWatcher expiryPreviewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCustomExpiryPreview(
                        knownExpiryCheckbox,
                        expiryInput,
                        knownShelfLifeCheckbox,
                        purchaseInput,
                        shelfLifeInput,
                        expiryPreviewText
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        if (expiryInput != null) {
            expiryInput.addTextChangedListener(expiryPreviewWatcher);
        }
        if (purchaseInput != null) {
            purchaseInput.addTextChangedListener(expiryPreviewWatcher);
        }
        if (shelfLifeInput != null) {
            shelfLifeInput.addTextChangedListener(expiryPreviewWatcher);
        }
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> saveCustomIngredient(
                    nameInput,
                    knownExpiryCheckbox,
                    expiryInput,
                    knownShelfLifeCheckbox,
                    purchaseInput,
                    shelfLifeInput
            ));
        }
    }

    /**
     * Binds the catalog ingredient form using default shelf life or a known expiry date.
     */
    private void bindIngredientEntryScreen(View screen) {
        View backButton = screen.findViewById(R.id.ingredientEntryBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> navigateBackOrRoot());
        }

        if (selectedCatalogIngredient == null) {
            switchRootScreen(SCREEN_INVENTORY);
            return;
        }

        TextView title = screen.findViewById(R.id.catalogIngredientTitle);
        CheckBox defaultShelfLifeCheckbox = screen.findViewById(R.id.catalogDefaultShelfLifeCheckbox);
        EditText purchaseInput = screen.findViewById(R.id.catalogPurchaseDateInput);
        View purchaseDatePickerButton = screen.findViewById(R.id.catalogPurchaseDatePickerButton);
        EditText shelfLifeInput = screen.findViewById(R.id.catalogShelfLifeInput);
        CheckBox saveShelfLifeDefaultCheckbox = screen.findViewById(R.id.catalogSaveShelfLifeDefaultCheckbox);
        CheckBox knownExpiryCheckbox = screen.findViewById(R.id.catalogKnownExpiryCheckbox);
        EditText expiryInput = screen.findViewById(R.id.catalogExpiryDateInput);
        View expiryDatePickerButton = screen.findViewById(R.id.catalogExpiryDatePickerButton);
        View expiryMethodCard = screen.findViewById(R.id.catalogExpiryMethodCard);
        View defaultShelfLifeMethodCard = screen.findViewById(R.id.catalogDefaultShelfLifeMethodCard);
        TextView expiryPreviewText = screen.findViewById(R.id.catalogExpiryPreviewText);
        TextView saveButton = screen.findViewById(R.id.catalogSaveButton);

        if (title != null) {
            title.setText(selectedCatalogIngredient.getIngredientName());
        }
        if (defaultShelfLifeCheckbox != null) {
            String text = "Use default shelf life: "
                    + selectedCatalogIngredient.getDefaultShelfLifeDays()
                    + " day(s)";
            defaultShelfLifeCheckbox.setText(text);
            defaultShelfLifeCheckbox.setChecked(true);
        }
        if (shelfLifeInput != null) {
            shelfLifeInput.setText(String.valueOf(selectedCatalogIngredient.getDefaultShelfLifeDays()));
        }
        if (saveShelfLifeDefaultCheckbox != null) {
            saveShelfLifeDefaultCheckbox.setChecked(false);
        }
        if (purchaseInput != null) {
            purchaseInput.setText(getTodayInputString());
        }
        bindMethodInput(purchaseInput, defaultShelfLifeCheckbox);
        bindMethodInput(expiryInput, knownExpiryCheckbox);
        bindMethodInput(shelfLifeInput, defaultShelfLifeCheckbox);
        bindDatePickerButton(purchaseDatePickerButton, purchaseInput, defaultShelfLifeCheckbox);
        bindDatePickerButton(expiryDatePickerButton, expiryInput, knownExpiryCheckbox);

        updateCatalogExpiryMethodUi(
                defaultShelfLifeCheckbox,
                purchaseInput,
                shelfLifeInput,
                saveShelfLifeDefaultCheckbox,
                knownExpiryCheckbox,
                expiryInput,
                defaultShelfLifeMethodCard,
                expiryMethodCard,
                expiryPreviewText
        );
        if (knownExpiryCheckbox != null) {
            knownExpiryCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && defaultShelfLifeCheckbox != null && defaultShelfLifeCheckbox.isChecked()) {
                    defaultShelfLifeCheckbox.setChecked(false);
                } else if (!isChecked && defaultShelfLifeCheckbox != null && !defaultShelfLifeCheckbox.isChecked()) {
                    defaultShelfLifeCheckbox.setChecked(true);
                }
                updateCatalogExpiryMethodUi(
                        defaultShelfLifeCheckbox,
                        purchaseInput,
                        shelfLifeInput,
                        saveShelfLifeDefaultCheckbox,
                        knownExpiryCheckbox,
                        expiryInput,
                        defaultShelfLifeMethodCard,
                        expiryMethodCard,
                        expiryPreviewText
                );
            });
        }
        if (defaultShelfLifeCheckbox != null) {
            defaultShelfLifeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && knownExpiryCheckbox != null && knownExpiryCheckbox.isChecked()) {
                    knownExpiryCheckbox.setChecked(false);
                } else if (!isChecked && knownExpiryCheckbox != null && !knownExpiryCheckbox.isChecked()) {
                    knownExpiryCheckbox.setChecked(true);
                }
                updateCatalogExpiryMethodUi(
                        defaultShelfLifeCheckbox,
                        purchaseInput,
                        shelfLifeInput,
                        saveShelfLifeDefaultCheckbox,
                        knownExpiryCheckbox,
                        expiryInput,
                        defaultShelfLifeMethodCard,
                        expiryMethodCard,
                        expiryPreviewText
                );
            });
        }
        if (saveShelfLifeDefaultCheckbox != null) {
            saveShelfLifeDefaultCheckbox.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> updateExpiryCheckboxDrawable(saveShelfLifeDefaultCheckbox)
            );
        }
        if (expiryMethodCard != null && knownExpiryCheckbox != null) {
            expiryMethodCard.setOnClickListener(v -> knownExpiryCheckbox.setChecked(true));
        }
        if (defaultShelfLifeMethodCard != null && defaultShelfLifeCheckbox != null) {
            defaultShelfLifeMethodCard.setOnClickListener(v -> defaultShelfLifeCheckbox.setChecked(true));
        }
        TextWatcher expiryPreviewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCatalogExpiryPreview(
                        defaultShelfLifeCheckbox,
                        purchaseInput,
                        shelfLifeInput,
                        knownExpiryCheckbox,
                        expiryInput,
                        expiryPreviewText
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        if (purchaseInput != null) {
            purchaseInput.addTextChangedListener(expiryPreviewWatcher);
        }
        if (expiryInput != null) {
            expiryInput.addTextChangedListener(expiryPreviewWatcher);
        }
        if (shelfLifeInput != null) {
            shelfLifeInput.addTextChangedListener(expiryPreviewWatcher);
        }
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> saveCatalogIngredient(
                    purchaseInput,
                    shelfLifeInput,
                    saveShelfLifeDefaultCheckbox,
                    knownExpiryCheckbox,
                    expiryInput
            ));
        }
    }

    /**
     * Shows recipe recommendations, optionally filtered by a search query from the dashboard.
     */
    private void bindRecipeRecommendationsScreen(View screen) {
        refreshInventoryStatuses();

        List<InventoryItem> inventory = inventoryService.getAllItems();
        List<Recipe> allRecipes = RecipeDatabase.getRecipes();
        List<Recipe> recommendedRecipes = recipeService.recommendRecipes(inventory, allRecipes);

        if (recommendedRecipes.isEmpty()) {
            // Keep page usable for demo even with sparse inventory.
            recommendedRecipes = allRecipes.subList(0, Math.min(5, allRecipes.size()));
        }

        String activeQuery = recipeSearchPending ? recipeSearchQuery : "";
        currentRecommendedRecipes = filterRecipesByQuery(recommendedRecipes, activeQuery);
        recipeSearchPending = false;
        recipeSearchQuery = "";

        LinearLayout recipeListContainer = screen.findViewById(R.id.recipeListContainer);
        if (recipeListContainer == null) {
            return;
        }

        recipeListContainer.removeAllViews();

        if (currentRecommendedRecipes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No recipes found for \"" + activeQuery + "\".");
            empty.setTextSize(14f);
            empty.setTextColor(getColor(R.color.fresh_blue));
            recipeListContainer.addView(empty);
            return;
        }

        for (int i = 0; i < currentRecommendedRecipes.size(); i++) {
            Recipe recipe = currentRecommendedRecipes.get(i);
            View card = createRecipeCard(recipe);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            card.setLayoutParams(cardParams);
            card.setOnClickListener(v -> {
                currentRecipe = recipe;
                openScreen(SCREEN_RECIPE_DETAIL);
            });
            recipeListContainer.addView(card);

            if (i < currentRecommendedRecipes.size() - 1) {
                View spacer = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(28)
                );
                spacer.setLayoutParams(params);
                recipeListContainer.addView(spacer);
            }
        }
    }

    /**
     * Filters recipe results by recipe name or ingredient name.
     */
    private List<Recipe> filterRecipesByQuery(List<Recipe> source, String query) {
        List<Recipe> results = new ArrayList<>();
        String target = normalize(query);
        if (target.isEmpty()) {
            results.addAll(source);
            return results;
        }

        for (Recipe recipe : source) {
            if (matchesRecipeQuery(recipe, target)) {
                results.add(recipe);
            }
        }
        return results;
    }

    private boolean matchesRecipeQuery(Recipe recipe, String query) {
        if (normalize(recipe.getName()).contains(query)) {
            return true;
        }
        for (String ingredientName : recipe.getIngredientsNeeded().keySet()) {
            if (normalize(ingredientName).contains(query)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Displays the selected recipe's image, ingredients, description, cooking time, and steps.
     */
    private void bindRecipeDetailScreen(View screen, Recipe recipe) {
        bindBackButton(screen, R.id.recipeDetailBackButton);

        if (recipe == null) {
            return;
        }

        TextView title = screen.findViewById(R.id.recipeDetailTitle);
        ImageView image = screen.findViewById(R.id.recipeDetailImage);
        TextView ingredients = screen.findViewById(R.id.recipeDetailIngredients);
        TextView description = screen.findViewById(R.id.recipeDetailDescription);
        TextView cookingTime = screen.findViewById(R.id.recipeDetailCookingTime);
        TextView steps = screen.findViewById(R.id.recipeDetailSteps);

        if (title != null) {
            title.setText(recipe.getName());
        }
        if (image != null) {
            bindRecipeImage(image, recipe);
        }
        if (ingredients != null) {
            ingredients.setText(formatIngredientsDetail(recipe));
        }
        if (description != null) {
            description.setText(recipe.getDescription());
        }
        if (cookingTime != null) {
            Integer minutes = recipe.getCookingTime();
            cookingTime.setText(minutes == null ? "Cooking time: N/A" : "Cooking time: " + minutes + " min");
        }
        if (steps != null) {
            steps.setText(formatSteps(recipe.getSteps()));
        }
    }

    /**
     * Displays inventory item details, delete/edit actions, and related recipe suggestions.
     */
    private void bindIngredientDetailScreen(View screen, InventoryItem item) {
        bindBackButton(screen, R.id.ingredientDetailBackButton);

        if (item == null) {
            switchRootScreen(SCREEN_INVENTORY);
            return;
        }

        expiryReminderService.updateItemStatus(item);
        inventoryService.updateItem(item);
        persistInventoryChanges();

        TextView name = screen.findViewById(R.id.ingredientDetailName);
        TextView expiryDate = screen.findViewById(R.id.ingredientDetailExpiryDate);
        TextView daysLeft = screen.findViewById(R.id.ingredientDetailDaysLeft);
        TextView storageMethods = screen.findViewById(R.id.ingredientStorageMethodsText);
        TextView deleteButton = screen.findViewById(R.id.ingredientDeleteButton);
        TextView editButton = screen.findViewById(R.id.ingredientEditButton);
        LinearLayout relatedRecipesContainer = screen.findViewById(R.id.ingredientRelatedRecipesContainer);

        if (name != null) {
            name.setText(item.getIngredientName());
        }
        if (expiryDate != null) {
            expiryDate.setText(formatDisplayDate(item.getExpiryDate()));
        }
        if (daysLeft != null) {
            int days = expiryReminderService.getDaysUntilExpiry(item);
            daysLeft.setText(formatDaysLeft(days));
            daysLeft.setTextColor(getStatusColor(item.getStatus()));
        }
        if (storageMethods != null) {
            storageMethods.setText(buildStorageMethodText(item));
        }
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                inventoryService.removeItemById(item.getId());
                persistInventoryChanges();
                currentInventoryItem = null;
                Toast.makeText(this, "Ingredient deleted.", Toast.LENGTH_SHORT).show();
                switchRootScreen(SCREEN_INVENTORY);
            });
        }
        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                editingInventoryItem = true;
                currentInventoryItem = item;
                selectedCategory = item.getCategory();
                openScreen(SCREEN_CUSTOM_INGREDIENT_ENTRY);
            });
        }
        if (relatedRecipesContainer != null) {
            renderRelatedRecipes(relatedRecipesContainer, item);
        }
    }

    /**
     * Renders the default ingredient catalog suggestions on the add ingredient screen.
     */
    private void renderDefaultIngredientSuggestions(LinearLayout suggestionsContainer) {
        suggestionsContainer.removeAllViews();
        suggestionsContainer.addView(createSuggestionHeader());

        List<Ingredient> ingredients = InventoryDatabase.getIngredients();
        int limit = Math.min(ingredients.size(), 20);
        for (int i = 0; i < limit; i++) {
            Ingredient ingredient = ingredients.get(i);
            View suggestion = createSuggestionRow(ingredient.getIngredientName(), i < limit - 1);
            suggestion.setOnClickListener(v -> openCatalogIngredientEntry(ingredient));
            suggestionsContainer.addView(suggestion);
        }
    }

    /**
     * Renders ingredient search matches and a manual-add option when there are no matches.
     */
    private void renderIngredientSearchResults(LinearLayout resultsContainer, String query) {
        resultsContainer.removeAllViews();

        List<Ingredient> matches = findSearchIngredientsByRelevance(query);
        if (matches.isEmpty()) {
            String ingredientName = query == null ? "" : query.trim();
            resultsContainer.addView(createEmptySuggestionText("No matches."));
            if (!ingredientName.isEmpty()) {
                View manualAddRow = createManualAddSearchResultRow(ingredientName);
                manualAddRow.setOnClickListener(v -> openCustomIngredientEntry(ingredientName));
                resultsContainer.addView(manualAddRow);
            }
            return;
        }

        int limit = Math.min(matches.size(), 6);
        for (int i = 0; i < limit; i++) {
            Ingredient ingredient = matches.get(i);
            View result = createSearchResultRow(ingredient.getIngredientName(), i < limit - 1);
            result.setOnClickListener(v -> openCatalogIngredientEntry(ingredient));
            resultsContainer.addView(result);
        }
    }

    /**
     * Opens the add form for a selected catalog ingredient.
     */
    private void openCatalogIngredientEntry(Ingredient ingredient) {
        selectedCatalogIngredient = ingredient;
        editingInventoryItem = false;
        currentInventoryItem = null;
        pendingCustomIngredientName = "";
        openScreen(SCREEN_INGREDIENT_ENTRY);
    }

    /**
     * Opens the manual ingredient form and pre-fills the typed ingredient name.
     */
    private void openCustomIngredientEntry(String ingredientName) {
        editingInventoryItem = false;
        currentInventoryItem = null;
        selectedCatalogIngredient = null;
        selectedCategory = null;
        pendingCustomIngredientName = ingredientName == null ? "" : ingredientName.trim();
        openScreen(SCREEN_CUSTOM_INGREDIENT_ENTRY);
    }

    private void hideIngredientSearchDropdown(EditText searchInput, View searchDropdown) {
        searchInput.clearFocus();
        searchDropdown.setVisibility(View.GONE);
    }

    private TextView createSuggestionHeader() {
        TextView header = new TextView(this);
        header.setText("SUGGESTED INGREDIENTS");
        header.setTextColor(getColor(R.color.fresh_green_dark));
        header.setTextSize(11f);
        header.setLetterSpacing(0.18f);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_medium), Typeface.NORMAL);
        header.setPadding(0, 0, 0, dpToPx(4));
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(28)
        ));
        return header;
    }

    private View createSuggestionRow(String text, boolean showDivider) {
        LinearLayout rowWrapper = new LinearLayout(this);
        rowWrapper.setOrientation(LinearLayout.VERTICAL);
        rowWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(35)
        ));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        TextView suggestion = new TextView(this);
        suggestion.setText(text);
        suggestion.setTextColor(getColor(R.color.fresh_text));
        suggestion.setTextSize(16f);
        suggestion.setGravity(Gravity.CENTER_VERTICAL);
        suggestion.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));

        FrameLayout addIcon = new FrameLayout(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(18),
                dpToPx(18)
        );
        addIcon.setLayoutParams(iconParams);
        addIcon.setBackgroundResource(R.drawable.bg_suggestion_add_circle);

        ImageView plus = new ImageView(this);
        FrameLayout.LayoutParams plusParams = new FrameLayout.LayoutParams(
                dpToPx(10),
                dpToPx(10),
                Gravity.CENTER
        );
        plus.setLayoutParams(plusParams);
        plus.setImageResource(R.drawable.ic_suggestion_add_plus);
        addIcon.addView(plus);

        row.addView(suggestion);
        row.addView(addIcon);
        rowWrapper.addView(row);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFECECEC);
        divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));
        rowWrapper.addView(divider);

        return rowWrapper;
    }

    private View createSearchResultRow(String text, boolean showDivider) {
        LinearLayout rowWrapper = new LinearLayout(this);
        rowWrapper.setOrientation(LinearLayout.VERTICAL);
        rowWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(35)
        ));

        TextView suggestion = new TextView(this);
        suggestion.setText(text);
        suggestion.setTextColor(getColor(R.color.fresh_text));
        suggestion.setTextSize(16f);
        suggestion.setGravity(Gravity.CENTER_VERTICAL);
        suggestion.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        rowWrapper.addView(suggestion);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFECECEC);
        divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));
        rowWrapper.addView(divider);

        return rowWrapper;
    }

    private View createManualAddSearchResultRow(String ingredientName) {
        LinearLayout rowWrapper = new LinearLayout(this);
        rowWrapper.setOrientation(LinearLayout.VERTICAL);
        rowWrapper.setClickable(true);
        rowWrapper.setFocusable(true);
        rowWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(42)
        ));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        TextView action = new TextView(this);
        action.setText("Add \"" + ingredientName + "\" manually");
        action.setTextColor(getColor(R.color.fresh_green_dark));
        action.setTextSize(16f);
        action.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_bold), Typeface.NORMAL);
        action.setGravity(Gravity.CENTER_VERTICAL);
        action.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));

        FrameLayout addIcon = new FrameLayout(this);
        addIcon.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(18),
                dpToPx(18)
        ));
        addIcon.setBackgroundResource(R.drawable.bg_suggestion_add_circle);

        ImageView plus = new ImageView(this);
        plus.setLayoutParams(new FrameLayout.LayoutParams(
                dpToPx(10),
                dpToPx(10),
                Gravity.CENTER
        ));
        plus.setImageResource(R.drawable.ic_suggestion_add_plus);
        addIcon.addView(plus);

        row.addView(action);
        row.addView(addIcon);
        rowWrapper.addView(row);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFECECEC);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));
        rowWrapper.addView(divider);

        return rowWrapper;
    }

    private TextView createEmptySuggestionText(String text) {
        TextView suggestion = new TextView(this);
        suggestion.setText(text);
        suggestion.setTextColor(getColor(R.color.fresh_tag_blue));
        suggestion.setTextSize(14f);
        suggestion.setGravity(Gravity.CENTER_VERTICAL);
        suggestion.setPadding(0, dpToPx(8), 0, dpToPx(2));
        return suggestion;
    }

    private void bindBackButton(View screen, int backButtonId) {
        View backButton = screen.findViewById(backButtonId);
        if (backButton != null) {
            backButton.setOnClickListener(v -> navigateBackOrRoot());
        }
    }

    /**
     * Finds catalog ingredients ordered by search relevance.
     */
    private List<Ingredient> findSearchIngredientsByRelevance(String query) {
        String target = normalize(query);
        List<Ingredient> matches = new ArrayList<>();
        for (Ingredient ingredient : InventoryDatabase.getIngredients()) {
            if (target.isEmpty() || searchRelevanceScore(ingredient, target) < Integer.MAX_VALUE) {
                matches.add(ingredient);
            }
        }
        if (!target.isEmpty()) {
            matches.sort((first, second) -> {
                int firstScore = searchRelevanceScore(first, target);
                int secondScore = searchRelevanceScore(second, target);
                if (firstScore != secondScore) {
                    return firstScore - secondScore;
                }
                int lengthDiff = first.getIngredientName().length() - second.getIngredientName().length();
                if (lengthDiff != 0) {
                    return lengthDiff;
                }
                return first.getIngredientName().compareToIgnoreCase(second.getIngredientName());
            });
        }
        return matches;
    }

    private int searchRelevanceScore(Ingredient ingredient, String target) {
        if (target.isEmpty()) {
            return 0;
        }
        String name = normalize(ingredient.getIngredientName());
        if (name.equals(target)) {
            return 0;
        }
        if (name.startsWith(target)) {
            return 1;
        }
        for (String word : name.split("\\s+")) {
            if (word.startsWith(target)) {
                return 2;
            }
        }
        if (name.contains(target)) {
            return 3;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Saves a catalog ingredient into the inventory and optionally updates its default shelf life.
     */
    private void saveCatalogIngredient(EditText purchaseInput,
                                       EditText shelfLifeInput,
                                       CheckBox saveShelfLifeDefaultCheckbox,
                                       CheckBox knownExpiryCheckbox,
                                       EditText expiryInput) {
        if (selectedCatalogIngredient == null) {
            Toast.makeText(this, "Please choose an ingredient first.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String purchaseDate = readDateOrToday(purchaseInput);
            String expiryDate;

            if (isChecked(knownExpiryCheckbox) && hasText(expiryInput)) {
                expiryDate = normalizeDateInput(expiryInput.getText().toString());
            } else {
                int shelfLifeDays = readCatalogShelfLifeDays(shelfLifeInput);
                expiryDate = addDays(purchaseDate, shelfLifeDays);
                if (isChecked(saveShelfLifeDefaultCheckbox)) {
                    InventoryDatabase.updateDefaultShelfLifeDays(
                            selectedCatalogIngredient.getIngredientId(),
                            shelfLifeDays
                    );
                    selectedCatalogIngredient.setDefaultShelfLifeDays(shelfLifeDays);
                    persistCatalogShelfLifeOverride(
                            selectedCatalogIngredient.getIngredientId(),
                            shelfLifeDays
                    );
                }
            }

            InventoryItem item = new InventoryItem(
                    inventoryService.getNextAvailableId(),
                    selectedCatalogIngredient.getIngredientName(),
                    selectedCatalogIngredient.getCategory(),
                    DEFAULT_QUANTITY,
                    purchaseDate,
                    expiryDate
            );
            expiryReminderService.updateItemStatus(item);
            inventoryService.addItem(item);
            persistInventoryChanges();

            currentInventoryItem = item;
            String message = isChecked(saveShelfLifeDefaultCheckbox) && !isChecked(knownExpiryCheckbox)
                    ? "Ingredient saved. Default shelf life updated."
                    : "Ingredient saved.";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            openScreen(SCREEN_INGREDIENT_DETAIL);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Shelf life must be a number.", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Saves a manually entered ingredient or updates an existing inventory item.
     */
    private void saveCustomIngredient(EditText nameInput,
                                      CheckBox knownExpiryCheckbox,
                                      EditText expiryInput,
                                      CheckBox knownShelfLifeCheckbox,
                                      EditText purchaseInput,
                                      EditText shelfLifeInput) {
        try {
            String ingredientName = readRequiredText(nameInput, "Ingredient name");
            String purchaseDate = readDateOrToday(purchaseInput);
            String expiryDate;

            if (isChecked(knownExpiryCheckbox) && hasText(expiryInput)) {
                expiryDate = normalizeDateInput(expiryInput.getText().toString());
            } else if (isChecked(knownShelfLifeCheckbox) && hasText(shelfLifeInput)) {
                int shelfLifeDays = Integer.parseInt(shelfLifeInput.getText().toString().trim());
                if (shelfLifeDays < 0) {
                    throw new IllegalArgumentException("Shelf life cannot be negative.");
                }
                expiryDate = addDays(purchaseDate, shelfLifeDays);
            } else {
                expiryDate = expiryReminderService.estimateExpiryDate(
                        ingredientName,
                        getSelectedCategoryOrDefault(),
                        purchaseDate
                );
            }

            InventoryItem item;
            if (editingInventoryItem && currentInventoryItem != null) {
                item = new InventoryItem(
                        currentInventoryItem.getId(),
                        ingredientName,
                        getSelectedCategoryOrDefault(),
                        currentInventoryItem.getQuantity(),
                        purchaseDate,
                        expiryDate
                );
                expiryReminderService.updateItemStatus(item);
                inventoryService.updateItem(item);
            } else {
                item = new InventoryItem(
                        inventoryService.getNextAvailableId(),
                        ingredientName,
                        getSelectedCategoryOrDefault(),
                        DEFAULT_QUANTITY,
                        purchaseDate,
                        expiryDate
                );
                expiryReminderService.updateItemStatus(item);
                inventoryService.addItem(item);
            }

            persistInventoryChanges();
            currentInventoryItem = item;
            editingInventoryItem = false;
            Toast.makeText(this, "Ingredient saved.", Toast.LENGTH_SHORT).show();
            openScreen(SCREEN_INGREDIENT_DETAIL);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Shelf life must be a number.", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindCategoryChoice(View screen, int viewId, String category) {
        View categoryView = screen.findViewById(viewId);
        if (categoryView != null) {
            categoryView.setOnClickListener(v -> {
                selectedCategory = category;
                updateCategorySelection(screen);
            });
        }
    }

    private void updateCategorySelection(View screen) {
        updateCategoryAlpha(screen, R.id.categoryVegetables, "Vegetables");
        updateCategoryAlpha(screen, R.id.categoryFruits, "Fruits");
        updateCategoryAlpha(screen, R.id.categoryMeat, "Meat");
        updateCategoryAlpha(screen, R.id.categoryOthers, DEFAULT_CATEGORY);
    }

    private void updateCategoryAlpha(View screen, int viewId, String category) {
        View categoryView = screen.findViewById(viewId);
        if (categoryView != null) {
            categoryView.setAlpha(category.equals(selectedCategory) ? 1.0f : 0.55f);
        }
    }

    private String getSelectedCategoryOrDefault() {
        return selectedCategory == null ? DEFAULT_CATEGORY : selectedCategory;
    }

    private void updateDateInputState(CheckBox checkbox, EditText input) {
        if (checkbox == null || input == null) {
            return;
        }
        boolean enabled = checkbox.isChecked();
        int paddingLeft = input.getPaddingLeft();
        int paddingTop = input.getPaddingTop();
        int paddingRight = input.getPaddingRight();
        int paddingBottom = input.getPaddingBottom();
        input.setEnabled(true);
        input.setAlpha(enabled ? 1.0f : 0.55f);
        input.setFocusable(enabled);
        input.setFocusableInTouchMode(enabled);
        input.setBackgroundResource(enabled ? R.drawable.bg_input_white : R.drawable.bg_input_disabled);
        input.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    private void updateShelfLifeInputState(CheckBox checkbox, EditText purchaseInput, EditText shelfLifeInput) {
        updateDateInputState(checkbox, purchaseInput);
        updateDateInputState(checkbox, shelfLifeInput);
    }

    /**
     * Updates the catalog ingredient expiry method controls and preview text.
     */
    private void updateCatalogExpiryMethodUi(CheckBox defaultShelfLifeCheckbox,
                                             EditText purchaseInput,
                                             EditText shelfLifeInput,
                                             CheckBox saveShelfLifeDefaultCheckbox,
                                             CheckBox knownExpiryCheckbox,
                                             EditText expiryInput,
                                             View defaultShelfLifeMethodCard,
                                             View expiryMethodCard,
                                             TextView expiryPreviewText) {
        updateExpiryCheckboxDrawable(defaultShelfLifeCheckbox);
        updateExpiryCheckboxDrawable(knownExpiryCheckbox);
        updateExpiryCheckboxDrawable(saveShelfLifeDefaultCheckbox);
        updateShelfLifeInputState(defaultShelfLifeCheckbox, purchaseInput, shelfLifeInput);
        updateOptionCheckboxState(defaultShelfLifeCheckbox, saveShelfLifeDefaultCheckbox);
        updateDateInputState(knownExpiryCheckbox, expiryInput);
        updateExpiryMethodCard(defaultShelfLifeMethodCard, isChecked(defaultShelfLifeCheckbox));
        updateExpiryMethodCard(expiryMethodCard, isChecked(knownExpiryCheckbox));
        updateCatalogExpiryPreview(
                defaultShelfLifeCheckbox,
                purchaseInput,
                shelfLifeInput,
                knownExpiryCheckbox,
                expiryInput,
                expiryPreviewText
        );
    }

    private void updateCatalogExpiryPreview(CheckBox defaultShelfLifeCheckbox,
                                            EditText purchaseInput,
                                            EditText shelfLifeInput,
                                            CheckBox knownExpiryCheckbox,
                                            EditText expiryInput,
                                            TextView expiryPreviewText) {
        if (expiryPreviewText == null || selectedCatalogIngredient == null) {
            return;
        }
        try {
            if (isChecked(knownExpiryCheckbox)) {
                expiryPreviewText.setText(hasText(expiryInput)
                        ? formatDateForInput(normalizeDateInput(expiryInput.getText().toString()))
                        : "Enter expiry date");
                return;
            }
            if (isChecked(defaultShelfLifeCheckbox)) {
                String purchaseDate = readDateOrToday(purchaseInput);
                expiryPreviewText.setText(formatDateForInput(addDays(purchaseDate, readCatalogShelfLifeDays(shelfLifeInput))));
                return;
            }
            expiryPreviewText.setText("Choose a method");
        } catch (NumberFormatException e) {
            expiryPreviewText.setText("Invalid shelf life");
        } catch (IllegalArgumentException e) {
            expiryPreviewText.setText("Check date format");
        }
    }

    private void updateOptionCheckboxState(CheckBox ownerCheckbox, CheckBox optionCheckbox) {
        if (optionCheckbox == null) {
            return;
        }
        boolean enabled = isChecked(ownerCheckbox);
        optionCheckbox.setEnabled(enabled);
        optionCheckbox.setAlpha(enabled ? 1.0f : 0.55f);
        if (!enabled) {
            optionCheckbox.setChecked(false);
        }
    }

    private int readCatalogShelfLifeDays(EditText shelfLifeInput) {
        String value = shelfLifeInput == null ? "" : shelfLifeInput.getText().toString().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Shelf life is required.");
        }
        int shelfLifeDays = Integer.parseInt(value);
        if (shelfLifeDays < 0) {
            throw new IllegalArgumentException("Shelf life cannot be negative.");
        }
        return shelfLifeDays;
    }

    /**
     * Loads saved shelf-life overrides for catalog ingredients from shared preferences.
     */
    private void loadCatalogShelfLifeOverrides() {
        SharedPreferences preferences = getSharedPreferences(CATALOG_SHELF_LIFE_PREFS, MODE_PRIVATE);
        for (Ingredient ingredient : InventoryDatabase.getIngredients()) {
            String key = getCatalogShelfLifePreferenceKey(ingredient.getIngredientId());
            if (preferences.contains(key)) {
                InventoryDatabase.updateDefaultShelfLifeDays(
                        ingredient.getIngredientId(),
                        preferences.getInt(key, ingredient.getDefaultShelfLifeDays())
                );
            }
        }
    }

    /**
     * Saves a custom default shelf life for a catalog ingredient.
     */
    private void persistCatalogShelfLifeOverride(int ingredientId, int shelfLifeDays) {
        getSharedPreferences(CATALOG_SHELF_LIFE_PREFS, MODE_PRIVATE)
                .edit()
                .putInt(getCatalogShelfLifePreferenceKey(ingredientId), shelfLifeDays)
                .apply();
    }

    private String getCatalogShelfLifePreferenceKey(int ingredientId) {
        return CATALOG_SHELF_LIFE_KEY_PREFIX + ingredientId;
    }

    /**
     * Updates the custom ingredient expiry method controls and preview text.
     */
    private void updateCustomExpiryMethodUi(CheckBox knownExpiryCheckbox,
                                            EditText expiryInput,
                                            CheckBox knownShelfLifeCheckbox,
                                            EditText purchaseInput,
                                            EditText shelfLifeInput,
                                            View expiryMethodCard,
                                            View shelfLifeMethodCard,
                                            TextView expiryPreviewText) {
        updateExpiryCheckboxDrawable(knownExpiryCheckbox);
        updateExpiryCheckboxDrawable(knownShelfLifeCheckbox);
        updateDateInputState(knownExpiryCheckbox, expiryInput);
        updateShelfLifeInputState(knownShelfLifeCheckbox, purchaseInput, shelfLifeInput);
        updateExpiryMethodCard(expiryMethodCard, isChecked(knownExpiryCheckbox));
        updateExpiryMethodCard(shelfLifeMethodCard, isChecked(knownShelfLifeCheckbox));
        updateCustomExpiryPreview(
                knownExpiryCheckbox,
                expiryInput,
                knownShelfLifeCheckbox,
                purchaseInput,
                shelfLifeInput,
                expiryPreviewText
        );
    }

    private void updateExpiryMethodCard(View card, boolean selected) {
        if (card == null) {
            return;
        }
        card.setBackgroundResource(selected
                ? R.drawable.bg_expiry_method_selected
                : R.drawable.bg_expiry_method_unselected);
        card.setAlpha(1.0f);
    }

    private void updateExpiryCheckboxDrawable(CheckBox checkbox) {
        if (checkbox == null) {
            return;
        }
        checkbox.setButtonDrawable(null);
        checkbox.setCompoundDrawablesWithIntrinsicBounds(
                checkbox.isChecked()
                        ? R.drawable.checkbox_expiry_checked
                        : R.drawable.checkbox_expiry_unchecked,
                0,
                0,
                0
        );
        checkbox.setCompoundDrawablePadding(dpToPx(8));
    }

    private void updateCustomExpiryPreview(CheckBox knownExpiryCheckbox,
                                           EditText expiryInput,
                                           CheckBox knownShelfLifeCheckbox,
                                           EditText purchaseInput,
                                           EditText shelfLifeInput,
                                           TextView expiryPreviewText) {
        if (expiryPreviewText == null) {
            return;
        }
        try {
            if (isChecked(knownExpiryCheckbox)) {
                expiryPreviewText.setText(hasText(expiryInput)
                        ? formatDateForInput(normalizeDateInput(expiryInput.getText().toString()))
                        : "Enter expiry date");
                return;
            }
            if (isChecked(knownShelfLifeCheckbox)) {
                if (!hasText(shelfLifeInput)) {
                    expiryPreviewText.setText("Enter shelf life");
                    return;
                }
                int shelfLifeDays = Integer.parseInt(shelfLifeInput.getText().toString().trim());
                if (shelfLifeDays < 0) {
                    expiryPreviewText.setText("Invalid shelf life");
                    return;
                }
                String purchaseDate = readDateOrToday(purchaseInput);
                expiryPreviewText.setText(formatDateForInput(addDays(purchaseDate, shelfLifeDays)));
                return;
            }
            expiryPreviewText.setText("Choose a method");
        } catch (NumberFormatException e) {
            expiryPreviewText.setText("Invalid shelf life");
        } catch (IllegalArgumentException e) {
            expiryPreviewText.setText("Check date format");
        }
    }

    /**
     * Renders dashboard expiry reminder tags for items expiring soon.
     */
    private void renderDashboardReminders(View screen) {
        ConstraintLayout reminderContainer = screen.findViewById(R.id.dashboardReminderContainer);
        if (reminderContainer == null) {
            return;
        }

        reminderContainer.removeAllViews();
        View statusLegend = createDashboardStatusLegend(reminderContainer);
        List<InventoryItem> reminderItems = getItemsExpiringWithinDays(DASHBOARD_REMINDER_DAYS);
        if (reminderItems.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No ingredients expiring soon.");
            empty.setTextColor(getColor(R.color.fresh_blue));
            empty.setTextSize(14f);
            ConstraintLayout.LayoutParams emptyParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            emptyParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            emptyParams.topToBottom = statusLegend.getId();
            emptyParams.topMargin = dpToPx(12);
            empty.setLayoutParams(emptyParams);
            reminderContainer.addView(empty);
            return;
        }

        Flow tagFlow = new Flow(this);
        tagFlow.setId(View.generateViewId());
        tagFlow.setWrapMode(Flow.WRAP_CHAIN);
        tagFlow.setHorizontalAlign(Flow.HORIZONTAL_ALIGN_START);
        tagFlow.setHorizontalStyle(Flow.CHAIN_PACKED);
        tagFlow.setHorizontalBias(0f);
        tagFlow.setFirstHorizontalBias(0f);
        tagFlow.setLastHorizontalBias(0f);
        tagFlow.setHorizontalGap(dpToPx(8));
        tagFlow.setVerticalGap(dpToPx(10));
        ConstraintLayout.LayoutParams flowParams = new ConstraintLayout.LayoutParams(
                0,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        flowParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        flowParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        flowParams.topToBottom = statusLegend.getId();
        flowParams.topMargin = dpToPx(12);
        tagFlow.setLayoutParams(flowParams);

        int[] tagIds = new int[reminderItems.size()];
        for (int i = 0; i < reminderItems.size(); i++) {
            InventoryItem item = reminderItems.get(i);
            TextView tag = createReminderTag(item);
            int tagId = View.generateViewId();
            tag.setId(tagId);
            tagIds[i] = tagId;
            tag.setOnClickListener(v -> {
                currentInventoryItem = item;
                openScreen(SCREEN_INGREDIENT_DETAIL);
            });
            reminderContainer.addView(tag);
        }
        tagFlow.setReferencedIds(tagIds);
        reminderContainer.addView(tagFlow);
    }

    private View createDashboardStatusLegend(ConstraintLayout reminderContainer) {
        View statusLegend = LayoutInflater.from(this)
                .inflate(R.layout.layout_dashboard_status_legend, reminderContainer, false);
        statusLegend.setId(View.generateViewId());
        ConstraintLayout.LayoutParams legendParams = new ConstraintLayout.LayoutParams(
                0,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        legendParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        legendParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        legendParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        statusLegend.setLayoutParams(legendParams);
        reminderContainer.addView(statusLegend);
        return statusLegend;
    }

    private TextView createReminderTag(InventoryItem item) {
        TextView tag = (TextView) LayoutInflater.from(this)
                .inflate(R.layout.item_ingredient_tag, rootContainer, false);
        tag.setText(item.getIngredientName().toLowerCase(Locale.US));
        int days = expiryReminderService.getDaysUntilExpiry(item);
        if (days < 0) {
            tag.setBackgroundResource(R.drawable.bg_tag_expired);
            tag.setTextColor(getColor(R.color.fresh_expired));
        } else if (days <= ExpiryReminderService.NEAR_EXPIRY_THRESHOLD_DAYS) {
            tag.setBackgroundResource(R.drawable.bg_tag_red);
            tag.setTextColor(getColor(R.color.fresh_red));
        } else {
            tag.setBackgroundResource(R.drawable.bg_tag_blue);
            tag.setTextColor(getColor(R.color.fresh_blue));
        }
        return tag;
    }

    private List<InventoryItem> getItemsExpiringWithinDays(int daysLimit) {
        List<InventoryItem> results = new ArrayList<>();
        for (InventoryItem item : inventoryService.getItemsSortedByExpiryDate()) {
            int days = expiryReminderService.getDaysUntilExpiry(item);
            if (days <= daysLimit) {
                results.add(item);
            }
        }
        return results;
    }

    /**
     * Renders recipe cards that use the selected inventory ingredient.
     */
    private void renderRelatedRecipes(LinearLayout relatedRecipesContainer, InventoryItem item) {
        relatedRecipesContainer.removeAllViews();

        List<Recipe> relatedRecipes = findRecipesUsingIngredient(item.getIngredientName());
        if (relatedRecipes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No related recipes yet.");
            empty.setTextColor(getColor(R.color.fresh_blue));
            empty.setTextSize(14f);
            relatedRecipesContainer.addView(empty);
            return;
        }

        int limit = Math.min(relatedRecipes.size(), 3);
        for (int i = 0; i < limit; i++) {
            Recipe recipe = relatedRecipes.get(i);
            View card = createRecipeCard(recipe);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.rightMargin = dpToPx(14);
            card.setLayoutParams(cardParams);
            card.setOnClickListener(v -> {
                currentRecipe = recipe;
                openScreen(SCREEN_RECIPE_DETAIL);
            });
            relatedRecipesContainer.addView(card);
        }
    }

    /**
     * Finds recipes containing the given ingredient name.
     */
    private List<Recipe> findRecipesUsingIngredient(String ingredientName) {
        List<Recipe> relatedRecipes = new ArrayList<>();
        String target = normalize(ingredientName);
        for (Recipe recipe : RecipeDatabase.getRecipes()) {
            for (String recipeIngredient : recipe.getIngredientsNeeded().keySet()) {
                if (normalize(recipeIngredient).equals(target)) {
                    relatedRecipes.add(recipe);
                    break;
                }
            }
        }
        return relatedRecipes;
    }

    private List<TextView> collectTextViews(View root) {
        List<TextView> textViews = new ArrayList<>();
        if (root instanceof TextView) {
            textViews.add((TextView) root);
            return textViews;
        }
        if (root instanceof LinearLayout) {
            LinearLayout group = (LinearLayout) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                textViews.addAll(collectTextViews(group.getChildAt(i)));
            }
        } else if (root instanceof android.widget.ScrollView) {
            android.widget.ScrollView scroll = (android.widget.ScrollView) root;
            if (scroll.getChildCount() > 0) {
                textViews.addAll(collectTextViews(scroll.getChildAt(0)));
            }
        }
        return textViews;
    }

    /**
     * Creates a reusable recipe card with image, title, ingredient summary, and cooking time.
     */
    private View createRecipeCard(Recipe recipe) {
        LinearLayout card = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.item_recipe_card, rootContainer, false);

        ImageView imageView = card.findViewById(R.id.recipeCardImage);
        if (imageView != null) {
            bindRecipeImage(imageView, recipe);
        }

        List<TextView> textViews = collectTextViews(card);
        if (textViews.size() >= 2) {
            textViews.get(0).setText(recipe.getName());
            textViews.get(1).setText(formatIngredientsSummary(recipe));
        }

        Integer cookingTime = recipe.getCookingTime();
        if (cookingTime != null) {
            TextView timeView = new TextView(this);
            timeView.setText(String.format(Locale.US, "Cooking Time: %d min", cookingTime));
            timeView.setTextSize(10f);
            timeView.setTextColor(getColor(R.color.fresh_blue));
            card.addView(timeView);
        }

        return card;
    }

    /**
     * Shows a recipe image when its drawable exists, otherwise falls back to a placeholder.
     */
    private void bindRecipeImage(ImageView imageView, Recipe recipe) {
        int imageResourceId = getRecipeImageResourceId(recipe);
        if (imageResourceId == 0) {
            imageView.setImageDrawable(null);
            imageView.setBackgroundResource(R.drawable.bg_recipe_image_placeholder);
            return;
        }

        imageView.setBackgroundResource(R.drawable.bg_recipe_image_placeholder);
        imageView.setImageResource(imageResourceId);
        imageView.setContentDescription(recipe.getName());
    }

    private int getRecipeImageResourceId(Recipe recipe) {
        String imageResName = recipe.getImageResName();
        if (imageResName == null || imageResName.trim().isEmpty()) {
            return 0;
        }

        Integer cachedResourceId = recipeImageResourceIds.get(imageResName);
        if (cachedResourceId != null) {
            return cachedResourceId;
        }

        int resourceId = getResources().getIdentifier(
                imageResName,
                "drawable",
                getPackageName()
        );
        recipeImageResourceIds.put(imageResName, resourceId);
        return resourceId;
    }

    private void preloadRecipeImages(List<Recipe> recipes) {
        for (Recipe recipe : recipes) {
            getRecipeImageResourceId(recipe);
        }
    }

    /**
     * Formats recipe ingredients and highlights inventory items that are expired or near expiry.
     */
    private CharSequence formatIngredientsSummary(Recipe recipe) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int count = 0;
        for (String ingredientName : recipe.getIngredientsNeeded().keySet()) {
            if (count > 0) {
                builder.append(", ");
            }
            appendIngredientWithStatusColor(builder, ingredientName);
            count++;
            if (count == 6 && recipe.getIngredientsNeeded().size() > 6) {
                builder.append("...");
                break;
            }
        }
        return builder;
    }

    private CharSequence formatIngredientsDetail(Recipe recipe) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (Map.Entry<String, Quantity> entry : recipe.getIngredientsNeeded().entrySet()) {
            Quantity quantity = entry.getValue();
            builder.append("- ");
            appendIngredientWithStatusColor(builder, entry.getKey());
            builder
                    .append(": ")
                    .append(String.valueOf(quantity.getAmount()))
                    .append(" ")
                    .append(quantity.getUnit())
                    .append("\n");
        }
        int length = builder.length();
        if (length > 0 && builder.charAt(length - 1) == '\n') {
            builder.delete(length - 1, length);
        }
        return builder;
    }

    private void appendIngredientWithStatusColor(SpannableStringBuilder builder, String ingredientName) {
        int start = builder.length();
        builder.append(ingredientName);
        Integer statusColor = getRecipeIngredientStatusColor(ingredientName);
        if (statusColor != null) {
            builder.setSpan(
                    new ForegroundColorSpan(statusColor),
                    start,
                    builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private Integer getRecipeIngredientStatusColor(String ingredientName) {
        boolean hasNearExpiryMatch = false;
        String target = normalize(ingredientName);

        for (InventoryItem item : inventoryService.getAllItems()) {
            if (!normalize(item.getIngredientName()).equals(target)) {
                continue;
            }

            String status = expiryReminderService.calculateStatus(item);
            if (InventoryItem.STATUS_EXPIRED.equals(status)) {
                return getColor(R.color.fresh_expired);
            }
            if (InventoryItem.STATUS_NEAR_EXPIRY.equals(status)) {
                hasNearExpiryMatch = true;
            }
        }

        if (hasNearExpiryMatch) {
            return getColor(R.color.fresh_red);
        }
        return null;
    }

    private String formatSteps(List<String> steps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            builder.append(i + 1)
                    .append(". ")
                    .append(steps.get(i))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String formatInventoryLine(InventoryItem item) {
        int daysUntilExpiry = expiryReminderService.getDaysUntilExpiry(item);
        if (daysUntilExpiry < 0) {
            int expiredDays = Math.abs(daysUntilExpiry);
            return item.getIngredientName() + " expired " + expiredDays + " " + formatDayUnit(expiredDays) + " ago";
        }
        if (daysUntilExpiry == 0) {
            return item.getIngredientName() + " expires today";
        }
        return item.getIngredientName() + " will expire in " + daysUntilExpiry + " " + formatDayUnit(daysUntilExpiry);
    }

    private String formatInventoryExpirySummary(int daysUntilExpiry) {
        if (daysUntilExpiry < 0) {
            int expiredDays = Math.abs(daysUntilExpiry);
            return "Expired " + expiredDays + " " + formatDayUnit(expiredDays) + " ago";
        }
        if (daysUntilExpiry == 0) {
            return "Expires today";
        }
        return "Will expire in " + daysUntilExpiry + " " + formatDayUnit(daysUntilExpiry);
    }

    private String formatDayUnit(int days) {
        return days == 1 ? "day" : "days";
    }

    private String getInventoryDisplayCategory(InventoryItem item) {
        String category = item.getCategory();
        if (INVENTORY_CATEGORY_VEGETABLES.equals(category)) {
            return INVENTORY_CATEGORY_VEGETABLES;
        }
        if (INVENTORY_CATEGORY_FRUITS.equals(category)) {
            return INVENTORY_CATEGORY_FRUITS;
        }
        if (INVENTORY_CATEGORY_MEAT.equals(category) || "Protein".equals(category)) {
            return INVENTORY_CATEGORY_MEAT;
        }
        return DEFAULT_CATEGORY;
    }

    private String formatDaysLeft(int days) {
        if (days < 0) {
            return "Expired";
        }
        if (days == 0) {
            return "Today";
        }
        return days + (days == 1 ? " Day Left" : " Days Left");
    }

    private String buildStorageMethodText(InventoryItem item) {
        int defaultShelfLife = expiryReminderService.getDefaultShelfLifeDays(
                item.getIngredientName(),
                item.getCategory()
        );
        return item.getIngredientName()
                + " is usually stored for about "
                + defaultShelfLife
                + " day(s). Keep it sealed and refrigerated when appropriate, and use it before the expiry date.";
    }

    private int getStatusColor(String status) {
        if (InventoryItem.STATUS_EXPIRED.equals(status)) {
            return getColor(R.color.fresh_expired);
        }
        if (InventoryItem.STATUS_NEAR_EXPIRY.equals(status)) {
            return getColor(R.color.fresh_red);
        }
        return getColor(R.color.fresh_green_dark);
    }

    /**
     * Refreshes expiry statuses for all inventory items and persists the latest values.
     */
    private void refreshInventoryStatuses() {
        List<InventoryItem> allItems = inventoryService.getAllItems();
        for (InventoryItem item : allItems) {
            expiryReminderService.updateItemStatus(item);
            inventoryService.updateItem(item);
        }
        persistInventoryChanges();
    }

    /**
     * Saves the current inventory list to persistent storage.
     */
    private void persistInventoryChanges() {
        inventoryStorage.saveItems(inventoryService.getAllItems());
    }

    private boolean isChecked(CheckBox checkBox) {
        return checkBox != null && checkBox.isChecked();
    }

    private boolean hasText(EditText input) {
        return input != null && input.getText() != null && !input.getText().toString().trim().isEmpty();
    }

    private String readRequiredText(EditText input, String fieldName) {
        if (!hasText(input)) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return input.getText().toString().trim();
    }

    private String readDateOrToday(EditText input) {
        if (!hasText(input)) {
            return getTodayString();
        }
        return normalizeDateInput(input.getText().toString());
    }

    private void bindDatePickerButton(View button, EditText input, CheckBox methodCheckbox) {
        if (button == null || input == null) {
            return;
        }
        button.setOnClickListener(v -> {
            selectMethodForInput(methodCheckbox);
            hideKeyboard(input);
            input.clearFocus();
            showDatePicker(input);
        });
    }

    private void bindMethodInput(EditText input, CheckBox methodCheckbox) {
        if (input == null) {
            return;
        }
        input.setOnClickListener(v -> {
            selectMethodForInput(methodCheckbox);
            input.requestFocus();
            scrollInputAboveKeyboard(input);
        });
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectMethodForInput(methodCheckbox);
                scrollInputAboveKeyboard(input);
            }
        });
    }

    private void scrollInputAboveKeyboard(EditText input) {
        ScrollView scrollView = findParentScrollView(input);
        if (scrollView == null) {
            return;
        }
        input.postDelayed(() -> {
            Rect inputRect = new Rect();
            input.getDrawingRect(inputRect);
            scrollView.offsetDescendantRectToMyCoords(input, inputRect);

            int visibleBottom = scrollView.getScrollY()
                    + scrollView.getHeight()
                    - scrollView.getPaddingBottom()
                    - Math.max(keyboardBottomInset, dpToPx(96));
            if (inputRect.bottom > visibleBottom) {
                int targetScrollY = scrollView.getScrollY() + inputRect.bottom - visibleBottom;
                scrollView.smoothScrollTo(0, targetScrollY);
            }
        }, 260);
    }

    private ScrollView findParentScrollView(View view) {
        View current = view;
        while (current != null) {
            if (current instanceof ScrollView) {
                return (ScrollView) current;
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void selectMethodForInput(CheckBox methodCheckbox) {
        if (methodCheckbox != null && !methodCheckbox.isChecked()) {
            methodCheckbox.setChecked(true);
        }
    }

    /**
     * Opens a compact day/month/year picker and writes the selected date into the input field.
     */
    private void showDatePicker(EditText input) {
        Calendar calendar = getCalendarForDatePicker(input);

        LinearLayout pickerContainer = new LinearLayout(this);
        pickerContainer.setOrientation(LinearLayout.VERTICAL);
        pickerContainer.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
        pickerContainer.setBackground(createDatePickerPopupBackground());

        LinearLayout pickerRow = new LinearLayout(this);
        pickerRow.setGravity(Gravity.CENTER);
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);

        NumberPicker dayPicker = createCompactNumberPicker(1, 31, calendar.get(Calendar.DAY_OF_MONTH));
        NumberPicker monthPicker = createCompactNumberPicker(0, 11, calendar.get(Calendar.MONTH));
        monthPicker.setDisplayedValues(MONTH_PICKER_VALUES);
        NumberPicker yearPicker = createCompactNumberPicker(
                calendar.get(Calendar.YEAR) - 10,
                calendar.get(Calendar.YEAR) + 10,
                calendar.get(Calendar.YEAR)
        );
        updateDayPickerBounds(dayPicker, monthPicker.getValue(), yearPicker.getValue());
        refreshDateNumberPickers(dayPicker, monthPicker, yearPicker);

        NumberPicker.OnValueChangeListener datePartChangeListener = (picker, oldVal, newVal) -> {
            updateDayPickerBounds(dayPicker, monthPicker.getValue(), yearPicker.getValue());
            refreshDateNumberPickers(dayPicker, monthPicker, yearPicker);
        };
        dayPicker.setOnValueChangedListener(datePartChangeListener);
        monthPicker.setOnValueChangedListener(datePartChangeListener);
        yearPicker.setOnValueChangedListener(datePartChangeListener);
        attachDatePickerScrollStyleRefresh(dayPicker, monthPicker, yearPicker);
        attachDatePickerScrollStyleRefresh(monthPicker, dayPicker, yearPicker);
        attachDatePickerScrollStyleRefresh(yearPicker, dayPicker, monthPicker);

        pickerRow.addView(dayPicker, createDatePartLayoutParams(54));
        pickerRow.addView(monthPicker, createDatePartLayoutParams(72));
        pickerRow.addView(yearPicker, createDatePartLayoutParams(74));
        pickerContainer.addView(pickerRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 0, 0, 0);

        TextView cancelButton = createDatePickerActionButton("Cancel", R.color.fresh_tag_blue);
        TextView doneButton = createDatePickerActionButton("Done", R.color.fresh_green_dark);
        actionRow.addView(cancelButton);
        actionRow.addView(doneButton);
        pickerContainer.addView(actionRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        PopupWindow popupWindow = new PopupWindow(
                pickerContainer,
                dpToPx(240),
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(dpToPx(8));

        cancelButton.setOnClickListener(v -> popupWindow.dismiss());
        doneButton.setOnClickListener(v -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, yearPicker.getValue());
            selected.set(Calendar.MONTH, monthPicker.getValue());
            selected.set(Calendar.DAY_OF_MONTH, dayPicker.getValue());
            input.setText(createDateFormat(INPUT_DATE_PATTERN).format(selected.getTime()));
            popupWindow.dismiss();
        });

        showDatePickerPopup(input, popupWindow, pickerContainer);
    }

    private GradientDrawable createDatePickerPopupBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.white));
        background.setStroke(dpToPx(1), getColor(R.color.fresh_green));
        background.setCornerRadius(dpToPx(8));
        return background;
    }

    private NumberPicker createCompactNumberPicker(int minValue, int maxValue, int value) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(minValue);
        picker.setMaxValue(maxValue);
        picker.setValue(value);
        picker.setWrapSelectorWheel(false);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        styleDateNumberPicker(picker);
        return picker;
    }

    private void styleDateNumberPicker(NumberPicker picker) {
        int textColor = getColor(R.color.fresh_text);
        int dividerColor = Color.rgb(142, 142, 147);
        float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                DATE_PICKER_TEXT_SIZE_SP,
                getResources().getDisplayMetrics()
        );
        for (int i = 0; i < picker.getChildCount(); i++) {
            View child = picker.getChildAt(i);
            if (child instanceof EditText) {
                EditText editText = (EditText) child;
                editText.setTextColor(textColor);
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, DATE_PICKER_TEXT_SIZE_SP);
                editText.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_regular));
            }
        }
        setNumberPickerPaintStyle(picker, textColor, textSizePx);
        setNumberPickerDividerColor(picker, dividerColor);
        picker.invalidate();
    }

    private void refreshDateNumberPickers(NumberPicker... pickers) {
        for (NumberPicker picker : pickers) {
            picker.post(() -> styleDateNumberPicker(picker));
        }
    }

    private void attachDatePickerScrollStyleRefresh(NumberPicker primaryPicker, NumberPicker... otherPickers) {
        primaryPicker.setOnScrollListener((view, scrollState) -> {
            if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                styleDateNumberPicker(primaryPicker);
                for (NumberPicker picker : otherPickers) {
                    styleDateNumberPicker(picker);
                }
            }
        });
    }

    private void setNumberPickerPaintStyle(NumberPicker picker, int color, float textSizePx) {
        try {
            Field selectorWheelPaintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            selectorWheelPaintField.setAccessible(true);
            Object paint = selectorWheelPaintField.get(picker);
            if (paint instanceof Paint) {
                Paint selectorWheelPaint = (Paint) paint;
                selectorWheelPaint.setColor(color);
                selectorWheelPaint.setTextSize(textSizePx);
                selectorWheelPaint.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_regular));
            }
        } catch (ReflectiveOperationException ignored) {
            // NumberPicker keeps selected text in a private paint on most Android versions.
        }
    }

    private void setNumberPickerDividerColor(NumberPicker picker, int color) {
        try {
            Field selectionDividerField = NumberPicker.class.getDeclaredField("mSelectionDivider");
            selectionDividerField.setAccessible(true);
            selectionDividerField.set(picker, new ColorDrawable(color));
        } catch (ReflectiveOperationException ignored) {
            // Some platform versions do not expose this field.
        }
    }

    private LinearLayout.LayoutParams createDatePartLayoutParams(int widthDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(widthDp),
                dpToPx(104)
        );
        params.leftMargin = dpToPx(2);
        params.rightMargin = dpToPx(2);
        return params;
    }

    private void updateDayPickerBounds(NumberPicker dayPicker, int month, int year) {
        int selectedDay = dayPicker.getValue();
        int maxDay = getMaxDayOfMonth(month, year);
        dayPicker.setMaxValue(maxDay);
        if (selectedDay > maxDay) {
            dayPicker.setValue(maxDay);
        }
    }

    private int getMaxDayOfMonth(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private TextView createDatePickerActionButton(String text, int colorResId) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(getColor(colorResId));
        button.setTextSize(14f);
        button.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_bold));
        button.setGravity(Gravity.CENTER);
        button.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        return button;
    }

    /**
     * Displays the date picker above or below the field depending on available screen space.
     */
    private void showDatePickerPopup(EditText input, PopupWindow popupWindow, View contentView) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(dpToPx(240), View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        contentView.measure(widthMeasureSpec, heightMeasureSpec);

        Rect visibleFrame = new Rect();
        input.getWindowVisibleDisplayFrame(visibleFrame);
        int[] inputLocation = new int[2];
        input.getLocationOnScreen(inputLocation);

        int popupHeight = contentView.getMeasuredHeight();
        int yBelow = inputLocation[1] + input.getHeight() + dpToPx(4);
        int spaceBelow = visibleFrame.bottom - yBelow;
        int spaceAbove = inputLocation[1] - visibleFrame.top - dpToPx(4);

        if (spaceBelow < popupHeight && spaceAbove > spaceBelow) {
            int x = Math.max(dpToPx(12), inputLocation[0]);
            int y = Math.max(visibleFrame.top + dpToPx(8), inputLocation[1] - popupHeight - dpToPx(4));
            popupWindow.showAtLocation(rootContainer, Gravity.NO_GRAVITY, x, y);
        } else {
            popupWindow.showAsDropDown(input, 0, dpToPx(4));
        }
    }

    private Calendar getCalendarForDatePicker(EditText input) {
        Calendar calendar = Calendar.getInstance();
        if (!hasText(input)) {
            return calendar;
        }
        try {
            calendar.setTime(parseDate(input.getText().toString().trim(), INPUT_DATE_PATTERN));
        } catch (IllegalArgumentException ignored) {
            // If the user is midway through typing, open the picker at today instead.
        }
        return calendar;
    }

    private String normalizeDateInput(String value) {
        String text = value.trim();
        if (text.matches("\\d{2}/\\d{2}/\\d{4}")) {
            Date parsed = parseDate(text, INPUT_DATE_PATTERN);
            return createDateFormat(ExpiryReminderService.DATE_PATTERN).format(parsed);
        }
        throw new IllegalArgumentException("Date must use DD/MM/YYYY.");
    }

    private String addDays(String date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parseDate(date, ExpiryReminderService.DATE_PATTERN));
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return createDateFormat(ExpiryReminderService.DATE_PATTERN).format(calendar.getTime());
    }

    private String getTodayString() {
        return createDateFormat(ExpiryReminderService.DATE_PATTERN).format(new Date());
    }

    private String getTodayInputString() {
        return createDateFormat(INPUT_DATE_PATTERN).format(new Date());
    }

    private String formatDateForInput(String date) {
        Date parsed = parseDate(date, ExpiryReminderService.DATE_PATTERN);
        return createDateFormat(INPUT_DATE_PATTERN).format(parsed);
    }

    private String formatDisplayDate(String date) {
        Date parsed = parseDate(date, ExpiryReminderService.DATE_PATTERN);
        return createDateFormat("MMM d, yyyy").format(parsed);
    }

    private Date parseDate(String date, String pattern) {
        try {
            return createDateFormat(pattern).parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date must use DD/MM/YYYY.", e);
        }
    }

    private SimpleDateFormat createDateFormat(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setLenient(false);
        return format;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.US);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Stores the information needed to restore a screen after navigating back.
     */
    private static class ScreenState {
        private final int layoutResId;
        private final int scrollY;
        private final Recipe currentRecipe;
        private final InventoryItem currentInventoryItem;
        private final Ingredient selectedCatalogIngredient;
        private final String selectedCategory;
        private final String recipeSearchQuery;
        private final boolean recipeSearchPending;
        private final boolean editingInventoryItem;

        private ScreenState(int layoutResId) {
            this(layoutResId, 0, null, null, null, null, "", false, false);
        }

        private ScreenState(int layoutResId,
                            int scrollY,
                            Recipe currentRecipe,
                            InventoryItem currentInventoryItem,
                            Ingredient selectedCatalogIngredient,
                            String selectedCategory,
                            String recipeSearchQuery,
                            boolean recipeSearchPending,
                            boolean editingInventoryItem) {
            this.layoutResId = layoutResId;
            this.scrollY = scrollY;
            this.currentRecipe = currentRecipe;
            this.currentInventoryItem = currentInventoryItem;
            this.selectedCatalogIngredient = selectedCatalogIngredient;
            this.selectedCategory = selectedCategory;
            this.recipeSearchQuery = recipeSearchQuery;
            this.recipeSearchPending = recipeSearchPending;
            this.editingInventoryItem = editingInventoryItem;
        }
    }
}
