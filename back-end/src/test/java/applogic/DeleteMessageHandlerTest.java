package applogic;

import dto.AuthDto;
import dto.ConversationDto;
import dto.UserDto;
import handler.HandlerFactory;
import org.bson.Document;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import request.ParsedRequest;
import response.StatusCodes;
import util.MockTestUtils;

import java.util.ArrayList;
import java.util.List;

public class DeleteMessageHandlerTest {

    @Test(singleThreaded = true)
    public void deleteMessageSuccessTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));
        ArrayList<UserDto> userReturnList = new ArrayList<>();
        userReturnList.add(user);

        String conversationId = String.valueOf(Math.random());
        Long timestamp = System.currentTimeMillis();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteMessage");
        parsedRequest.setQueryParam("conversationId", conversationId);
        parsedRequest.setQueryParam("timestamp", String.valueOf(timestamp));

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        // Mock conversation with message count
        var conversation = new ConversationDto();
        conversation.setConversationId(conversationId);
        conversation.setMessageCount(5);
        List<ConversationDto> conversationList = new ArrayList<>();
        conversationList.add(conversation);

        Mockito.when(testUtils.mockConversationDao.query("conversationId", conversationId))
                .thenReturn(conversationList);
        Mockito.doNothing().when(testUtils.mockMessageDao).deleteByMultiple(Mockito.any(Document.class));
        Mockito.doNothing().when(testUtils.mockConversationDao).put(Mockito.any(ConversationDto.class));

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);
        Mockito.verify(testUtils.mockAuthDao).query("hash", auth.getHash());

        // Verify deleteByMultiple was called with correct criteria
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        Mockito.verify(testUtils.mockMessageDao).deleteByMultiple(documentCaptor.capture());
        Document capturedDoc = documentCaptor.getValue();
        Assert.assertEquals(capturedDoc.getString("conversationId"), conversationId);
        Assert.assertEquals(capturedDoc.getLong("timestamp"), timestamp);

        // Verify conversation message count was decremented
        ArgumentCaptor<ConversationDto> conversationCaptor = ArgumentCaptor.forClass(ConversationDto.class);
        Mockito.verify(testUtils.mockConversationDao).put(conversationCaptor.capture());
        Assert.assertEquals(conversationCaptor.getValue().getMessageCount(), 4); // 5 - 1
    }

    @Test(singleThreaded = true)
    public void deleteMessageUnauthorizedTest() {
        var testUtils = new MockTestUtils();
        String conversationId = String.valueOf(Math.random());
        Long timestamp = System.currentTimeMillis();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteMessage");
        parsedRequest.setQueryParam("conversationId", conversationId);
        parsedRequest.setQueryParam("timestamp", String.valueOf(timestamp));
        // No auth cookie set

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.UNAUTHORIZED);
        Mockito.verify(testUtils.mockMessageDao, Mockito.never()).deleteByMultiple(Mockito.any());
        Mockito.verify(testUtils.mockConversationDao, Mockito.never()).put(Mockito.any());
    }

    @Test(singleThreaded = true)
    public void deleteMessageMissingConversationIdTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));
        Long timestamp = System.currentTimeMillis();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteMessage");
        // No conversationId query param
        parsedRequest.setQueryParam("timestamp", String.valueOf(timestamp));

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Conversation ID is required");
        Mockito.verify(testUtils.mockMessageDao, Mockito.never()).deleteByMultiple(Mockito.any());
    }

    @Test(singleThreaded = true)
    public void deleteMessageMissingTimestampTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));
        String conversationId = String.valueOf(Math.random());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteMessage");
        parsedRequest.setQueryParam("conversationId", conversationId);
        // No timestamp query param

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Timestamp is required");
        Mockito.verify(testUtils.mockMessageDao, Mockito.never()).deleteByMultiple(Mockito.any());
    }

    @Test(singleThreaded = true)
    public void deleteMessageInvalidTimestampTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));
        String conversationId = String.valueOf(Math.random());

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteMessage");
        parsedRequest.setQueryParam("conversationId", conversationId);
        parsedRequest.setQueryParam("timestamp", "invalid-timestamp"); // Invalid format

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.BAD_REQUEST);
        Assert.assertFalse(builder.getBody().status);
        Assert.assertEquals(builder.getBody().message, "Invalid timestamp format");
        Mockito.verify(testUtils.mockMessageDao, Mockito.never()).deleteByMultiple(Mockito.any());
    }

    @Test(singleThreaded = true)
    public void deleteMessageConversationNotFoundTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));
        String conversationId = String.valueOf(Math.random());
        Long timestamp = System.currentTimeMillis();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteMessage");
        parsedRequest.setQueryParam("conversationId", conversationId);
        parsedRequest.setQueryParam("timestamp", String.valueOf(timestamp));

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        // Mock empty conversation list (conversation not found)
        Mockito.when(testUtils.mockConversationDao.query("conversationId", conversationId))
                .thenReturn(new ArrayList<>());
        Mockito.doNothing().when(testUtils.mockMessageDao).deleteByMultiple(Mockito.any(Document.class));

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        Assert.assertTrue(builder.getBody().status);
        // Verify message was deleted but conversation wasn't updated (since it doesn't exist)
        Mockito.verify(testUtils.mockMessageDao).deleteByMultiple(Mockito.any(Document.class));
        Mockito.verify(testUtils.mockConversationDao, Mockito.never()).put(Mockito.any());
    }

    @Test(singleThreaded = true)
    public void deleteMessageCountDecrementsToZeroTest() {
        var testUtils = new MockTestUtils();
        var user = new UserDto();
        user.setUserName(String.valueOf(Math.random()));
        String conversationId = String.valueOf(Math.random());
        Long timestamp = System.currentTimeMillis();

        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setPath("/deleteMessage");
        parsedRequest.setQueryParam("conversationId", conversationId);
        parsedRequest.setQueryParam("timestamp", String.valueOf(timestamp));

        var auth = testUtils.createLogin(user.getUserName());
        parsedRequest.setCookieValue("auth", auth.getHash());

        // Mock conversation with message count of 1
        var conversation = new ConversationDto();
        conversation.setConversationId(conversationId);
        conversation.setMessageCount(1);
        List<ConversationDto> conversationList = new ArrayList<>();
        conversationList.add(conversation);

        Mockito.when(testUtils.mockConversationDao.query("conversationId", conversationId))
                .thenReturn(conversationList);
        Mockito.doNothing().when(testUtils.mockMessageDao).deleteByMultiple(Mockito.any(Document.class));
        Mockito.doNothing().when(testUtils.mockConversationDao).put(Mockito.any(ConversationDto.class));

        var handler = HandlerFactory.getHandler(parsedRequest);
        var builder = handler.handleRequest(parsedRequest);
        var res = builder.build();

        Assert.assertEquals(res.status, StatusCodes.OK);
        // Verify message count goes to 0 (not negative)
        ArgumentCaptor<ConversationDto> conversationCaptor = ArgumentCaptor.forClass(ConversationDto.class);
        Mockito.verify(testUtils.mockConversationDao).put(conversationCaptor.capture());
        Assert.assertEquals(conversationCaptor.getValue().getMessageCount(), 0); // Math.max(0, 1-1)
    }
}

