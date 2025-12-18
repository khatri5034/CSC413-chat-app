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

public class BlockedUserHandlerTests {

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

    // ==================== AddBlockedUserHandler Tests ====================

    @Test(singleThreaded = true)
    public void addBlockedUserSuccessTest() {
        var testUtils = new MockTestUtils();

        // Create current user with random name to avoid collisions
        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setBlockedUsers(new ArrayList<>());

        // Create user to block
        var blockedUser = new UserDto();
        blockedUser.setUserName("blocked_" + Math.random());

        // Set up auth first
        var auth = testUtils.createLogin(currentUser.getUserName());

        // Mock database queries using doReturn pattern
        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());
        Mockito.doReturn(List.of(blockedUser))
                .when(testUtils.mockUserDao).query("userName", blockedUser.getUserName());

        // Set up request
        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addBlockedUser");
        parsedRequest.setBody("{\"userName\": \"" + blockedUser.getUserName() + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        // Execute handler
        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        // Verify response
        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);

        // Verify user was blocked
        Assert.assertTrue(currentUser.getBlockedUsers().contains(blockedUser.getUserName()));

        // Verify user was saved
        Mockito.verify(testUtils.mockUserDao).put(currentUser);
    }

    @Test(singleThreaded = true)
    public void addBlockedUserUnauthorizedTest() {
        var testUtils = new MockTestUtils();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addBlockedUser");
        parsedRequest.setBody("{\"userName\": \"someuser\"}");
        // No auth cookie set

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.UNAUTHORIZED);
    }

    @Test(singleThreaded = true)
    public void addBlockedUserUserNotFoundTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setBlockedUsers(new ArrayList<>());

        String nonexistentUser = "nonexistent_" + Math.random();

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());
        Mockito.doReturn(List.of()) // Empty list - user not found
                .when(testUtils.mockUserDao).query("userName", nonexistentUser);

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addBlockedUser");
        parsedRequest.setBody("{\"userName\": \"" + nonexistentUser + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "User not found");
    }

    @Test(singleThreaded = true)
    public void addBlockedUserAlreadyBlockedTest() {
        var testUtils = new MockTestUtils();

        var blockedUser = new UserDto();
        blockedUser.setUserName("blocked_" + Math.random());

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        var blockedList = new ArrayList<String>();
        blockedList.add(blockedUser.getUserName()); // Already blocked
        currentUser.setBlockedUsers(blockedList);

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());
        Mockito.doReturn(List.of(blockedUser))
                .when(testUtils.mockUserDao).query("userName", blockedUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addBlockedUser");
        parsedRequest.setBody("{\"userName\": \"" + blockedUser.getUserName() + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "User is already blocked");
    }

    @Test(singleThreaded = true)
    public void addBlockedUserCannotBlockSelfTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setBlockedUsers(new ArrayList<>());

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addBlockedUser");
        parsedRequest.setBody("{\"userName\": \"" + currentUser.getUserName() + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Cannot block yourself");
    }

    @Test(singleThreaded = true)
    public void addBlockedUserMissingUsernameTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setBlockedUsers(new ArrayList<>());

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/addBlockedUser");
        parsedRequest.setBody("{\"userName\": \"\"}"); // Empty username
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Username is required");
    }

    // ==================== RemoveBlockedUserHandler Tests ====================

    @Test(singleThreaded = true)
    public void removeBlockedUserSuccessTest() {
        var testUtils = new MockTestUtils();

        String blockedName = "blocked_" + Math.random();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        var blockedList = new ArrayList<String>();
        blockedList.add(blockedName);
        currentUser.setBlockedUsers(blockedList);

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/removeBlockedUser");
        parsedRequest.setBody("{\"userName\": \"" + blockedName + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);

        // Verify user was removed from block list
        Assert.assertFalse(currentUser.getBlockedUsers().contains(blockedName));

        // Verify user was saved
        Mockito.verify(testUtils.mockUserDao).put(currentUser);
    }

    @Test(singleThreaded = true)
    public void removeBlockedUserUnauthorizedTest() {
        var testUtils = new MockTestUtils();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/removeBlockedUser");
        parsedRequest.setBody("{\"userName\": \"someuser\"}");

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.UNAUTHORIZED);
    }

    @Test(singleThreaded = true)
    public void removeBlockedUserNotInListTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setBlockedUsers(new ArrayList<>()); // Empty block list

        String notBlocked = "notblocked_" + Math.random();

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/removeBlockedUser");
        parsedRequest.setBody("{\"userName\": \"" + notBlocked + "\"}");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "User is not in your block list");
    }

    @Test(singleThreaded = true)
    public void removeBlockedUserMissingUsernameTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setBlockedUsers(new ArrayList<>());

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/removeBlockedUser");
        parsedRequest.setBody("{\"userName\": \"\"}"); // Empty username
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Username is required");
    }

    // ==================== GetBlockedUsersHandler Tests ====================

    @Test(singleThreaded = true)
    public void getBlockedUsersSuccessTest() {
        var testUtils = new MockTestUtils();

        var blocked1 = new UserDto();
        blocked1.setUserName("blocked1_" + Math.random());

        var blocked2 = new UserDto();
        blocked2.setUserName("blocked2_" + Math.random());

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        var blockedList = new ArrayList<String>();
        blockedList.add(blocked1.getUserName());
        blockedList.add(blocked2.getUserName());
        currentUser.setBlockedUsers(blockedList);

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());
        Mockito.doReturn(List.of(blocked1))
                .when(testUtils.mockUserDao).query("userName", blocked1.getUserName());
        Mockito.doReturn(List.of(blocked2))
                .when(testUtils.mockUserDao).query("userName", blocked2.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/getBlockedUsers");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);
        Assert.assertEquals(builder.getBody().data.size(), 2);
    }

    @Test(singleThreaded = true)
    public void getBlockedUsersEmptyListTest() {
        var testUtils = new MockTestUtils();

        var currentUser = new UserDto();
        currentUser.setUserName("user_" + Math.random());
        currentUser.setBlockedUsers(new ArrayList<>());

        // Set up auth
        var auth = testUtils.createLogin(currentUser.getUserName());

        Mockito.doReturn(List.of(currentUser))
                .when(testUtils.mockUserDao).query("userName", currentUser.getUserName());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/getBlockedUsers");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);
        Assert.assertEquals(builder.getBody().data.size(), 0);
    }

    @Test(singleThreaded = true)
    public void getBlockedUsersUnauthorizedTest() {
        var testUtils = new MockTestUtils();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/getBlockedUsers");

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.UNAUTHORIZED);
    }

    @Test(singleThreaded = true)
    public void getBlockedUsersUserNotFoundTest() {
        var testUtils = new MockTestUtils();

        String userName = "user_" + Math.random();

        // Set up auth
        var auth = testUtils.createLogin(userName);

        Mockito.doReturn(List.of()) // User not found
                .when(testUtils.mockUserDao).query("userName", userName);

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/getBlockedUsers");
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "User not found");
    }
}

