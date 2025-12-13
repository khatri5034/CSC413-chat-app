package handler;

import auth.AuthFilter;
import dao.UserDao;
import dto.UserDto;
import request.ParsedRequest;
import response.ResponseBuilder;
import response.RestApiAppResponse;
import response.StatusCodes;

import java.util.List;

public class GetFriendsHandler implements BaseHandler {

    @Override
    public ResponseBuilder handleRequest(ParsedRequest request) {
        // Check if user is logged in
        AuthFilter.AuthResult authResult = AuthFilter.doFilter(request);
        if (!authResult.isLoggedIn) {
            return new ResponseBuilder().setStatus(StatusCodes.UNAUTHORIZED);
        }

        UserDao userDao = UserDao.getInstance();

        // Get current user
        UserDto currentUser = userDao.query("userName", authResult.userName)
                .stream()
                .findFirst()
                .orElse(null);

        if (currentUser == null) {
            var res = new RestApiAppResponse<>(false, null, "User not found");
            return new ResponseBuilder().setStatus(StatusCodes.BAD_REQUEST).setBody(res);
        }

        // Get friend UserDto objects for each friend username
        List<UserDto> friends = currentUser.getFriends().stream()
                .map(friendName -> userDao.query("userName", friendName)
                        .stream()
                        .findFirst()
                        .orElse(null))
                .filter(friend -> friend != null)
                .toList();

        var res = new RestApiAppResponse<>(true, friends, null);
        return new ResponseBuilder().setStatus(StatusCodes.OK).setBody(res);
    }
}

