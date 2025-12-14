package handler;

import auth.AuthFilter;
import dao.ConversationDao;
import dao.MessageDao;
import request.ParsedRequest;
import response.ResponseBuilder;
import response.RestApiAppResponse;
import response.StatusCodes;

public class DeleteChatHandler implements BaseHandler {

    @Override
    public ResponseBuilder handleRequest(ParsedRequest request) {
        AuthFilter.AuthResult authResult = AuthFilter.doFilter(request);
        if (!authResult.isLoggedIn) {
            return new ResponseBuilder().setStatus(StatusCodes.UNAUTHORIZED);
        }

        String conversationId = request.getQueryParam("conversationId");
        if (conversationId == null || conversationId.trim().isEmpty()) {
            var res = new RestApiAppResponse<>(false, null, "Conversation ID is required");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        MessageDao messageDao = MessageDao.getInstance();
        ConversationDao conversationDao = ConversationDao.getInstance();

        // Delete all messages with this conversationId
        messageDao.delete("conversationId", conversationId);
        
        // Delete the conversation
        conversationDao.delete("conversationId", conversationId);

        var res = new RestApiAppResponse<>(true, null, null);
        return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);
    }
}

