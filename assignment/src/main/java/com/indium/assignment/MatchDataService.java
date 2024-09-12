package com.indium.assignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indium.assignment.entity.*;
import com.indium.assignment.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchDataService {

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
    public void parseAndSaveMatchData(InputStream inputStream) throws IOException {
        JsonNode rootNode = objectMapper.readTree(inputStream);

        // Parsing match data
        Match match = parseMatchData(rootNode);
        matchRepository.save(match);

        // Parsing teams data
        List<Team> teams = parseTeamsData(rootNode, match);
        teamRepository.saveAll(teams);

        // Parsing deliveries data
        List<Delivery> deliveries = parseDeliveriesData(rootNode, match);
        deliveryRepository.saveAll(deliveries);

        // Parsing officials data
        List<Official> officials = parseOfficialsData(rootNode, match);
        officialRepository.saveAll(officials);

        // Parsing powerplays data
        List<Powerplay> powerplays = parsePowerplaysData(rootNode, match);
        powerplayRepository.saveAll(powerplays);
    }

    private Match parseMatchData(JsonNode rootNode) {
        Match match = new Match();
        match.setCity(rootNode.path("city").asText());

        // Handle the date array
        JsonNode datesNode = rootNode.path("dates");
        if (datesNode.isArray() && datesNode.size() > 0) {
            String dateStr = datesNode.get(0).asText(); // Get the first date in the array
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    // Append default time if needed, assuming date is like "2008-04-27"
                    match.setDates(LocalDateTime.parse(dateStr + "T00:00:00"));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid date format: " + dateStr);
                }
            } else {
                throw new RuntimeException("Date field is empty in the JSON file");
            }
        } else {
            throw new RuntimeException("Dates field is missing or not an array in the JSON file");
        }

        match.setMatchNumber(rootNode.path("match_number").asInt());
        match.setEventName(rootNode.path("event").path("name").asText());
        match.setMatchType(rootNode.path("match_type").asText());
        match.setGender(rootNode.path("gender").asText());
        match.setSeason(rootNode.path("season").asText());
        match.setTossWinner(rootNode.path("toss_winner").asText());
        match.setTossDecision(rootNode.path("toss_decision").asText());
        match.setWinner(rootNode.path("winner").asText());
        match.setOutcomeByWickets(rootNode.path("outcome_by_wickets").asInt());
        match.setOvers(rootNode.path("overs").asInt());
        match.setPlayerOfMatch(rootNode.path("player_of_match").asText());
        return match;
    }


    private List<Team> parseTeamsData(JsonNode rootNode, Match match) {
        List<Team> teams = new ArrayList<>();
        JsonNode teamsNode = rootNode.path("teams");

        for (JsonNode teamNode : teamsNode) {
            Team team = new Team();
            team.setTeamName(teamNode.path("team_name").asText());
            team.setMatch(match);

            // Parse players of the team
            List<Player> players = new ArrayList<>();
            for (JsonNode playerNode : teamNode.path("players")) {
                Player player = new Player();
                player.setPlayerName(playerNode.asText());
                player.setTeam(team);
                player.setMatch(match);
                players.add(player);
            }
            team.setPlayers(players);
            teams.add(team);
        }

        return teams;
    }

    private List<Delivery> parseDeliveriesData(JsonNode rootNode, Match match) {
        List<Delivery> deliveries = new ArrayList<>();
        JsonNode deliveriesNode = rootNode.path("deliveries");

        for (JsonNode deliveryNode : deliveriesNode) {
            Delivery delivery = new Delivery();
            delivery.setOverNumber(deliveryNode.path("over_number").asInt());
            delivery.setBallNumber(deliveryNode.path("ball_number").asInt());
            delivery.setBatter(deliveryNode.path("batter").asText());
            delivery.setBowler(deliveryNode.path("bowler").asText());
            delivery.setNonStriker(deliveryNode.path("non_striker").asText());
            delivery.setRunsBatter(deliveryNode.path("runs_batter").asInt());
            delivery.setRunsExtras(deliveryNode.path("runs_extras").asInt());
            delivery.setRunsTotal(deliveryNode.path("runs_total").asInt());
            delivery.setWicketType(deliveryNode.path("wicket_type").asText());
            delivery.setPlayerOut(deliveryNode.path("player_out").asText());
            delivery.setMatch(match);
            deliveries.add(delivery);
        }

        return deliveries;
    }

    private List<Official> parseOfficialsData(JsonNode rootNode, Match match) {
        List<Official> officials = new ArrayList<>();
        JsonNode officialsNode = rootNode.path("officials");

        for (JsonNode officialNode : officialsNode) {
            Official official = new Official();
            official.setOfficialType(officialNode.path("official_type").asText());
            official.setOfficialName(officialNode.path("official_name").asText());
            official.setMatch(match);
            officials.add(official);
        }

        return officials;
    }

    private List<Powerplay> parsePowerplaysData(JsonNode rootNode, Match match) {
        List<Powerplay> powerplays = new ArrayList<>();
        JsonNode powerplaysNode = rootNode.path("powerplays");

        for (JsonNode powerplayNode : powerplaysNode) {
            Powerplay powerplay = new Powerplay();
            powerplay.setFromOver(powerplayNode.path("from_over").asDouble());
            powerplay.setToOver(powerplayNode.path("to_over").asDouble());
            powerplay.setType(powerplayNode.path("type").asText());
            powerplay.setMatch(match);
            powerplays.add(powerplay);
        }

        return powerplays;
    }
}
