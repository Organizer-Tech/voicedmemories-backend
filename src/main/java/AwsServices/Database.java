package AwsServices;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Singleton class that handles all DynamoDb operations
 */
public class Database extends DynamoDB {
    private static Database instance;
    private final Table photoTable;
    private final Table shareTable;

    private Database() {
        super(AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_WEST_2).build());
        photoTable = getTable(Constants.PHOTO_TABLE_NAME);
        shareTable = getTable(Constants.SHARE_TABLE_NAME);
    }

    /**
     * Gets the instance of the Database
     * @return the instance
     */
    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }

        return instance;
    }

    /**
     * Adds a new entry to DynamoDb
     *
     * @param entry    Json holding the attributes of the entry
     * @param photoKey The Key to retrieve the photo from S3
     * @param audioKey The Key to retrieve the audio from S3
     * @return The url to access the newly created database entry
     * <p>
     */
    public String createEntry(JsonObject entry, Map<String, String> parameters, String photoKey, String audioKey, int position, String uuid) {
        String timestamp = new Timestamp(System.currentTimeMillis()).toString();
        String album = parameters.get("album");
        album = album.replace("%20", " ");

        Item item = new Item()
                .withPrimaryKey("User Email", parameters.get("email"), "Photo Id", uuid)
                .withString("Album", album)
                .withString("Photo Title", entry.get("photo title").getAsString())
                .withString("Photo Key", photoKey)
                .withInt("Position", position)
                .withString("Timestamp", timestamp);

        if (audioKey != null) {
            item.withString("Audio Key", audioKey);
        }

        photoTable.putItem(item);

        return buildUrl(parameters.get("email"), parameters.get("album"), uuid);
    }

    public void createShareEntry(String sharedUrl, String photoTitle, String signedPhotoUrl, String signedAudioUrl) {
        String uuid = UUID.randomUUID().toString();
        String timestamp = new Timestamp(System.currentTimeMillis()).toString();

        System.out.println(uuid);

        Item item = new Item()
                .withPrimaryKey("SharedUrl", sharedUrl, "ID", uuid)
                .withString("PhotoTitle", photoTitle)
                .withString("SignedPhotoUrl", signedPhotoUrl)
                .withString("SignedAudioUrl", signedAudioUrl)
                .withString("Timestamp", timestamp);


        PutItemOutcome outcome = shareTable.putItem(item);
    }

    /**
     * Updates an entry in DynamoDb. Any null fields will not be updated. The UUID and email are required fields
     *
     * @param email       The email of the customer
     * @param album       The name of the album
     * @param photoTitle  The title of the photo
     * @param photoKey    The Key to retrieve the photo from S3
     * @param audioKey    The Key to retrieve the audio from S3
     * @param uuid        The ID of the photo/audio that is part of the primary key    
     * @return            A JSON representation of the updated database item
     * <p>
     */
    public String updateEntry(String email, String album, String photoTitle, String photoKey, String audioKey, String uuid, int position) throws AmazonDynamoDBException {
        String timestamp = new Timestamp(System.currentTimeMillis()).toString();

        PrimaryKey pk = new PrimaryKey("User Email", email, "Photo Id", uuid);
        Item beforeUpdate = photoTable.getItem(pk);

        JsonObject jsonUpdates = new JsonObject();
        if (beforeUpdate == null) {
            throw new AmazonDynamoDBException("No item found with for user " + email + " and photo id " + uuid);
        }

        ArrayList<AttributeUpdate> updates = new ArrayList<>();
        updates.add(new AttributeUpdate("Timestamp").put(timestamp));
        jsonUpdates.add("Timestamp", buildUpdateResponse(beforeUpdate.getString("Timestamp"), timestamp));
        if (album != null && !album.trim().isEmpty() && !album.equals(beforeUpdate.getString("Album"))) {
            updates.add(new AttributeUpdate("Album").put(album));
            jsonUpdates.add("Album", buildUpdateResponse(beforeUpdate.get("Album").toString(), album));
        }
        if (photoTitle != null && !photoTitle.trim().isEmpty() && !photoTitle.equals(beforeUpdate.getString("Photo Title"))) {
            updates.add(new AttributeUpdate("Photo Title").put(photoTitle));
            jsonUpdates.add("Photo Title", buildUpdateResponse(beforeUpdate.get("Photo Title").toString(), photoTitle));
        }
        if (photoKey != null && !photoKey.trim().isEmpty() && !photoKey.equals(beforeUpdate.getString("Photo Key"))) {
            updates.add(new AttributeUpdate("Photo Key").put(photoKey));
            jsonUpdates.add("Photo Key", buildUpdateResponse(beforeUpdate.get("Photo Key").toString(), photoKey));
        }
        if (audioKey != null && !audioKey.trim().isEmpty() && !audioKey.equals(beforeUpdate.getString("Audio Key"))) {
            updates.add(new AttributeUpdate("Audio Key").put(audioKey));
            String beforeUpdateAudio = "";
            if (beforeUpdate.hasAttribute("Audio Key")) {
                beforeUpdateAudio = beforeUpdate.get("Audio Key").toString();
            }

            jsonUpdates.add("Audio Key", buildUpdateResponse(beforeUpdateAudio, audioKey));
        }
        if (position != -1 && position != beforeUpdate.getInt("Position")) {
            updates.add(new AttributeUpdate("Position").put(position));
            jsonUpdates.add("Position", buildUpdateResponse(beforeUpdate.get("Position").toString(), "" + position));
        }

        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(pk);

        for (AttributeUpdate update : updates) {
            updateItemSpec.addAttributeUpdate(update);
        }

        JsonObject jsonOutcome = new JsonObject();
        jsonOutcome.addProperty("url", buildUrl(email, album, uuid));
        jsonOutcome.add("updated", jsonUpdates);

        UpdateItemOutcome outcome = photoTable.updateItem(updateItemSpec);
        return jsonOutcome.toString();
    }


    /**
     * Gets the url of every photo in a user's account. Organized by album.
     *
     * @param email The email of the user.
     * @return A JsonObject representing the url of each photo.
     */

    public JsonObject getPhotoUrls(String email) {
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#email = :v_email")
                .withNameMap(new NameMap()
                        .with("#email", "User Email"))
                .withValueMap(new ValueMap()
                        .withString(":v_email", email));

        ItemCollection<QueryOutcome> items = photoTable.query(spec);
        JsonObject albums = new JsonObject();

        for (Item item : items) {
            String albumName = item.get("Album").toString();
            String photoId = item.get("Photo Id").toString();
            int position = item.getInt("Position");
            String photoTitle = item.get("Photo Title").toString();
            String url = buildUrl(email, albumName, photoId);
            JsonObject photo = new JsonObject();
            JsonArray album;

            if (albums.has(albumName)) {
                album = albums.getAsJsonArray(albumName);
            } else {
                album = new JsonArray();
            }

            photo.addProperty("photo title", photoTitle);
            photo.addProperty("position", position);
            photo.addProperty("url", url);

            album.add(photo);
            albums.add(albumName, album);
        }

        return albums;
    }

    public JsonArray getSharedLinks(String sharedUrl) {
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#sharedUrl = :v_sharedUrl")
                .withNameMap(new NameMap()
                        .with("#sharedUrl", "SharedUrl"))
                .withValueMap(new ValueMap()
                        .withString(":v_sharedUrl", sharedUrl));

        ItemCollection<QueryOutcome> items = shareTable.query(spec);
        JsonArray signedUrls = new JsonArray();

        for (Item item : items) {
            String signedPhotoUrl = item.get("SignedPhotoUrl").toString();
            System.out.println(signedPhotoUrl);
            String signedAudioUrl = item.get("SignedAudioUrl").toString();
            //int position = item.getInt("Position");
            String photoTitle = item.get("PhotoTitle").toString();
            Timestamp timestamp = Timestamp.valueOf(item.get("Timestamp").toString());

            if (linkIsExpired(timestamp)) {
                return null;
            }

            JsonObject urls = new JsonObject();
            urls.addProperty("photoTitle", photoTitle);
            //urls.addProperty("Position", position);
            urls.addProperty("signedPhotoUrl", signedPhotoUrl);
            urls.addProperty("signedAudioUrl", signedAudioUrl);

            signedUrls.add(urls);
        }

        return signedUrls;
    }

    public boolean linkIsExpired(Timestamp timestamp) {
        Instant now = Instant.now();
        Instant expiration = timestamp.toInstant().plus(1, ChronoUnit.DAYS);

        return now.isAfter(expiration);
    }

    public List<Map<String, String>> getAlbumPhotos(String email, String albumName) {
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#email = :v_email")
                .withFilterExpression("#album = :v_album")
                .withNameMap(new NameMap()
                        .with("#email", "User Email")
                        .with("#album", "Album"))
                .withValueMap(new ValueMap()
                        .withString(":v_email", email)
                        .withString(":v_album", albumName));

        ItemCollection<QueryOutcome> items = photoTable.query(spec);
        List<Map<String, String>> photos = new ArrayList<>();
        for (Item item : items) {
            String photoTitle = item.get("Photo Title").toString();
            String photoKey = item.get("Photo Key").toString();
            String audioKey = "";

            if (item.hasAttribute("Audio Key")) {
                audioKey = item.get("Audio Key").toString();
            }

            Map<String, String> photo = new HashMap<>();
            photo.put("photo title", photoTitle);
            photo.put("photo key", photoKey);
            photo.put("audio key", audioKey);

            photos.add(photo);
        }

        return photos;
    }

    public Map<String, String> getFileKeys(Map<String, String> parameters) {
        Map<String, String> fileKeys = new HashMap<>();

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#email = :v_email and #id = :v_id")
                .withNameMap(new NameMap()
                        .with("#email", "User Email")
                        .with("#id", "Photo Id"))
                .withValueMap(new ValueMap()
                        .withString(":v_email", parameters.get("email"))
                        .withString(":v_id", parameters.get("id")));

        ItemCollection<QueryOutcome> items = photoTable.query(spec);

        for (Item item : items) {
            fileKeys.put("position", item.get("Position").toString());
            fileKeys.put("photoKey", item.get("Photo Key").toString());

            if (item.hasAttribute("Audio Key")) {
                fileKeys.put("audioKey", item.get("Audio Key").toString());
            }
        }

        return fileKeys;
    }

    public List<Map<String, String>> getAllFileKeys(String email) {
        List<Map<String, String>> allFileKeys = new ArrayList<>();

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#email = :v_email")
                .withNameMap(new NameMap()
                        .with("#email", "User Email"))
                .withValueMap(new ValueMap()
                        .withString(":v_email", email));

        ItemCollection<QueryOutcome> items = photoTable.query(spec);

        for (Item item : items) {
            Map<String, String> fileKeys = new HashMap<>();
            fileKeys.put("photoKey", item.get("Photo Key").toString());
            fileKeys.put("id", item.get("Photo Id").toString());

            if (item.hasAttribute("Audio Key")) {
                fileKeys.put("audioKey", item.get("Audio Key").toString());
            }

            allFileKeys.add(fileKeys);
        }

        return allFileKeys;
    }

    public boolean itemExists(String email, String id) {
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#email = :v_email and #id = :v_id")
                .withNameMap(new NameMap()
                        .with("#email", "User Email")
                        .with("#id", "Photo Id"))
                .withValueMap(new ValueMap()
                        .withString(":v_email", email)
                        .withString(":v_id", id));

        ItemCollection<QueryOutcome> items = photoTable.query(spec);
        int count = 0;
        for (Item item : items) {
            count++;
        }
        return count > 0;
    }

    private String buildUrl(String email, String album, String id) {
        album = album.replace(" ", "%20");
        return Constants.BASE_URL + "/" + email + "/" + album + "/" + id;
    }

    private JsonObject buildUpdateResponse(String oldValue, String newValue) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("old", oldValue);
        jsonObject.addProperty("new", newValue);
        return jsonObject;
    }

    /**
     * Removes the selected photo from the database.
     *
     * @param email The email of the user.
     * @param photoId The ID of the photo.
     */
    public void deletePhoto(String email, String photoId) {
        photoTable.deleteItem("User Email", email, "Photo Id", photoId);
    }

    public void deleteAllPhotos(String email, List<Map<String, String>> fileKeys) {
        for (Map<String, String> fileKey : fileKeys) {
            deletePhoto(email, fileKey.get("id"));
        }
    }
}
