'use client';

import React from 'react';

interface FriendDto {
  userName: string;
  uniqueId?: string;
}

interface Props {
  currentUser?: string;
  onOpenChat: (username: string) => void;
}

export default function FriendsList({ currentUser, onOpenChat }: Props) {
  const [friends, setFriends] = React.useState<FriendDto[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [newFriend, setNewFriend] = React.useState('');
  const [actionLoading, setActionLoading] = React.useState(false);

  const loadFriends = React.useCallback(async () => {
    if (!currentUser) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const res = await fetch('/api/getFriends');
      const data = await res.json();
      
      if (data.status) {
        setFriends(data.data || []);
      } else {
        setError(data.message || 'Failed to load friends');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to load friends');
    } finally {
      setLoading(false);
    }
  }, [currentUser]);

  React.useEffect(() => {
    loadFriends();
  }, [loadFriends]);

  const handleAddFriend = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newFriend.trim();
    if (!trimmed || actionLoading) return;

    setActionLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/addFriend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ friendUserName: trimmed }),
      });
      const data = await res.json();

      if (data.status) {
        setNewFriend('');
        loadFriends();
      } else {
        setError(data.message || 'Failed to add friend');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to add friend');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveFriend = async (friendUserName: string) => {
    if (actionLoading) return;

    setActionLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/removeFriend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ friendUserName }),
      });
      const data = await res.json();

      if (data.status) {
        loadFriends();
      } else {
        setError(data.message || 'Failed to remove friend');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to remove friend');
    } finally {
      setActionLoading(false);
    }
  };

  const getInitials = (name?: string) => {
    if (!name) return '?';
    const parts = name.split(/\s+/);
    const initials = parts.length === 1 ? parts[0].slice(0, 2) : (parts[0][0] + parts[1][0]);
    return initials.toUpperCase();
  };

  return (
    <div style={{ marginTop: 20, maxWidth: 720 }}>
      <h2 style={{ margin: '8px 0', fontSize: 18 }}>Friends</h2>

      <form onSubmit={handleAddFriend} style={{ marginBottom: 12, display: 'flex', gap: 8 }}>
        <input
          aria-label="Add friend by username"
          placeholder="Enter username to add as friend"
          value={newFriend}
          onChange={(e) => setNewFriend(e.target.value)}
          disabled={actionLoading}
          style={{
            flex: 1,
            padding: '10px 12px',
            borderRadius: 10,
            border: '1px solid #e6e6e6',
            boxShadow: 'inset 0 1px 4px rgba(16,24,40,0.04)',
            outline: 'none',
            fontSize: 14,
          }}
        />
        <button
          type="submit"
          disabled={actionLoading}
          style={{
            padding: '10px 14px',
            borderRadius: 10,
            border: 'none',
            background: actionLoading ? '#94a3b8' : 'linear-gradient(180deg, #10b981, #059669)',
            color: 'white',
            fontWeight: 600,
            boxShadow: '0 6px 18px rgba(16,185,129,0.15)',
            cursor: actionLoading ? 'not-allowed' : 'pointer',
          }}
        >
          {actionLoading ? '...' : 'Add Friend'}
        </button>
      </form>

      {error && (
        <div style={{ 
          color: '#dc2626', 
          padding: '8px 12px', 
          marginBottom: 12, 
          background: '#fef2f2', 
          borderRadius: 8,
          fontSize: 14 
        }}>
          {error}
        </div>
      )}

      <div
        style={{
          background: '#ffffff',
          borderRadius: 12,
          padding: 12,
          boxShadow: '0 6px 24px rgba(15, 23, 42, 0.06)',
        }}
      >
        {loading ? (
          <div style={{ padding: 20, color: '#666' }}>Loading friends…</div>
        ) : friends.length === 0 ? (
          <div style={{ color: '#666', padding: 12 }}>No friends yet. Add someone above!</div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'grid', gap: 10 }}>
            {friends.map((friend) => (
              <li
                key={friend.uniqueId || friend.userName}
                style={{
                  padding: 12,
                  borderRadius: 10,
                  border: '1px solid #f0f2f5',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  transition: 'transform 120ms ease, box-shadow 120ms ease',
                  boxShadow: '0 2px 6px rgba(16,24,40,0.04)',
                }}
              >
                <div 
                  style={{ display: 'flex', gap: 12, alignItems: 'center', cursor: 'pointer', flex: 1 }}
                  onClick={() => onOpenChat(friend.userName)}
                >
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: 10,
                      background: '#ecfdf5',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      color: '#059669',
                      fontSize: 14,
                      boxShadow: 'inset 0 -6px 16px rgba(16,185,129,0.06)',
                    }}
                  >
                    {getInitials(friend.userName)}
                  </div>

                  <div>
                    <div style={{ fontWeight: 700, fontSize: 15 }}>{friend.userName}</div>
                    <div style={{ color: '#666', fontSize: 13, marginTop: 2 }}>Click to chat</div>
                  </div>
                </div>

                <button
                  onClick={() => handleRemoveFriend(friend.userName)}
                  disabled={actionLoading}
                  style={{
                    padding: '6px 12px',
                    borderRadius: 8,
                    border: '1px solid #fecaca',
                    background: '#fef2f2',
                    color: '#dc2626',
                    fontSize: 13,
                    fontWeight: 500,
                    cursor: actionLoading ? 'not-allowed' : 'pointer',
                  }}
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

