package com.zosh.seed;

import com.zosh.modal.*;
import com.zosh.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalSeedService {

    private final SeedRunRepository seedRunRepository;
    private final BaseEntitySeeder baseEntitySeeder;
    private final HistoricalOrderGenerator historicalOrderGenerator;
    private final RefundScenarioGenerator refundScenarioGenerator;
    private final SeedValidationReporter seedValidationReporter;

    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final ShiftReportRepository shiftReportRepository;

    @Transactional
    public void seedHistoricalData(SeedScenarioConfig config) {
        if (!config.isEnabled()) {
            return;
        }

        if (config.isReset()) {
            resetDemoData();
        } else if (seedRunRepository.findByScenarioKey(config.getScenarioKey()).isPresent()) {
            log.info("Scenario {} already seeded. Use --seed.reset=true to recreate.", config.getScenarioKey());
            return;
        }

        Random random = new Random(config.getRandomSeed());
        SeedMetrics metrics = new SeedMetrics();

        HistoricalSeedContext context = baseEntitySeeder.seedBaseEntities(config, random, metrics);
        List<Order> orders = historicalOrderGenerator.generate(config, context, random, metrics);
        refundScenarioGenerator.generateRefunds(config, orders, random, metrics);

        SeedRun seedRun = new SeedRun();
        seedRun.setScenarioKey(config.getScenarioKey());
        seedRun.setVolume(config.getVolume().name());
        seedRun.setDays(config.getDays());
        seedRun.setRandomSeed(config.getRandomSeed());
        seedRunRepository.save(seedRun);

        seedValidationReporter.report(config, metrics);
    }

    private void resetDemoData() {
        log.warn("Reset mode enabled. Cleaning previously seeded demo data.");
        List<Store> demoStores = storeRepository.findAll().stream()
                .filter(store -> store.getBrand() != null && store.getBrand().startsWith("Demo Retail "))
                .toList();
        if (demoStores.isEmpty()) {
            seedRunRepository.deleteAll();
            return;
        }

        Set<Long> storeIds = demoStores.stream().map(Store::getId).collect(Collectors.toSet());
        List<Branch> branches = branchRepository.findAll().stream()
                .filter(branch -> branch.getStore() != null && storeIds.contains(branch.getStore().getId()))
                .toList();
        List<Long> branchIds = branches.stream().map(Branch::getId).toList();

        // Clear references before deleting branches/stores to avoid transient association flush errors.
        List<User> usersWithDemoAssignments = userRepository.findAll().stream()
                .filter(user -> (user.getBranch() != null && branchIds.contains(user.getBranch().getId()))
                        || (user.getStore() != null && storeIds.contains(user.getStore().getId())))
                .toList();
        if (!usersWithDemoAssignments.isEmpty()) {
            usersWithDemoAssignments.forEach(user -> {
                user.setBranch(null);
                user.setStore(null);
            });
            userRepository.saveAll(usersWithDemoAssignments);
        }

        for (Long branchId : branchIds) {
            List<Refund> refunds = refundRepository.findByBranchId(branchId);
            if (!refunds.isEmpty()) {
                refundRepository.deleteAll(refunds);
            }
        }

        for (Branch branch : branches) {
            List<ShiftReport> reports = shiftReportRepository.findByBranch(branch);
            if (!reports.isEmpty()) {
                shiftReportRepository.deleteAll(reports);
            }
            List<Order> orders = orderRepository.findByBranchId(branch.getId());
            if (!orders.isEmpty()) {
                orderRepository.deleteAll(orders);
            }
            List<Inventory> inventories = inventoryRepository.findByBranchIdOrderByIdAsc(branch.getId());
            if (!inventories.isEmpty()) {
                inventoryRepository.deleteAll(inventories);
            }
        }

        List<Product> productsToDelete = new ArrayList<>();
        List<Category> categoriesToDelete = new ArrayList<>();
        for (Long storeId : storeIds) {
            productsToDelete.addAll(productRepository.findByStoreId(storeId));
            categoriesToDelete.addAll(categoryRepository.findByStoreId(storeId));
        }

        if (!productsToDelete.isEmpty()) {
            productRepository.deleteAll(productsToDelete);
        }
        if (!categoriesToDelete.isEmpty()) {
            categoryRepository.deleteAll(categoriesToDelete);
        }

        if (!branches.isEmpty()) {
            branchRepository.deleteAll(branches);
        }

        List<Store> refreshedStores = storeRepository.findAllById(storeIds);
        for (Store store : refreshedStores) {
            store.setStoreAdmin(null);
        }
        if (!refreshedStores.isEmpty()) {
            storeRepository.saveAll(refreshedStores);
            storeRepository.deleteAll(refreshedStores);
        }

        List<User> users = userRepository.findAll();
        List<User> demoUsers = users.stream()
                .filter(user -> user.getEmail() != null && user.getEmail().endsWith("@seed.local"))
                .collect(Collectors.toList());
        for (User user : demoUsers) {
            user.setBranch(null);
            user.setStore(null);
        }
        if (!demoUsers.isEmpty()) {
            userRepository.saveAll(demoUsers);
            userRepository.deleteAll(demoUsers);
        }

        List<Customer> demoCustomers = customerRepository.findAll().stream()
                .filter(customer -> customer.getEmail() != null && customer.getEmail().endsWith("@seed.local"))
                .toList();
        if (!demoCustomers.isEmpty()) {
            customerRepository.deleteAll(demoCustomers);
        }

        seedRunRepository.deleteAll();
        log.info("Seed reset complete.");
    }
}
