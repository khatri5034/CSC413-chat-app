'use client';

import React from 'react';

interface BlockedUserDto {
  userName: string;
  uniqueId?: string;
}

interface Props {
  currentUser?: string;
}

export default function BlockedList({ currentUser }: Props) {
  const [blockedUsers, setBlockedUsers] = React.useState<BlockedUserDto[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [newBlockedUser, setNewBlockedUser] = React.useState('');
  const [actionLoading, setActionLoading] = React.useState(false);

  const loadBlockedUsers = React.useCallback(async () => {
    if (!currentUser) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const res = await fetch('/api/getBlockedUsers');
      const data = await res.json();
      
      if (data.status) {
        setBlockedUsers(data.data || []);
      } else {
        setError(data.message || 'Failed to load blocked users');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to load blocked users');
    } finally {
      setLoading(false);
    }
  }, [currentUser]);

  React.useEffect(() => {
    loadBlockedUsers();
  }, [loadBlockedUsers]);

  const handleAddBlockedUser = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newBlockedUser.trim();
    if (!trimmed || actionLoading) return;

    setActionLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/addBlockedUser', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userName: trimmed }),
      });
      const data = await res.json();

      if (data.status) {
        setNewBlockedUser('');
        loadBlockedUsers();
      } else {
        setError(data.message || 'Failed to block user');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to block user');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveBlockedUser = async (userName: string) => {
    if (actionLoading) return;

    setActionLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/removeBlockedUser', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userName }),
      });
      const data = await res.json();

      if (data.status) {
        loadBlockedUsers();
      } else {
        setError(data.message || 'Failed to unblock user');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to unblock user');
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
      <h2 style={{ margin: '8px 0', fontSize: 18 }}>Blocked Users</h2>

      <form onSubmit={handleAddBlockedUser} style={{ marginBottom: 12, display: 'flex', gap: 8 }}>
        <input
          aria-label="Block user by username"
          placeholder="Enter username to block"
          value={newBlockedUser}
          onChange={(e) => setNewBlockedUser(e.target.value)}
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
            background: actionLoading ? '#94a3b8' : 'linear-gradient(180deg, #ef4444, #dc2626)',
            color: 'white',
            fontWeight: 600,
            boxShadow: '0 6px 18px rgba(239,68,68,0.15)',
            cursor: actionLoading ? 'not-allowed' : 'pointer',
          }}
        >
          {actionLoading ? '...' : 'Block'}
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
          <div style={{ padding: 20, color: '#666' }}>Loading blocked users…</div>
        ) : blockedUsers.length === 0 ? (
          <div style={{ color: '#666', padding: 12 }}>No blocked users. Block someone above if needed.</div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'grid', gap: 10 }}>
            {blockedUsers.map((user) => (
              <li
                key={user.uniqueId || user.userName}
                style={{
                  padding: 12,
                  borderRadius: 10,
                  border: '1px solid #fee2e2',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  transition: 'transform 120ms ease, box-shadow 120ms ease',
                  boxShadow: '0 2px 6px rgba(239,68,68,0.08)',
                  background: '#fef2f2',
                }}
              >
                <div style={{ display: 'flex', gap: 12, alignItems: 'center', flex: 1 }}>
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: 10,
                      background: '#fee2e2',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      color: '#dc2626',
                      fontSize: 14,
                      boxShadow: 'inset 0 -6px 16px rgba(239,68,68,0.06)',
                    }}
                  >
                    {getInitials(user.userName)}
                  </div>

                  <div>
                    <div style={{ fontWeight: 700, fontSize: 15, color: '#991b1b' }}>{user.userName}</div>
                    <div style={{ color: '#dc2626', fontSize: 13, marginTop: 2 }}>Blocked</div>
                  </div>
                </div>

                <button
                  onClick={() => handleRemoveBlockedUser(user.userName)}
                  disabled={actionLoading}
                  style={{
                    padding: '6px 12px',
                    borderRadius: 8,
                    border: '1px solid #dc2626',
                    background: '#ffffff',
                    color: '#dc2626',
                    fontSize: 13,
                    fontWeight: 500,
                    cursor: actionLoading ? 'not-allowed' : 'pointer',
                    transition: 'background 120ms ease, color 120ms ease',
                  }}
                  onMouseEnter={(e) => {
                    if (!actionLoading) {
                      (e.currentTarget as HTMLElement).style.background = '#dc2626';
                      (e.currentTarget as HTMLElement).style.color = '#ffffff';
                    }
                  }}
                  onMouseLeave={(e) => {
                    (e.currentTarget as HTMLElement).style.background = '#ffffff';
                    (e.currentTarget as HTMLElement).style.color = '#dc2626';
                  }}
                >
                  Unblock
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

