package fr.villagersyt.host;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import javax.json.JsonObject;
import javax.json.JsonParser;
import javax.json.JsonReaderFactory;
import java.util.Map;

public class HostCreateCommand extends Command {

	    public HostCreateCommand() {
	        super("hostcreate", "host.create");
	    }

	    @Override
	    public void execute(CommandSender sender, String[] args) {
	        if (!(sender instanceof ProxiedPlayer)) {
	            sender.sendMessage("§cCette commande doit être exécutée par un joueur.");
	            return;
	        }

	        if (args.length != 2) {
	            sender.sendMessage("§cUtilisation incorrecte de la commande. Utilisation : /host create <version de Minecraft> <nom du Host>");
	            return;
	        }

	        ProxiedPlayer player = (ProxiedPlayer) sender;
	        String version = args[0];
	        String hostName = args[1];

	        if (!isValidMinecraftVersion(version)) {
	            sender.sendMessage("§cVotre version est invalide. Vous pouvez choisir une version entre la 1.16.1 et la 1.20.2.");
	            return;
	        }

	        // Vérifier si le nom du host est valide
	        if (hostName.length() > 60) {
	            sender.sendMessage("§cLe nom du host ne doit pas dépasser 60 caractères.");
	            return;
	        }

	        if (!checkHostCreationPermissions(player)) {
	            sender.sendMessage("§cVous n'avez pas la permission nécessaire pour créer un autre host.");
	            return;
	        }

	        // Créer le serveur Pterodactyl avec les paramètres spécifiés
	        String pterodactylApiKey = "CléPtero";
	        String pterodactylApiUrl = "https://panel.dreamclouds.fr";

	        createPterodactylServer(pterodactylApiUrl, pterodactylApiKey, player.getName(), hostName, version);

	        sender.sendMessage("§aServeur Pterodactyl créé avec succès : Host | " + player.getName() + " | " + hostName);
	    }

	    private boolean isValidMinecraftVersion(String version) {
	        // Liste des versions valides
	        List<String> validVersions = Arrays.asList("1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4", "1.20.1", "1.20.2");
	        return validVersions.contains(version);
	    }

	    private boolean checkHostCreationPermissions(ProxiedPlayer player) {
	        int maxHosts = 1; // Par défaut, une seule création d'host autorisée

	        if (player.hasPermission("host.create.2")) {
	            maxHosts = 2;
	        } else if (player.hasPermission("host.create.3")) {
	            maxHosts = 3;
	        } else if (player.hasPermission("host.create.4")) {
	            maxHosts = 4;
	        } else if (player.hasPermission("host.create.5")) {
	            maxHosts = 5;
	        }
	        
	        return true;
	     }
	     private void createPterodactylServer(String apiUrl, String apiKey, String playerName, String hostName, String minecraftVersion) {
	        try {
	            // Vérifiez si un serveur avec le même nom existe déjà
	            if (serverWithNameExists(apiUrl, apiKey, hostName)) {
	                System.err.println("Erreur : Il y a déjà un serveur avec le nom '" + hostName + "'.");
	                return;
	            }

	            // Utilisation de l'API HTTP pour créer le serveur Pterodactyl
	            HttpClient client = HttpClient.newHttpClient();

	            String jsonPayload = "{\"name\":\"Host | " + playerName + " | " + hostName + "\","
	                    + "\"user\":\"" + playerName + "\","
	                    + "\"nest\":1," // Nest ID pour Vanilla
	                    + "\"egg\":1," // Egg ID pour Vanilla
	                    + "\"docker_image\":\"quay.io/pterodactyl/core:java" + minecraftVersion + "\","
	                    + "\"limits\":{\"memory\":512,\"swap\":0,\"disk\":2048,\"io\":250,\"cpu\":50},"
	                    + "\"feature_limits\":{\"databases\":0,\"allocations\":0,\"backups\":0},"
	                    + "\"start_on_completion\":true}";

	            HttpRequest request = HttpRequest.newBuilder()
	                    .uri(URI.create(apiUrl + "/client/servers"))
	                    .header("Content-Type", "application/json")
	                    .header("Authorization", "Bearer " + apiKey)
	                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
	                    .build();

	            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString);
	// Vérifiez la réponse de la requête
	            if (response.statusCode() == 200) {
	                // La requête a réussi, le serveur Pterodactyl a été créé avec succès
	                // Vous pouvez ajouter des actions supplémentaires si nécessaire
	            } else {
	                // La requête a échoué, affichez un message d'erreur
	                System.err.println("Erreur lors de la création du serveur Pterodactyl. Code d'erreur : " + response.statusCode());
	                System.err.println("Réponse : " + response.body());
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	            System.err.println("Une erreur s'est produite lors de la création du serveur Pterodactyl : " + e.getMessage());
	        }
	    }

	    private boolean serverWithNameExists(String apiUrl, String apiKey, String hostName) throws Exception {
	        HttpClient client = HttpClient.newHttpClient();

	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(apiUrl + "/client/servers"))
	                .header("Content-Type", "application/json")
	                .header("Authorization", "Bearer " + apiKey)
	                .GET()
	                .build();

	        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

	        if (response.statusCode() == 200) {
	            // Analysez la réponse pour voir si un serveur avec le même nom existe déjà
	            JsonParser jsonParser = JsonParserFactory.getJsonParser();
	            List<Map<String, Object>> servers = jsonParser.parseList(response.body());

	            for (Map<String, Object> server : servers) {
	                String serverName = (String) server.get("name");
	                if (serverName.equalsIgnoreCase(hostName)) {
	                    // Un serveur avec le même nom existe déjà
	                    return true;
	                }
	            }
	        } else {
	            System.err.println("Erreur lors de la vérification de l'existence du serveur : " + response.statusCode());
	            System.err.println("Réponse : " + response.body());
	        }

	        return false;
	    }
}