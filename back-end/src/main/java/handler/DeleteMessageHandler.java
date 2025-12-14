package handler;

import auth.AuthFilter;
import dao.ConversationDao;
import dao.MessageDao;
import dto.ConversationDto;
import org.bson.Document;
import request.ParsedRequest;
import response.ResponseBuilder;
import response.RestApiAppResponse;
import response.StatusCodes;

public class DeleteMessageHandler implements BaseHandler {

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
            ConversationDao conversationDao = ConversationDao.getInstance();
            
            // Delete the specific message by conversationId and timestamp
            Document criteria = new Document("conversationId", conversationId)
                    .append("timestamp", timestamp);
            messageDao.deleteByMultiple(criteria);
            
            // Update conversation message count
            var conversations = conversationDao.query("conversationId", conversationId);
            if (!conversations.isEmpty()) {
                ConversationDto conversation = conversations.get(0);
                int newCount = Math.max(0, conversation.getMessageCount() - 1);
                conversation.setMessageCount(newCount);
                conversationDao.put(conversation);
            }
            
            var res = new RestApiAppResponse<>(true, null, null);
            return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);
        } catch (NumberFormatException e) {
            var res = new RestApiAppResponse<>(false, null, "Invalid timestamp format");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }
    }
}

