import React from 'react';
import { avatarHue, initials, vehicleEmoji } from '../../constants/entityVisuals';
import '../../styles/components.css';

function EntityAvatar({ kind = 'person', type, name, size = 36 }) {
  if (kind === 'vehicle') {
    return (
      <span className="entity-avatar entity-avatar-vehicle" style={{ width: size, height: size, fontSize: size * 0.5 }} title={type}>
        {vehicleEmoji(type)}
      </span>
    );
  }
  return (
    <span
      className="entity-avatar"
      style={{
        width: size,
        height: size,
        fontSize: size * 0.32,
        background: `hsl(${avatarHue(name)}, 52%, 42%)`,
      }}
      title={name}
    >
      {initials(name)}
    </span>
  );
}

export default EntityAvatar;
