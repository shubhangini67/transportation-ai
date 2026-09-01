import React from 'react';
import { TileLayer } from 'react-leaflet';
import { MAP_TILE } from '../../constants/indiaMap';

function IndiaTileLayer() {
  return <TileLayer url={MAP_TILE.url} attribution={MAP_TILE.attribution} />;
}

export default IndiaTileLayer;
