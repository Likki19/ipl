package com.indium.assignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indium.assignment.entity.*;
import com.indium.assignment.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchDataService {

    private static final Logger log = LoggerFactory.getLogger(MatchDataService.class);

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OfficialRepository officialRepository;

    @Autowired
    private PowerplayRepository powerplayRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void uploadJsonFile(MultipartFile file) throws IOException {
        log.info("Starting transaction for file: {}", file.getOriginalFilename());
        try {
            JsonNode rootNode = objectMapper.readTree(file.getInputStream());
            log.info("Parsed JSON structure: {}", rootNode.toPrettyString());

            Match match = parseMatchData(rootNode);
            log.info("Parsed match data: {}", match);
            match = matchRepository.save(match);
            log.info("Saved match with ID: {}", match.getMatchId());

            List<Team> teams = parseTeamsData(rootNode, match);
            log.info("Parsed teams data: {}", teams);
            List<Team> savedTeams = teamRepository.saveAll(teams);
            log.info("Saved teams: {}", savedTeams);

            List<Player> allPlayers = new ArrayList<>();
            for (Team team : savedTeams) {
                allPlayers.addAll(team.getPlayers());
            }
            List<Player> savedPlayers = playerRepository.saveAll(allPlayers);
            log.info("Saved players: count = {}", savedPlayers.size());

            // ... (rest of the method remains the same)
        } catch (Exception e) {
            log.error("Error processing file, rolling back transaction: ", e);
            throw e;
        }
    }

    private Match parseMatchData(JsonNode rootNode) {
        log.debug("Parsing match data");
        JsonNode infoNode = rootNode.path("info");

        Match match = new Match();
        match.setCity(getTextValue(infoNode, "city"));
        match.setDates(parseDates(infoNode.path("dates")));
        parseEventDetails(infoNode.path("event"), match);
        match.setMatchType(getTextValue(infoNode, "match_type"));
        match.setGender(getTextValue(infoNode, "gender"));
        match.setSeason(getTextValue(infoNode, "season"));
        parseTossDetails(infoNode.path("toss"), match);
        parseOutcomeDetails(infoNode.path("outcome"), match);
        match.setOvers(infoNode.path("overs").asInt());
        match.setPlayerOfMatch(parsePlayerOfMatch(infoNode.path("player_of_match")));

        log.debug("Parsed match data: id={}, city={}, date={}", match.getMatchId(), match.getCity(), match.getDates());
        return match;
    }

    private String getTextValue(JsonNode node, String fieldName) {
        return node.path(fieldName).asText("");
    }

    private LocalDateTime parseDates(JsonNode datesNode) {
        if (datesNode.isArray() && datesNode.size() > 0) {
            String dateStr = datesNode.get(0).asText();
            return LocalDateTime.parse(dateStr + "T00:00:00");
        }
        return null;
    }

    private void parseEventDetails(JsonNode eventNode, Match match) {
        match.setMatchNumber(eventNode.path("match_number").asInt());
        match.setEventName(getTextValue(eventNode, "name"));
    }

    private void parseTossDetails(JsonNode tossNode, Match match) {
        match.setTossWinner(getTextValue(tossNode, "winner"));
        match.setTossDecision(getTextValue(tossNode, "decision"));
    }

    private void parseOutcomeDetails(JsonNode outcomeNode, Match match) {
        match.setWinner(getTextValue(outcomeNode, "winner"));
        match.setOutcomeByWickets(outcomeNode.path("by").path("wickets").asInt());
    }

    private String parsePlayerOfMatch(JsonNode playerOfMatchNode) {
        if (playerOfMatchNode.isArray() && playerOfMatchNode.size() > 0) {
            return playerOfMatchNode.get(0).asText();
        }
        return "";
    }

    private List<Team> parseTeamsData(JsonNode rootNode, Match match) {
        log.debug("Parsing teams and players data");
        List<Team> teams = new ArrayList<>();
        JsonNode teamsNode = rootNode.path("info").path("players");
        log.debug("Teams node: {}", teamsNode);

        if (teamsNode.isObject()) {
            teamsNode.fields().forEachRemaining(entry -> {
                String teamName = entry.getKey();
                JsonNode playersNode = entry.getValue();

                Team team = new Team();
                team.setTeamName(teamName);
                team.setMatch(match);
                log.debug("Parsing team: {}", teamName);

                List<Player> players = new ArrayList<>();
                if (playersNode.isArray()) {
                    for (JsonNode playerNode : playersNode) {
                        Player player = new Player();
                        player.setPlayerName(playerNode.asText());
                        player.setTeam(team);
                        player.setMatch(match);
                        players.add(player);
                        log.debug("Player added: {}", player.getPlayerName());
                    }
                } else {
                    log.warn("Players node for team {} is not an array", teamName);
                }

                team.setPlayers(players);
                teams.add(team);
            });
        } else {
            log.warn("Teams node is not an object or is missing");
        }

        log.debug("Teams parsed: count = {}", teams.size());
        return teams;
    }

    private List<Delivery> parseDeliveriesData(JsonNode rootNode, Match match) {
        log.debug("Parsing deliveries data");
        List<Delivery> deliveries = new ArrayList<>();
        JsonNode inningsNode = rootNode.path("innings");
        log.debug("Innings node: {}", inningsNode);

        if (inningsNode.isArray()) {
            for (JsonNode inning : inningsNode) {
                JsonNode overs = inning.path("overs");
                for (JsonNode over : overs) {
                    int overNumber = over.path("over").asInt();
                    JsonNode deliveriesNode = over.path("deliveries");
                    for (JsonNode deliveryNode : deliveriesNode) {
                        Delivery delivery = new Delivery();
                        delivery.setOverNumber(overNumber);
                        delivery.setBallNumber(deliveryNode.path("ball").asInt());
                        delivery.setBatter(deliveryNode.path("batter").asText());
                        delivery.setBowler(deliveryNode.path("bowler").asText());
                        delivery.setNonStriker(deliveryNode.path("non_striker").asText());

                        JsonNode runsNode = deliveryNode.path("runs");
                        delivery.setRunsBatter(runsNode.path("batter").asInt());
                        delivery.setRunsExtras(runsNode.path("extras").asInt());
                        delivery.setRunsTotal(runsNode.path("total").asInt());

                        delivery.setMatch(match);
                        deliveries.add(delivery);
                    }
                }
            }
        } else {
            log.warn("Innings node is not an array or is missing");
        }

        log.debug("Deliveries parsed: count = {}", deliveries.size());
        return deliveries;
    }

    private List<Official> parseOfficialsData(JsonNode rootNode, Match match) {
        log.debug("Parsing officials data");
        List<Official> officials = new ArrayList<>();
        JsonNode officialsNode = rootNode.path("info").path("officials");
        log.debug("Officials node: {}", officialsNode);

        if (officialsNode.isObject()) {
            for (String officialType : new String[]{"umpires", "referee"}) {
                JsonNode officialTypeNode = officialsNode.path(officialType);
                if (officialTypeNode.isArray()) {
                    for (JsonNode officialNode : officialTypeNode) {
                        Official official = new Official();
                        official.setOfficialType(officialType);
                        official.setOfficialName(officialNode.asText());
                        official.setMatch(match);
                        officials.add(official);
                        log.debug("Official parsed: {} - {}", officialType, official.getOfficialName());
                    }
                }
            }
        } else {
            log.warn("Officials node is not an object or is missing");
        }

        log.debug("Officials parsed: count = {}", officials.size());
        return officials;
    }

    private List<Powerplay> parsePowerplaysData(JsonNode rootNode, Match match) {
        log.debug("Parsing powerplays data");
        List<Powerplay> powerplays = new ArrayList<>();
        JsonNode inningsNode = rootNode.path("innings");
        log.debug("Innings node: {}", inningsNode);

        if (inningsNode.isArray()) {
            for (JsonNode inning : inningsNode) {
                JsonNode powerplaysNode = inning.path("powerplays");
                if (powerplaysNode.isArray()) {
                    for (JsonNode powerplayNode : powerplaysNode) {
                        Powerplay powerplay = new Powerplay();
                        powerplay.setFromOver(powerplayNode.path("from").asDouble());
                        powerplay.setToOver(powerplayNode.path("to").asDouble());
                        powerplay.setType(powerplayNode.path("type").asText());
                        powerplay.setMatch(match);
                        powerplays.add(powerplay);
                        log.debug("Powerplay parsed: {} - {}", powerplay.getFromOver(), powerplay.getToOver());
                    }
                }
            }
        } else {
            log.warn("Innings node is not an array or is missing");
        }

        log.debug("Powerplays parsed: count = {}", powerplays.size());
        return powerplays;
    }
}