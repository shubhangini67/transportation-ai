package com.tms.service;

import com.tms.dto.request.AiAskRequest;
import com.tms.dto.request.AiChatTurn;
import com.tms.dto.response.AiAskResponse;
import com.tms.dto.response.DispatchPlanResponse;
import com.tms.dto.response.DriverScorecardResponse;
import com.tms.dto.response.FuelAnalyticsResponse;
import com.tms.dto.response.MaintenanceAlertResponse;
import com.tms.dto.response.OperationsAlertResponse;
import com.tms.entity.Route;
import com.tms.entity.Vehicle;
import com.tms.enums.TripStatus;
import com.tms.enums.VehicleStatus;
import com.tms.repository.RouteRepository;
import com.tms.repository.TripRepository;
import com.tms.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCopilotService {

    private static final String BOT_NAME = "Copilot";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "Add a new driver",
            "Delete a vehicle",
            "Which trips are delayed?",
            "Who should I dispatch Delhi to Jaipur?"
    );

    private final OperationsService operationsService;
    private final MaintenanceService maintenanceService;
    private final DispatchService dispatchService;
    private final DriverPerformanceService driverPerformanceService;
    private final FuelAnalyticsService fuelAnalyticsService;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final LlmGateway llmGateway;
    private final AiActionService aiActionService;

    public AiAskResponse status() {
        boolean live = llmGateway.isConfigured();
        return toResponse(
                live ? "Copilot is online." : "Copilot is on saved answers until live chat reconnects.",
                live,
                "STATUS",
                100,
                List.of(),
                List.of(),
                DEFAULT_SUGGESTIONS);
    }

    public AiAskResponse briefing(String pagePath) {
        AiAskRequest request = new AiAskRequest();
        request.setMessage("briefing");
        request.setPagePath(pagePath);
        return ask(request);
    }

    @Transactional
    public AiAskResponse ask(AiAskRequest request) {
        String question = request.getMessage().trim();
        Optional<AiAskResponse> action = aiActionService.tryHandle(question);
        if (action.isPresent()) {
            return action.get();
        }

        Intent intent = classify(question.toLowerCase(Locale.ROOT));
        Draft draft = switch (intent) {
            case OPERATIONS -> operationsDraft();
            case MAINTENANCE -> maintenanceDraft();
            case DISPATCH -> dispatchDraft(question);
            case TRACKING -> trackingDraft();
            case SCORECARD -> scorecardDraft();
            case FUEL -> fuelDraft();
            case RATES -> ratesDraft();
            case IDENTITY -> identityDraft();
            case CHAT -> chatDraft();
            case GREETING -> greetingDraft(question);
            case BRIEFING -> briefingDraft();
        };

        boolean usedLlm = false;
        String answer = draft.answer;
        if (llmGateway.isConfigured()) {
            Optional<String> llm = llmGateway.complete(buildPrompt(intent, question, request, draft));
            if (llm.isPresent()) {
                answer = llm.get();
                usedLlm = true;
            }
        }

        return toResponse(answer, usedLlm, intent.name(), draft.confidence, draft.facts, draft.links, draft.suggestions);
    }

    public AiAskResponse resetSession() {
        aiActionService.clearSession();
        return status();
    }

    private String buildPrompt(Intent intent, String question, AiAskRequest request, Draft draft) {
        String history = "";
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            history = request.getHistory().stream()
                    .limit(6)
                    .map(t -> safeRole(t) + ": " + nullToEmpty(t.getContent()))
                    .collect(Collectors.joining("\n"));
        }
        String mode = switch (intent) {
            case GREETING -> """
                    The user said hi / hello / good evening. You are Copilot on Transportation AI.
                    Greet them back in a natural way (Hi / Hello / Good evening). 2-4 short sentences.
                    You may mention one live number from FACTS. Do NOT paste a SHIFT HANDOVER or ACT FIRST list.
                    Never mention Groq, OpenAI, Gemini, Ollama, API keys, or model names.
                    """;
            case BRIEFING -> """
                    The user asked for the live shift board (briefing / status / shift board). Dump FACTS as a handover.
                    Never mention Groq, OpenAI, models, or API keys.
                    """;
            case IDENTITY -> """
                    The user asked who you are. You are Copilot on Transportation AI. You can create/update/delete fleet records by asking form fields in order, then you give a list link.
                    Also live trips, GPS, dispatch. 4-6 sentences. Do not mention Groq, OpenAI, API keys, or model names.
                    """;
            case CHAT -> """
                    Answer the user's actual question as Copilot on Transportation AI. If it is not about the fleet, answer briefly then offer fleet help.
                    Do NOT dump a shift handover unless they asked for status. Never invent plates.
                    Never mention Groq, OpenAI, Gemini, Ollama, API keys, or model names.
                    """;
            default -> """
                    Answer the user's operational question using FACTS and the DRAFT. Do not invent vehicles or numbers.
                    Short bullets. One next action.
                    """;
        };
        return """
                %s
                User page: %s

                LIVE FACTS (only use if relevant; do not invent extra rows):
                %s

                DRAFT (fallback if facts are enough):
                %s

                RECENT CHAT:
                %s

                USER QUESTION (answer THIS):
                %s
                """.formatted(
                mode,
                request.getPagePath() == null || request.getPagePath().isBlank() ? "/dashboard" : request.getPagePath(),
                draft.facts.isEmpty() ? "(none)" : String.join("\n", draft.facts),
                draft.answer,
                history.isBlank() ? "(none)" : history,
                question
        );
    }

    private Intent classify(String q) {
        if (isPoliteGreeting(q)) return Intent.GREETING;
        if (isBriefingTrigger(q)) return Intent.BRIEFING;
        if (isIdentity(q)) return Intent.IDENTITY;
        if (contains(q, "delay", "overdue", "late", "exception", "stuck", "sla")) return Intent.OPERATIONS;
        if (contains(q, "service", "maintenance", "workshop", "odometer", "repair")) return Intent.MAINTENANCE;
        if (contains(q, "dispatch", "assign", "who should", "recommend", "pair", "lane")) return Intent.DISPATCH;
        if (contains(q, "where", "gps", "track", "live", "map", "location", "en route")) return Intent.TRACKING;
        if (contains(q, "score", "best driver", "on-time", "scorecard", "performance")) return Intent.SCORECARD;
        if (contains(q, "fuel", "diesel", "cost per km", "mileage")) return Intent.FUEL;
        if (contains(q, "rate", "gst", "quote", "freight", "tariff")) return Intent.RATES;
        if (contains(q, "status", "briefing", "handover", "shift board", "what's happening", "whats happening", "overview")) {
            return Intent.BRIEFING;
        }
        return Intent.CHAT;
    }

    private boolean isIdentity(String q) {
        return contains(q, "who are you", "who r you", "who r u", "what are you", "what r you",
                "what's your name", "whats your name", "your name", "introduce yourself",
                "what can you do", "what do you do", "who is this", "are you a bot", "are you chatgpt",
                "are you grok", "are you gemini");
    }

    private boolean isPoliteGreeting(String q) {
        String n = normalize(q);
        if (n.isBlank() || n.length() > 48) return false;
        if (contains(n, "delay", "dispatch", "trip", "truck", "fuel", "service", "gps", "rate", "gst", "briefing", "handover")) {
            return false;
        }
        String first = n.split(" ")[0];
        return first.matches("hi+") || List.of("hello", "hey", "yo", "namaste", "namaskar").contains(first)
                || n.startsWith("good morning") || n.startsWith("good afternoon") || n.startsWith("good evening")
                || n.startsWith("good eve") || n.startsWith("whats up") || n.startsWith("what is up");
    }

    private boolean isBriefingTrigger(String q) {
        String n = normalize(q);
        if (n.isBlank() || n.length() > 80) return false;
        return n.equals("briefing") || n.equals("handover") || n.equals("start")
                || n.contains("shift board") || n.equals("status")
                || n.startsWith("show briefing") || n.startsWith("show the board");
    }

    private static String normalize(String q) {
        return q.replaceAll("[^a-z\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private Draft greetingDraft(String question) {
        String n = normalize(question.toLowerCase(Locale.ROOT));
        String first = n.isBlank() ? "hi" : n.split(" ")[0];
        String hello = first.matches("hi+") || n.startsWith("hey") || n.equals("yo") ? "Hi"
                : n.contains("morning") ? "Good morning"
                : n.contains("afternoon") ? "Good afternoon"
                : (n.contains("evening") || n.contains("eve")) ? "Good evening"
                : "Hello";
        List<String> facts = snapshotFacts();
        String answer = hello + ". I'm Copilot — I can fill forms for you. "
                + "Say add a driver, update a vehicle, or delete a route, and I will ask each field in order.";
        return new Draft(answer, facts, List.of(link("/operations", "Operations"), link("/fleet-map", "Live tracking")),
                DEFAULT_SUGGESTIONS, 86);
    }

    private Draft identityDraft() {
        String answer = """
                I'm Copilot, the operations assistant on Transportation AI.
                I can add, update, or delete drivers, vehicles, routes, bookings, expenses, and geofences — I ask the same fields as the form, then save, then give you a link to check the list.
                I also read live trips, GPS, workshop, scorecards, and I can recommend who to dispatch.
                Try: add a new driver. Or ask about delays.
                """;
        return new Draft(answer.trim(), snapshotFacts(),
                List.of(link("/operations", "Operations")), DEFAULT_SUGGESTIONS, 88);
    }

    private Draft chatDraft() {
        String answer = llmGateway.isConfigured()
                ? "Answer the user in normal sentences as Copilot."
                : "I can help with this fleet — delays, dispatch, GPS, workshop. Ask in normal language.";
        return new Draft(answer, snapshotFacts(), List.of(
                link("/operations", "Operations"),
                link("/dispatch", "Smart Dispatch")
        ), DEFAULT_SUGGESTIONS, 70);
    }

    private List<String> snapshotFacts() {
        List<String> facts = new ArrayList<>();
        facts.add("In progress " + tripRepository.countByStatus(TripStatus.IN_PROGRESS));
        facts.add("Planned " + tripRepository.countByStatus(TripStatus.PLANNED));
        facts.add("SLA exceptions " + operationsService.getAlerts().size());
        facts.add("Maintenance alerts " + maintenanceService.getAlerts().size());
        return facts;
    }

    private Draft briefingDraft() {
        long planned = tripRepository.countByStatus(TripStatus.PLANNED);
        long live = tripRepository.countByStatus(TripStatus.IN_PROGRESS);
        List<OperationsAlertResponse> ops = operationsService.getAlerts();
        List<MaintenanceAlertResponse> maint = maintenanceService.getAlerts();
        List<Vehicle> gps = vehicleRepository.findAll().stream()
                .filter(v -> v.getLatitude() != null && v.getStatus() == VehicleStatus.BUSY)
                .toList();
        List<DriverScorecardResponse> cards = new ArrayList<>(driverPerformanceService.scorecards());
        cards.sort(Comparator.comparingInt(DriverScorecardResponse::getScore).reversed());

        List<String> facts = new ArrayList<>();
        facts.add("Clock " + LocalDateTime.now().format(CLOCK));
        facts.add("In progress " + live);
        facts.add("Planned " + planned);
        facts.add("SLA exceptions " + ops.size());
        facts.add("Maintenance alerts " + maint.size());
        facts.add("GPS live vehicles " + gps.size());
        ops.stream().limit(4).forEach(a -> facts.add("EXC " + a.getSeverity() + " " + a.getVehicleNumber()
                + " " + a.getRouteLabel() + " " + a.getCode() + " " + a.getMinutesOverdue() + " min · " + a.getDriverName()));
        maint.stream().limit(3).forEach(a -> facts.add("MAINT " + a.getSeverity() + " " + a.getVehicleNumber() + " " + a.getMessage()));
        gps.stream().limit(4).forEach(v -> facts.add("GPS " + v.getVehicleNumber() + " " + v.getCurrentLocation()));
        if (!cards.isEmpty()) {
            facts.add("TOP DRIVER " + cards.get(0).getDriverName() + " score " + cards.get(0).getScore()
                    + " on-time " + cards.get(0).getOnTimePercent() + "%");
        }

        StringBuilder answer = new StringBuilder();
        answer.append("SHIFT HANDOVER · ").append(LocalDateTime.now().format(CLOCK)).append('\n');
        answer.append("Live trips: ").append(live).append("   Planned: ").append(planned)
                .append("   SLA exceptions: ").append(ops.size())
                .append("   Maintenance: ").append(maint.size()).append('\n');
        if (ops.isEmpty()) {
            answer.append("No delayed / overdue trips right now.\n");
        } else {
            answer.append("ACT FIRST\n");
            ops.stream().limit(3).forEach(a -> answer.append("• ")
                    .append(a.getVehicleNumber()).append("  ").append(a.getRouteLabel())
                    .append("  ").append(a.getCode().replace('_', ' '))
                    .append("  +").append(a.getMinutesOverdue()).append(" min  (")
                    .append(a.getDriverName()).append(")\n"));
        }
        if (!maint.isEmpty()) {
            answer.append("WORKSHOP\n");
            maint.stream().limit(2).forEach(a -> answer.append("• ")
                    .append(a.getVehicleNumber()).append(" — ").append(a.getMessage()).append('\n'));
        }
        if (!gps.isEmpty()) {
            answer.append("ON THE ROAD\n");
            gps.stream().limit(3).forEach(v -> answer.append("• ")
                    .append(v.getVehicleNumber()).append("  ")
                    .append(v.getCurrentLocation() == null ? "GPS fix" : v.getCurrentLocation()).append('\n'));
        }
        if (!cards.isEmpty()) {
            answer.append("BEST DRIVER  ").append(cards.get(0).getDriverName())
                    .append("  band ").append(cards.get(0).getBand())
                    .append("  on-time ").append(cards.get(0).getOnTimePercent()).append("%\n");
        }
        answer.append("Say a lane (Delhi to Jaipur), delays, or who needs service.");

        return new Draft(answer.toString().trim(), facts, List.of(
                link("/operations", "Operations"),
                link("/fleet-map", "Live tracking"),
                link("/dispatch", "Smart Dispatch"),
                link("/maintenance", "Maintenance")
        ), DEFAULT_SUGGESTIONS, 90);
    }

    private Draft operationsDraft() {
        List<OperationsAlertResponse> alerts = operationsService.getAlerts();
        List<String> facts = alerts.stream()
                .limit(6)
                .map(a -> a.getSeverity() + " " + a.getCode() + " · " + a.getVehicleNumber()
                        + " · " + a.getRouteLabel() + " · " + a.getMinutesOverdue() + " min · " + a.getDriverName())
                .toList();
        String answer;
        if (alerts.isEmpty()) {
            answer = "SLA is clean — no overdue starts or delayed in-progress trips.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(alerts.size()).append(" exception(s). Highest: ")
                    .append(alerts.get(0).getVehicleNumber()).append(" on ")
                    .append(alerts.get(0).getRouteLabel()).append(" (")
                    .append(alerts.get(0).getCode().replace('_', ' ')).append(", +")
                    .append(alerts.get(0).getMinutesOverdue()).append(" min).\n");
            alerts.stream().limit(4).forEach(a -> sb.append("• ").append(a.getVehicleNumber())
                    .append("  ").append(a.getRouteLabel()).append("  +").append(a.getMinutesOverdue()).append(" min\n"));
            sb.append("Open Operations, then Live tracking for that plate.");
            answer = sb.toString().trim();
        }
        return new Draft(answer, facts, List.of(
                link("/operations", "Operations board"),
                link("/fleet-map", "Live tracking")
        ), List.of("Where is the live fleet?", "Which trucks need service?"), alerts.isEmpty() ? 80 : 94);
    }

    private Draft maintenanceDraft() {
        List<MaintenanceAlertResponse> alerts = maintenanceService.getAlerts();
        List<String> facts = alerts.stream()
                .limit(6)
                .map(a -> a.getSeverity() + " · " + a.getVehicleNumber() + " · " + a.getMessage())
                .toList();
        long overdue = alerts.stream().filter(a -> "OVERDUE".equals(a.getSeverity())).count();
        String answer;
        if (alerts.isEmpty()) {
            answer = "Workshop queue is empty. No vehicle is in the service window.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(overdue).append(" OVERDUE, ").append(alerts.size() - overdue).append(" due soon.\n");
            alerts.stream().limit(4).forEach(a -> sb.append("• ").append(a.getVehicleNumber())
                    .append("  ").append(a.getSeverity()).append(" — ").append(a.getMessage()).append('\n'));
            answer = sb.toString().trim();
        }
        return new Draft(answer, facts, List.of(link("/maintenance", "Maintenance board"), link("/vehicles", "Vehicles")),
                List.of("Which trips are delayed?", "Who is the best driver?"), 91);
    }

    private Draft dispatchDraft(String question) {
        Route route = matchRoute(question).orElseGet(() ->
                routeRepository.findByActiveTrue().stream().findFirst().orElse(null));
        if (route == null) {
            return new Draft("No active routes loaded — cannot rank a pair yet.",
                    List.of(), List.of(link("/dispatch", "Smart Dispatch")), DEFAULT_SUGGESTIONS, 40);
        }
        try {
            DispatchPlanResponse plan = dispatchService.recommend(route.getId(), 8);
            List<String> facts = new ArrayList<>();
            facts.add("Lane: " + plan.getRouteLabel());
            facts.add("Engine: " + plan.getEngine());
            plan.getRecommendations().stream().limit(3).forEach(r ->
                    facts.add("#" + r.getRank() + " score " + r.getScore() + " · " + r.getVehicleNumber()
                            + " + " + r.getDriverName() + " · " + r.getReason()));
            String answer;
            if (plan.getRecommendations().isEmpty()) {
                answer = "No free vehicle+driver pair for " + plan.getRouteLabel() + ". Free a BUSY truck or wait for a completion.";
            } else {
                DispatchPlanResponse.DispatchRecommendation top = plan.getRecommendations().get(0);
                StringBuilder sb = new StringBuilder();
                sb.append(plan.getRouteLabel()).append('\n');
                sb.append("Recommend  ").append(top.getVehicleNumber()).append("  +  ").append(top.getDriverName())
                        .append("  (score ").append(top.getScore()).append(")\n");
                sb.append(top.getReason()).append('\n');
                plan.getRecommendations().stream().skip(1).limit(2).forEach(r ->
                        sb.append("Alt ").append(r.getRank()).append(": ").append(r.getVehicleNumber())
                                .append(" + ").append(r.getDriverName()).append(" (").append(r.getScore()).append(")\n"));
                sb.append("Confirm on Smart Dispatch to create the trip. Engine: ").append(plan.getEngine());
                answer = sb.toString().trim();
            }
            return new Draft(answer, facts, List.of(link("/dispatch", "Smart Dispatch")),
                    List.of("Which trips are delayed?", "Quote GST for Delhi to Jaipur"), 88);
        } catch (Exception ex) {
            return new Draft("Dispatch optimizer did not respond. Open Smart Dispatch and pick the lane there.",
                    List.of(), List.of(link("/dispatch", "Smart Dispatch")), DEFAULT_SUGGESTIONS, 55);
        }
    }

    private Draft trackingDraft() {
        List<Vehicle> live = vehicleRepository.findAll().stream()
                .filter(v -> v.getLatitude() != null && v.getStatus() == VehicleStatus.BUSY)
                .toList();
        List<String> facts = live.stream().limit(6)
                .map(v -> v.getVehicleNumber() + " · " + v.getCurrentLocation()
                        + " · " + String.format(Locale.US, "%.3f,%.3f", v.getLatitude(), v.getLongitude()))
                .toList();
        long inProgress = tripRepository.countByStatus(TripStatus.IN_PROGRESS);
        StringBuilder sb = new StringBuilder();
        sb.append(inProgress).append(" trip(s) in progress. ").append(live.size()).append(" BUSY vehicles with GPS.\n");
        live.stream().limit(5).forEach(v -> sb.append("• ").append(v.getVehicleNumber()).append("  ")
                .append(v.getCurrentLocation() == null ? "en route" : v.getCurrentLocation()).append('\n'));
        sb.append("Open Live tracking for pulsing pins on the India map.");
        return new Draft(sb.toString().trim(), facts,
                List.of(link("/fleet-map", "Live tracking"), link("/trips?status=IN_PROGRESS", "Active trips")),
                List.of("Which trips are delayed?", "Who should I dispatch Delhi to Jaipur?"), 86);
    }

    private Draft scorecardDraft() {
        List<DriverScorecardResponse> cards = new ArrayList<>(driverPerformanceService.scorecards());
        cards.sort(Comparator.comparingInt(DriverScorecardResponse::getScore).reversed());
        List<String> facts = cards.stream().limit(5)
                .map(c -> c.getBand() + " " + c.getScore() + " · " + c.getDriverName()
                        + " · on-time " + c.getOnTimePercent() + "% · delayed " + c.getDelayedTrips())
                .toList();
        String answer;
        if (cards.isEmpty()) {
            answer = "No driver scorecards yet.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Leaderboard (on-time vs route ETA + fuel on completed trips)\n");
            cards.stream().limit(5).forEach(c -> sb.append("• ").append(c.getDriverName())
                    .append("  ").append(c.getBand()).append("  ").append(c.getScore())
                    .append("  on-time ").append(c.getOnTimePercent()).append("%\n"));
            answer = sb.toString().trim();
        }
        return new Draft(answer, facts, List.of(link("/scorecards", "Scorecards")),
                List.of("Who should I dispatch Delhi to Jaipur?", "Which trucks need service?"), 87);
    }

    private Draft fuelDraft() {
        try {
            FuelAnalyticsResponse fuel = fuelAnalyticsService.getAnalytics(LocalDate.now().minusDays(30), LocalDate.now());
            List<String> facts = List.of(
                    "Spend ₹" + fuel.getTotalFuelSpend(),
                    "Avg ₹/km " + fuel.getAverageCostPerKm(),
                    "Distance " + fuel.getTotalDistanceKm() + " km"
            );
            String answer = "Fuel last 30 days: ₹" + fuel.getTotalFuelSpend()
                    + " over " + fuel.getTotalDistanceKm() + " km (₹" + fuel.getAverageCostPerKm()
                    + "/km). Open Fuel analytics for the outliers.";
            return new Draft(answer, facts, List.of(link("/fuel-analytics", "Fuel analytics")),
                    List.of("Which trucks need service?", "Quote GST for Delhi to Jaipur"), 84);
        } catch (Exception ex) {
            return new Draft("Could not roll up 30-day fuel. Open Fuel analytics.",
                    List.of(), List.of(link("/fuel-analytics", "Fuel analytics")), DEFAULT_SUGGESTIONS, 60);
        }
    }

    private Draft ratesDraft() {
        return new Draft(
                "Tariff = max(distance × per-km, minimum freight), then GST 18%.\nOpen Freight rates → Delhi → Jaipur → TRUCK → Quote GST.",
                List.of("GST 18% on taxable freight", "Quote floors at min charge"),
                List.of(link("/rates", "Freight rates")),
                List.of("Who should I dispatch Delhi to Jaipur?"),
                82);
    }

    private Optional<Route> matchRoute(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        return routeRepository.findByActiveTrue().stream()
                .filter(r -> r.getOrigin() != null && r.getDestination() != null)
                .filter(r -> q.contains(r.getOrigin().toLowerCase(Locale.ROOT))
                        && q.contains(r.getDestination().toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    private static boolean contains(String q, String... keys) {
        for (String k : keys) {
            if (q.contains(k)) return true;
        }
        return false;
    }

    private static String safeRole(AiChatTurn turn) {
        if (turn == null || turn.getRole() == null) return "user";
        return "assistant".equalsIgnoreCase(turn.getRole()) ? "assistant" : "user";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static AiAskResponse.Link link(String path, String label) {
        return AiAskResponse.Link.builder().path(path).label(label).build();
    }

    private AiAskResponse toResponse(
            String answer,
            boolean live,
            String intent,
            int confidence,
            List<String> facts,
            List<AiAskResponse.Link> links,
            List<String> suggestions) {
        return AiAskResponse.builder()
                .botName(BOT_NAME)
                .answer(answer)
                .live(live)
                .usedLlm(live)
                .intent(intent)
                .confidence(confidence)
                .facts(facts == null ? List.of() : facts)
                .links(links == null ? List.of() : links)
                .suggestions(suggestions == null ? DEFAULT_SUGGESTIONS : suggestions)
                .build();
    }

    private enum Intent { OPERATIONS, MAINTENANCE, DISPATCH, TRACKING, SCORECARD, FUEL, RATES, BRIEFING, GREETING, IDENTITY, CHAT }

    private record Draft(String answer, List<String> facts, List<AiAskResponse.Link> links, List<String> suggestions, int confidence) {}
}
