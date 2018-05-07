package net.extus.youtubetrends;// Sample Java code for user authorization

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Lists;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.*;
import com.google.api.services.youtube.YouTube;
import com.sun.demo.jvmti.hprof.Tracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;


public class Main {
    private static final String APPLICATION_NAME = "API Sample";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/java-youtube-api-test");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private static final Collection<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/youtube.force-ssl",
            "https://www.googleapis.com/auth/youtubepartner");

    private static final List<TagTracker> TRACKED_TAGS = Lists.newArrayList();

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = Main.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader( in ));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    public static TagTracker getTagTracker(final String tagName) {
        Optional<TagTracker> tagTracker = TRACKED_TAGS.stream().filter(tag -> tag.getTagName().equals(tagName)).findFirst();
        if(tagTracker.isPresent())
            return tagTracker.get();

        TagTracker tracker = new TagTracker(tagName);
        TRACKED_TAGS.add(tracker);
        return tracker;
    }

    /**
     * Build and return an authorized API client service, such as a YouTube
     * Data API client service.
     * @return an authorized API client service
     * @throws IOException
     */
    public static YouTube getYouTubeService() throws IOException {
        Credential credential = authorize();
        return new YouTube.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException {

        YouTube youtube = getYouTubeService();

        try {
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("part", "snippet,contentDetails,statistics");
            parameters.put("chart", "mostPopular");
            parameters.put("regionCode", "US");
            parameters.put("videoCategoryId", "");

            YouTube.Videos.List videosListMostPopularRequest = youtube.videos().list(parameters.get("part"));
            if (parameters.containsKey("chart") && parameters.get("chart") != "") {
                videosListMostPopularRequest.setChart(parameters.get("chart"));
            }

            if (parameters.containsKey("regionCode") && parameters.get("regionCode") != "") {
                videosListMostPopularRequest.setRegionCode(parameters.get("regionCode"));
            }

            if (parameters.containsKey("videoCategoryId") && parameters.get("videoCategoryId") != "") {
                videosListMostPopularRequest.setVideoCategoryId(parameters.get("videoCategoryId"));
            }

            VideoListResponse response = videosListMostPopularRequest.execute();
            response.getItems().forEach(video -> {
                if (video == null)
                    return;
                video.getSnippet().getTags().forEach(topic -> {
                    TagTracker tracker = getTagTracker(topic);
                    if (tracker.isTrackingVideo(video.getId()))
                        return;

                    tracker.getViews().addAndGet(video.getStatistics().getViewCount().intValue());
                });
            });

            calculateMostPopularTags();

        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void calculateMostPopularTags() {
        Arrays.sort(TRACKED_TAGS.toArray());
        TRACKED_TAGS.forEach(tagTracker -> {
            System.out.printf("The tag %s has the total views of %s \n", tagTracker.getTagName(), tagTracker.getViews().get());
        });
    }
}