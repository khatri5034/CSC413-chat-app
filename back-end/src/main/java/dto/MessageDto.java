package dto;

import org.bson.Document;

import java.time.Instant;

public class MessageDto extends BaseDto {

    private String fromId;
    private String toId;
    private String message;
    private Long timestamp;
    private String conversationId;

    // NEW: Read receipt fields
    private String status;        // "sent", "delivered", "read"
    private Long deliveredAt;     // timestamp when delivered
    private Long readAt;          // timestamp when read

    public MessageDto() {
        timestamp = Instant.now().toEpochMilli();
        // NEW: Initialize read receipt fields
        status = "sent";
        deliveredAt = null;
        readAt = null;
    }

    @Override
    public void fromDocument(Document document) {
        this.fromId = document.getString("fromId");
        this.toId = document.getString("toId");
        this.message = document.getString("message");
        this.timestamp = document.getLong("timestamp");
        this.conversationId = document.getString("conversationId");

        // NEW: Read receipt fields from document
        this.status = document.getString("status");
        this.deliveredAt = document.getLong("deliveredAt");
        this.readAt = document.getLong("readAt");

        // Handle legacy messages without status field
        if (this.status == null) {
            this.status = "sent";
        }
    }

    @Override
    public Document toDocument() {
        var doc = new Document();
        doc.append("fromId", fromId);
        doc.append("toId", toId);
        doc.append("message", message);
        doc.append("timestamp", timestamp);
        doc.append("conversationId", conversationId);

        // NEW: Add read receipt fields to document
        doc.append("status", status);
        doc.append("deliveredAt", deliveredAt);
        doc.append("readAt", readAt);

        return doc;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    // NEW: Getters and setters for read receipt fields
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Long deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Long getReadAt() {
        return readAt;
    }

    public void setReadAt(Long readAt) {
        this.readAt = readAt;
    }

}