import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class NetworkHttpServer {
    private static Properties config;
    private static int PORT;
    private static Path DOCUMENT_ROOT;
    private static List<String> SUPPORTED_EXTENSIONS;
    private static String PHP_INTERPRETER;
    private static CountDownLatch clientConnectedLatch = new CountDownLatch(1);

    // Chargement de la configuration
    private static void loadConfiguration() throws IOException {
        config = new Properties();
        try (InputStream input = new FileInputStream("server-config.properties")) {
            config.load(input);

            PORT = Integer.parseInt(config.getProperty("server.port", "8000"));
            DOCUMENT_ROOT = Paths.get(config.getProperty("document.root", "./www")).toAbsolutePath();
            SUPPORTED_EXTENSIONS = Arrays.asList(
                    config.getProperty("supported.extensions", "html,htm,php,txt,css,js").split(","));
            PHP_INTERPRETER = Paths.get(config.getProperty("php.interpreter", "php")).toString();

            // Créer le répertoire racine s'il n'existe pas
            Files.createDirectories(DOCUMENT_ROOT);
        }
    }

    public static void main(String[] args) {
        try {
            loadConfiguration();
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new RootHandler());
            server.setExecutor(null);
            server.start();

            System.out.println("Server running on port: " + PORT);
            printNetworkInterfaces(PORT);

            new Thread(() -> {
                try {
                    clientConnectedLatch.await();
                    SwingUtilities.invokeLater(() -> {
                        EnhancedNetworkBrowser browser = new EnhancedNetworkBrowser();
                        browser.setVisible(true);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            clientConnectedLatch.countDown();
            String path = exchange.getRequestURI().getPath();

            // Construire le chemin complet du fichier
            Path filePath = DOCUMENT_ROOT.resolve(path.substring(1).replaceAll("/+", "/"));

            // Lister les fichiers si c'est un répertoire
            if (Files.isDirectory(filePath)) {
                serveDirectoryListing(exchange, filePath);
                return;
            }

            // Vérifier l'extension du fichier
            String fileName = filePath.getFileName().toString();
            String extension = fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()
                    : "";

            if (!SUPPORTED_EXTENSIONS.contains(extension)) {
                sendErrorResponse(exchange, 403, "Forbidden");
                return;
            }

            // Gestion spéciale pour PHP
            if ("php".equals(extension)) {
                servePHPFile(exchange, filePath);
            } else {
                serveStaticFile(exchange, filePath);
            }
        }

        private void serveDirectoryListing(HttpExchange exchange, Path dirPath) throws IOException {
            StringBuilder htmlResponse = new StringBuilder();
            htmlResponse.append("<!DOCTYPE html><html><head>");
            htmlResponse.append("<title>Fichiers sur serveur</title>");
            htmlResponse.append("<meta charset='UTF-8'>");
            htmlResponse.append("<style>");
            htmlResponse.append(
                    "body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }");
            htmlResponse.append("h1 { color: #333; }");
            htmlResponse.append("ul { list-style-type: none; padding: 0; }");
            htmlResponse.append("li { margin-bottom: 10px; display: flex; align-items: center; }");
            htmlResponse.append("a { text-decoration: none; color: #0066cc; margin-left: 10px; }");
            htmlResponse.append("a:hover { text-decoration: underline; }");
            htmlResponse.append(".icon { margin-right: 10px; }");
            htmlResponse.append("</style>");
            htmlResponse.append("</head><body>");
            htmlResponse.append("<h1>Fichiers sur serveur</h1>");
            htmlResponse.append("<ul>");

            // Récupérer l'URL de base
            String baseUrl = "http://" + exchange.getRequestHeaders().getFirst("Host");

            // Ajouter un lien parent pour remonter
            Path relativeDirPath = DOCUMENT_ROOT.relativize(dirPath);
            if (!relativeDirPath.toString().isEmpty() && !relativeDirPath.toString().equals(".")) {
                Path parentPath = relativeDirPath.getParent();
                String parentLink = parentPath != null
                        ? "/" + parentPath.toString()
                        : "/";
                htmlResponse.append(String.format(
                        "<li><span class='icon'>[D]</span><a href='%s'>..[Parent Directory]</a></li>",
                        baseUrl + parentLink.replace("\\", "/")));
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                for (Path entry : stream) {
                    String linkText = entry.getFileName().toString();
                    String linkPath = baseUrl + "/" + DOCUMENT_ROOT.relativize(entry).toString().replace("\\", "/");

                    // Différencier les dossiers et les fichiers
                    if (Files.isDirectory(entry)) {
                        htmlResponse.append(String.format(
                                "<li><span class='icon'>[D]</span><a href='%s'>%s/</a></li>",
                                linkPath,
                                linkText));
                    } else {
                        // Ajouter des icônes basées sur l'extension de fichier
                        String icon = getFileIcon(linkText);
                        htmlResponse.append(String.format(
                                "<li><span class='icon'>%s</span><a href='%s'>%s</a></li>",
                                icon,
                                linkPath,
                                linkText));
                    }
                }
            }

            htmlResponse.append("</ul>");
            htmlResponse.append("</body></html>");

            byte[] response = htmlResponse.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

        // Méthode utilitaire pour choisir une icône basée sur l'extension de fichier
        private String getFileIcon(String fileName) {
            String extension = fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()
                    : "";

            switch (extension) {
                case "txt":
                    return "[-]"; // Document texte
                case "pdf":
                    return "[P]"; // Document PDF
                case "doc":
                case "docx":
                    return "[W]"; // Document Word
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                    return "[I]"; // Image
                case "mp3":
                case "wav":
                    return "[♪]"; // Audio (le symbole musical est généralement bien supporté)
                case "mp4":
                case "avi":
                    return "[V]"; // Vidéo
                case "html":
                case "htm":
                    return "[H]"; // HTML
                case "css":
                    return "[C]"; // CSS
                case "js":
                    return "[J]"; // JavaScript
                case "zip":
                case "rar":
                case "7z":
                    return "[Z]"; // Archive
                case "php":
                    return "[P]"; // PHP
                default:
                    return "[F]"; // Fichier par défaut
            }
        }

        private void serveStaticFile(HttpExchange exchange, Path filePath) throws IOException {
            if (!Files.exists(filePath)) {
                sendErrorResponse(exchange, 404, "File Not Found");
                return;
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String mimeType = Files.probeContentType(filePath);

            // Gestion spéciale pour les fichiers HTML et PHP
            String fileName = filePath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".php")) {
                mimeType = "text/html; charset=UTF-8";
            }

            exchange.getResponseHeaders().set("Content-Type", mimeType != null ? mimeType : "application/octet-stream");
            exchange.sendResponseHeaders(200, fileContent.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileContent);
            }
        }

        private void servePHPFile(HttpExchange exchange, Path filePath) throws IOException {
            if (!Files.exists(filePath)) {
                sendErrorResponse(exchange, 404, "PHP File Not Found");
                return;
            }

            try {
                ProcessBuilder pb = new ProcessBuilder(PHP_INTERPRETER, filePath.toString());
                pb.redirectErrorStream(true);
                pb.directory(filePath.getParent().toFile());

                System.out.println("Executing PHP with command: " + PHP_INTERPRETER + " " + filePath);

                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroy();
                    sendErrorResponse(exchange, 500, "PHP Execution Timeout");
                    return;
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String errorOutput = output.toString();
                    System.err.println("PHP Error Output: " + errorOutput);
                    sendErrorResponse(exchange, 500, "PHP Execution Failed (Exit code: " + exitCode + ")");
                    return;
                }

                byte[] response = output.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }

            } catch (Exception e) {
                System.err.println("Error executing PHP: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "PHP Processing Error: " + e.getMessage());
            }
        }

        private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            String response = String.format("<html><body><h1>%d %s</h1></body></html>", statusCode, message);
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    // Méthode printNetworkInterfaces reste identique
    private static void printNetworkInterfaces(int port) {
        // Implémentation précédente
        try {
            System.out.println("\nAvailable Network Interfaces:");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr.getHostAddress().contains(":"))
                        continue;

                    System.out.println("Interface: " + iface.getName());
                    System.out.println("  IP: " + addr.getHostAddress());
                    System.out.println("  URL: http://" + addr.getHostAddress() + ":" + PORT);
                    System.out.println();
                }
            }
        } catch (SocketException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

}