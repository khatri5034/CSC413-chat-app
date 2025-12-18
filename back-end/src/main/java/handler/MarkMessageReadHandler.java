package handler;

import auth.AuthFilter;
import dao.MessageDao;
import dto.MessageDto;
import org.bson.Document;
import request.ParsedRequest;
import response.ResponseBuilder;
import response.RestApiAppResponse;
import response.StatusCodes;

import java.time.Instant;

public class MarkMessageReadHandler implements BaseHandler {

    @Override
    public ResponseBuilder handleRequest(ParsedRequest request) {
        AuthFilter.AuthResult authResult = AuthFilter.doFilter(request);
        if (!authResult.isLoggedIn) {
            return new ResponseBuilder().setStatus(StatusCodes.UNAUTHORIZED);
        }

        String conversationId = request.getQueryParam("conversationId");
        String timestampStr = request.getQueryParam("timestamp");

        if (conversationId == null || conversationId.trim().isEmpty()) {
            var res = new RestApiAppResponse<>(false, null, "Conversation ID is required");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            var res = new RestApiAppResponse<>(false, null, "Timestamp is required");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        try {
            Long timestamp = Long.parseLong(timestampStr);

            MessageDao messageDao = MessageDao.getInstance();

            Document criteria = new Document("conversationId", conversationId)
                    .append("timestamp", timestamp);
            var messages = messageDao.queryByMultiple(criteria);

            if (messages.isEmpty()) {
                var res = new RestApiAppResponse<>(false, null, "Message not found");
                return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
            }

            MessageDto message = messages.get(0);

            if (!message.getToId().equals(authResult.userName)) {
                var res = new RestApiAppResponse<>(false, null, "Unauthorized: Only the recipient can mark message as read");
                return new ResponseBuilder().setStatus(StatusCodes.UNAUTHORIZED).setBody(res);
            }

            if ("read".equals(message.getStatus())) {
                var res = new RestApiAppResponse<>(true, null, "Message already marked as read");
                return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);
            }

            message.setStatus("read");
            message.setReadAt(Instant.now().toEpochMilli());

            if (message.getDeliveredAt() == null) {
                message.setDeliveredAt(Instant.now().toEpochMilli());
            }

            messageDao.put(message);

            var res = new RestApiAppResponse<>(true, null, "Message marked as read");
            return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);

        } catch (NumberFormatException e) {
            var res = new RestApiAppResponse<>(false, null, "Invalid timestamp format");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        } catch (Exception e) {
            var res = new RestApiAppResponse<>(false, null, "Error marking message as read: " + e.getMessage());
            return new ResponseBuilder().setStatus(StatusCodes.SERVER_ERROR).setBody(res);
        }
    }
}