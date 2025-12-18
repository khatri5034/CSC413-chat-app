'use client';

import React from 'react';

export default function ReadReceipts(props: any) {
    const { status = 'sent', readAt, deliveredAt, timestamp, isSentByCurrentUser } = props;

    if (!isSentByCurrentUser) {
        return null;
    }

    let icon = '✓';
    let color = '#999';

    if (status === 'read') {
        icon = '✓✓';
        color = '#4A90E2';
    } else if (status === 'delivered') {
        icon = '✓✓';
        color = '#999';
    }

    let text = '';
    if (status === 'read' && readAt) {
        text = `Read at ${new Date(readAt).toLocaleTimeString()}`;
    } else if (status === 'delivered' && deliveredAt) {
        text = `Delivered at ${new Date(deliveredAt).toLocaleTimeString()}`;
    } else if (timestamp) {
        text = `Sent at ${new Date(timestamp).toLocaleTimeString()}`;
    }

    return (
        <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            marginTop: '4px',
            fontSize: '11px',
            color: '#666'
        }}>
            <span style={{ marginRight: '4px' }}>{text}</span>
            <span style={{ fontSize: '12px', color: color, marginLeft: '6px' }}>
        {icon}
      </span>
        </div>
    );
}