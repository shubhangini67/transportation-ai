package com.tms.service;

import com.tms.dto.request.BookingRequest;
import com.tms.dto.request.DriverRequest;
import com.tms.dto.request.ExpenseRequest;
import com.tms.dto.request.GeofenceRequest;
import com.tms.dto.request.RouteRequest;
import com.tms.dto.request.VehicleRequest;
import com.tms.dto.response.AiAskResponse;
import com.tms.dto.response.BookingResponse;
import com.tms.dto.response.DriverResponse;
import com.tms.dto.response.ExpenseResponse;
import com.tms.dto.response.GeofenceResponse;
import com.tms.dto.response.RouteResponse;
import com.tms.dto.response.TripResponse;
import com.tms.dto.response.VehicleResponse;
import com.tms.entity.Driver;
import com.tms.entity.Vehicle;
import com.tms.enums.DriverStatus;
import com.tms.enums.ExpenseCategory;
import com.tms.enums.GeofenceType;
import com.tms.enums.TripStatus;
import com.tms.enums.VehicleStatus;
import com.tms.enums.VehicleType;
import com.tms.exception.BadRequestException;
import com.tms.exception.DuplicateResourceException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.DriverRepository;
import com.tms.repository.GeofenceRepository;
import com.tms.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversational create / update / delete. Asks form fields in screen order,
 * writes through the same services as the UI, then returns a list link.
 */
@Service
@RequiredArgsConstructor
public class AiActionService {

    private static final String BOT = "Copilot";
    private static final long SESSION_TTL_SECONDS = 20 * 60;
    private static final Pattern PHONE = Pattern.compile("(\\+?[0-9][0-9\\s-]{6,18}[0-9])");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern TO_LANE = Pattern.compile("(.+?)\\s+(?:to|->)\\s+(.+)");
    private static final Map<String, double[]> CITIES = Map.ofEntries(
            Map.entry("delhi", new double[]{28.6139, 77.2090}),
            Map.entry("new delhi", new double[]{28.6139, 77.2090}),
            Map.entry("mumbai", new double[]{19.0760, 72.8777}),
            Map.entry("jaipur", new double[]{26.9124, 75.7873}),
            Map.entry("bangalore", new double[]{12.9716, 77.5946}),
            Map.entry("bengaluru", new double[]{12.9716, 77.5946}),
            Map.entry("chennai", new double[]{13.0827, 80.2707}),
            Map.entry("hyderabad", new double[]{17.3850, 78.4867}),
            Map.entry("pune", new double[]{18.5204, 73.8567}),
            Map.entry("kolkata", new double[]{22.5726, 88.3639}),
            Map.entry("ahmedabad", new double[]{23.0225, 72.5714})
    );

    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final RouteService routeService;
    private final BookingService bookingService;
    private final ExpenseService expenseService;
    private final GeofenceService geofenceService;
    private final TripService tripService;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final GeofenceRepository geofenceRepository;
    private final LlmGateway llmGateway;

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @Transactional
    public Optional<AiAskResponse> tryHandle(String raw) {
        String message = raw == null ? "" : raw.trim();
        if (message.isBlank()) return Optional.empty();

        String user = currentUser();
        if (user == null) return Optional.empty();

        Session session = sessions.get(user);
        if (session != null && session.expired()) {
            sessions.remove(user);
            session = null;
        }

        String q = message.toLowerCase(Locale.ROOT);
        if (isCancel(q) && session != null) {
            sessions.remove(user);
            return Optional.of(reply("Cancelled. Nothing was changed. Say add a driver when you want to start again.",
                    List.of(), List.of("Add a new driver", "Add a vehicle", "Which trips are delayed?")));
        }

        if (session != null && looksLikeGreeting(q)) {
            sessions.remove(user);
            return Optional.empty();
        }

        ParsedAction fresh = parseAction(q);
        if (fresh != null) {
            session = new Session(fresh.verb, fresh.entity);
            seedFromUtterance(session, message);
            session.touch();
            sessions.put(user, session);
            return Optional.of(advance(session, user));
        }

        if (session != null) {
            applyAnswer(session, message);
            session.touch();
            sessions.put(user, session);
            return Optional.of(advance(session, user));
        }

        return Optional.empty();
    }

    public void clearSession() {
        String user = currentUser();
        if (user != null) sessions.remove(user);
    }

    private AiAskResponse advance(Session session, String user) {
        if (session.verb != Verb.CREATE && session.targetId == null) {
            if (session.candidates != null && !session.candidates.isEmpty()) {
                return pickList(session);
            }
            return identifyPrompt(session);
        }
        if (session.verb == Verb.UPDATE && isBlank(session.pendingField)) {
            return fieldPrompt(session);
        }
        if (session.verb != Verb.DELETE) {
            Field next = nextMissing(session);
            if (next != null) {
                return askField(session, next);
            }
        }
        if (!session.confirmed) {
            return confirmPrompt(session);
        }
        try {
            AiAskResponse done = execute(session);
            sessions.remove(user);
            return done;
        } catch (DuplicateResourceException | BadRequestException | ResourceNotFoundException ex) {
            session.confirmed = false;
            return reply(ex.getMessage() + "\nFix it and say yes again, or say cancel.",
                    List.of(listLink(session.entity)), List.of("yes", "cancel"));
        } catch (Exception ex) {
            session.confirmed = false;
            return reply("I could not save that: " + ex.getMessage() + "\nSay cancel and try from the list page, or yes to retry.",
                    List.of(listLink(session.entity)), List.of("yes", "cancel"));
        }
    }

    private void applyAnswer(Session session, String message) {
        String q = message.toLowerCase(Locale.ROOT).trim();

        if (session.verb != Verb.CREATE && session.candidates != null && session.targetId == null) {
            Candidate chosen = pickCandidate(session, message);
            if (chosen != null) {
                session.targetId = chosen.id;
                session.targetLabel = chosen.label;
                session.candidates = null;
            }
            return;
        }

        if (session.verb != Verb.CREATE && session.targetId == null) {
            session.lookupHint = message.trim();
            resolveTarget(session);
            return;
        }

        if (session.verb == Verb.UPDATE && isBlank(session.pendingField)) {
            String field = matchField(session.entity, q);
            String value = valueAfterField(message, field);
            if (field != null) {
                session.pendingField = field;
                if (!isBlank(value)) {
                    putSlot(session, field, value);
                }
            }
            return;
        }

        if (session.verb == Verb.DELETE) {
            if (isYes(q)) session.confirmed = true;
            else if (isNo(q)) session.confirmed = false;
            return;
        }

        Field next = nextMissing(session);
        if (next == null) {
            if (isYes(q)) session.confirmed = true;
            else if (isSkipRest(q) || isYes(q)) session.confirmed = true;
            return;
        }

        if (isSkipRest(q) && remainingOptional(session)) {
            markRemainingSkipped(session);
            return;
        }
        if (isSkip(q)) {
            if (next.required) {
                session.lastError = "That field is required — I cannot skip it.";
                return;
            }
            session.slots.put(next.key, "");
            return;
        }

        String error = validate(session.entity, next.key, message);
        if (error != null) {
            session.lastError = error;
            return;
        }
        putSlot(session, next.key, normalize(session.entity, next.key, message));
        session.lastError = null;
    }

    private void seedFromUtterance(Session session, String message) {
        String q = message.toLowerCase(Locale.ROOT);
        session.lookupHint = extractLookup(session, message);

        if (session.verb == Verb.CREATE) {
            seedCreateSlots(session, message, q);
            return;
        }

        if (!isBlank(session.lookupHint)) {
            resolveTarget(session);
        }
        if (session.verb == Verb.UPDATE) {
            String field = matchField(session.entity, q);
            if (field != null) {
                session.pendingField = field;
                String value = valueAfterField(message, field);
                if (!isBlank(value)) {
                    putSlot(session, field, value);
                }
            }
        }
    }

    private void seedCreateSlots(Session session, String message, String q) {
        Matcher email = EMAIL.matcher(message);
        if (email.find() && hasField(session.entity, "email")) {
            session.slots.put("email", email.group());
        }
        Matcher phone = PHONE.matcher(message.replace(" ", ""));
        if (!phone.find()) phone = PHONE.matcher(message);
        if (phone.find() && hasField(session.entity, "phone")) {
            session.slots.put("phone", normalize(session.entity, "phone", phone.group()));
        }

        switch (session.entity) {
            case DRIVER -> {
                String name = capture(message, "(?:named|name is|name)\\s+([A-Za-z][A-Za-z.\\s]{1,60}?)(?:\\s+(?:phone|mobile|license|licence|email|status)|$)");
                if (isBlank(name)) name = capture(q, "driver\\s+(?:named\\s+)?([a-z][a-z.\\s]{1,60}?)(?:\\s+(?:phone|mobile|license|email)|$)");
                if (!isBlank(name) && !isNoiseName(name)) session.slots.put("name", title(name));
                String license = capture(message, "(?:license|licence|dl)\\s*(?:number|no\\.?|#)?\\s*[:\\-]?\\s*([A-Za-z0-9\\-/]{4,30})");
                if (!isBlank(license)) session.slots.put("licenseNumber", license.trim());
            }
            case VEHICLE -> {
                String plate = capture(message, "(?:number|plate|vehicle)\\s*[:\\-]?\\s*([A-Za-z0-9\\-]{5,20})");
                if (isBlank(plate)) plate = capture(message, "\\b([A-Z]{2}[-\\s]?[0-9]{1,2}[-\\s]?[A-Z]{1,3}[-\\s]?[0-9]{3,4})\\b");
                if (!isBlank(plate)) session.slots.put("vehicleNumber", plate.replace(" ", "").toUpperCase(Locale.ROOT));
                VehicleType type = parseVehicleType(q);
                if (type != null) session.slots.put("type", type.name());
                String cap = capture(q, "(?:capacity|seats?)\\s*[:\\-]?\\s*(\\d+)");
                if (!isBlank(cap)) session.slots.put("capacity", cap);
            }
            case ROUTE -> {
                Matcher lane = TO_LANE.matcher(stripVerb(q));
                if (lane.find()) {
                    session.slots.put("origin", title(lane.group(1).replaceAll("(?:add|create|new|a|route|lane)", " ").trim()));
                    session.slots.put("destination", title(lane.group(2).replaceAll("\\d.*", "").replace("km", "").trim()));
                }
                String km = capture(q, "(\\d+(?:\\.\\d+)?)\\s*km");
                if (!isBlank(km)) session.slots.put("distance", km);
                String mins = capture(q, "(\\d+)\\s*(?:min|minutes)");
                if (!isBlank(mins)) session.slots.put("estimatedTimeMinutes", mins);
            }
            case BOOKING -> {
                String name = capture(message, "(?:named|name is|customer)\\s+([A-Za-z][A-Za-z.\\s]{1,60}?)(?:\\s+(?:phone|seats?)|$)");
                if (!isBlank(name)) session.slots.put("customerName", title(name));
                String seats = capture(q, "(\\d+)\\s*seats?");
                if (!isBlank(seats)) session.slots.put("seatCount", seats);
            }
            case EXPENSE -> {
                ExpenseCategory cat = parseExpense(q);
                if (cat != null) session.slots.put("category", cat.name());
                String amt = capture(q, "(?:rs|inr|₹)?\\s*(\\d+(?:\\.\\d+)?)");
                if (!isBlank(amt)) session.slots.put("amount", amt);
            }
            case GEOFENCE -> {
                String name = capture(message, "(?:named|name is|zone)\\s+([A-Za-z][A-Za-z0-9.\\s]{1,60}?)(?:\\s+(?:at|in|type|radius)|$)");
                if (!isBlank(name)) session.slots.put("name", title(name));
                GeofenceType type = parseGeofenceType(q);
                if (type != null) session.slots.put("type", type.name());
                seedCity(session, q);
            }
        }
    }

    private void resolveTarget(Session session) {
        String hint = session.lookupHint == null ? "" : session.lookupHint.trim();
        if (hint.isBlank()) return;
        List<Candidate> found = search(session.entity, hint);
        if (found.size() == 1) {
            session.targetId = found.get(0).id;
            session.targetLabel = found.get(0).label;
            session.candidates = null;
        } else if (found.size() > 1) {
            session.candidates = found.stream().limit(6).toList();
        } else {
            session.lastError = "I could not find " + session.entity.label + " \"" + hint + "\".";
        }
    }

    private List<Candidate> search(EntityType entity, String hint) {
        String q = hint.trim();
        return switch (entity) {
            case DRIVER -> {
                List<Driver> byName = driverRepository.findByNameContainingIgnoreCase(q);
                if (byName.isEmpty()) {
                    driverRepository.findByLicenseNumber(q).ifPresent(byName::add);
                }
                yield byName.stream()
                        .map(d -> new Candidate(d.getId().toString(), d.getName() + " · " + d.getLicenseNumber()))
                        .toList();
            }
            case VEHICLE -> {
                List<Vehicle> list = new ArrayList<>();
                vehicleRepository.findByVehicleNumberIgnoreCase(q.replace(" ", "")).ifPresent(list::add);
                if (list.isEmpty()) {
                    list.addAll(vehicleRepository.findByVehicleNumberContainingIgnoreCase(q));
                }
                yield list.stream()
                        .map(v -> new Candidate(v.getId().toString(), v.getVehicleNumber() + " · " + v.getType()))
                        .toList();
            }
            case ROUTE -> routeService.searchRoutes(q).stream()
                    .map(r -> new Candidate(String.valueOf(r.getId()), r.getOrigin() + " → " + r.getDestination()))
                    .toList();
            case BOOKING -> bookingService.getAllBookings(0, 8, null, q).getContent().stream()
                    .map(b -> new Candidate(String.valueOf(b.getId()), b.getCustomerName() + " · " + b.getSeatCount() + " seats"))
                    .toList();
            case EXPENSE -> expenseService.getAllExpenses(0, 8, null, null, null).getContent().stream()
                    .map(e -> new Candidate(e.getId().toString(),
                            e.getCategory() + " ₹" + e.getAmount() + " · " + e.getExpenseDate()))
                    .toList();
            case GEOFENCE -> geofenceRepository.findByNameContainingIgnoreCase(q).stream()
                    .map(g -> new Candidate(g.getId().toString(), g.getName() + " · " + g.getType()))
                    .toList();
        };
    }

    private AiAskResponse execute(Session session) {
        if (session.verb == Verb.DELETE && !isAdmin()) {
            return reply("Only an admin can delete. I can leave it as-is, or an admin can remove it from the list.",
                    List.of(listLink(session.entity)), List.of("Add a new driver"));
        }
        String label;
        String check;
        switch (session.entity) {
            case DRIVER -> {
                if (session.verb == Verb.CREATE) {
                    DriverResponse d = driverService.createDriver(toDriverRequest(session));
                    label = d.getName();
                } else if (session.verb == Verb.UPDATE) {
                    DriverResponse d = driverService.updateDriver(UUID.fromString(session.targetId), overlayDriver(session));
                    label = d.getName();
                } else {
                    label = session.targetLabel;
                    driverService.deleteDriver(UUID.fromString(session.targetId));
                }
                check = "driver list";
            }
            case VEHICLE -> {
                if (session.verb == Verb.CREATE) {
                    VehicleResponse v = vehicleService.createVehicle(toVehicleRequest(session));
                    label = v.getVehicleNumber();
                } else if (session.verb == Verb.UPDATE) {
                    VehicleResponse v = vehicleService.updateVehicle(UUID.fromString(session.targetId), overlayVehicle(session));
                    label = v.getVehicleNumber();
                } else {
                    label = session.targetLabel;
                    vehicleService.deleteVehicle(UUID.fromString(session.targetId));
                }
                check = "vehicle list";
            }
            case ROUTE -> {
                if (session.verb == Verb.CREATE) {
                    RouteResponse r = routeService.createRoute(toRouteRequest(session));
                    label = r.getOrigin() + " → " + r.getDestination();
                } else if (session.verb == Verb.UPDATE) {
                    RouteResponse r = routeService.updateRoute(Long.parseLong(session.targetId), overlayRoute(session));
                    label = r.getOrigin() + " → " + r.getDestination();
                } else {
                    label = session.targetLabel;
                    routeService.deleteRoute(Long.parseLong(session.targetId));
                }
                check = "route list";
            }
            case BOOKING -> {
                if (session.verb == Verb.CREATE) {
                    BookingResponse b = bookingService.createBooking(toBookingRequest(session));
                    label = b.getCustomerName();
                } else if (session.verb == Verb.UPDATE) {
                    BookingResponse b = bookingService.updateBooking(Long.parseLong(session.targetId), overlayBooking(session));
                    label = b.getCustomerName();
                } else {
                    label = session.targetLabel;
                    bookingService.deleteBooking(Long.parseLong(session.targetId));
                }
                check = "booking list";
            }
            case EXPENSE -> {
                if (session.verb == Verb.CREATE) {
                    ExpenseResponse e = expenseService.createExpense(toExpenseRequest(session));
                    label = e.getCategory() + " ₹" + e.getAmount();
                } else if (session.verb == Verb.UPDATE) {
                    ExpenseResponse e = expenseService.updateExpense(UUID.fromString(session.targetId), overlayExpense(session));
                    label = e.getCategory() + " ₹" + e.getAmount();
                } else {
                    label = session.targetLabel;
                    expenseService.deleteExpense(UUID.fromString(session.targetId));
                }
                check = "expense list";
            }
            case GEOFENCE -> {
                if (session.verb == Verb.CREATE) {
                    GeofenceResponse g = geofenceService.create(toGeofenceRequest(session));
                    label = g.getName();
                } else if (session.verb == Verb.UPDATE) {
                    GeofenceResponse g = geofenceService.update(UUID.fromString(session.targetId), overlayGeofence(session));
                    label = g.getName();
                } else {
                    label = session.targetLabel;
                    geofenceService.delete(UUID.fromString(session.targetId));
                }
                check = "geofence list";
            }
            default -> {
                label = session.entity.label;
                check = "list";
            }
        }
        String verbed = switch (session.verb) {
            case CREATE -> "added";
            case UPDATE -> "updated";
            case DELETE -> "deleted";
        };
        String answer = "Done — I " + verbed + " " + label + ".\nYou can check it in the " + check + " by clicking here.";
        return reply(answer, List.of(listLink(session.entity)),
                List.of("Add a new driver", "Add a vehicle", "Which trips are delayed?"));
    }

    private AiAskResponse identifyPrompt(Session session) {
        String who = switch (session.entity) {
            case DRIVER -> "Which driver? Type the name or license number.";
            case VEHICLE -> "Which vehicle? Type the plate number.";
            case ROUTE -> "Which route? Type origin, destination, or Delhi to Jaipur.";
            case BOOKING -> "Which booking? Type the customer name.";
            case EXPENSE -> {
                List<ExpenseResponse> recent = expenseService.getAllExpenses(0, 6, null, null, null).getContent();
                session.candidates = recent.stream()
                        .map(e -> new Candidate(e.getId().toString(), e.getCategory() + " ₹" + e.getAmount() + " · " + e.getExpenseDate()))
                        .toList();
                yield "Which expense? Reply with the number.";
            }
            case GEOFENCE -> "Which geofence? Type the zone name.";
        };
        String err = session.lastError == null ? "" : session.lastError + "\n";
        session.lastError = null;
        if (session.entity == EntityType.EXPENSE && session.candidates != null) {
            return pickList(session);
        }
        return reply(err + session.verb.label + " " + session.entity.label + ".\n" + who,
                List.of(listLink(session.entity)), List.of("cancel"));
    }

    private AiAskResponse pickList(Session session) {
        if (session.candidates == null || session.candidates.isEmpty()) {
            return reply("No matches. Try another name, or say cancel.", List.of(listLink(session.entity)), List.of("cancel"));
        }
        StringBuilder sb = new StringBuilder("I found more than one. Reply with the number:\n");
        List<String> chips = new ArrayList<>();
        for (int i = 0; i < session.candidates.size(); i++) {
            sb.append(i + 1).append(") ").append(session.candidates.get(i).label).append('\n');
            chips.add(String.valueOf(i + 1));
        }
        chips.add("cancel");
        return reply(sb.toString().trim(), List.of(listLink(session.entity)), chips);
    }

    private AiAskResponse fieldPrompt(Session session) {
        List<Field> fields = fields(session.entity);
        String names = fields.stream().map(f -> f.label.toLowerCase(Locale.ROOT)).reduce((a, b) -> a + ", " + b).orElse("a field");
        return reply("What should I change for " + session.targetLabel + "?\n" + names + ".",
                List.of(listLink(session.entity)), fields.stream().limit(4).map(f -> f.label).toList());
    }

    private AiAskResponse askField(Session session, Field field) {
        String prefix = session.verb == Verb.CREATE
                ? "I'll add a " + session.entity.label + ". "
                : "Updating " + nullTo(session.targetLabel, session.entity.label) + ". ";
        if (session.lastError != null) {
            prefix = session.lastError + "\n";
            session.lastError = null;
        }
        String extra = field.required ? "" : " Say skip to leave this blank.";
        if ("tripId".equals(field.key)) {
            return askTrip(session, prefix);
        }
        List<String> chips = new ArrayList<>();
        if (!field.required) chips.add("skip");
        chips.addAll(field.hints);
        chips.add("cancel");
        return reply(prefix + field.prompt + extra, List.of(), chips.stream().distinct().limit(4).toList());
    }

    private AiAskResponse askTrip(Session session, String prefix) {
        List<TripResponse> trips = tripService.getTripsByStatus(TripStatus.PLANNED);
        if (trips.isEmpty()) {
            return reply("There is no PLANNED trip to book. Create a trip on Smart Dispatch first.",
                    List.of(link("/dispatch", "Open Smart Dispatch")), List.of("cancel"));
        }
        session.candidates = trips.stream().limit(8)
                .map(t -> new Candidate(t.getId().toString(),
                        (t.getRouteOrigin() == null ? "Trip" : t.getRouteOrigin() + " → " + t.getRouteDestination())
                                + " · " + t.getVehicleNumber()))
                .toList();
        StringBuilder sb = new StringBuilder(prefix).append("Which trip? Reply with the number:\n");
        List<String> chips = new ArrayList<>();
        for (int i = 0; i < session.candidates.size(); i++) {
            sb.append(i + 1).append(") ").append(session.candidates.get(i).label).append('\n');
            chips.add(String.valueOf(i + 1));
        }
        return reply(sb.toString().trim(), List.of(link("/trips", "Open trips")), chips);
    }

    private AiAskResponse confirmPrompt(Session session) {
        StringBuilder sb = new StringBuilder();
        if (session.verb == Verb.DELETE) {
            sb.append("Delete ").append(session.targetLabel).append("?\nThis cannot be undone. Reply yes or no.");
        } else {
            sb.append(session.verb == Verb.CREATE ? "I'll save this " : "I'll update this ");
            sb.append(session.entity.label).append(":\n");
            for (Field f : fields(session.entity)) {
                String v = session.slots.get(f.key);
                if (isBlank(v)) continue;
                sb.append("• ").append(f.label).append(": ").append(display(f.key, v)).append('\n');
            }
            sb.append("Reply yes to save, or no to cancel.");
        }
        return reply(sb.toString().trim(), List.of(listLink(session.entity)), List.of("yes", "no"));
    }

    private Field nextMissing(Session session) {
        if (session.verb == Verb.UPDATE) {
            if (isBlank(session.pendingField)) return null;
            if (!session.slots.containsKey(session.pendingField) || isBlank(session.slots.get(session.pendingField))) {
                return fields(session.entity).stream()
                        .filter(f -> f.key.equals(session.pendingField))
                        .findFirst()
                        .orElse(null);
            }
            return null;
        }
        if ("tripId".equals(peekNextKey(session)) && session.candidates != null && session.targetId == null
                && !session.slots.containsKey("tripId")) {
            return fields(session.entity).stream().filter(f -> "tripId".equals(f.key)).findFirst().orElse(null);
        }
        for (Field f : fields(session.entity)) {
            if (!session.slots.containsKey(f.key)) return f;
        }
        return null;
    }

    private String peekNextKey(Session session) {
        for (Field f : fields(session.entity)) {
            if (!session.slots.containsKey(f.key)) return f.key;
        }
        return null;
    }

    private boolean remainingOptional(Session session) {
        return fields(session.entity).stream()
                .filter(f -> !session.slots.containsKey(f.key))
                .allMatch(f -> !f.required);
    }

    private void markRemainingSkipped(Session session) {
        for (Field f : fields(session.entity)) {
            session.slots.putIfAbsent(f.key, "");
        }
    }

    private List<Field> fields(EntityType entity) {
        return switch (entity) {
            case DRIVER -> List.of(
                    field("name", "Name", "What is the driver's full name?", true),
                    field("phone", "Phone", "Phone number? Use +91 and 10 digits, like +919876543210.", true),
                    field("licenseNumber", "License number", "License number?", true),
                    field("email", "Email", "Email address?", false, "skip"),
                    field("status", "Status", "Status? ACTIVE or INACTIVE.", false, "ACTIVE", "INACTIVE")
            );
            case VEHICLE -> List.of(
                    field("vehicleNumber", "Vehicle number", "Vehicle / plate number?", true),
                    field("type", "Type", "Type? TRUCK, BUS, MINI_BUS, or VAN.", true, "TRUCK", "BUS", "VAN"),
                    field("capacity", "Capacity", "Capacity (seats or load units)?", true),
                    field("status", "Status", "Status? AVAILABLE, BUSY, or MAINTENANCE.", false, "AVAILABLE"),
                    field("make", "Make", "Make? (Tata, Ashok Leyland…)", false, "skip"),
                    field("model", "Model", "Model?", false, "skip"),
                    field("year", "Year", "Year?", false, "skip"),
                    field("currentLocation", "Location", "Current location / city?", false, "skip"),
                    field("odometerKm", "Odometer", "Odometer in km?", false, "skip")
            );
            case ROUTE -> List.of(
                    field("origin", "Origin", "Origin city?", true),
                    field("destination", "Destination", "Destination city?", true),
                    field("distance", "Distance", "Distance in km?", true),
                    field("estimatedTimeMinutes", "ETA minutes", "Estimated time in minutes?", true),
                    field("description", "Description", "Short description?", false, "skip")
            );
            case BOOKING -> List.of(
                    field("customerName", "Customer name", "Customer full name?", true),
                    field("customerPhone", "Customer phone", "Customer phone?", true),
                    field("customerEmail", "Customer email", "Customer email?", false, "skip"),
                    field("tripId", "Trip", "Which trip?", true),
                    field("seatCount", "Seats", "How many seats?", true),
                    field("notes", "Notes", "Notes?", false, "skip")
            );
            case EXPENSE -> List.of(
                    field("category", "Category", "Category? FUEL, TOLL, MAINTENANCE, DRIVER_ALLOWANCE, or OTHER.", true, "FUEL", "TOLL"),
                    field("amount", "Amount", "Amount in ₹?", true),
                    field("expenseDate", "Date", "Date? YYYY-MM-DD, or say today.", true, "today"),
                    field("description", "Description", "Description?", false, "skip")
            );
            case GEOFENCE -> List.of(
                    field("name", "Name", "Zone name?", true),
                    field("type", "Type", "Type? DEPOT, DELIVERY_ZONE, RESTRICTED_ZONE, or CUSTOM.", false, "DEPOT"),
                    field("radiusMeters", "Radius", "Radius in meters? (e.g. 800)", true),
                    field("latitude", "Location", "City (Delhi, Mumbai, Jaipur…) or lat,lng?", true, "Delhi", "Mumbai")
            );
        };
    }

    private String validate(EntityType entity, String key, String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isBlank()) return "I need a value for that.";
        return switch (key) {
            case "phone", "customerPhone" -> normalizePhone(v) == null ? "Phone must look like +919876543210." : null;
            case "email", "customerEmail" -> v.contains("@") ? null : "That does not look like an email. Or say skip.";
            case "capacity", "seatCount", "year", "odometerKm", "estimatedTimeMinutes", "radiusMeters" ->
                    parsePositive(v) == null ? "I need a positive number." : null;
            case "distance", "amount" -> parseDecimal(v) == null ? "I need a number." : null;
            case "type" -> entity == EntityType.VEHICLE
                    ? (parseVehicleType(v) == null ? "Type must be TRUCK, BUS, MINI_BUS, or VAN." : null)
                    : (parseGeofenceType(v) == null ? "Type must be DEPOT, DELIVERY_ZONE, RESTRICTED_ZONE, or CUSTOM." : null);
            case "status" -> entity == EntityType.DRIVER
                    ? (parseDriverStatus(v) == null ? "Status must be ACTIVE or INACTIVE." : null)
                    : (parseVehicleStatus(v) == null ? "Status must be AVAILABLE, BUSY, or MAINTENANCE." : null);
            case "category" -> parseExpense(v) == null ? "Category must be FUEL, TOLL, MAINTENANCE, DRIVER_ALLOWANCE, or OTHER." : null;
            case "expenseDate" -> parseDate(v) == null ? "Use YYYY-MM-DD or say today." : null;
            case "latitude" -> parseLatLngOrCity(v) == null ? "Say a city like Delhi, or lat,lng like 28.61, 77.21." : null;
            case "tripId" -> null;
            default -> null;
        };
    }

    private String normalize(EntityType entity, String key, String raw) {
        String v = raw.trim();
        return switch (key) {
            case "phone", "customerPhone" -> normalizePhone(v);
            case "status" -> entity == EntityType.DRIVER
                    ? parseDriverStatus(v).name()
                    : parseVehicleStatus(v).name();
            case "type" -> entity == EntityType.VEHICLE ? parseVehicleType(v).name() : parseGeofenceType(v).name();
            case "category" -> parseExpense(v).name();
            case "expenseDate" -> parseDate(v).toString();
            case "vehicleNumber" -> v.replace(" ", "").toUpperCase(Locale.ROOT);
            case "licenseNumber" -> v.replace(" ", "").toUpperCase(Locale.ROOT);
            case "latitude" -> {
                double[] ll = parseLatLngOrCity(v);
                yield ll[0] + "," + ll[1];
            }
            case "capacity", "seatCount", "year", "odometerKm", "estimatedTimeMinutes", "radiusMeters" ->
                    String.valueOf(parsePositive(v));
            case "distance", "amount" -> parseDecimal(v);
            case "tripId" -> v;
            default -> v;
        };
    }

    private void putSlot(Session session, String key, String value) {
        if ("latitude".equals(key) && value != null && value.contains(",")) {
            String[] p = value.split(",", 2);
            session.slots.put("latitude", p[0].trim());
            session.slots.put("longitude", p[1].trim());
            return;
        }
        if ("tripId".equals(key) && session.candidates != null) {
            Candidate c = pickCandidate(session, value);
            if (c != null) {
                session.slots.put("tripId", c.id);
                session.candidates = null;
                return;
            }
        }
        session.slots.put(key, value);
        if (session.verb == Verb.UPDATE) {
            session.pendingField = key;
        }
    }

    private DriverRequest toDriverRequest(Session s) {
        DriverRequest r = new DriverRequest();
        r.setName(s.slots.get("name"));
        r.setPhone(s.slots.get("phone"));
        r.setLicenseNumber(s.slots.get("licenseNumber"));
        r.setEmail(blankToNull(s.slots.get("email")));
        DriverStatus st = parseDriverStatus(s.slots.get("status"));
        r.setStatus(st != null ? st : DriverStatus.ACTIVE);
        return r;
    }

    private VehicleRequest toVehicleRequest(Session s) {
        VehicleRequest r = new VehicleRequest();
        r.setVehicleNumber(s.slots.get("vehicleNumber"));
        r.setType(parseVehicleType(s.slots.get("type")));
        r.setCapacity(Integer.parseInt(s.slots.get("capacity")));
        VehicleStatus st = parseVehicleStatus(s.slots.get("status"));
        r.setStatus(st != null ? st : VehicleStatus.AVAILABLE);
        r.setMake(blankToNull(s.slots.get("make")));
        r.setModel(blankToNull(s.slots.get("model")));
        r.setCurrentLocation(blankToNull(s.slots.get("currentLocation")));
        if (!isBlank(s.slots.get("year"))) r.setYear(Integer.parseInt(s.slots.get("year")));
        if (!isBlank(s.slots.get("odometerKm"))) r.setOdometerKm(Integer.parseInt(s.slots.get("odometerKm")));
        if (!isBlank(s.slots.get("lastServiceDate"))) r.setLastServiceDate(LocalDate.parse(s.slots.get("lastServiceDate")));
        if (!isBlank(s.slots.get("nextServiceDueKm"))) r.setNextServiceDueKm(Integer.parseInt(s.slots.get("nextServiceDueKm")));
        return r;
    }

    private RouteRequest toRouteRequest(Session s) {
        RouteRequest r = new RouteRequest();
        r.setOrigin(s.slots.get("origin"));
        r.setDestination(s.slots.get("destination"));
        r.setDistance(Double.parseDouble(s.slots.get("distance")));
        r.setEstimatedTimeMinutes(Integer.parseInt(s.slots.get("estimatedTimeMinutes")));
        r.setDescription(blankToNull(s.slots.get("description")));
        r.setActive(!"false".equalsIgnoreCase(s.slots.get("active")));
        return r;
    }

    private BookingRequest toBookingRequest(Session s) {
        BookingRequest r = new BookingRequest();
        r.setCustomerName(s.slots.get("customerName"));
        r.setCustomerPhone(s.slots.get("customerPhone"));
        r.setCustomerEmail(blankToNull(s.slots.get("customerEmail")));
        r.setTripId(UUID.fromString(s.slots.get("tripId")));
        r.setSeatCount(Integer.parseInt(s.slots.get("seatCount")));
        r.setNotes(blankToNull(s.slots.get("notes")));
        return r;
    }

    private ExpenseRequest toExpenseRequest(Session s) {
        ExpenseRequest r = new ExpenseRequest();
        r.setCategory(parseExpense(s.slots.get("category")));
        r.setAmount(new BigDecimal(s.slots.get("amount")));
        r.setExpenseDate(parseDate(s.slots.get("expenseDate")));
        r.setDescription(blankToNull(s.slots.get("description")));
        return r;
    }

    private GeofenceRequest toGeofenceRequest(Session s) {
        GeofenceRequest r = new GeofenceRequest();
        r.setName(s.slots.get("name"));
        r.setDescription(blankToNull(s.slots.get("description")));
        r.setLatitude(Double.parseDouble(s.slots.get("latitude")));
        r.setLongitude(Double.parseDouble(s.slots.get("longitude")));
        r.setRadiusMeters(Double.parseDouble(s.slots.get("radiusMeters")));
        GeofenceType t = parseGeofenceType(s.slots.get("type"));
        r.setType(t != null ? t : GeofenceType.CUSTOM);
        r.setActive(!"false".equalsIgnoreCase(s.slots.get("active")));
        return r;
    }

    private DriverRequest overlayDriver(Session s) {
        DriverResponse d = driverService.getDriverById(UUID.fromString(s.targetId));
        DriverRequest r = new DriverRequest();
        r.setName(slotOr(s, "name", d.getName()));
        r.setPhone(slotOr(s, "phone", d.getPhone()));
        r.setLicenseNumber(slotOr(s, "licenseNumber", d.getLicenseNumber()));
        r.setEmail(slotOr(s, "email", d.getEmail()));
        DriverStatus st = parseDriverStatus(s.slots.get("status"));
        r.setStatus(st != null ? st : d.getStatus());
        return r;
    }

    private VehicleRequest overlayVehicle(Session s) {
        VehicleResponse v = vehicleService.getVehicleById(UUID.fromString(s.targetId));
        VehicleRequest r = new VehicleRequest();
        r.setVehicleNumber(slotOr(s, "vehicleNumber", v.getVehicleNumber()));
        VehicleType type = parseVehicleType(s.slots.get("type"));
        r.setType(type != null ? type : v.getType());
        r.setCapacity(!isBlank(s.slots.get("capacity")) ? Integer.parseInt(s.slots.get("capacity")) : v.getCapacity());
        VehicleStatus st = parseVehicleStatus(s.slots.get("status"));
        r.setStatus(st != null ? st : v.getStatus());
        r.setMake(slotOr(s, "make", v.getMake()));
        r.setModel(slotOr(s, "model", v.getModel()));
        r.setCurrentLocation(slotOr(s, "currentLocation", v.getCurrentLocation()));
        r.setYear(!isBlank(s.slots.get("year")) ? Integer.parseInt(s.slots.get("year")) : v.getYear());
        r.setOdometerKm(!isBlank(s.slots.get("odometerKm")) ? Integer.parseInt(s.slots.get("odometerKm")) : v.getOdometerKm());
        r.setLastServiceDate(v.getLastServiceDate());
        r.setNextServiceDueKm(v.getNextServiceDueKm());
        return r;
    }

    private RouteRequest overlayRoute(Session s) {
        RouteResponse existing = routeService.getRouteById(Long.parseLong(s.targetId));
        RouteRequest r = new RouteRequest();
        r.setOrigin(slotOr(s, "origin", existing.getOrigin()));
        r.setDestination(slotOr(s, "destination", existing.getDestination()));
        r.setDistance(!isBlank(s.slots.get("distance")) ? Double.parseDouble(s.slots.get("distance")) : existing.getDistance());
        r.setEstimatedTimeMinutes(!isBlank(s.slots.get("estimatedTimeMinutes"))
                ? Integer.parseInt(s.slots.get("estimatedTimeMinutes")) : existing.getEstimatedTimeMinutes());
        r.setDescription(slotOr(s, "description", existing.getDescription()));
        r.setActive(existing.getActive());
        return r;
    }

    private BookingRequest overlayBooking(Session s) {
        BookingResponse b = bookingService.getBookingById(Long.parseLong(s.targetId));
        BookingRequest r = new BookingRequest();
        r.setCustomerName(slotOr(s, "customerName", b.getCustomerName()));
        r.setCustomerPhone(slotOr(s, "customerPhone", b.getCustomerPhone()));
        r.setCustomerEmail(slotOr(s, "customerEmail", b.getCustomerEmail()));
        r.setTripId(!isBlank(s.slots.get("tripId")) ? UUID.fromString(s.slots.get("tripId")) : b.getTripId());
        r.setSeatCount(!isBlank(s.slots.get("seatCount")) ? Integer.parseInt(s.slots.get("seatCount")) : b.getSeatCount());
        r.setNotes(slotOr(s, "notes", b.getNotes()));
        return r;
    }

    private ExpenseRequest overlayExpense(Session s) {
        ExpenseResponse e = expenseService.getAllExpenses(0, 80, null, null, null).getContent().stream()
                .filter(x -> x.getId().toString().equals(s.targetId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", s.targetId));
        ExpenseRequest r = new ExpenseRequest();
        ExpenseCategory cat = parseExpense(s.slots.get("category"));
        r.setCategory(cat != null ? cat : e.getCategory());
        r.setAmount(!isBlank(s.slots.get("amount")) ? new BigDecimal(s.slots.get("amount")) : e.getAmount());
        r.setExpenseDate(!isBlank(s.slots.get("expenseDate")) ? parseDate(s.slots.get("expenseDate")) : e.getExpenseDate());
        r.setDescription(slotOr(s, "description", e.getDescription()));
        r.setTripId(e.getTripId());
        r.setVehicleId(e.getVehicleId());
        return r;
    }

    private GeofenceRequest overlayGeofence(Session s) {
        GeofenceResponse g = geofenceService.getById(UUID.fromString(s.targetId));
        GeofenceRequest r = new GeofenceRequest();
        r.setName(slotOr(s, "name", g.getName()));
        r.setDescription(slotOr(s, "description", g.getDescription()));
        r.setLatitude(!isBlank(s.slots.get("latitude")) ? Double.parseDouble(s.slots.get("latitude")) : g.getLatitude());
        r.setLongitude(!isBlank(s.slots.get("longitude")) ? Double.parseDouble(s.slots.get("longitude")) : g.getLongitude());
        r.setRadiusMeters(!isBlank(s.slots.get("radiusMeters")) ? Double.parseDouble(s.slots.get("radiusMeters")) : g.getRadiusMeters());
        GeofenceType t = parseGeofenceType(s.slots.get("type"));
        r.setType(t != null ? t : g.getType());
        r.setActive(g.getActive());
        return r;
    }

    private static String slotOr(Session s, String key, String fallback) {
        return !isBlank(s.slots.get(key)) ? s.slots.get(key) : fallback;
    }

    private ParsedAction parseAction(String q) {
        EntityType entity = detectEntity(q);
        if (entity == null) return null;
        Verb verb = detectVerb(q);
        if (verb == null) return null;
        return new ParsedAction(verb, entity);
    }

    private Verb detectVerb(String q) {
        if (contains(q, "delete", "deleted", "remove", "removed", "drop ")) return Verb.DELETE;
        if (contains(q, "update", "edit", "change", "modify", "correct")) return Verb.UPDATE;
        if (contains(q, "add", "create", "register", "new ")) return Verb.CREATE;
        return null;
    }

    private EntityType detectEntity(String q) {
        if (contains(q, "driver")) return EntityType.DRIVER;
        if (contains(q, "geofence", "geo fence", "geo-fence", "zone")) return EntityType.GEOFENCE;
        if (contains(q, "booking", "passenger", "customer")) return EntityType.BOOKING;
        if (contains(q, "expense", "fuel bill", "toll")) return EntityType.EXPENSE;
        if (contains(q, "route", "lane")) return EntityType.ROUTE;
        if (contains(q, "vehicle", "truck", "bus", "van", "fleet unit", "lorry")) return EntityType.VEHICLE;
        return null;
    }

    private String extractLookup(Session session, String message) {
        String q = message.trim();
        if (session.verb == Verb.CREATE) return "";
        Matcher named = Pattern.compile(
                "(?:driver|vehicle|truck|bus|route|booking|geofence|expense)s?\\s+(?:named\\s+)?(.+)$",
                Pattern.CASE_INSENSITIVE).matcher(q);
        if (named.find()) {
            String hit = cleanLookup(named.group(1).replaceAll("(?i)\\b(phone|email|license|status|type|capacity)\\b.*", "").trim());
            if (!isBlank(hit)) return hit;
        }
        String entity = session.entity.name().toLowerCase(Locale.ROOT);
        Pattern a = Pattern.compile("(?:delete|deleted|remove|removed|update|edit|change)\\s+(?:the\\s+)?(.+?)\\s+" + entity, Pattern.CASE_INSENSITIVE);
        Matcher m = a.matcher(q);
        if (m.find()) return cleanLookup(m.group(1));
        String stripped = stripVerb(q);
        if (!isBlank(stripped) && stripped.length() < 80) return cleanLookup(stripped);
        if (session.entity == EntityType.VEHICLE) {
            Matcher plate = Pattern.compile("\\b([A-Za-z]{2}[-\\s]?[0-9]{1,2}[-\\s]?[A-Za-z]{1,3}[-\\s]?[0-9]{3,4}|[A-Za-z0-9\\-]{5,20})\\b").matcher(q);
            if (plate.find()) return plate.group(1);
        }
        return "";
    }

    private String cleanLookup(String s) {
        return s.replaceAll("(?i)\\b(the|a|an|please|named|called)\\b", " ").replaceAll("\\s+", " ").trim();
    }

    private String matchField(EntityType entity, String q) {
        Map<String, String> aliases = switch (entity) {
            case DRIVER -> Map.ofEntries(
                    Map.entry("name", "name"), Map.entry("phone", "phone"), Map.entry("mobile", "phone"),
                    Map.entry("license", "licenseNumber"), Map.entry("licence", "licenseNumber"), Map.entry("dl", "licenseNumber"),
                    Map.entry("email", "email"), Map.entry("mail", "email"), Map.entry("status", "status"));
            case VEHICLE -> Map.ofEntries(
                    Map.entry("number", "vehicleNumber"), Map.entry("plate", "vehicleNumber"),
                    Map.entry("type", "type"), Map.entry("capacity", "capacity"), Map.entry("status", "status"),
                    Map.entry("make", "make"), Map.entry("model", "model"), Map.entry("year", "year"),
                    Map.entry("location", "currentLocation"), Map.entry("odometer", "odometerKm"));
            case ROUTE -> Map.of("origin", "origin", "from", "origin", "destination", "destination", "to", "destination",
                    "distance", "distance", "time", "estimatedTimeMinutes", "eta", "estimatedTimeMinutes",
                    "description", "description");
            case BOOKING -> Map.of("name", "customerName", "phone", "customerPhone", "email", "customerEmail",
                    "trip", "tripId", "seat", "seatCount", "seats", "seatCount", "notes", "notes");
            case EXPENSE -> Map.of("category", "category", "amount", "amount", "date", "expenseDate", "description", "description");
            case GEOFENCE -> Map.of("name", "name", "type", "type", "radius", "radiusMeters", "location", "latitude",
                    "city", "latitude", "lat", "latitude");
        };
        for (Map.Entry<String, String> e : aliases.entrySet()) {
            if (q.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private String valueAfterField(String message, String field) {
        if (field == null) return null;
        Matcher m = Pattern.compile("(?:to|as|=|: )\\s*(.+)$", Pattern.CASE_INSENSITIVE).matcher(message.trim());
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private Candidate pickCandidate(Session session, String message) {
        String t = message.trim();
        if (t.matches("\\d+")) {
            int i = Integer.parseInt(t) - 1;
            if (i >= 0 && i < session.candidates.size()) return session.candidates.get(i);
        }
        for (Candidate c : session.candidates) {
            if (c.label.toLowerCase(Locale.ROOT).contains(t.toLowerCase(Locale.ROOT))) return c;
        }
        return null;
    }

    private void seedCity(Session session, String q) {
        double[] ll = parseLatLngOrCity(q);
        if (ll != null) {
            session.slots.put("latitude", String.valueOf(ll[0]));
            session.slots.put("longitude", String.valueOf(ll[1]));
        }
    }

    private boolean hasField(EntityType entity, String key) {
        return fields(entity).stream().anyMatch(f -> f.key.equals(key));
    }

    private AiAskResponse.Link listLink(EntityType entity) {
        return switch (entity) {
            case DRIVER -> link("/drivers", "Open driver list");
            case VEHICLE -> link("/vehicles", "Open vehicle list");
            case ROUTE -> link("/routes", "Open route list");
            case BOOKING -> link("/bookings", "Open booking list");
            case EXPENSE -> link("/expenses", "Open expense list");
            case GEOFENCE -> link("/geofences", "Open geofence list");
        };
    }

    private AiAskResponse reply(String answer, List<AiAskResponse.Link> links, List<String> suggestions) {
        boolean live = llmGateway.isConfigured();
        return AiAskResponse.builder()
                .botName(BOT)
                .answer(answer)
                .live(live)
                .usedLlm(false)
                .intent("ACTION")
                .confidence(96)
                .facts(List.of())
                .links(links == null ? List.of() : links)
                .suggestions(suggestions == null ? List.of() : suggestions)
                .build();
    }

    private static AiAskResponse.Link link(String path, String label) {
        return AiAskResponse.Link.builder().path(path).label(label).build();
    }

    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) return null;
        if ("anonymousUser".equals(auth.getName())) return null;
        return auth.getName();
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) return true;
        }
        return false;
    }

    private static boolean isCancel(String q) {
        return q.equals("cancel") || q.equals("stop") || q.equals("abort") || q.contains("never mind")
                || q.equals("forget it") || q.equals("forgetit");
    }

    private static boolean looksLikeGreeting(String q) {
        String n = q.replaceAll("[^a-z\\s]", " ").replaceAll("\\s+", " ").trim();
        if (n.isBlank() || n.length() > 40) return false;
        if (contains(n, "driver", "vehicle", "route", "booking", "expense", "geofence", "add", "delete", "update")) {
            return false;
        }
        return n.matches("hi+") || List.of("hello", "hey", "yo", "namaste").contains(n)
                || n.startsWith("good morning") || n.startsWith("good afternoon") || n.startsWith("good evening");
    }

    private static boolean isYes(String q) {
        return q.equals("yes") || q.equals("y") || q.equals("ok") || q.equals("okay") || q.equals("confirm")
                || q.equals("do it") || q.equals("go") || q.equals("proceed") || q.equals("save") || q.startsWith("yes ");
    }

    private static boolean isNo(String q) {
        return q.equals("no") || q.equals("n") || q.equals("nope") || q.equals("don't") || q.equals("dont");
    }

    private static boolean isSkip(String q) {
        return q.equals("skip") || q.equals("none") || q.equals("n/a") || q.equals("na") || q.equals("-")
                || q.equals("later") || q.equals("no email");
    }

    private static boolean isSkipRest(String q) {
        return q.contains("skip rest") || q.equals("that's all") || q.equals("thats all") || q.equals("done")
                || q.equals("finish") || q.equals("no more");
    }

    private static boolean isNoiseName(String name) {
        String n = name.toLowerCase(Locale.ROOT).trim();
        return n.isBlank() || n.equals("a") || n.equals("new") || n.equals("the") || n.startsWith("with");
    }

    private static boolean contains(String q, String... keys) {
        for (String k : keys) {
            if (q.contains(k)) return true;
        }
        return false;
    }

    private static String capture(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String stripVerb(String q) {
        return q.replaceAll("(?i)\\b(add|create|register|new|a|an|the|please|update|edit|change|delete|remove|driver|vehicle|route|booking|expense|geofence)\\b", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static String title(String s) {
        if (s == null) return "";
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase(Locale.ROOT)).append(' ');
        }
        return sb.toString().trim();
    }

    private static String normalizePhone(String v) {
        String d = v.replaceAll("[\\s-]", "");
        if (d.matches("\\d{10}")) d = "+91" + d;
        if (d.matches("91\\d{10}")) d = "+" + d;
        return d.matches("\\+?[0-9]{7,15}") ? d : null;
    }

    private static Integer parsePositive(String v) {
        try {
            int n = Integer.parseInt(v.replaceAll("[^0-9]", ""));
            return n > 0 ? n : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String parseDecimal(String v) {
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(v.replace(",", ""));
        return m.find() ? m.group(1) : null;
    }

    private static LocalDate parseDate(String v) {
        if (v == null) return null;
        String q = v.toLowerCase(Locale.ROOT).trim();
        if (q.equals("today") || q.equals("now")) return LocalDate.now();
        try {
            return LocalDate.parse(q);
        } catch (Exception e) {
            return null;
        }
    }

    private static VehicleType parseVehicleType(String v) {
        if (v == null) return null;
        String q = v.toLowerCase(Locale.ROOT);
        if (q.contains("mini")) return VehicleType.MINI_BUS;
        if (q.contains("bus")) return VehicleType.BUS;
        if (q.contains("van")) return VehicleType.VAN;
        if (q.contains("truck") || q.contains("lorry") || q.contains("trk")) return VehicleType.TRUCK;
        try {
            return VehicleType.valueOf(v.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (Exception e) {
            return null;
        }
    }

    private static VehicleStatus parseVehicleStatus(String v) {
        if (v == null) return null;
        String q = v.toLowerCase(Locale.ROOT);
        if (q.contains("avail") || q.contains("free")) return VehicleStatus.AVAILABLE;
        if (q.contains("busy") || q.contains("on trip")) return VehicleStatus.BUSY;
        if (q.contains("maint") || q.contains("workshop")) return VehicleStatus.MAINTENANCE;
        try {
            return VehicleStatus.valueOf(v.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static DriverStatus parseDriverStatus(String v) {
        if (v == null) return null;
        String q = v.toLowerCase(Locale.ROOT);
        if (q.contains("inactive") || q.equals("off")) return DriverStatus.INACTIVE;
        if (q.contains("active") || q.equals("on")) return DriverStatus.ACTIVE;
        try {
            return DriverStatus.valueOf(v.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static ExpenseCategory parseExpense(String v) {
        if (v == null) return null;
        String q = v.toLowerCase(Locale.ROOT);
        if (q.contains("fuel") || q.contains("diesel")) return ExpenseCategory.FUEL;
        if (q.contains("toll")) return ExpenseCategory.TOLL;
        if (q.contains("maint") || q.contains("repair")) return ExpenseCategory.MAINTENANCE;
        if (q.contains("allowance") || q.contains("bata")) return ExpenseCategory.DRIVER_ALLOWANCE;
        if (q.contains("other")) return ExpenseCategory.OTHER;
        try {
            return ExpenseCategory.valueOf(v.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (Exception e) {
            return null;
        }
    }

    private static GeofenceType parseGeofenceType(String v) {
        if (v == null) return null;
        String q = v.toLowerCase(Locale.ROOT);
        if (q.contains("depot") || q.contains("yard")) return GeofenceType.DEPOT;
        if (q.contains("restrict")) return GeofenceType.RESTRICTED_ZONE;
        if (q.contains("deliver")) return GeofenceType.DELIVERY_ZONE;
        if (q.contains("custom")) return GeofenceType.CUSTOM;
        try {
            return GeofenceType.valueOf(v.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (Exception e) {
            return null;
        }
    }

    private static double[] parseLatLngOrCity(String v) {
        if (v == null) return null;
        String q = v.toLowerCase(Locale.ROOT).trim();
        for (Map.Entry<String, double[]> e : CITIES.entrySet()) {
            if (q.contains(e.getKey())) return e.getValue();
        }
        Matcher m = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)").matcher(v);
        if (m.find()) {
            return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))};
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s;
    }

    private static String nullTo(String s, String d) {
        return isBlank(s) ? d : s;
    }

    private static String display(String key, String v) {
        if ("tripId".equals(key) && v.length() > 8) return v.substring(0, 8) + "…";
        return v;
    }

    private static Field field(String key, String label, String prompt, boolean required, String... hints) {
        return new Field(key, label, prompt, required, List.of(hints));
    }

    private enum Verb {
        CREATE("Adding"), UPDATE("Updating"), DELETE("Deleting");
        final String label;
        Verb(String label) { this.label = label; }
    }

    private enum EntityType {
        DRIVER("driver"), VEHICLE("vehicle"), ROUTE("route"),
        BOOKING("booking"), EXPENSE("expense"), GEOFENCE("geofence");
        final String label;
        EntityType(String label) { this.label = label; }
    }

    private record ParsedAction(Verb verb, EntityType entity) {}
    private record Candidate(String id, String label) {}
    private record Field(String key, String label, String prompt, boolean required, List<String> hints) {}

    private static final class Session {
        final Verb verb;
        final EntityType entity;
        final Map<String, String> slots = new LinkedHashMap<>();
        String lookupHint;
        String targetId;
        String targetLabel;
        String pendingField;
        String lastError;
        List<Candidate> candidates;
        boolean confirmed;
        Instant touched = Instant.now();

        Session(Verb verb, EntityType entity) {
            this.verb = verb;
            this.entity = entity;
        }

        void touch() {
            touched = Instant.now();
        }

        boolean expired() {
            return Instant.now().isAfter(touched.plusSeconds(SESSION_TTL_SECONDS));
        }
    }
}
