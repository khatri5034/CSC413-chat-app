'use client';

import React from 'react';
import ReadReceipts from './ReadReceipts';

interface Props {
  currentUser?: string;
  // optional username to open when ChatBar mounts or changes
  targetUser?: string;
  onUserUpdate?: () => void;
}

interface MessageDto {
  fromId?: string;
  toId: string;
  message?: string;
  conversationId?: string;
  uniqueId?: string;
  timestamp?: number;
  // NEW: Read receipt fields
  status?: string;
  deliveredAt?: number | null;
  readAt?: number | null;
  // ui helpers
  from?: string;
  to?: string;
  text?: string;
  time?: string;
}

export default function ChatBar({ currentUser, targetUser, onUserUpdate }: Props) {
  const [messages, setMessages] = React.useState<MessageDto[]>([]);
  const [text, setText] = React.useState('');
  const [deletingTimestamp, setDeletingTimestamp] = React.useState<number | null>(null);
  const messagesRef = React.useRef<HTMLDivElement | null>(null);
  const messageRefs = React.useRef<Map<number, HTMLDivElement>>(new Map());

  const ids = currentUser && targetUser ? [currentUser, targetUser].slice().sort() : [];
  const conversationId = ids.length === 2 ? `${ids[0]}_${ids[1]}` : '';

  const loadConversation = React.useCallback(async () => {
    if (!conversationId) return;
    try {
      const res = await fetch(`/api/getConversation?conversationId=${encodeURIComponent(conversationId)}`);
      if (!res.ok) {
        console.error('Failed to load conversation', await res.text());
        return;
      }
      const data = await res.json();
      setMessages(data.data || []);
    } catch (err) {
      console.error('Error loading conversation', err);
    }
  }, [conversationId]);

  // NEW: Mark unread messages as read when chat opens
  const markUnreadMessagesAsRead = React.useCallback(async () => {
    if (!conversationId || !currentUser) return;

    try {
      // Find unread messages where current user is the recipient
      const unreadMessages = messages.filter(
          (msg) => msg.toId === currentUser && msg.status !== 'read' && msg.timestamp
      );

      // Mark each unread message as read
      for (const msg of unreadMessages) {
        try {
          await fetch(
              `/api/markMessageRead?conversationId=${encodeURIComponent(conversationId)}&timestamp=${msg.timestamp}`,
              { method: 'POST' }
          );
        } catch (err) {
          console.error('Error marking message as read:', err);
        }
      }

      // Reload conversation if any messages were marked as read
      if (unreadMessages.length > 0) {
        setTimeout(() => loadConversation(), 500);
      }
    } catch (err) {
      console.error('Error marking messages as read:', err);
    }
  }, [conversationId, currentUser, messages, loadConversation]);

  // NEW: Set up IntersectionObserver to mark messages as read when visible
  React.useEffect(() => {
    if (!currentUser || messages.length === 0) return;

    const observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              const timestamp = Number(entry.target.getAttribute('data-timestamp'));
              const fromId = entry.target.getAttribute('data-from-id');
              const toId = entry.target.getAttribute('data-to-id');
              const status = entry.target.getAttribute('data-status');

              // Only mark as read if current user is recipient and message is not already read
              if (toId === currentUser && fromId !== currentUser && status !== 'read' && timestamp) {
                // Mark as read
                fetch(
                    `/api/markMessageRead?conversationId=${encodeURIComponent(conversationId)}&timestamp=${timestamp}`,
                    { method: 'POST' }
                )
                    .then(() => {
                      // Update status in local state
                      setMessages((prev) =>
                          prev.map((msg) =>
                              msg.timestamp === timestamp ? { ...msg, status: 'read', readAt: Date.now() } : msg
                          )
                      );
                    })
                    .catch((err) => console.error('Error marking message as read:', err));
              }
            }
          });
        },
        { threshold: 0.5 } // Message must be 50% visible
    );

    // Observe all message elements
    messageRefs.current.forEach((element) => {
      if (element) {
        observer.observe(element);
      }
    });

    return () => {
      observer.disconnect();
    };
  }, [messages, currentUser, conversationId]);

  React.useEffect(() => {
    if (messagesRef.current) {
      messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
    }
  }, [messages]);

  React.useEffect(() => {
    loadConversation();
  }, [loadConversation]);

  // NEW: Mark messages as read when chat opens or changes
  React.useEffect(() => {
    if (messages.length > 0) {
      markUnreadMessagesAsRead();
    }
  }, [targetUser]); // Only trigger when targetUser changes

  if (!currentUser || !targetUser) {
    return null;
  }

  async function send() {
    if (!text.trim()) return;

    const payload: MessageDto = {
      toId: targetUser || '',
      message: text.trim(),
      conversationId,
      fromId: currentUser,
    };

    // Add optimistic message immediately
    const optimisticMsg: MessageDto = {
      fromId: currentUser,
      toId: targetUser || '',
      message: text.trim(),
      status: 'sent', // NEW: Default status
    };
    setMessages((prev) => [...prev, optimisticMsg]);
    setText('');

    try {
      const res = await fetch('/api/sendMessage', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (res.ok) {
        // Reload conversation to get all messages from server
        loadConversation();
        // Refresh user stats (messagesSent/messagesReceived)
        onUserUpdate?.();
      } else {
        console.error('Failed to send message', await res.text());
      }
    } catch (err) {
      console.error('Network error sending message', err);
    }
  }

  async function handleDeleteMessage(timestamp: number) {
    if (!timestamp || deletingTimestamp === timestamp) return;

    setDeletingTimestamp(timestamp);
    try {
      const res = await fetch(
          `/api/deleteMessage?conversationId=${encodeURIComponent(conversationId)}&timestamp=${timestamp}`,
          { method: 'DELETE' }
      );
      const data = await res.json();

      if (data.status) {
        loadConversation();
        // Refresh user stats after deleting message
        onUserUpdate?.();
      } else {
        console.error('Failed to delete message', data.message);
      }
    } catch (err) {
      console.error('Error deleting message', err);
    } finally {
      setDeletingTimestamp(null);
    }
  }

  return (
      <div style={{ marginTop: 24, borderTop: '1px solid #eee', paddingTop: 16 }}>
        <h2 style={{ margin: '0 0 8px 0' }}>Chat</h2>

        <div
            ref={messagesRef}
            style={{
              height: 160,
              overflowY: 'auto',
              padding: 8,
              border: '1px solid #e6e6e6',
              borderRadius: 6,
              background: '#fafafa',
            }}
        >
          {messages.length === 0 ? (
              <div style={{ color: '#666' }}>No messages yet.</div>
          ) : (
              messages.map((m, i) => (
                  <div
                      key={i}
                      ref={(el) => {
                        if (el && m.timestamp) {
                          messageRefs.current.set(m.timestamp, el);
                        }
                      }}
                      data-timestamp={m.timestamp}
                      data-from-id={m.fromId}
                      data-to-id={m.toId}
                      data-status={m.status}
                      style={{
                        marginBottom: 8,
                        padding: 8,
                        background: '#fff',
                        borderRadius: 6,
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'flex-start',
                        gap: 8,
                        // NEW: Highlight unread messages received by current user
                        borderLeft: m.toId === currentUser && m.status !== 'read' ? '3px solid #4A90E2' : 'none',
                        paddingLeft: m.toId === currentUser && m.status !== 'read' ? 6 : 8,
                      }}
                  >
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 12, color: '#666' }}>
                        {(m.time || '')} • {(m.from || m.fromId)} → {(m.to || m.toId)}
                      </div>
                      <div>{m.text || m.message}</div>
                      {/* NEW: Show read receipts for sent messages */}
                      <ReadReceipts
                          status={m.status}
                          readAt={m.readAt}
                          deliveredAt={m.deliveredAt}
                          timestamp={m.timestamp}
                          isSentByCurrentUser={m.fromId === currentUser}
                      />
                    </div>
                    {m.timestamp && currentUser === m.fromId && (
                        <button
                            onClick={() => handleDeleteMessage(m.timestamp!)}
                            disabled={deletingTimestamp === m.timestamp}
                            style={{
                              padding: '4px 8px',
                              borderRadius: 4,
                              border: '1px solid #fecaca',
                              background: deletingTimestamp === m.timestamp ? '#f3f4f6' : '#fef2f2',
                              color: '#dc2626',
                              fontSize: 11,
                              fontWeight: 500,
                              cursor: deletingTimestamp === m.timestamp ? 'not-allowed' : 'pointer',
                              whiteSpace: 'nowrap'
                            }}
                        >
                          {deletingTimestamp === m.timestamp ? '...' : 'Delete'}
                        </button>
                    )}
                  </div>
              ))
          )}
        </div>

        <div style={{ marginTop: 8 }}>
        <textarea
            placeholder="Type a message"
            value={text}
            onChange={(e) => setText(e.target.value)}
            style={{ width: '100%', padding: 8, minHeight: 72, resize: 'vertical' }}
        />
        </div>

        <div style={{ display: 'flex', gap: 8, marginTop: 8, alignItems: 'center' }}>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
                onClick={send}
                style={{ height: 44, padding: '0 16px', alignSelf: 'center' }}
            >
              Send
            </button>
          </div>
        </div>
      </div>
  );
}