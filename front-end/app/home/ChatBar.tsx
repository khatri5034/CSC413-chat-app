'use client';

import React from 'react';

interface Props {
  currentUser?: string;
  // optional username to open when ChatBar mounts or changes
  targetUser?: string;
}

interface MessageDto {
  fromId?: string;
  toId: string;
  message?: string;
  conversationId?: string;
  uniqueId?: string;
  // ui helpers
  from?: string;
  to?: string;
  text?: string;
  time?: string;
}

export default function ChatBar({ currentUser, targetUser }: Props) {
  const [messages, setMessages] = React.useState<MessageDto[]>([]);
  const [text, setText] = React.useState('');
  const messagesRef = React.useRef<HTMLDivElement | null>(null);

  const ids = currentUser && targetUser ? [currentUser, targetUser].slice().sort() : [];
  const conversationId = ids.length === 2 ? `${ids[0]}_${ids[1]}` : '';

  React.useEffect(() => {
    if (messagesRef.current) {
      messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
    }
  }, [messages]);

  React.useEffect(() => {
    if (!conversationId) return;
    
    async function loadConversation() {
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
    }
    
    loadConversation();
  }, [conversationId]);

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
        const convRes = await fetch(`/api/getConversation?conversationId=${encodeURIComponent(conversationId)}`);
        if (convRes.ok) {
          const data = await convRes.json();
          setMessages(data.data || []);
        }
      } else {
        console.error('Failed to send message', await res.text());
      }
    } catch (err) {
      console.error('Network error sending message', err);
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
            <div key={i} style={{ marginBottom: 8 }}>
              <div style={{ fontSize: 12, color: '#666' }}>
                {(m.time || '')} • {(m.from || m.fromId)} → {(m.to || m.toId)}
              </div>
              <div>{m.text || m.message}</div>
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
