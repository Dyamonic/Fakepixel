package club.abelix.core.app.data.stream.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record YouTubeVideosResponse(
        @JsonProperty("items")    List<Item> items,
        @JsonProperty("pageInfo") PageInfo   pageInfo
) {
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public record Item(
            @JsonProperty("id")                   String               id,
            @JsonProperty("snippet")              Snippet              snippet,
            @JsonProperty("liveStreamingDetails") LiveStreamingDetails liveStreamingDetails
    ) { }

    public record Snippet(
            @JsonProperty("publishedAt")         String     publishedAt,
            @JsonProperty("channelId")           String     channelId,
            @JsonProperty("title")               String     title,
            @JsonProperty("description")         String     description,
            @JsonProperty("thumbnails")          Thumbnails thumbnails,
            @JsonProperty("channelTitle")        String     channelTitle,
            @JsonProperty("tags")                List<String> tags,
            @JsonProperty("categoryId")          String     categoryId,
            @JsonProperty("liveBroadcastContent") String    liveBroadcastContent,
            @JsonProperty("defaultLanguage")     String     defaultLanguage,
            @JsonProperty("localized")           Localized  localized,
            @JsonProperty("defaultAudioLanguage") String    defaultAudioLanguage
    ) { }

    public record Thumbnails(
            @JsonProperty("default")  Thumbnail defaultThumb,
            @JsonProperty("medium")   Thumbnail medium,
            @JsonProperty("high")     Thumbnail high,
            @JsonProperty("standard") Thumbnail standard,
            @JsonProperty("maxres")   Thumbnail maxres
    ) { }

    public record Thumbnail(
            @JsonProperty("url")    String url,
            @JsonProperty("width")  Integer width,
            @JsonProperty("height") Integer height
    ) { }

    public record Localized(
            @JsonProperty("title")       String title,
            @JsonProperty("description") String description
    ) { }

    public record LiveStreamingDetails(
            @JsonProperty("actualStartTime")   String  actualStartTime,
            @JsonProperty("actualEndTime")     String  actualEndTime,
            @JsonProperty("concurrentViewers") String  concurrentViewers,
            @JsonProperty("activeLiveChatId")  String  activeLiveChatId
    ) {
        public Integer concurrentViewersAsInt() {
            try {
                return concurrentViewers == null ? null : Integer.parseInt(concurrentViewers);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public record PageInfo(
            @JsonProperty("totalResults")   Integer totalResults,
            @JsonProperty("resultsPerPage") Integer resultsPerPage
    ) { }
}
