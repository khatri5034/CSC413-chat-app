package handler;

import auth.AuthFilter;
import dao.ConversationDao;
import dao.MessageDao;
import dao.UserDao;
import dto.ConversationDto;
import dto.MessageDto;
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
            UserDao userDao = UserDao.getInstance();
            
            // Get the message to know who sent/received it
            Document criteria = new Document("conversationId", conversationId)
                    .append("timestamp", timestamp);
            var messages = messageDao.queryByMultiple(criteria);
            
            if (!messages.isEmpty()) {
                MessageDto message = messages.get(0);
                String fromId = message.getFromId();
                String toId = message.getToId();
                
                // Decrease counts by 1
                var fromUser = userDao.query("userName", fromId).stream().findFirst().orElse(null);
                var toUser = userDao.query("userName", toId).stream().findFirst().orElse(null);
                
                if (fromUser != null) {
                    fromUser.setMessagesSent(Math.max(0, fromUser.getMessagesSent() - 1));
                    userDao.put(fromUser);
                }
                if (toUser != null) {
                    toUser.setMessagesRecieved(Math.max(0, toUser.getMessagesRecieved() - 1));
                    userDao.put(toUser);
                }
            }
            
            // Delete the message
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

