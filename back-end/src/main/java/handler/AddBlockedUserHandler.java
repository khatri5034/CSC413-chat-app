package handler;

import auth.AuthFilter;
import dao.UserDao;
import dto.UserDto;
import request.ParsedRequest;
import response.ResponseBuilder;
import response.RestApiAppResponse;
import response.StatusCodes;

import java.util.List;

public class AddBlockedUserHandler implements BaseHandler {

    @Override
    public ResponseBuilder handleRequest(ParsedRequest request) {
        // Check if user is logged in
        AuthFilter.AuthResult authResult = AuthFilter.doFilter(request);
        if (!authResult.isLoggedIn) {
            return new ResponseBuilder().setStatus(StatusCodes.UNAUTHORIZED);
        }

        // Parse the request body to get blocked user's username
        BlockedUserRequest blockedUserRequest = GsonTool.GSON.fromJson(request.getBody(), BlockedUserRequest.class);
        if (blockedUserRequest == null || blockedUserRequest.userName == null
                || blockedUserRequest.userName.trim().isEmpty()) {
            var res = new RestApiAppResponse<>(false, null, "Username is required");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        String blockedUserName = blockedUserRequest.userName.trim();

        // Can't block yourself
        if (blockedUserName.equals(authResult.userName)) {
            var res = new RestApiAppResponse<>(false, null, "Cannot block yourself");
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

        // Check if blocked user exists
        UserDto blockedUser = userDao.query("userName", blockedUserName)
                .stream()
                .findFirst()
                .orElse(null);

        if (blockedUser == null) {
            var res = new RestApiAppResponse<>(false, null, "User not found");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        // Check if already blocked
        if (currentUser.getBlockedUsers().contains(blockedUserName)) {
            var res = new RestApiAppResponse<>(false, null, "User is already blocked");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        // Add user to block list
        currentUser.getBlockedUsers().add(blockedUserName);
        userDao.put(currentUser);

        var res = new RestApiAppResponse<>(true, List.of(currentUser), null);
        return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);
    }

    // Inner class for parsing request body
    private static class BlockedUserRequest {
        String userName;
    }
}

