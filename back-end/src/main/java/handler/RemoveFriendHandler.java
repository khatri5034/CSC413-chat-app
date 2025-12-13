package handler;

import auth.AuthFilter;
import dao.UserDao;
import dto.UserDto;
import request.ParsedRequest;
import response.ResponseBuilder;
import response.RestApiAppResponse;
import response.StatusCodes;

import java.util.List;

public class RemoveFriendHandler implements BaseHandler {

    @Override
    public ResponseBuilder handleRequest(ParsedRequest request) {
        // Check if user is logged in
        AuthFilter.AuthResult authResult = AuthFilter.doFilter(request);
        if (!authResult.isLoggedIn) {
            return new ResponseBuilder().setStatus(StatusCodes.UNAUTHORIZED);
        }

        // Parse the request body to get friend's username
        FriendRequest friendRequest = GsonTool.GSON.fromJson(request.getBody(), FriendRequest.class);
        if (friendRequest == null || friendRequest.friendUserName == null
                || friendRequest.friendUserName.trim().isEmpty()) {
            var res = new RestApiAppResponse<>(false, null, "Friend username is required");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        String friendUserName = friendRequest.friendUserName.trim();

        UserDao userDao = UserDao.getInstance();

        // Get current user
        UserDto currentUser = userDao.query("userName", authResult.userName)
                .stream()
                .findFirst()
                .orElse(null);

        if (currentUser == null) {
            var res = new RestApiAppResponse<>(false, null, "Current user not found");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        // Check if friend is in the list
        if (!currentUser.getFriends().contains(friendUserName)) {
            var res = new RestApiAppResponse<>(false, null, "User is not in your friends list");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        // Remove friend from list
        currentUser.getFriends().remove(friendUserName);
        userDao.put(currentUser);

        var res = new RestApiAppResponse<>(true, List.of(currentUser), null);
        return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);
    }

    // Inner class for parsing request body
    private static class FriendRequest {
        String friendUserName;
    }
}

