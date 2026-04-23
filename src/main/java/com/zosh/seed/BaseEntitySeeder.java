package com.zosh.seed;

import com.zosh.domain.PaymentType;
import com.zosh.domain.StoreStatus;
import com.zosh.domain.UserRole;
import com.zosh.modal.*;
import com.zosh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BaseEntitySeeder {

    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    public HistoricalSeedContext seedBaseEntities(SeedScenarioConfig config, Random random, SeedMetrics metrics) {
        List<Store> stores = seedStores(config);
        List<Branch> branches = seedBranches(config, stores);
        List<Category> categories = seedCategories(stores);
        List<Product> products = seedProducts(config, stores, categories, random);
        seedBranchManagers(branches);
        List<User> cashiers = seedCashiers(config, branches);
        List<Customer> customers = seedCustomers(config);
        int inventoriesCreated = seedInventory(branches, products, random);
        metrics.setInventoriesCreated(inventoriesCreated);

        Map<Long, List<Product>> productsByStoreId = products.stream()
                .collect(Collectors.groupingBy(product -> product.getStore().getId()));
        Map<Long, List<User>> cashiersByBranchId = cashiers.stream()
                .collect(Collectors.groupingBy(user -> user.getBranch().getId()));
        Map<Long, BranchBehaviorProfile> behaviorByBranchId = createBranchProfiles(branches, productsByStoreId, random);

        metrics.setStoresCreated(stores.size());
        metrics.setBranchesCreated(branches.size());
        metrics.setCategoriesCreated(categories.size());
        metrics.setProductsCreated(products.size());
        metrics.setCustomersCreated(customers.size());
        metrics.setCashiersCreated(cashiers.size());

        return HistoricalSeedContext.builder()
                .stores(stores)
                .branches(branches)
                .categories(categories)
                .products(products)
                .customers(customers)
                .cashiers(cashiers)
                .productsByStoreId(productsByStoreId)
                .cashiersByBranchId(cashiersByBranchId)
                .behaviorByBranchId(behaviorByBranchId)
                .build();
    }

    private List<Store> seedStores(SeedScenarioConfig config) {
        List<Store> stores = new ArrayList<>();
        for (int i = 0; i < config.getStoresCount(); i++) {
            String adminEmail = "demo.store.admin." + (i + 1) + "@seed.local";
            User existingAdmin = userRepository.findByEmail(adminEmail);
            User admin = existingAdmin;
            if (admin == null) {
                admin = new User();
                admin.setFullName("Demo Store Admin " + (i + 1));
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("12345678"));
                admin.setPhone(String.format("9000000%03d", i + 1));
                admin.setRole(UserRole.ROLE_STORE_ADMIN);
                admin.setVerified(true);
                admin = userRepository.save(admin);
            }

            Store store = Store.builder()
                    .brand("Demo Retail " + (i + 1))
                    .description("Seeded historical demo store " + (i + 1))
                    .storeType("Retail")
                    .storeAdmin(admin)
                    .status(StoreStatus.ACTIVE)
                    .build();
            Store savedStore = storeRepository.save(store);
            admin.setStore(savedStore);
            userRepository.save(admin);
            stores.add(savedStore);
        }
        return stores;
    }

    private void seedBranchManagers(List<Branch> branches) {
        for (int i = 0; i < branches.size(); i++) {
            Branch branch = branches.get(i);
            String email = "bm" + (i + 1) + "@seed.local";
            User manager = userRepository.findByEmail(email);
            if (manager == null) {
                manager = new User();
            }

            manager.setFullName("Branch Manager " + (i + 1));
            manager.setEmail(email);
            manager.setPassword(passwordEncoder.encode("12345678"));
            manager.setPhone(String.format("7100000%03d", i + 1));
            manager.setRole(UserRole.ROLE_BRANCH_MANAGER);
            manager.setBranch(branch);
            manager.setStore(branch.getStore());
            manager.setVerified(true);
            manager = userRepository.save(manager);

            branch.setManager(manager);
            branchRepository.save(branch);
        }
    }

    private List<Branch> seedBranches(SeedScenarioConfig config, List<Store> stores) {
        List<Branch> branches = new ArrayList<>();
        String[] branchSuffixes = {"Central", "North", "South", "East", "West", "Metro", "Mall", "Market"};

        for (int i = 0; i < config.getBranchesCount(); i++) {
            Store store = stores.get(i % stores.size());
            String suffix = branchSuffixes[i % branchSuffixes.length];
            Branch branch = Branch.builder()
                    .name(store.getBrand() + " - " + suffix)
                    .address((100 + i) + " Demo Avenue")
                    .email("branch." + (i + 1) + "@seed.local")
                    .phone(String.format("8000000%03d", i + 1))
                    .store(store)
                    .workingDays(new ArrayList<>(List.of(
                            "MONDAY",
                            "TUESDAY",
                            "WEDNESDAY",
                            "THURSDAY",
                            "FRIDAY",
                            "SATURDAY",
                            "SUNDAY"
                    )))
                    .openTime(LocalTime.of(8, 0))
                    .closeTime(LocalTime.of(22, 0))
                    .build();
            branches.add(branchRepository.save(branch));
        }
        return branches;
    }

    private List<Category> seedCategories(List<Store> stores) {
        List<String> names = List.of("Beverages", "Snacks", "Dairy", "Bakery", "Personal Care", "Household");
        List<Category> categories = new ArrayList<>();
        for (Store store : stores) {
            for (String name : names) {
                Category category = Category.builder()
                        .name(name)
                        .store(store)
                        .build();
                categories.add(categoryRepository.save(category));
            }
        }
        return categories;
    }

    private List<Product> seedProducts(SeedScenarioConfig config,
                                       List<Store> stores,
                                       List<Category> categories,
                                       Random random) {
        List<Product> products = new ArrayList<>();
        String[] brands = {"FreshCo", "DailyBest", "UrbanChoice", "PrimeLine", "ValueMart", "SunHarvest"};

        for (Store store : stores) {
            List<Category> storeCategories = categories.stream()
                    .filter(category -> Objects.equals(category.getStore().getId(), store.getId()))
                    .toList();

            for (int i = 0; i < config.getProductsPerStore(); i++) {
                Category category = storeCategories.get(i % storeCategories.size());
                String name = category.getName() + " Product " + (i + 1);
                double mrp = 80 + random.nextInt(700);
                double sellingPrice = Math.max(30.0, mrp * (0.75 + random.nextDouble() * 0.2));

                Product product = Product.builder()
                        .name(name)
                        .sku("SEED-" + store.getId() + "-" + String.format("%04d", i + 1))
                        .description("Seeded " + name + " for historical demo data")
                        .mrp(round2(mrp))
                        .sellingPrice(round2(sellingPrice))
                        .brand(brands[i % brands.length])
                        .category(category)
                        .store(store)
                        .image(buildStockImageUrl(category.getName(), i + 1))
                        .build();
                products.add(productRepository.save(product));
            }
        }
        return products;
    }

    private List<User> seedCashiers(SeedScenarioConfig config, List<Branch> branches) {
        List<User> cashiers = new ArrayList<>();
        for (Branch branch : branches) {
            for (int i = 0; i < config.getCashiersPerBranch(); i++) {
                String email = "cashier." + branch.getId() + "." + (i + 1) + "@seed.local";
                User cashier = userRepository.findByEmail(email);
                if (cashier == null) {
                    cashier = new User();
                    cashier.setFullName("Cashier " + branch.getName() + " " + (i + 1));
                    cashier.setEmail(email);
                    cashier.setPassword(passwordEncoder.encode("12345678"));
                    cashier.setPhone(String.format("700%07d", branch.getId() * 10 + i));
                    cashier.setRole(UserRole.ROLE_BRANCH_CASHIER);
                    cashier.setBranch(branch);
                    cashier.setStore(branch.getStore());
                    cashier.setVerified(true);
                    cashier = userRepository.save(cashier);
                }
                cashiers.add(cashier);
            }
        }
        return cashiers;
    }

    private List<Customer> seedCustomers(SeedScenarioConfig config) {
        String[] firstNames = {
                "John", "Joseph", "Michael", "David", "James", "Robert", "Daniel", "William",
                "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Susan", "Jessica", "Sarah"
        };
        String[] lastNames = {
                "Smith", "Johnson", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor",
                "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Clark"
        };

        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < config.getCustomersCount(); i++) {
            Customer customer = new Customer();
            String firstName = firstNames[i % firstNames.length];
            String lastName = lastNames[(i / firstNames.length) % lastNames.length];
            customer.setFullName(firstName + " " + lastName);
            customer.setEmail("customer." + (i + 1) + "@seed.local");
            customer.setPhone(String.format("600%07d", i + 1));
            customer.setLoyaltyPoints(0);
            customers.add(customerRepository.save(customer));
        }
        return customers;
    }

    private String buildStockImageUrl(String categoryName, int sequence) {
        String normalizedCategory = categoryName == null
                ? "product"
                : categoryName.toLowerCase(Locale.ROOT).replace(" ", "-");
        return "https://picsum.photos/seed/stock-" + normalizedCategory + "-" + sequence + "/640/640";
    }

    private int seedInventory(List<Branch> branches, List<Product> products, Random random) {
        int count = 0;
        for (Branch branch : branches) {
            Long storeId = branch.getStore().getId();
            List<Product> storeProducts = products.stream()
                    .filter(product -> Objects.equals(product.getStore().getId(), storeId))
                    .toList();
            for (Product product : storeProducts) {
                Inventory inventory = Inventory.builder()
                        .branch(branch)
                        .product(product)
                        .quantity(200 + random.nextInt(500))
                        .build();
                inventoryRepository.save(inventory);
                count++;
            }
        }
        return count;
    }

    private Map<Long, BranchBehaviorProfile> createBranchProfiles(List<Branch> branches,
                                                                  Map<Long, List<Product>> productsByStoreId,
                                                                  Random random) {
        Map<Long, BranchBehaviorProfile> map = new HashMap<>();
        for (int i = 0; i < branches.size(); i++) {
            Branch branch = branches.get(i);
            List<Product> products = productsByStoreId.getOrDefault(branch.getStore().getId(), List.of());
            int maxProductIndex = Math.max(1, products.size() - 1);
            List<Integer> topProductIndexes = List.of(
                    random.nextInt(maxProductIndex),
                    random.nextInt(maxProductIndex),
                    random.nextInt(maxProductIndex)
            );

            double baseline = 10 + (i * 2.5);
            double weekend = (i % 2 == 0) ? 1.35 : 1.15;
            double weekday = (i % 3 == 0) ? 1.05 : 0.95;
            double trend = 1.0 + (i * 0.03);

            Map<PaymentType, Double> paymentMix = new EnumMap<>(PaymentType.class);
            paymentMix.put(PaymentType.CASH, Math.max(0.15, 0.45 - (i * 0.04)));
            paymentMix.put(PaymentType.UPI, Math.min(0.65, 0.35 + (i * 0.05)));
            paymentMix.put(PaymentType.CARD, 1.0 - paymentMix.get(PaymentType.CASH) - paymentMix.get(PaymentType.UPI));

            map.put(branch.getId(), new BranchBehaviorProfile(
                    branch.getName(),
                    baseline,
                    weekend,
                    weekday,
                    trend,
                    paymentMix,
                    topProductIndexes
            ));
        }
        return map;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
