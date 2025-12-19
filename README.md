# Final Project
Final project groups of 3-4

Open front-end via vscode
Open back-end via intelij

Run both together + mongodb in order to get full stack project running

## Team members
Ian Kligman : Friends list
Philip Chen : Read Receipts
Joshua Gonzalez : Block List
Kiran Khatri : Delete Chat 

- Add name
- Add feature
- post recording

Add short description to readme on what handler/method each team member worked on :
    
    IAN KLIGMAN:
        + Added friends list field in UserDto.java
        + Added handlers: AddFriendHandler, RemoveFriendHandler, GetFriendsHandler (and included routes for each in HandlerFactory)
        + Added unit tests: FriendHandlerTests
        + Added FriendsList.tsx for the friends list frontend
        + Changed page.tsx to integrate FriendsList

        The friends list feature allows users to add other registered users to their own individual friends lists.
        They can remove a friend at any time. The database keeps track of User's "friends" feild (array)

    PHILIP CHEN:
        + Modified MessageDto.java to add read receipt fields (status, deliveredAt, readAt)
        + Added handlers: MarkMessageReadHandler, GetMessageReceiptHandler, GetUnreadCountHandler
        + Added routes in HandlerFactory for: /markMessageRead, /getMessageReceipt, /getUnreadCount
        + Created ReadReceipts.tsx component to display receipt status with checkmarks
        + Modified ChatBar.tsx to integrate read receipts and auto-mark messages as read
        + Modified page.tsx to show unread message count badge with real-time updates

        The read receipts feature allows users to see when their sent messages are delivered 
        and read. Messages show visual indicators: single checkmark (sent), double gray 
        checkmark (delivered), double blue checkmark (read). Recipients see an unread count 
        badge. Messages are automatically marked as read when viewed. Security enforced: only 
        recipients can mark messages as read, only senders can view read receipts.

    JOSHUA GONZALEZ:
        + Added Blocked list field in UserDto.java
        + Added handlers: AddBlockedUSerHandler, RemoveBlockedUserHandler, GetBLockedUserHandler (and included routes for each in HandlerFactory)
        + Added unit tests: BlockedUserHandlerTests
        + Added BlockedList.tsx for the Block list frontend
        + Changed page.tsx to integrate BlockeList

        Allows the user to add other users into a blocked list. They can remove them at anytime but cannot
        add users into their friends list if already blocked.

    KIRAN KHATRI:
        + Added DeleteMessageHandler to delete individual messages and update user message counts
        + Added queryByMultiple method to BaseDao.java for querying with multiple criteria
        + Added delete functionality to ChatBar.tsx (delete individual messages)
        + Updated page.tsx to auto-refresh user stats (messagesSent/messagesReceived) after delete operations
        + Added route in HandlerFactory for /deleteMessage

        The delete functionality allows users to:
        - Delete individual messages from conversations (only messages they sent)
        - Automatically updates user statistics (messagesSent/messagesReceived) when messages are deleted
        - Frontend automatically refreshes user stats without page reload

Video Demos:
    IAN KLIGMAN (Friends List) : https://www.youtube.com/watch?v=h85DVJskR74 <br> 
    Kiran Khatri (Delete Chat) : https://drive.google.com/file/d/1iJCIjG39moGHNUW9T8g_SGmT3rPYKPhc/view?usp=sharing
    Joshua Gonzalez (Block List) : https://youtu.be/EqCGC8rMCls
    Philip Chen (Read Receipts) : https://drive.google.com/file/d/16YewHQHLn83D9a-W1ZwgnnuRo_z9eoFB/view?usp=sharing


