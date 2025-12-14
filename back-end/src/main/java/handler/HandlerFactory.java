package handler;

import request.ParsedRequest;

public class HandlerFactory {
    // routes based on the path. Add your custom handlers here
    public static BaseHandler getHandler(ParsedRequest request) {
        switch (request.getPath()) {
            case "/createUser":
                return new CreateUserHandler();
            case "/sendMessage":
                return new SendMessageHandler();
            case "/getConversations":
                return new GetConversationsHandler();
            case "/getConversation":
                return new GetConversationHandler();
            case "/login":
                return new LoginHandler();
            case "/getUser":
                return new GetUserHandler();
            case "/addFriend":
                return new AddFriendHandler();
            case "/removeFriend":
                return new RemoveFriendHandler();
            case "/getFriends":
                return new GetFriendsHandler();
            case "/deleteMessage":
                return new DeleteMessageHandler();
            default:
                return new FallbackHandler();
        }
    }

}
