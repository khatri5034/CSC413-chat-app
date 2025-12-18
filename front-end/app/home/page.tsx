'use client';

import Layout from "@/app/layout";
import React from "react";
import ChatBar from "./ChatBar";
import ChatList from "./ChatList";
import FriendsList from "./FriendsList";
import BlockedList from "./BlockedList";

interface UserDto {
  userName: string;
  totalConversations: number;
  messagesSent: number;
  messagesRecieved: number;
}

export default function Home() {

  const [user, setUser] = React.useState<UserDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [activeChatUser, setActiveChatUser] = React.useState<string | null>(null);
  // NEW: Unread count state
  const [unreadCount, setUnreadCount] = React.useState<number>(0);

  const loadUser = React.useCallback(() => {
    fetch("/api/getUser")
        .then((res) => res.json())
        .then((apiRes) => {
          console.log(apiRes);
          setUser(apiRes.data[0]);
        })
        .catch((err) => {
          console.error(err);
        })
        .finally(() => setLoading(false));
  }, []);

  // NEW: Load unread count
  const loadUnreadCount = React.useCallback(() => {
    fetch("/api/getUnreadCount")
        .then((res) => res.json())
        .then((apiRes) => {
          if (apiRes.status && apiRes.data) {
            setUnreadCount(apiRes.data.unreadCount || 0);
          }
        })
        .catch((err) => {
          console.error('Error loading unread count:', err);
        });
  }, []);

  React.useEffect(() => {
    loadUser();
    loadUnreadCount();
  }, [loadUser, loadUnreadCount]);

  // NEW: Poll for unread count updates every 10 seconds
  React.useEffect(() => {
    const interval = setInterval(() => {
      loadUnreadCount();
    }, 10000); // 10 seconds

    return () => clearInterval(interval);
  }, [loadUnreadCount]);

  // NEW: Reload unread count when returning from a chat
  React.useEffect(() => {
    if (!activeChatUser) {
      loadUnreadCount();
    }
  }, [activeChatUser, loadUnreadCount]);

  const formatNumber = (n?: number) => (typeof n === 'number' ? n.toLocaleString() : '—');

  // Early return when loading finished but no user data
  if (!loading && !user) {
    return (
        <div style={{ padding: 20 }}>
          <h1 style={{ margin: 0 }}>Welcome!</h1>
          <p style={{ marginTop: 8 }}>No user data available.</p>
        </div>
    );
  }

  return (
      <div style={{ padding: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <h1 style={{ margin: 0 }}>Welcome{user ? `, ${user.userName}` : ''}!</h1>
          {/* NEW: Unread count badge */}
          {unreadCount > 0 && (
              <span
                  style={{
                    background: '#dc2626',
                    color: 'white',
                    borderRadius: '12px',
                    padding: '4px 10px',
                    fontSize: '13px',
                    fontWeight: 600,
                    minWidth: '24px',
                    textAlign: 'center',
                    display: 'inline-block',
                  }}
                  title={`${unreadCount} unread message${unreadCount !== 1 ? 's' : ''}`}
              >
            {unreadCount}
          </span>
          )}
        </div>
        <p style={{ marginTop: 8 }}>Here's your dashboard overview.</p>

        {loading ? (
            <p>Loading dashboard…</p>
        ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginTop: 16 }}>
              <div style={{ padding: 16, border: '1px solid #e6e6e6', borderRadius: 8 }}>
                <h3 style={{ margin: 0 }}>Total Conversations</h3>
                <div style={{ fontSize: 20, fontWeight: 600, marginTop: 8 }}>{formatNumber(user?.totalConversations)}</div>
              </div>

              <div style={{ padding: 16, border: '1px solid #e6e6e6', borderRadius: 8 }}>
                <h3 style={{ margin: 0 }}>Messages Sent</h3>
                <div style={{ fontSize: 20, fontWeight: 600, marginTop: 8 }}>{formatNumber(user?.messagesSent)}</div>
              </div>

              <div style={{ padding: 16, border: '1px solid #e6e6e6', borderRadius: 8 }}>
                <h3 style={{ margin: 0 }}>Messages Received</h3>
                <div style={{ fontSize: 20, fontWeight: 600, marginTop: 8 }}>{formatNumber(user?.messagesRecieved)}</div>
              </div>
            </div>
        )}

        {!activeChatUser && (
            <>
              <FriendsList currentUser={user?.userName} onOpenChat={(username) => setActiveChatUser(username)} />
              <BlockedList currentUser={user?.userName} />
              <ChatList currentUser={user?.userName} onOpenChat={(username) => setActiveChatUser(username)} onUserUpdate={loadUser} />
            </>
        )}

        {activeChatUser ? (
            <div style={{ marginTop: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ fontWeight: 600 }}>Chat with {activeChatUser}</div>
                <button onClick={() => setActiveChatUser(null)} style={{ padding: '6px 10px' }} aria-label="Back to chat list">
                  Back to chats
                </button>
              </div>
              <ChatBar
                  currentUser={user?.userName}
                  targetUser={activeChatUser}
                  onUserUpdate={() => {
                    loadUser();
                    loadUnreadCount(); // NEW: Also reload unread count
                  }}
              />
            </div>
        ) : null}
      </div>
  );
}