package com.tms.config;

import com.tms.entity.*;
import com.tms.enums.*;
import com.tms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Seeds demo data programmatically for the dev profile (H2 in-memory DB).
 * When Flyway is enabled (prod), the SQL migration V2__seed_demo_data.sql
 * handles seeding instead — this bean is not loaded in that case.
 *
 * This is sample/demo data only — not live operational data.
 */
@Component
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final LorryReceiptRepository lrRepository;
    private final BookingRepository bookingRepository;
    private final GeofenceRepository geofenceRepository;
    private final ExpenseRepository expenseRepository;
    private final AuditLogRepository auditLogRepository;
    private final FreightRateCardRepository freightRateCardRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        log.info("Seeding initial data...");

        seedUsers();
        List<Vehicle> vehicles = seedVehicles();
        List<Driver> drivers = seedDrivers();
        List<Route> routes = seedRoutes();
        List<LorryReceipt> lorryReceipts = seedLorryReceipts();
        List<Trip> trips = seedTrips(vehicles, drivers, lorryReceipts, routes);
        int live = 0;
        for (Trip t : trips) {
            if (t.getStatus() == TripStatus.IN_PROGRESS && live == 0) {
                t.setTrackingToken("LANE-DEMO");
                live++;
            } else if (t.getStatus() == TripStatus.IN_PROGRESS && live == 1) {
                t.setTrackingToken("LIVE-DEMO");
                live++;
            } else {
                t.setTrackingToken(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            }
        }
        tripRepository.saveAll(trips);
        seedBookings(trips);
        seedExpenses(trips, vehicles);
        seedGeofences();
        seedFreightRates();
        seedAuditLogs(vehicles, drivers, trips);

        log.info("Data seeding completed — {} users, {} vehicles, {} drivers, {} routes, {} LRs, {} trips, {} bookings, {} expenses, {} geofences, {} audit logs.",
                userRepository.count(), vehicleRepository.count(), driverRepository.count(),
                routeRepository.count(), lrRepository.count(), tripRepository.count(), bookingRepository.count(),
                expenseRepository.count(), geofenceRepository.count(), auditLogRepository.count());
    }

    // ───────────────────────────── Users ─────────────────────────────
    private void seedUsers() {
        userRepository.saveAll(List.of(
                User.builder().username("admin").email("admin@tms.com")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("Ravi Mehta").role(UserRole.ADMIN).build(),
                User.builder().username("admin2").email("admin2@tms.com")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("Priya Sharma").role(UserRole.ADMIN).build(),

                User.builder().username("dispatcher").email("dispatcher@tms.com")
                        .password(passwordEncoder.encode("dispatch123"))
                        .fullName("Amit Singh").role(UserRole.DISPATCHER).build(),
                User.builder().username("dispatcher2").email("dispatcher2@tms.com")
                        .password(passwordEncoder.encode("dispatch123"))
                        .fullName("Anita Verma").role(UserRole.DISPATCHER).build(),
                User.builder().username("dispatcher3").email("dispatcher3@tms.com")
                        .password(passwordEncoder.encode("dispatch123"))
                        .fullName("Suresh Iyer").role(UserRole.DISPATCHER).build(),

                User.builder().username("driver1").email("driver1@tms.com")
                        .password(passwordEncoder.encode("driver123"))
                        .fullName("Ramesh Yadav").role(UserRole.DRIVER).build(),
                User.builder().username("driver2").email("driver2@tms.com")
                        .password(passwordEncoder.encode("driver123"))
                        .fullName("Kavita Reddy").role(UserRole.DRIVER).build(),
                User.builder().username("driver3").email("driver3@tms.com")
                        .password(passwordEncoder.encode("driver123"))
                        .fullName("Raj Patel").role(UserRole.DRIVER).build(),
                User.builder().username("driver4").email("driver4@tms.com")
                        .password(passwordEncoder.encode("driver123"))
                        .fullName("Vikram Singh").role(UserRole.DRIVER).build(),

                User.builder().username("client1").email("client1@tms.com")
                        .password(passwordEncoder.encode("client123"))
                        .fullName("Neha Gupta").role(UserRole.CLIENT).build(),
                User.builder().username("client2").email("client2@tms.com")
                        .password(passwordEncoder.encode("client123"))
                        .fullName("Ahmed Khan").role(UserRole.CLIENT).build(),
                User.builder().username("client3").email("client3@tms.com")
                        .password(passwordEncoder.encode("client123"))
                        .fullName("Anjali Nair").role(UserRole.CLIENT).build(),
                User.builder().username("client4").email("client4@tms.com")
                        .password(passwordEncoder.encode("client123"))
                        .fullName("Rohan Joshi").role(UserRole.CLIENT).build(),
                User.builder().username("client5").email("client5@tms.com")
                        .password(passwordEncoder.encode("client123"))
                        .fullName("Pooja Desai").role(UserRole.CLIENT).build()
        ));
    }

    // ───────────────────────────── Vehicles ─────────────────────────────
    private List<Vehicle> seedVehicles() {
        return vehicleRepository.saveAll(List.of(
                Vehicle.builder().vehicleNumber("DL-01-AB-1234").type(VehicleType.TRUCK).capacity(20)
                        .status(VehicleStatus.AVAILABLE).currentLocation("Main Depot, Delhi")
                        .make("Tata").model("Prima").year(2023)
                        .latitude(28.6139).longitude(77.2090).odometerKm(84200)
                        .lastServiceDate(LocalDate.now().minusDays(40)).nextServiceDueKm(95000).build(),
                Vehicle.builder().vehicleNumber("HR-26-CD-5678").type(VehicleType.TRUCK).capacity(30)
                        .status(VehicleStatus.MAINTENANCE).currentLocation("Service Center, Gurugram")
                        .make("Ashok Leyland").model("1920").year(2022)
                        .latitude(28.4595).longitude(77.0266).odometerKm(186400)
                        .lastServiceDate(LocalDate.now().minusDays(210)).nextServiceDueKm(180000).build(),
                Vehicle.builder().vehicleNumber("MH-12-EF-9012").type(VehicleType.TRUCK).capacity(25)
                        .status(VehicleStatus.AVAILABLE).currentLocation("Warehouse, Mumbai")
                        .make("BharatBenz").model("3528").year(2024)
                        .latitude(19.2183).longitude(72.9781).odometerKm(41200)
                        .lastServiceDate(LocalDate.now().minusDays(20)).nextServiceDueKm(60000).build(),
                Vehicle.builder().vehicleNumber("DL-03-GH-3456").type(VehicleType.TRUCK).capacity(18)
                        .status(VehicleStatus.AVAILABLE).currentLocation("Main Depot, Delhi")
                        .make("Eicher").model("Pro 6048").year(2023)
                        .latitude(28.7041).longitude(77.1025).odometerKm(67800)
                        .lastServiceDate(LocalDate.now().minusDays(55)).nextServiceDueKm(80000).build(),
                Vehicle.builder().vehicleNumber("KA-03-JK-7890").type(VehicleType.TRUCK).capacity(35)
                        .status(VehicleStatus.AVAILABLE).currentLocation("Logistics Park, Bengaluru")
                        .make("Mahindra").model("Blazo X 42").year(2024)
                        .latitude(12.9716).longitude(77.5946).odometerKm(22100)
                        .lastServiceDate(LocalDate.now().minusDays(15)).nextServiceDueKm(40000).build(),

                Vehicle.builder().vehicleNumber("DL-04-LM-1122").type(VehicleType.VAN).capacity(5)
                        .status(VehicleStatus.AVAILABLE).currentLocation("Main Depot, Delhi")
                        .make("Tata").model("Winger").year(2024)
                        .latitude(22.5726).longitude(88.3639).odometerKm(33400)
                        .lastServiceDate(LocalDate.now().minusDays(30)).nextServiceDueKm(45000).build(),
                Vehicle.builder().vehicleNumber("MH-14-NP-3344").type(VehicleType.VAN).capacity(4)
                        .status(VehicleStatus.AVAILABLE).currentLocation("Branch Office, Pune")
                        .make("Force").model("Traveller").year(2023)
                        .latitude(18.5204).longitude(73.8567).odometerKm(28900)
                        .lastServiceDate(LocalDate.now().minusDays(25)).nextServiceDueKm(40000).build(),
                Vehicle.builder().vehicleNumber("HR-26-QR-5566").type(VehicleType.VAN).capacity(6)
                        .status(VehicleStatus.MAINTENANCE).currentLocation("Service Center, Gurugram")
                        .make("Mahindra").model("Supro").year(2022)
                        .latitude(28.4089).longitude(77.3178).odometerKm(97400)
                        .lastServiceDate(LocalDate.now().minusDays(190)).nextServiceDueKm(100000).build(),

                Vehicle.builder().vehicleNumber("DL-1P-ST-7788").type(VehicleType.BUS).capacity(50)
                        .status(VehicleStatus.AVAILABLE).currentLocation("ISBT Kashmere Gate, Delhi")
                        .make("Volvo").model("9400").year(2024)
                        .latitude(28.6676).longitude(77.2273).odometerKm(51200)
                        .lastServiceDate(LocalDate.now().minusDays(12)).nextServiceDueKm(70000).build(),
                Vehicle.builder().vehicleNumber("TS-09-UV-9900").type(VehicleType.BUS).capacity(45)
                        .status(VehicleStatus.AVAILABLE).currentLocation("MGBS Bus Station, Hyderabad")
                        .make("Ashok Leyland").model("Lynx").year(2023)
                        .latitude(13.0827).longitude(80.2707).odometerKm(44800)
                        .lastServiceDate(LocalDate.now().minusDays(18)).nextServiceDueKm(60000).build(),

                Vehicle.builder().vehicleNumber("DL-05-WX-1212").type(VehicleType.MINI_BUS).capacity(20)
                        .status(VehicleStatus.AVAILABLE).currentLocation("Main Depot, Delhi")
                        .make("Force").model("Traveller 26").year(2024)
                        .latitude(28.5355).longitude(77.3910).odometerKm(19800)
                        .lastServiceDate(LocalDate.now().minusDays(8)).nextServiceDueKm(35000).build(),
                Vehicle.builder().vehicleNumber("TN-09-YZ-3434").type(VehicleType.MINI_BUS).capacity(15)
                        .status(VehicleStatus.AVAILABLE).currentLocation("Branch Office, Chennai")
                        .make("Tata").model("Starbus Mini").year(2023)
                        .latitude(13.0475).longitude(80.2480).odometerKm(36100)
                        .lastServiceDate(LocalDate.now().minusDays(22)).nextServiceDueKm(50000).build()
        ));
    }

    // ───────────────────────────── Drivers ─────────────────────────────
    private List<Driver> seedDrivers() {
        return driverRepository.saveAll(List.of(
                Driver.builder().name("Ramesh Yadav").licenseNumber("DL-0420240001234")
                        .phone("+91-98765-43210").email("driver1@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Kavita Reddy").licenseNumber("MH-1220240002345")
                        .phone("+91-98765-43211").email("driver2@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Raj Patel").licenseNumber("GJ-0120240003456")
                        .phone("+91-98765-43212").email("driver3@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Vikram Singh").licenseNumber("RJ-1420240004567")
                        .phone("+91-98765-43213").email("driver4@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Arjun Mehta").licenseNumber("KA-0320240005678")
                        .phone("+91-98765-43214").email("arjun@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Sunita Rao").licenseNumber("TN-0920240006789")
                        .phone("+91-98765-43215").email("sunita@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Manoj Tiwari").licenseNumber("UP-3220240007890")
                        .phone("+91-98765-43216").email("manoj@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Deepak Sharma").licenseNumber("HR-2620240008901")
                        .phone("+91-98765-43217").email("deepak@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Lakshmi Pillai").licenseNumber("KL-0720240009012")
                        .phone("+91-98765-43218").email("lakshmi@tms.com").status(DriverStatus.ACTIVE).build(),
                Driver.builder().name("Farhan Sheikh").licenseNumber("TS-0920240010123")
                        .phone("+91-98765-43219").email("farhan@tms.com").status(DriverStatus.ACTIVE).build()
        ));
    }

    // ───────────────────────────── Routes ─────────────────────────────
    private List<Route> seedRoutes() {
        return routeRepository.saveAll(List.of(
                Route.builder().origin("Delhi").destination("Jaipur")
                        .distance(268.0).estimatedTimeMinutes(240)
                        .description("Delhi to Jaipur via NH48").build(),
                Route.builder().origin("Mumbai").destination("Pune")
                        .distance(148.0).estimatedTimeMinutes(180)
                        .description("Mumbai to Pune via NH48").build(),
                Route.builder().origin("Bengaluru").destination("Chennai")
                        .distance(346.0).estimatedTimeMinutes(360)
                        .description("Bengaluru to Chennai via NH48").build(),
                Route.builder().origin("Hyderabad").destination("Vijayawada")
                        .distance(275.0).estimatedTimeMinutes(280)
                        .description("Hyderabad to Vijayawada via NH65").build(),
                Route.builder().origin("Ahmedabad").destination("Surat")
                        .distance(263.0).estimatedTimeMinutes(240)
                        .description("Ahmedabad to Surat via NH48").build(),
                Route.builder().origin("Jaipur").destination("Udaipur")
                        .distance(395.0).estimatedTimeMinutes(360)
                        .description("Jaipur to Udaipur via NH48 / NH27").build(),
                Route.builder().origin("Chennai").destination("Madurai")
                        .distance(462.0).estimatedTimeMinutes(420)
                        .description("Chennai to Madurai via NH38").build(),
                Route.builder().origin("Kolkata").destination("Bhubaneswar")
                        .distance(442.0).estimatedTimeMinutes(390)
                        .description("Kolkata to Bhubaneswar via NH16").build(),
                Route.builder().origin("Chennai").destination("Puducherry")
                        .distance(160.0).estimatedTimeMinutes(180)
                        .description("Chennai to Puducherry via ECR / NH32").build(),
                Route.builder().origin("Delhi").destination("Agra")
                        .distance(233.0).estimatedTimeMinutes(180)
                        .description("Delhi to Agra via Yamuna Expressway").build(),
                Route.builder().origin("Delhi").destination("Mumbai")
                        .distance(1415.0).estimatedTimeMinutes(1080)
                        .description("Delhi to Mumbai via NH48 (long-haul)").build(),
                Route.builder().origin("Chennai").destination("Bengaluru")
                        .distance(346.0).estimatedTimeMinutes(360)
                        .description("Chennai to Bengaluru via NH48 (return leg)").build()
        ));
    }

    // ───────────────────────────── Lorry Receipts ─────────────────────────────
    private List<LorryReceipt> seedLorryReceipts() {
        return lrRepository.saveAll(List.of(
                LorryReceipt.builder().lrNumber("LR-2026-0001").consignor("Tata Steel").consignee("JSW Steel")
                        .origin("Delhi").destination("Jaipur").material("Steel Pipes")
                        .weight(5000.0).quantity(100).status(LrStatus.CREATED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0002").consignor("Reliance Retail").consignee("DMart")
                        .origin("Mumbai").destination("Pune").material("Electronics")
                        .weight(2000.0).quantity(50).status(LrStatus.CREATED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0003").consignor("Sun Pharma").consignee("Cipla")
                        .origin("Hyderabad").destination("Vijayawada").material("Pharmaceutical Raw Materials")
                        .weight(3500.0).quantity(75).status(LrStatus.CREATED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0004").consignor("Amul").consignee("Mother Dairy")
                        .origin("Jaipur").destination("Udaipur").material("Frozen Foods")
                        .weight(8000.0).quantity(200).status(LrStatus.CREATED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0005").consignor("Arvind Mills").consignee("Raymond")
                        .origin("Bengaluru").destination("Chennai").material("Cotton Fabric Rolls")
                        .weight(4500.0).quantity(120).status(LrStatus.CREATED).build(),

                LorryReceipt.builder().lrNumber("LR-2026-0006").consignor("Bharat Forge").consignee("Maruti Suzuki")
                        .origin("Kolkata").destination("Bhubaneswar").material("Auto Spare Parts")
                        .weight(1800.0).quantity(300).status(LrStatus.IN_TRANSIT).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0007").consignor("UltraTech Cement").consignee("L&T Construction")
                        .origin("Delhi").destination("Jaipur").material("Cement Bags")
                        .weight(12000.0).quantity(240).status(LrStatus.IN_TRANSIT).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0008").consignor("Infosys").consignee("TCS")
                        .origin("Ahmedabad").destination("Surat").material("Server Equipment")
                        .weight(900.0).quantity(15).status(LrStatus.IN_TRANSIT).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0009").consignor("BigBasket").consignee("Reliance Fresh")
                        .origin("Chennai").destination("Madurai").material("Organic Vegetables")
                        .weight(6000.0).quantity(150).status(LrStatus.IN_TRANSIT).build(),

                LorryReceipt.builder().lrNumber("LR-2026-0010").consignor("Godrej Interio").consignee("Pepperfry")
                        .origin("Mumbai").destination("Pune").material("Furniture")
                        .weight(7500.0).quantity(45).status(LrStatus.DELIVERED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0011").consignor("Tata Steel").consignee("L&T Construction")
                        .origin("Bengaluru").destination("Chennai").material("Structural Steel")
                        .weight(15000.0).quantity(60).status(LrStatus.DELIVERED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0012").consignor("Reliance Retail").consignee("Croma")
                        .origin("Chennai").destination("Puducherry").material("Laptops & Monitors")
                        .weight(1200.0).quantity(80).status(LrStatus.DELIVERED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0013").consignor("Asian Paints").consignee("Berger Paints")
                        .origin("Jaipur").destination("Udaipur").material("Paint & Chemicals")
                        .weight(4000.0).quantity(180).status(LrStatus.DELIVERED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0014").consignor("Usha International").consignee("Singer India")
                        .origin("Delhi").destination("Agra").material("Sewing Machines")
                        .weight(2200.0).quantity(30).status(LrStatus.DELIVERED).build(),
                LorryReceipt.builder().lrNumber("LR-2026-0015").consignor("Bharat Forge").consignee("Tata Motors")
                        .origin("Hyderabad").destination("Vijayawada").material("Engine Components")
                        .weight(3000.0).quantity(500).status(LrStatus.DELIVERED).build()
        ));
    }

    // ───────────────────────────── Trips ─────────────────────────────
    private List<Trip> seedTrips(List<Vehicle> vehicles, List<Driver> drivers, List<LorryReceipt> lrs, List<Route> routes) {
        LocalDateTime now = LocalDateTime.now();

        Trip trip1 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(0))
                .driver(drivers.get(0))
                .route(routes.get(0))
                .status(TripStatus.PLANNED)
                .startTime(now.minusHours(6))
                .endTime(now.plusHours(2))
                .notes("Delhi → Jaipur freight — steel pipes & cement")
                .lorryReceipts(List.of(lrs.get(0), lrs.get(6)))
                .build());
        vehicles.get(0).setStatus(VehicleStatus.BUSY);
        vehicleRepository.save(vehicles.get(0));

        Trip trip2 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(3))
                .driver(drivers.get(2))
                .route(routes.get(1))
                .status(TripStatus.PLANNED)
                .startTime(now.plusDays(2).withHour(6).withMinute(0))
                .endTime(now.plusDays(2).withHour(12).withMinute(0))
                .notes("Mumbai → Pune electronics delivery")
                .lorryReceipts(List.of(lrs.get(1)))
                .build());
        vehicles.get(3).setStatus(VehicleStatus.BUSY);
        vehicleRepository.save(vehicles.get(3));

        Trip trip3 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(4))
                .driver(drivers.get(6))
                .route(routes.get(5))
                .status(TripStatus.PLANNED)
                .startTime(now.plusDays(3).withHour(5).withMinute(30))
                .endTime(now.plusDays(3).withHour(11).withMinute(30))
                .notes("Jaipur → Udaipur frozen food shipment")
                .lorryReceipts(List.of(lrs.get(3)))
                .build());
        vehicles.get(4).setStatus(VehicleStatus.BUSY);
        vehicleRepository.save(vehicles.get(4));

        Trip trip4 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(8))
                .driver(drivers.get(4))
                .route(routes.get(0))
                .status(TripStatus.PLANNED)
                .startTime(now.plusDays(1).withHour(7).withMinute(0))
                .endTime(now.plusDays(1).withHour(11).withMinute(0))
                .notes("Delhi → Jaipur passenger bus service")
                .lorryReceipts(List.of())
                .build());
        vehicles.get(8).setStatus(VehicleStatus.BUSY);
        vehicleRepository.save(vehicles.get(8));

        Trip trip5 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(10))
                .driver(drivers.get(5))
                .route(routes.get(2))
                .status(TripStatus.PLANNED)
                .startTime(now.plusDays(4).withHour(9).withMinute(0))
                .endTime(now.plusDays(4).withHour(14).withMinute(0))
                .notes("Bengaluru → Chennai shuttle service")
                .lorryReceipts(List.of(lrs.get(4)))
                .build());

        Trip trip6 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(5))
                .driver(drivers.get(1))
                .route(routes.get(7))
                .status(TripStatus.IN_PROGRESS)
                .startTime(now.minusHours(10))
                .endTime(now.plusHours(1))
                .notes("Kolkata → Bhubaneswar auto parts — in transit")
                .lorryReceipts(List.of(lrs.get(5)))
                .build());
        vehicles.get(5).setStatus(VehicleStatus.BUSY);
        vehicleRepository.save(vehicles.get(5));

        Trip trip7 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(2))
                .driver(drivers.get(3))
                .route(routes.get(4))
                .status(TripStatus.IN_PROGRESS)
                .startTime(now.minusHours(2))
                .endTime(now.plusHours(4))
                .notes("Ahmedabad → Surat server equipment")
                .lorryReceipts(List.of(lrs.get(7)))
                .build());
        vehicles.get(2).setStatus(VehicleStatus.BUSY);
        vehicleRepository.save(vehicles.get(2));

        Trip trip8 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(9))
                .driver(drivers.get(9))
                .route(routes.get(6))
                .status(TripStatus.IN_PROGRESS)
                .startTime(now.minusHours(1))
                .endTime(now.plusHours(5))
                .notes("Chennai → Madurai express — organic goods")
                .lorryReceipts(List.of(lrs.get(8)))
                .build());
        vehicles.get(9).setStatus(VehicleStatus.BUSY);
        vehicleRepository.save(vehicles.get(9));

        Trip trip9 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(6))
                .driver(drivers.get(1))
                .route(routes.get(1))
                .status(TripStatus.COMPLETED)
                .startTime(now.minusDays(5).withHour(8).withMinute(0))
                .endTime(now.minusDays(5).withHour(14).withMinute(0))
                .notes("Mumbai → Pune furniture delivery — completed on time")
                .lorryReceipts(List.of(lrs.get(9)))
                .build());

        Trip trip10 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(0))
                .driver(drivers.get(0))
                .route(routes.get(2))
                .status(TripStatus.COMPLETED)
                .startTime(now.minusDays(7).withHour(6).withMinute(0))
                .endTime(now.minusDays(7).withHour(16).withMinute(0))
                .notes("Bengaluru → Chennai structural steel — heavy load")
                .lorryReceipts(List.of(lrs.get(10)))
                .build());

        Trip trip11 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(6))
                .driver(drivers.get(4))
                .route(routes.get(8))
                .status(TripStatus.COMPLETED)
                .startTime(now.minusDays(3).withHour(9).withMinute(0))
                .endTime(now.minusDays(3).withHour(13).withMinute(0))
                .notes("Chennai → Puducherry laptops delivery")
                .lorryReceipts(List.of(lrs.get(11)))
                .build());

        Trip trip12 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(4))
                .driver(drivers.get(6))
                .route(routes.get(5))
                .status(TripStatus.COMPLETED)
                .startTime(now.minusDays(10).withHour(5).withMinute(0))
                .endTime(now.minusDays(10).withHour(11).withMinute(0))
                .notes("Jaipur → Udaipur paint & chemicals")
                .lorryReceipts(List.of(lrs.get(12)))
                .build());

        Trip trip13 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(11))
                .driver(drivers.get(9))
                .route(routes.get(9))
                .status(TripStatus.COMPLETED)
                .startTime(now.minusDays(6).withHour(10).withMinute(0))
                .endTime(now.minusDays(6).withHour(14).withMinute(0))
                .notes("Delhi → Agra sewing machines")
                .lorryReceipts(List.of(lrs.get(13)))
                .build());

        Trip trip14 = tripRepository.save(Trip.builder()
                .vehicle(vehicles.get(2))
                .driver(drivers.get(3))
                .route(routes.get(3))
                .status(TripStatus.COMPLETED)
                .startTime(now.minusDays(8).withHour(4).withMinute(0))
                .endTime(now.minusDays(8).withHour(12).withMinute(0))
                .notes("Hyderabad → Vijayawada engine components — completed")
                .lorryReceipts(List.of(lrs.get(14)))
                .build());

        return List.of(trip1, trip2, trip3, trip4, trip5, trip6, trip7, trip8,
                trip9, trip10, trip11, trip12, trip13, trip14);
    }

    // ───────────────────────────── Bookings ─────────────────────────────
    private void seedBookings(List<Trip> trips) {
        Trip busTripDelhiJaipur = trips.get(3);
        bookingRepository.saveAll(List.of(
                Booking.builder().customerName("Neha Gupta").customerPhone("+91-98100-22001")
                        .customerEmail("neha@example.com").trip(busTripDelhiJaipur).seatCount(2)
                        .status(BookingStatus.CONFIRMED).notes("Window seats preferred").build(),
                Booking.builder().customerName("Ahmed Khan").customerPhone("+91-98100-22002")
                        .customerEmail("ahmed@example.com").trip(busTripDelhiJaipur).seatCount(4)
                        .status(BookingStatus.CONFIRMED).notes("Family trip — 2 adults, 2 children").build(),
                Booking.builder().customerName("Anjali Nair").customerPhone("+91-98100-22003")
                        .customerEmail("anjali@example.com").trip(busTripDelhiJaipur).seatCount(1)
                        .status(BookingStatus.CONFIRMED).notes("Business travel").build(),
                Booking.builder().customerName("Rohan Joshi").customerPhone("+91-98100-22004")
                        .customerEmail("rohan@example.com").trip(busTripDelhiJaipur).seatCount(3)
                        .status(BookingStatus.CANCELLED).notes("Plans changed — cancelled by customer").build(),
                Booking.builder().customerName("Pooja Desai").customerPhone("+91-98100-22005")
                        .customerEmail("pooja@example.com").trip(busTripDelhiJaipur).seatCount(2)
                        .status(BookingStatus.CONFIRMED).notes(null).build()
        ));

        Trip minibusTripBlrChennai = trips.get(4);
        bookingRepository.saveAll(List.of(
                Booking.builder().customerName("Karan Malhotra").customerPhone("+91-98400-33001")
                        .customerEmail("karan.m@example.com").trip(minibusTripBlrChennai).seatCount(5)
                        .status(BookingStatus.CONFIRMED).notes("Group booking — colleagues").build(),
                Booking.builder().customerName("Priya Nair").customerPhone("+91-98400-33002")
                        .customerEmail("priya.n@example.com").trip(minibusTripBlrChennai).seatCount(2)
                        .status(BookingStatus.CONFIRMED).notes(null).build(),
                Booking.builder().customerName("Sanjay Kulkarni").customerPhone("+91-98400-33003")
                        .customerEmail("sanjay.k@example.com").trip(minibusTripBlrChennai).seatCount(1)
                        .status(BookingStatus.CANCELLED).notes("Rescheduled to next week").build()
        ));

        Trip freightTrip1 = trips.get(0);
        bookingRepository.saveAll(List.of(
                Booking.builder().customerName("Tata Steel").customerPhone("+91-11-2345-0401")
                        .customerEmail("logistics@tatasteel.com").trip(freightTrip1).seatCount(1)
                        .status(BookingStatus.CONFIRMED).notes("Freight booking — steel pipes consignment").build(),
                Booking.builder().customerName("UltraTech Cement").customerPhone("+91-11-2345-0402")
                        .customerEmail("dispatch@ultratech.com").trip(freightTrip1).seatCount(1)
                        .status(BookingStatus.CONFIRMED).notes("Freight booking — cement bags").build()
        ));

        Trip completedTrip1 = trips.get(8);
        bookingRepository.saveAll(List.of(
                Booking.builder().customerName("Godrej Interio").customerPhone("+91-22-2345-0501")
                        .customerEmail("ops@godrej.com").trip(completedTrip1).seatCount(1)
                        .status(BookingStatus.COMPLETED).notes("Furniture delivery completed successfully").build()
        ));

        Trip completedTrip2 = trips.get(10);
        bookingRepository.saveAll(List.of(
                Booking.builder().customerName("Reliance Retail").customerPhone("+91-44-2345-0601")
                        .customerEmail("shipping@relianceretail.com").trip(completedTrip2).seatCount(1)
                        .status(BookingStatus.COMPLETED).notes("Laptops delivered — POD signed").build(),
                Booking.builder().customerName("Croma").customerPhone("+91-44-2345-0602")
                        .customerEmail("receiving@croma.com").trip(completedTrip2).seatCount(1)
                        .status(BookingStatus.COMPLETED).notes("Monitors received in good condition").build()
        ));

        Trip inProgressTrip = trips.get(5);
        bookingRepository.saveAll(List.of(
                Booking.builder().customerName("Bharat Forge").customerPhone("+91-33-2345-0701")
                        .customerEmail("orders@bharatforge.com").trip(inProgressTrip).seatCount(1)
                        .status(BookingStatus.CONFIRMED).notes("Spare parts shipment — handle with care").build()
        ));

        Trip inProgressBus = trips.get(7);
        bookingRepository.saveAll(List.of(
                Booking.builder().customerName("Meera Krishnan").customerPhone("+91-98401-08001")
                        .customerEmail("meera.k@example.com").trip(inProgressBus).seatCount(3)
                        .status(BookingStatus.CONFIRMED).notes("Traveling with elderly parents").build(),
                Booking.builder().customerName("Rahul Banerjee").customerPhone("+91-98401-08002")
                        .customerEmail("rahul.b@example.com").trip(inProgressBus).seatCount(1)
                        .status(BookingStatus.CONFIRMED).notes(null).build(),
                Booking.builder().customerName("Sneha Iyer").customerPhone("+91-98401-08003")
                        .customerEmail("sneha.i@example.com").trip(inProgressBus).seatCount(2)
                        .status(BookingStatus.CANCELLED).notes("Train booked instead").build()
        ));
    }

    // ───────────────────────────── Expenses (incl. Fuel) ─────────────────────────────
    private void seedExpenses(List<Trip> trips, List<Vehicle> vehicles) {
        LocalDate today = LocalDate.now();

        Trip t9 = trips.get(8);
        expenseRepository.saveAll(List.of(
                Expense.builder().trip(t9).vehicle(vehicles.get(6)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("4500.00")).description("Diesel fill-up before trip")
                        .expenseDate(today.minusDays(5)).build(),
                Expense.builder().trip(t9).vehicle(vehicles.get(6)).category(ExpenseCategory.TOLL)
                        .amount(new BigDecimal("350.00")).description("Mumbai–Pune Expressway FASTag")
                        .expenseDate(today.minusDays(5)).build()
        ));

        Trip t10 = trips.get(9);
        expenseRepository.saveAll(List.of(
                Expense.builder().trip(t10).vehicle(vehicles.get(0)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("12500.00")).description("Full tank diesel — heavy load")
                        .expenseDate(today.minusDays(7)).build(),
                Expense.builder().trip(t10).vehicle(vehicles.get(0)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("3200.00")).description("Top-up en route near Vellore")
                        .expenseDate(today.minusDays(7)).build(),
                Expense.builder().trip(t10).vehicle(vehicles.get(0)).category(ExpenseCategory.TOLL)
                        .amount(new BigDecimal("800.00")).description("NH48 FASTag charges")
                        .expenseDate(today.minusDays(7)).build(),
                Expense.builder().trip(t10).vehicle(vehicles.get(0)).category(ExpenseCategory.DRIVER_ALLOWANCE)
                        .amount(new BigDecimal("1500.00")).description("Driver daily allowance")
                        .expenseDate(today.minusDays(7)).build()
        ));

        Trip t11 = trips.get(10);
        expenseRepository.saveAll(List.of(
                Expense.builder().trip(t11).vehicle(vehicles.get(6)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("3800.00")).description("Diesel fill-up Chennai depot")
                        .expenseDate(today.minusDays(3)).build(),
                Expense.builder().trip(t11).vehicle(vehicles.get(6)).category(ExpenseCategory.TOLL)
                        .amount(new BigDecimal("200.00")).description("ECR / NH32 toll")
                        .expenseDate(today.minusDays(3)).build()
        ));

        Trip t12 = trips.get(11);
        expenseRepository.saveAll(List.of(
                Expense.builder().trip(t12).vehicle(vehicles.get(4)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("11800.00")).description("Full tank before Jaipur departure")
                        .expenseDate(today.minusDays(10)).build(),
                Expense.builder().trip(t12).vehicle(vehicles.get(4)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("2500.00")).description("Top-up midway near Ajmer")
                        .expenseDate(today.minusDays(10)).build(),
                Expense.builder().trip(t12).vehicle(vehicles.get(4)).category(ExpenseCategory.DRIVER_ALLOWANCE)
                        .amount(new BigDecimal("1200.00")).description("Driver allowance")
                        .expenseDate(today.minusDays(10)).build()
        ));

        Trip t13 = trips.get(12);
        expenseRepository.saveAll(List.of(
                Expense.builder().trip(t13).vehicle(vehicles.get(11)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("3500.00")).description("Fuel — Delhi depot")
                        .expenseDate(today.minusDays(6)).build(),
                Expense.builder().trip(t13).vehicle(vehicles.get(11)).category(ExpenseCategory.TOLL)
                        .amount(new BigDecimal("250.00")).description("Yamuna Expressway FASTag")
                        .expenseDate(today.minusDays(6)).build()
        ));

        Trip t14 = trips.get(13);
        expenseRepository.saveAll(List.of(
                Expense.builder().trip(t14).vehicle(vehicles.get(2)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("9800.00")).description("Diesel — Hyderabad depot")
                        .expenseDate(today.minusDays(8)).build(),
                Expense.builder().trip(t14).vehicle(vehicles.get(2)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("4200.00")).description("Top-up near Suryapet")
                        .expenseDate(today.minusDays(8)).build(),
                Expense.builder().trip(t14).vehicle(vehicles.get(2)).category(ExpenseCategory.MAINTENANCE)
                        .amount(new BigDecimal("2800.00")).description("Tire rotation pre-trip")
                        .expenseDate(today.minusDays(9)).build(),
                Expense.builder().trip(t14).vehicle(vehicles.get(2)).category(ExpenseCategory.TOLL)
                        .amount(new BigDecimal("450.00")).description("NH65 FASTag charges")
                        .expenseDate(today.minusDays(8)).build()
        ));

        expenseRepository.saveAll(List.of(
                Expense.builder().vehicle(vehicles.get(0)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("8500.00")).description("Monthly fuel — DL-01-AB-1234")
                        .expenseDate(today.minusDays(35)).build(),
                Expense.builder().vehicle(vehicles.get(2)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("7200.00")).description("Monthly fuel — MH-12-EF-9012")
                        .expenseDate(today.minusDays(40)).build(),
                Expense.builder().vehicle(vehicles.get(4)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("9100.00")).description("Monthly fuel — KA-03-JK-7890")
                        .expenseDate(today.minusDays(45)).build(),
                Expense.builder().vehicle(vehicles.get(6)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("3200.00")).description("Monthly fuel — MH-14-NP-3344")
                        .expenseDate(today.minusDays(38)).build(),
                Expense.builder().vehicle(vehicles.get(11)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("2800.00")).description("Monthly fuel — TN-09-YZ-3434")
                        .expenseDate(today.minusDays(42)).build(),
                Expense.builder().vehicle(vehicles.get(0)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("10200.00")).description("Monthly fuel — DL-01-AB-1234")
                        .expenseDate(today.minusDays(65)).build(),
                Expense.builder().vehicle(vehicles.get(2)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("8800.00")).description("Monthly fuel — MH-12-EF-9012")
                        .expenseDate(today.minusDays(70)).build(),
                Expense.builder().vehicle(vehicles.get(4)).category(ExpenseCategory.FUEL)
                        .amount(new BigDecimal("11500.00")).description("Monthly fuel — KA-03-JK-7890")
                        .expenseDate(today.minusDays(75)).build()
        ));
    }

    // ───────────────────────────── Audit Logs ─────────────────────────────
    private void seedAuditLogs(List<Vehicle> vehicles, List<Driver> drivers, List<Trip> trips) {
        LocalDateTime now = LocalDateTime.now();

        auditLogRepository.saveAll(List.of(
                AuditLog.builder().entityType("User").entityId("admin").action(AuditAction.CREATE)
                        .changedBy("system").timestamp(now.minusDays(30))
                        .newValue("{\"username\":\"admin\",\"role\":\"ADMIN\",\"email\":\"admin@tms.com\"}").build(),
                AuditLog.builder().entityType("User").entityId("dispatcher").action(AuditAction.CREATE)
                        .changedBy("admin").timestamp(now.minusDays(28))
                        .newValue("{\"username\":\"dispatcher\",\"role\":\"DISPATCHER\",\"email\":\"dispatcher@tms.com\"}").build(),
                AuditLog.builder().entityType("User").entityId("driver1").action(AuditAction.CREATE)
                        .changedBy("admin").timestamp(now.minusDays(28))
                        .newValue("{\"username\":\"driver1\",\"role\":\"DRIVER\",\"email\":\"driver1@tms.com\"}").build(),

                AuditLog.builder().entityType("Vehicle").entityId(vehicles.get(0).getId().toString()).action(AuditAction.CREATE)
                        .changedBy("admin").timestamp(now.minusDays(25))
                        .newValue("{\"vehicleNumber\":\"DL-01-AB-1234\",\"type\":\"TRUCK\",\"status\":\"AVAILABLE\"}").build(),
                AuditLog.builder().entityType("Vehicle").entityId(vehicles.get(1).getId().toString()).action(AuditAction.CREATE)
                        .changedBy("admin").timestamp(now.minusDays(25))
                        .newValue("{\"vehicleNumber\":\"HR-26-CD-5678\",\"type\":\"TRUCK\",\"status\":\"AVAILABLE\"}").build(),
                AuditLog.builder().entityType("Vehicle").entityId(vehicles.get(1).getId().toString()).action(AuditAction.UPDATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(20))
                        .oldValue("{\"status\":\"AVAILABLE\"}")
                        .newValue("{\"status\":\"MAINTENANCE\"}").build(),

                AuditLog.builder().entityType("Driver").entityId(drivers.get(7).getId().toString()).action(AuditAction.UPDATE)
                        .changedBy("admin").timestamp(now.minusDays(15))
                        .oldValue("{\"status\":\"ACTIVE\"}")
                        .newValue("{\"status\":\"INACTIVE\"}").build(),

                AuditLog.builder().entityType("Trip").entityId(trips.get(8).getId().toString()).action(AuditAction.CREATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(6))
                        .newValue("{\"vehicle\":\"MH-14-NP-3344\",\"driver\":\"Kavita Reddy\",\"status\":\"PLANNED\"}").build(),
                AuditLog.builder().entityType("Trip").entityId(trips.get(8).getId().toString()).action(AuditAction.UPDATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(5).withHour(8))
                        .oldValue("{\"status\":\"PLANNED\"}")
                        .newValue("{\"status\":\"IN_PROGRESS\"}").build(),
                AuditLog.builder().entityType("Trip").entityId(trips.get(8).getId().toString()).action(AuditAction.UPDATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(5).withHour(14))
                        .oldValue("{\"status\":\"IN_PROGRESS\"}")
                        .newValue("{\"status\":\"COMPLETED\"}").build(),

                AuditLog.builder().entityType("Trip").entityId(trips.get(9).getId().toString()).action(AuditAction.CREATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(8))
                        .newValue("{\"vehicle\":\"DL-01-AB-1234\",\"driver\":\"Ramesh Yadav\",\"status\":\"PLANNED\"}").build(),
                AuditLog.builder().entityType("Trip").entityId(trips.get(9).getId().toString()).action(AuditAction.UPDATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(7).withHour(6))
                        .oldValue("{\"status\":\"PLANNED\"}")
                        .newValue("{\"status\":\"IN_PROGRESS\"}").build(),
                AuditLog.builder().entityType("Trip").entityId(trips.get(9).getId().toString()).action(AuditAction.UPDATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(7).withHour(16))
                        .oldValue("{\"status\":\"IN_PROGRESS\"}")
                        .newValue("{\"status\":\"COMPLETED\"}").build(),

                AuditLog.builder().entityType("Booking").entityId("1").action(AuditAction.CREATE)
                        .changedBy("client1").timestamp(now.minusDays(4))
                        .newValue("{\"customerName\":\"Neha Gupta\",\"seatCount\":2,\"status\":\"CONFIRMED\"}").build(),
                AuditLog.builder().entityType("Booking").entityId("4").action(AuditAction.UPDATE)
                        .changedBy("client4").timestamp(now.minusDays(3))
                        .oldValue("{\"status\":\"CONFIRMED\"}")
                        .newValue("{\"status\":\"CANCELLED\"}").build(),

                AuditLog.builder().entityType("Expense").entityId("fuel-001").action(AuditAction.CREATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(7))
                        .newValue("{\"category\":\"FUEL\",\"amount\":12500,\"vehicle\":\"DL-01-AB-1234\"}").build(),
                AuditLog.builder().entityType("Expense").entityId("toll-001").action(AuditAction.CREATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(7))
                        .newValue("{\"category\":\"TOLL\",\"amount\":800,\"vehicle\":\"DL-01-AB-1234\"}").build(),

                AuditLog.builder().entityType("LorryReceipt").entityId("LR-2026-0010").action(AuditAction.UPDATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(5))
                        .oldValue("{\"status\":\"IN_TRANSIT\"}")
                        .newValue("{\"status\":\"DELIVERED\"}").build(),
                AuditLog.builder().entityType("LorryReceipt").entityId("LR-2026-0006").action(AuditAction.UPDATE)
                        .changedBy("dispatcher").timestamp(now.minusDays(3))
                        .oldValue("{\"status\":\"CREATED\"}")
                        .newValue("{\"status\":\"IN_TRANSIT\"}").build(),

                AuditLog.builder().entityType("User").entityId("client1").action(AuditAction.UPDATE)
                        .changedBy("admin").timestamp(now.minusDays(10))
                        .oldValue("{\"role\":\"CLIENT\",\"active\":true}")
                        .newValue("{\"role\":\"CLIENT\",\"active\":true}").build(),

                AuditLog.builder().entityType("Vehicle").entityId(vehicles.get(5).getId().toString()).action(AuditAction.UPDATE)
                        .changedBy("system").timestamp(now.minusHours(3))
                        .oldValue("{\"latitude\":null,\"longitude\":null}")
                        .newValue("{\"latitude\":22.5726,\"longitude\":88.3639,\"currentLocation\":\"En route to Bhubaneswar\"}").build(),

                AuditLog.builder().entityType("Geofence").entityId("delhi-depot").action(AuditAction.CREATE)
                        .changedBy("admin").timestamp(now.minusDays(2))
                        .newValue("{\"name\":\"Delhi Main Depot\",\"type\":\"DEPOT\",\"radiusMeters\":500}").build()
        ));
    }

    // ───────────────────────────── Geofences ─────────────────────────────
    private void seedGeofences() {
        geofenceRepository.saveAll(List.of(
                Geofence.builder().name("Delhi Main Depot").description("Main depot in Delhi")
                        .latitude(28.6139).longitude(77.2090).radiusMeters(500.0)
                        .type(GeofenceType.DEPOT).build(),
                Geofence.builder().name("Mumbai Warehouse").description("Mumbai distribution warehouse")
                        .latitude(19.0760).longitude(72.8777).radiusMeters(750.0)
                        .type(GeofenceType.DEPOT).build(),
                Geofence.builder().name("Bengaluru Logistics Park").description("Bengaluru logistics and loading area")
                        .latitude(12.9716).longitude(77.5946).radiusMeters(1000.0)
                        .type(GeofenceType.DELIVERY_ZONE).build(),
                Geofence.builder().name("Gurugram Restricted").description("Restricted zone — no unauthorized entry")
                        .latitude(28.4595).longitude(77.0266).radiusMeters(300.0)
                        .type(GeofenceType.RESTRICTED_ZONE).build(),
                Geofence.builder().name("JNPT Nhava Sheva").description("JNPT container terminal — inbound ocean freight")
                        .latitude(18.9490).longitude(72.9490).radiusMeters(1200.0)
                        .type(GeofenceType.DEPOT).build(),
                Geofence.builder().name("Chennai Port CFS").description("Chennai port container freight station")
                        .latitude(13.0827).longitude(80.2910).radiusMeters(800.0)
                        .type(GeofenceType.DELIVERY_ZONE).build(),
                Geofence.builder().name("Hyderabad Gachibowli Hub").description("IT corridor delivery hub")
                        .latitude(17.4401).longitude(78.3489).radiusMeters(600.0)
                        .type(GeofenceType.DELIVERY_ZONE).build(),
                Geofence.builder().name("Jaipur Sitapura Depot").description("Sitapura industrial area staging yard")
                        .latitude(26.7895).longitude(75.8472).radiusMeters(700.0)
                        .type(GeofenceType.DEPOT).build()
        ));
    }

    private void seedFreightRates() {
        freightRateCardRepository.saveAll(List.of(
                FreightRateCard.builder().origin("Delhi").destination("Jaipur").vehicleType(VehicleType.TRUCK)
                        .ratePerKm(new BigDecimal("38.00")).minCharge(new BigDecimal("8000.00")).gstPercent(new BigDecimal("18.00")).active(true).build(),
                FreightRateCard.builder().origin("Delhi").destination("Jaipur").vehicleType(VehicleType.BUS)
                        .ratePerKm(new BigDecimal("22.00")).minCharge(new BigDecimal("5000.00")).gstPercent(new BigDecimal("18.00")).active(true).build(),
                FreightRateCard.builder().origin("Mumbai").destination("Pune").vehicleType(VehicleType.TRUCK)
                        .ratePerKm(new BigDecimal("42.00")).minCharge(new BigDecimal("4500.00")).gstPercent(new BigDecimal("18.00")).active(true).build(),
                FreightRateCard.builder().origin("Bengaluru").destination("Chennai").vehicleType(VehicleType.TRUCK)
                        .ratePerKm(new BigDecimal("36.00")).minCharge(new BigDecimal("9000.00")).gstPercent(new BigDecimal("18.00")).active(true).build(),
                FreightRateCard.builder().origin("Jaipur").destination("Udaipur").vehicleType(VehicleType.TRUCK)
                        .ratePerKm(new BigDecimal("33.00")).minCharge(new BigDecimal("8500.00")).gstPercent(new BigDecimal("18.00")).active(true).build(),
                FreightRateCard.builder().origin("Delhi").destination("Mumbai").vehicleType(VehicleType.TRUCK)
                        .ratePerKm(new BigDecimal("31.00")).minCharge(new BigDecimal("28000.00")).gstPercent(new BigDecimal("18.00")).active(true).build()
        ));
    }
}
