package handler;

import auth.AuthFilter;
import dao.UserDao;
import dto.UserDto;
import request.ParsedRequest;
import response.ResponseBuilder;
import response.RestApiAppResponse;
import response.StatusCodes;

import java.util.List;

public class AddFriendHandler implements BaseHandler {

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

        // Can't add yourself as a friend
        if (friendUserName.equals(authResult.userName)) {
            var res = new RestApiAppResponse<>(false, null, "Cannot add yourself as a friend");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

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

        // Check if friend exists
        UserDto friendUser = userDao.query("userName", friendUserName)
                .stream()
                .findFirst()
                .orElse(null);

        if (friendUser == null) {
            var res = new RestApiAppResponse<>(false, null, "User not found");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        // Check if already friends
        if (currentUser.getFriends().contains(friendUserName)) {
            var res = new RestApiAppResponse<>(false, null, "Already friends with this user");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        // Add friend to list
        currentUser.getFriends().add(friendUserName);
        userDao.put(currentUser);

        var res = new RestApiAppResponse<>(true, List.of(currentUser), null);
        return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);
    }

    // Inner class for parsing request body
    private static class FriendRequest {
        String friendUserName;
    }
}

