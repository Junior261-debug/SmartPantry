# Smart Pantry Manager

Owner: Junior Gola

A Java Android app that helps reduce food waste. You record what is in your pantry (with optional
expiry dates) and the app suggests recipes you can cook **using only what you already have** - no
shopping trip needed.


## Features

**Core (assignment requirements)**
- Pantry CRUD: add, edit, delete ingredients (name, quantity, unit, optional expiry date) with input validation
- Pantry list screen (RecyclerView + custom adapter bound to the database)
- 21 seeded recipes (name, ingredients, steps) pre-loaded on first run
- **Suggested Recipes** screen using the strict-matching rule: a recipe is listed only if *every*
  ingredient is in the pantry in at least the required quantity (4 of 5 ingredients = not suggested)
- Recipe detail screen, Settings screen, friendly message when no recipe matches
- Bottom navigation bar + Intents with extras between screens

**Extras**
- **Almost There** list: recipes missing exactly one ingredient, shown separately from the strict list
- **Expiry-first ranking**: recipes using the most soon-to-expire (3 days or less) ingredients come first
- **Cook this**: deducts the recipe's quantities from the pantry (soonest-expiring stock first) and removes items that reach zero, in a single database transaction
- **Custom recipes**: users add their own recipes; they take part in matching exactly like the seeded ones
- **Search and filters**: search box on the pantry list; filter chips on suggestions (quick meals, uses expiring food, my recipes)
- **Daily expiry reminder** using WorkManager, tied to the Settings toggle (09:00, plus a "send test" button)
- **Synonyms**: aubergine = eggplant, coriander = cilantro, courgette = zucchini, prawn = shrimp, and more (`Synonyms.java`)
- **Real database migrations** (`Schema.java` / `DbHelper.onUpgrade`) - upgrading keeps all user data
- Material 3 components, bottom navigation, empty-state illustrations

## Matching robustness
- Names are normalised: `Tomatoes`, ` tomato `, `TOMATO` are equal; plurals (`berries`, `peaches`, `cookies`) and synonyms are handled
- Units are converted within a family: **1 kg satisfies 500 g**, litres/ml/cups/tbsp/tsp, oz/lb. Different families never match (500 ml is not 500 g)
- Duplicate pantry rows for the same ingredient are added together

## Database choice: SQLite (SQLiteOpenHelper)
Chosen because the app is entirely personal and local: a pantry must work offline in a kitchen, needs no
account, no server and no network permission, and the data is small and relational (pantry, recipes,
recipe ingredients). SQLite is built into Android, persists across restarts, and supports transactions,
which "Cook this" relies on so that either all deductions are applied or none. Firebase and PostgreSQL
would add a cloud dependency and a backend that this scope does not need.

Schema (version 2): `pantry`, `recipes` (with `minutes`, `is_custom`), `recipe_ingredients`.
Migrations live in `Schema.upgrade(from, to)`; to change the schema later, bump `Schema.VERSION` and add an `if (from < N && to >= N)` block.

## Setup and run
1. Open the project folder in **Android Studio** (Hedgehog or newer). Let Gradle sync.
2. Select an emulator (API 24+) or a device and press Run.
3. On Android 13+, allow notifications when asked (needed for the daily reminder).

## Run the unit tests
```
./gradlew test
```
or right-click `app/src/test/java` in Android Studio and choose *Run Tests*. 51 JUnit tests cover the
matcher (4 of 5 ingredients fails, plurals, 1 kg satisfies 500 g, synonyms, almost-there, ranking),
the cook planner, search/filters, validation and migration SQL.

## Project layout
```
app/src/main/java/com/example/smartpantry
  RecipeMatcher, MatchResult, Units, Synonyms, DateUtil   matching logic (pure Java, unit-tested)
  CookPlanner, Filters, Validation                        cook rules, search/filters, form validation
  Schema, DbHelper                                        SQLite schema, migrations, CRUD
  PantryActivity, SuggestedRecipesActivity, RecipesActivity, SettingsActivity    bottom-nav screens
  AddEditIngredientActivity, RecipeDetailActivity, AddRecipeActivity            screens opened by Intent
  PantryAdapter, SuggestionAdapter, RecipeListAdapter     RecyclerView adapters
  ReminderScheduler, ExpiryReminderWorker, SmartPantryApp WorkManager reminder
```

## Out of scope (per the brief)
No maps, GPS/location, payments or Play Store publishing.
