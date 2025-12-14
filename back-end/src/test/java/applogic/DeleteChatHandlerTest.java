package applogic;

import dto.AuthDto;
import dto.UserDto;
import handler.HandlerFactory;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import request.ParsedRequest;
import response.StatusCodes;
import util.MockTestUtils;

import java.util.ArrayList;

public class DeleteChatHandlerTest {

    @Test(singleThreaded = true)
    public void deleteChatSuccessTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));
        ArrayList<UserDto> userReturnList = new ArrayList<>();
        userReturnList.add(user);

        String conversationId = String.valueOf(Math.random());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteChat");
        parsedRequest.setQueryParam("conversationId", conversationId);

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        Mockito.doNothing().when(testUtils.mockMessageDao).delete("conversationId", conversationId);
        Mockito.doNothing().when(testUtils.mockConversationDao).delete("conversationId", conversationId);

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);
        Mockito.verify(testUtils.mockAuthDao).query("hash", auth.getHash());
        Mockito.verify(testUtils.mockMessageDao).delete("conversationId", conversationId);
        Mockito.verify(testUtils.mockConversationDao).delete("conversationId", conversationId);
    }

    @Test(singleThreaded = true)
    public void deleteChatUnauthorizedTest() {
        var testUtils = new MockTestUtils();
        String conversationId = String.valueOf(Math.random());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteChat");
        parsedRequest.setQueryParam("conversationId", conversationId);
        // No auth cookie set

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.UNAUTHORIZED);
        Mockito.verify(testUtils.mockMessageDao, Mockito.never()).delete(Mockito.anyString(), Mockito.any());
        Mockito.verify(testUtils.mockConversationDao, Mockito.never()).delete(Mockito.anyString(), Mockito.any());
    }

    @Test(singleThreaded = true)
    public void deleteChatMissingConversationIdTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteChat");
        // No conversationId query param set

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Conversation ID is required");
        Mockito.verify(testUtils.mockMessageDao, Mockito.never()).delete(Mockito.anyString(), Mockito.any());
        Mockito.verify(testUtils.mockConversationDao, Mockito.never()).delete(Mockito.anyString(), Mockito.any());
    }

    @Test(singleThreaded = true)
    public void deleteChatEmptyConversationIdTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteChat");
        parsedRequest.setQueryParam("conversationId", ""); // Empty conversationId

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Conversation ID is required");
        Mockito.verify(testUtils.mockMessageDao, Mockito.never()).delete(Mockito.anyString(), Mockito.any());
        Mockito.verify(testUtils.mockConversationDao, Mockito.never()).delete(Mockito.anyString(), Mockito.any());
    }
}

