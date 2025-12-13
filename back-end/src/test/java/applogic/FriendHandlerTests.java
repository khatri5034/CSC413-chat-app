package applogic;

import dao.AuthDao;
import dao.ConversationDao;
import dao.MessageDao;
import dao.UserDao;
import dto.UserDto;
import handler.HandlerFactory;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import request.ParsedRequest;
import response.StatusCodes;
import util.MockTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FriendHandlerTests {

    /**
     * Reset the cached singleton instances before each test.
     * This ensures each test gets a fresh mock from MockTestUtils.
     */
    @BeforeMethod
    public void resetDaoInstances() throws Exception {
        // Reset UserDao instance
        Field userDaoInstance = UserDao.class.getDeclaredField("instance");
        userDaoInstance.setAccessible(true);
        userDaoInstance.set(null, null);

        // Reset AuthDao instance
        Field authDaoInstance = AuthDao.class.getDeclaredField("instance");
        authDaoInstance.setAccessible(true);
        authDaoInstance.set(null, null);

        // Reset MessageDao instance
        Field messageDaoInstance = MessageDao.class.getDeclaredField("instance");
        messageDaoInstance.setAccessible(true);
        messageDaoInstance.set(null, null);

        // Reset ConversationDao instance
        Field conversationDaoInstance = ConversationDao.class.getDeclaredField("instance");
        conversationDaoInstance.setAccessible(true);
        conversationDaoInstance.set(null, null);
    }

    // ==================== AddFriendHandler Tests ====================

    @Test(singleThreaded = true)
    public void addFriendSuccessTest() {
        var testUtils = new MockTestUtils();

        // Create current user with random name to avoid collisions
        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setFriends(new ArrayList<>());

        // Create friend user
        var friendUser = new UserDto();
        friendUser.setUserName("friend_" + Math.random());

        // Set up auth first
        var auth = testUtils.createLogin(currentUser.getUserName());

        // Mock database queries using doReturn pattern
        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());
        Mockito.doReturn(List.of(friendUser))
                .when(testUtils.mockUserDao).query("userName", friendUser.getUserName());

        // Set up request
        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addFriend");
        parsedRequest.setBody("{\"friendUserName\": \"" + friendUser.getUserName() + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        // Execute handler
        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        // Verify response
        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);

        // Verify friend was added
        Assert.assertTrue(currentUser.getFriends().contains(friendUser.getUserName()));

        // Verify user was saved
        Mockito.verify(testUtils.mockUserDao).put(currentUser);
    }

    @Test(singleThreaded = true)
    public void addFriendUnauthorizedTest() {
        var testUtils = new MockTestUtils();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addFriend");
        parsedRequest.setBody("{\"friendUserName\": \"someuser\"}");
        // No auth cookie set

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.UNAUTHORIZED);
    }

    @Test(singleThreaded = true)
    public void addFriendUserNotFoundTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setFriends(new ArrayList<>());

        String nonexistentUser = "nonexistent_" + Math.random();

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());
        Mockito.doReturn(List.of()) // Empty list - user not found
                .when(testUtils.mockUserDao).query("userName", nonexistentUser);

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addFriend");
        parsedRequest.setBody("{\"friendUserName\": \"" + nonexistentUser + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "User not found");
    }

    @Test(singleThreaded = true)
    public void addFriendAlreadyFriendsTest() {
        var testUtils = new MockTestUtils();

        var friendUser = new UserDto();
        friendUser.setUserName("friend_" + Math.random());

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        var friendsList = new ArrayList<String>();
        friendsList.add(friendUser.getUserName()); // Already friends
        currentUser.setFriends(friendsList);

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());
        Mockito.doReturn(List.of(friendUser))
                .when(testUtils.mockUserDao).query("userName", friendUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addFriend");
        parsedRequest.setBody("{\"friendUserName\": \"" + friendUser.getUserName() + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Already friends with this user");
    }

    @Test(singleThreaded = true)
    public void addFriendCannotAddSelfTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setFriends(new ArrayList<>());

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addFriend");
        parsedRequest.setBody("{\"friendUserName\": \"" + currentUser.getUserName() + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Cannot add yourself as a friend");
    }

    // ==================== RemoveFriendHandler Tests ====================

    @Test(singleThreaded = true)
    public void removeFriendSuccessTest() {
        var testUtils = new MockTestUtils();

        String friendName = "friend_" + Math.random();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        var friendsList = new ArrayList<String>();
        friendsList.add(friendName);
        currentUser.setFriends(friendsList);

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/removeFriend");
        parsedRequest.setBody("{\"friendUserName\": \"" + friendName + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);

        // Verify friend was removed
        Assert.assertFalse(currentUser.getFriends().contains(friendName));

        // Verify user was saved
        Mockito.verify(testUtils.mockUserDao).put(currentUser);
    }

    @Test(singleThreaded = true)
    public void removeFriendUnauthorizedTest() {
        var testUtils = new MockTestUtils();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/removeFriend");
        parsedRequest.setBody("{\"friendUserName\": \"someuser\"}");

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.UNAUTHORIZED);
    }

    @Test(singleThreaded = true)
    public void removeFriendNotInListTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setFriends(new ArrayList<>()); // Empty friends list

        String notFriend = "notfriend_" + Math.random();

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/removeFriend");
        parsedRequest.setBody("{\"friendUserName\": \"" + notFriend + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "User is not in your friends list");
    }

    // ==================== GetFriendsHandler Tests ====================

    @Test(singleThreaded = true)
    public void getFriendsSuccessTest() {
        var testUtils = new MockTestUtils();

        var friend1 = new UserDto();
        friend1.setUserName("friend1_" + Math.random());

        var friend2 = new UserDto();
        friend2.setUserName("friend2_" + Math.random());

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        var friendsList = new ArrayList<String>();
        friendsList.add(friend1.getUserName());
        friendsList.add(friend2.getUserName());
        currentUser.setFriends(friendsList);

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());
        Mockito.doReturn(List.of(friend1))
                .when(testUtils.mockUserDao).query("userName", friend1.getUserName());
        Mockito.doReturn(List.of(friend2))
                .when(testUtils.mockUserDao).query("userName", friend2.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/getFriends");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);
        Assert.assertEquals(builder.getBody().data.size(), 2);
    }

    @Test(singleThreaded = true)
    public void getFriendsEmptyListTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setFriends(new ArrayList<>());

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/getFriends");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);
        Assert.assertEquals(builder.getBody().data.size(), 0);
    }

    @Test(singleThreaded = true)
    public void getFriendsUnauthorizedTest() {
        var testUtils = new MockTestUtils();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/getFriends");

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.UNAUTHORIZED);
    }
}
