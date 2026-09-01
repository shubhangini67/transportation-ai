import React from 'react';

/** Line drawing of a tractor-trailer — technical, not a cartoon. */
function TruckSchematic({ className }) {
  return (
    <svg
      className={className}
      viewBox="0 0 640 280"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <g stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" strokeLinecap="round">
        <rect x="218" y="78" width="372" height="118" rx="4" />
        <path d="M218 78h372M218 118h372M218 158h372" opacity="0.35" />
        <path d="M268 78v118M338 78v118M408 78v118M478 78v118M548 78v118" opacity="0.28" />
        <path d="M92 96h108l18 28v72H92V96z" />
        <path d="M92 124h126" />
        <rect x="104" y="102" width="52" height="20" rx="2" opacity="0.7" />
        <path d="M200 96v-18h-28l-12 18" />
        <circle cx="128" cy="210" r="22" />
        <circle cx="128" cy="210" r="10" />
        <circle cx="268" cy="210" r="22" />
        <circle cx="268" cy="210" r="10" />
        <circle cx="478" cy="210" r="22" />
        <circle cx="478" cy="210" r="10" />
        <circle cx="548" cy="210" r="22" />
        <circle cx="548" cy="210" r="10" />
        <path d="M150 210h96M290 210h166M500 210h26" opacity="0.5" />
        <path d="M92 196H64v-36h28" />
        <path d="M40 228h580" opacity="0.4" />
        <path d="M40 236h12M80 236h12M120 236h12M160 236h12M200 236h12M240 236h12M280 236h12M320 236h12M360 236h12M400 236h12M440 236h12M480 236h12M520 236h12M560 236h12M600 236h12" opacity="0.45" />
      </g>
    </svg>
  );
}

export default TruckSchematic;
