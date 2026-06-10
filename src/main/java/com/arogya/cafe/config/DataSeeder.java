package com.arogya.cafe.config;

import com.arogya.cafe.catalog.entity.Category;
import com.arogya.cafe.catalog.repository.CategoryRepository;
import com.arogya.cafe.catalog.entity.Ingredient;
import com.arogya.cafe.catalog.repository.IngredientRepository;
import com.arogya.cafe.catalog.entity.ItemIngredient;
import com.arogya.cafe.catalog.repository.ItemIngredientRepository;
import com.arogya.cafe.catalog.entity.MenuItem;
import com.arogya.cafe.catalog.repository.MenuItemRepository;
import com.arogya.cafe.common.enums.StaffRole;
import com.arogya.cafe.inventory.entity.InventoryStock;
import com.arogya.cafe.inventory.repository.InventoryStockRepository;
import com.arogya.cafe.inventory.entity.Supplier;
import com.arogya.cafe.inventory.repository.SupplierRepository;
import com.arogya.cafe.ordering.entity.Customer;
import com.arogya.cafe.ordering.repository.CustomerRepository;
import com.arogya.cafe.security.entity.Staff;
import com.arogya.cafe.security.repository.StaffRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the worked-example dataset (Arogya Cafe quick-ref) on startup in dev.
 * Idempotent: does nothing if staff already exist. Enabled via app.seed.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "password123";

    private final CategoryRepository categories;
    private final IngredientRepository ingredients;
    private final MenuItemRepository menuItems;
    private final ItemIngredientRepository itemIngredients;
    private final InventoryStockRepository stocks;
    private final SupplierRepository suppliers;
    private final CustomerRepository customers;
    private final StaffRepository staff;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CategoryRepository categories, IngredientRepository ingredients, MenuItemRepository menuItems,
                      ItemIngredientRepository itemIngredients, InventoryStockRepository stocks,
                      SupplierRepository suppliers, CustomerRepository customers, StaffRepository staff,
                      PasswordEncoder passwordEncoder) {
        this.categories = categories;
        this.ingredients = ingredients;
        this.menuItems = menuItems;
        this.itemIngredients = itemIngredients;
        this.stocks = stocks;
        this.suppliers = suppliers;
        this.customers = customers;
        this.staff = staff;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (staff.count() > 0) {
            log.info("Seed skipped: staff already present");
            return;
        }

        // Staff — one login per role.
        staff.save(new Staff("Maya Manager", StaffRole.MANAGER, "manager", passwordEncoder.encode(DEFAULT_PASSWORD)));
        staff.save(new Staff("Cathy Cashier", StaffRole.CASHIER, "cashier", passwordEncoder.encode(DEFAULT_PASSWORD)));
        staff.save(new Staff("Chandra Chef", StaffRole.CHEF, "chef", passwordEncoder.encode(DEFAULT_PASSWORD)));
        staff.save(new Staff("Sam Server", StaffRole.SERVER, "server", passwordEncoder.encode(DEFAULT_PASSWORD)));

        // Categories
        Category shakes = categories.save(new Category("Milk & Shakes"));
        categories.save(new Category("Tea & Coffee"));

        // Ingredients + their stock balances
        Ingredient premix = ingredients.save(new Ingredient("Premix Sachet", "pcs"));
        Ingredient milk = ingredients.save(new Ingredient("Milk", "ml"));
        Ingredient kulhad = ingredients.save(new Ingredient("Kulhad Clay Cup", "pcs"));

        stocks.save(new InventoryStock(premix, bd(50), bd(10)));
        stocks.save(new InventoryStock(milk, bd(20000), bd(5000)));
        stocks.save(new InventoryStock(kulhad, bd(100), bd(20)));

        // Supplier
        suppliers.save(new Supplier("Sharma Dairy", "+91-98765-43210"));

        // Menu item + recipe (Badam Shake Regular = Rs 180 -> 1 premix + 180 ml milk)
        MenuItem badam = menuItems.save(new MenuItem("Badam Shake", "Regular", bd(180), shakes));
        itemIngredients.save(new ItemIngredient(badam, premix, bd(1), "pcs", "Regular"));
        itemIngredients.save(new ItemIngredient(badam, milk, bd(180), "ml", "Regular"));

        // Default walk-in customer
        customers.save(new Customer("Walk-in", "NA"));

        log.info("Seeded worked-example data. Logins (password '{}'): manager / cashier / chef / server",
                DEFAULT_PASSWORD);
    }

    private static BigDecimal bd(int v) {
        return BigDecimal.valueOf(v);
    }
}
