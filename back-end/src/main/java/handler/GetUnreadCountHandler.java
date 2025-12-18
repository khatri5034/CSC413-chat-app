package handler;

import auth.AuthFilter;
import dao.MessageDao;
import dto.MessageDto;
import request.ParsedRequest;
import response.ResponseBuilder;
import response.RestApiAppResponse;
import response.StatusCodes;

import java.util.List;

public class GetUnreadCountHandler implements BaseHandler {

    @Override
    public ResponseBuilder handleRequest(ParsedRequest request) {
        AuthFilter.AuthResult authResult = AuthFilter.doFilter(request);
        if (!authResult.isLoggedIn) {
            return new ResponseBuilder().setStatus(StatusCodes.UNAUTHORIZED);
        }

        try {
            MessageDao messageDao = MessageDao.getInstance();

            List<MessageDto> messages = messageDao.query("toId", authResult.userName);

            long unreadCount = messages.stream()
                    .filter(msg -> !"read".equals(msg.getStatus()))
                    .count();

            var res = new RestApiAppResponse<>(true, null, "Unread count: " + unreadCount);
            return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);

        } catch (Exception e) {
            var res = new RestApiAppResponse<>(false, null, "Error getting unread count: " + e.getMessage());
            return new ResponseBuilder().setStatus(StatusCodes.SERVER_ERROR).setBody(res);
        }
    }
}