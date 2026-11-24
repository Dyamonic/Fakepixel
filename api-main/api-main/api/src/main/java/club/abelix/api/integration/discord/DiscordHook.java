package club.abelix.api.integration.discord;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DiscordHook {
    private String url = "https://discord.com/api/webhooks/946820188471427072/1jx18VjQdoQZ8eMagzwVcAmaDds7V4VzPA7CDUDuSWoh_BeeiU5uyJf1yJkz0Xsu07af";
    @Setter private String content;
    @Setter private String username;
    @Setter private String avatarUrl;
    @Setter private boolean tts;
    private final List<EmbedObject> embeds = new ArrayList<>();



    public DiscordHook() {
    }
    public DiscordHook(String url) {
        this.url = url;
    }

    public void addEmbed(EmbedObject embed) {
        this.embeds.add(embed);
    }


    public void execute() {
        Thread.ofVirtual().start(this::execute0);
    }

    private void execute0() {
        if (this.content == null && this.embeds.isEmpty()) {
            throw new IllegalArgumentException("Set content or add at least one EmbedObject");
        }



        JSONObject json = new JSONObject();

        if (this.content != null) {
            json.put("content", this.content);
        }
        if (this.username != null) {
            json.put("username", this.username);
        }
        if (this.avatarUrl != null) {
            json.put("avatar_url", this.avatarUrl);
        }
        json.put("tts", this.tts);

        if (!this.embeds.isEmpty()) {
            JSONArray embedArray = new JSONArray();

            for (EmbedObject embed : this.embeds) {
                JSONObject jsonEmbed = new JSONObject();

                if (embed.getTitle() != null) {
                    jsonEmbed.put("title", embed.getTitle());
                }
                if (embed.getTimestamp() != null) {
                    jsonEmbed.put("timestamp", embed.getTimestamp());
                }
                if (embed.getDescription() != null) {
                    jsonEmbed.put("description", embed.getDescription());
                }
                if (embed.getUrl() != null) {
                    jsonEmbed.put("url", embed.getUrl());
                }

                if (embed.getColor() != null) {
                    Color color = embed.getColor();
                    int rgb = color.getRed();
                    rgb = (rgb << 8) + color.getGreen();
                    rgb = (rgb << 8) + color.getBlue();

                    jsonEmbed.put("color", rgb);
                }

                EmbedObject.Footer footer = embed.getFooter();
                EmbedObject.Image image = embed.getImage();
                EmbedObject.Thumbnail thumbnail = embed.getThumbnail();
                EmbedObject.Author author = embed.getAuthor();
                List<EmbedObject.Field> fields = embed.getFields();

                if (footer != null) {
                    JSONObject jsonFooter = new JSONObject();
                    jsonFooter.put("text", footer.text());
                    if (footer.iconUrl() != null) {
                        jsonFooter.put("icon_url", footer.iconUrl());
                    }
                    jsonEmbed.put("footer", jsonFooter);
                }

                if (image != null) {
                    JSONObject jsonImage = new JSONObject();
                    jsonImage.put("url", image.url());
                    jsonEmbed.put("image", jsonImage);
                }

                if (thumbnail != null) {
                    JSONObject jsonThumbnail = new JSONObject();
                    jsonThumbnail.put("url", thumbnail.url());
                    jsonEmbed.put("thumbnail", jsonThumbnail);
                }

                if (author != null) {
                    JSONObject jsonAuthor = new JSONObject();
                    jsonAuthor.put("name", author.name());
                    if (author.url() != null) {
                        jsonAuthor.put("url", author.url());
                    }
                    if (author.iconUrl() != null) {
                        jsonAuthor.put("icon_url", author.iconUrl());
                    }
                    jsonEmbed.put("author", jsonAuthor);
                }

                if (!fields.isEmpty()) {
                    JSONArray jsonFields = new JSONArray();
                    for (EmbedObject.Field field : fields) {
                        JSONObject jsonField = new JSONObject();
                        jsonField.put("name", field.name());
                        jsonField.put("value", field.value());
                        jsonField.put("inline", field.inline());
                        jsonFields.put(jsonField);
                    }
                    jsonEmbed.put("fields", jsonFields);
                }

                embedArray.put(jsonEmbed);
            }

            json.put("embeds", embedArray);
        }

        try {
            URL url = new URL(this.url);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "Java-DiscordWebhook");
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(json.toString().getBytes(StandardCharsets.UTF_8));
                stream.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                // Failure
                System.out.println("Failed to send Discord webhook. Response Code: " + responseCode);
                System.out.println("Response Message: " + connection.getResponseMessage());
                System.out.println("Hook Content: " + content);
            }

            connection.disconnect();
        } catch (Exception e) {
            System.out.println("Caught exception while sending hook message! " + e.getMessage());
            System.out.println("Hook Content: " + content);
            e.printStackTrace();
        }
    }

    @Getter public static class EmbedObject {
        private String title;
        private String description;
        private String url;
        private Color color;
        private String timestamp;

        private Footer footer;
        private Thumbnail thumbnail;
        private Image image;
        private Author author;
        private final List<Field> fields = new ArrayList<>();

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public EmbedObject setUrl(String url) {
            this.url = url;
            return this;
        }

        public EmbedObject setColor(Color color) {
            this.color = color;
            return this;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public EmbedObject setTimestamp(Instant instant) {
            this.timestamp = DateTimeFormatter.ISO_INSTANT.format(instant);
            return this;
        }

        public EmbedObject setImage(String url) {
            this.image = new Image(url);
            return this;
        }

        public EmbedObject setFooter(String text, String icon) {
            this.footer = new Footer(text, icon);
            return this;
        }

        public EmbedObject setAuthor(String name, String url, String icon) {
            this.author = new Author(name, url, icon);
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }

        private record Footer(String text, String iconUrl) {
        }

        private record Thumbnail(String url) {
        }

        private record Image(String url) {
        }

        private record Author(String name, String url, String iconUrl) {
        }

        private record Field(String name, String value, boolean inline) {
        }
    }
}

