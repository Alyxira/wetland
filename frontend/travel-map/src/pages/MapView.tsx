import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Plus, Minus, Compass, AlertCircle, Layers, ArrowLeft } from "lucide-react";
import { SPOTS, CURRENT_LOCATION, Spot, fetchWetlandSpots } from "../data/spots";
import { ScenicPanel } from "../components/ScenicPanel";
import {
  FACILITY_CATEGORIES,
  FacilityCategory,
  FacilityCategoryKey,
  NearbyFacility,
  RouteMode,
  RouteSummary,
} from "@/lib/travelMapTypes";

declare global {
  interface Window {
    AMap?: any;
    _AMapSecurityConfig?: { securityJsCode: string };
    __AMAP_SECURITY_JS_CODE?: string;
  }
}

type LngLatTuple = [number, number];
type MapFitPadding = [number, number, number, number];
type SpotCoordSource = "wgs84" | "gcj02" | "amap";

interface RoutePlanResult {
  path: LngLatTuple[];
  summary: RouteSummary;
}

const AMAP_KEY = String(import.meta.env.VITE_AMAP_KEY || "").trim();
const AMAP_SECURITY_JS_CODE = String(import.meta.env.VITE_AMAP_SECURITY_JS_CODE || "").trim();
const AMAP_PLUGIN_LIST = [
  "AMap.PlaceSearch",
  "AMap.Driving",
  "AMap.Walking",
  "AMap.Riding",
  "AMap.GeometryUtil",
  "AMap.MarkerCluster",
].join(",");
const FACILITY_SEARCH_RADIUS_METERS = 50000;
const FACILITY_SEARCH_PAGE_SIZE = 10;
const MARKER_COLOR_MINT = "#63d8bb";
const MARKER_COLOR_CORAL = "#ff7f63";
const CLUSTER_AUTO_DISABLE_ZOOM = 11;
const rawSpotCoordSource = String(import.meta.env.VITE_SPOT_COORD_SOURCE || "wgs84").trim().toLowerCase();
const SPOT_COORD_SOURCE: SpotCoordSource = (
  rawSpotCoordSource === "gcj02" || rawSpotCoordSource === "amap"
    ? rawSpotCoordSource
    : "wgs84"
);
const GCJ_EARTH_A = 6378245.0;
const GCJ_EE = 0.00669342162296594323;
const SPOT_COORD_OVERRIDES: Array<{ pattern: RegExp; coord: LngLatTuple; source: SpotCoordSource }> = [
  // 班公错为跨境湖泊，数据库中心点会偏到西侧，手动锚到中国侧湖区并参与坐标系转换。
  { pattern: /班公错/i, coord: [79.86, 33.74], source: "wgs84" },
  // 韭山列岛为海岛群，数据库范围中点容易落在海面，手动锚到群岛陆地区域。
  { pattern: /韭山列岛|非山列岛/i, coord: [122.16, 29.42], source: "wgs84" },
  // 珠海中华白海豚湿地范围较大，中点偏南落海，锚到万山群岛陆地区域。
  { pattern: /珠海.*白海豚|中华白海豚/i, coord: [113.92, 22.10], source: "wgs84" },
];
const CURRENT_LOCATION_ON_MAP = (() => {
  const [lng, lat] = toMapCoordinate(CURRENT_LOCATION.lng, CURRENT_LOCATION.lat);
  return { ...CURRENT_LOCATION, lng, lat };
})();

let amapLoaderPromise: Promise<any> | null = null;

function outOfChina(lng: number, lat: number) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

function transformLat(lng: number, lat: number) {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
  ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0;
  ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0;
  return ret;
}

function transformLng(lng: number, lat: number) {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
  ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0;
  ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0;
  return ret;
}

function wgs84ToGcj02(lng: number, lat: number): LngLatTuple {
  if (outOfChina(lng, lat)) {
    return [lng, lat];
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = lat / 180.0 * Math.PI;
  let magic = Math.sin(radLat);
  magic = 1 - GCJ_EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / ((GCJ_EARTH_A * (1 - GCJ_EE)) / (magic * sqrtMagic) * Math.PI);
  dLng = (dLng * 180.0) / (GCJ_EARTH_A / sqrtMagic * Math.cos(radLat) * Math.PI);
  const mgLat = lat + dLat;
  const mgLng = lng + dLng;
  return [mgLng, mgLat];
}

function toMapCoordinateBySource(lng: number, lat: number, source: SpotCoordSource): LngLatTuple {
  const normalizedLng = Number(lng);
  const normalizedLat = Number(lat);
  if (!Number.isFinite(normalizedLng) || !Number.isFinite(normalizedLat)) {
    return [Number(lng), Number(lat)];
  }
  if (source === "gcj02" || source === "amap") {
    return [normalizedLng, normalizedLat];
  }
  return wgs84ToGcj02(normalizedLng, normalizedLat);
}

function toMapCoordinate(lng: number, lat: number): LngLatTuple {
  return toMapCoordinateBySource(lng, lat, SPOT_COORD_SOURCE);
}

function resolveSpotCoordinateOverride(spot: Spot): { coord: LngLatTuple; source: SpotCoordSource } | null {
  const name = String(spot?.name || "").trim();
  if (!name) return null;
  const matched = SPOT_COORD_OVERRIDES.find((item) => item.pattern.test(name));
  return matched ? { coord: matched.coord, source: matched.source } : null;
}

function normalizeSpotCoordinate(spot: Spot): Spot {
  const override = resolveSpotCoordinateOverride(spot);
  if (override) {
    const [lng, lat] = toMapCoordinateBySource(override.coord[0], override.coord[1], override.source);
    return {
      ...spot,
      lng,
      lat,
    };
  }
  const [lng, lat] = toMapCoordinate(spot.lng, spot.lat);
  return { ...spot, lng, lat };
}

function normalizeSpotsForMap(items: Spot[]): Spot[] {
  return items.map(normalizeSpotCoordinate);
}

function createEmptyRoutePlans(): Record<RouteMode, RoutePlanResult | null> {
  return {
    driving: null,
    walking: null,
    riding: null,
  };
}

function createEmptyFacilitiesMap(): Record<FacilityCategoryKey, NearbyFacility[]> {
  return {
    hotel: [],
    food: [],
    parking: [],
    service: [],
  };
}

function buildRouteCacheKey(spot: Spot | null) {
  if (!spot) return "";
  const spotId = String(spot.id || "").trim();
  if (!spotId) return "";
  return `${spotId}:${Number(spot.lng)}:${Number(spot.lat)}`;
}

function buildFacilityCacheKey(spotId: string, categoryKey: FacilityCategoryKey) {
  return `${spotId}::${categoryKey}`;
}

function escapeHtml(text: string) {
  return String(text || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function toNumericLngLatPair(value: any): LngLatTuple | null {
  if (Array.isArray(value) && value.length >= 2) {
    const lng = Number(value[0]);
    const lat = Number(value[1]);
    if (Number.isFinite(lng) && Number.isFinite(lat)) {
      return [lng, lat];
    }
  }

  const lngFromGetter = Number(value?.getLng?.());
  const latFromGetter = Number(value?.getLat?.());
  if (Number.isFinite(lngFromGetter) && Number.isFinite(latFromGetter)) {
    return [lngFromGetter, latFromGetter];
  }

  const lng = Number(value?.lng);
  const lat = Number(value?.lat);
  if (Number.isFinite(lng) && Number.isFinite(lat)) {
    return [lng, lat];
  }

  return null;
}

function appendUniquePoint(path: LngLatTuple[], point: any) {
  const normalized = toNumericLngLatPair(point);
  if (!normalized) return;
  const last = path[path.length - 1];
  if (last && Math.abs(last[0] - normalized[0]) < 1e-7 && Math.abs(last[1] - normalized[1]) < 1e-7) {
    return;
  }
  path.push(normalized);
}

function toRadians(degree: number) {
  return (degree * Math.PI) / 180;
}

function haversineDistanceMeters(from: LngLatTuple, to: LngLatTuple) {
  const earthRadius = 6378137;
  const dLat = toRadians(to[1] - from[1]);
  const dLng = toRadians(to[0] - from[0]);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(from[1])) * Math.cos(toRadians(to[1])) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadius * c;
}

function distanceMeters(from: LngLatTuple, to: LngLatTuple) {
  if (
    typeof window !== "undefined"
    && window.AMap
    && window.AMap.GeometryUtil
    && typeof window.AMap.GeometryUtil.distance === "function"
  ) {
    return Number(window.AMap.GeometryUtil.distance(from, to)) || haversineDistanceMeters(from, to);
  }
  return haversineDistanceMeters(from, to);
}

function formatDistance(meters: number) {
  if (!Number.isFinite(meters) || meters <= 0) return "未知";
  if (meters < 1000) return `${Math.max(1, Math.round(meters))}m`;
  const km = meters / 1000;
  return `${km.toFixed(km >= 10 ? 0 : 1)}km`;
}

function createSpotPinSvg(fillColor: string) {
  return `
    <svg viewBox="0 0 32 44" width="32" height="44" aria-hidden="true" focusable="false">
      <path d="M16 1C8.82 1 3 6.82 3 14c0 9.5 10.6 20.88 12.16 22.48.47.48 1.21.48 1.68 0C18.4 34.88 29 23.5 29 14 29 6.82 23.18 1 16 1z" fill="${fillColor}" />
      <circle cx="16" cy="14" r="5.2" fill="rgba(255,255,255,0.95)" />
    </svg>
  `;
}

function createSpotMarkerContent(spotName: string, isActive: boolean) {
  return `
    <div class="tm-spot-marker ${isActive ? "is-active" : ""}">
      <div class="tm-spot-pin">
        ${createSpotPinSvg(isActive ? MARKER_COLOR_CORAL : MARKER_COLOR_MINT)}
      </div>
      <div class="tm-spot-tooltip">${escapeHtml(spotName)}</div>
    </div>
  `;
}

function createClusterMarkerContent(count: number, options?: { isClicked?: boolean }) {
  const safeCount = Number.isFinite(count) ? Math.max(0, Math.round(count)) : 0;
  const clusterLabel = safeCount > 99 ? "99+" : String(safeCount);
  return `
    <div class="tm-cluster-marker ${options?.isClicked ? "is-clicked" : ""}">
      <span class="tm-cluster-ring tm-cluster-ring-1" aria-hidden="true"></span>
      <span class="tm-cluster-ring tm-cluster-ring-2" aria-hidden="true"></span>
      <span class="tm-cluster-core">${clusterLabel}</span>
      <span class="tm-cluster-preview" aria-hidden="true">
        <span class="tm-cluster-preview-title">${safeCount} 个湿地</span>
        <span class="tm-cluster-preview-subtitle">点击展开查看</span>
      </span>
    </div>
  `;
}

function applySpotMarkerLayout(marker: any) {
  if (!marker) return;
  if (typeof marker.setAnchor === "function") {
    marker.setAnchor("bottom-center");
  }
  if (typeof marker.setOffset === "function" && window.AMap?.Pixel) {
    marker.setOffset(new window.AMap.Pixel(0, 0));
  }
}

function applyClusterMarkerLayout(marker: any) {
  if (!marker) return;
  if (typeof marker.setAnchor === "function") {
    marker.setAnchor("center");
  }
  if (typeof marker.setOffset === "function" && window.AMap?.Pixel) {
    marker.setOffset(new window.AMap.Pixel(-34, -34));
  }
  if (typeof marker.setzIndex === "function") {
    // Keep cluster overlays above spot labels/tooltips to avoid hover-card occlusion.
    marker.setzIndex(260);
  }
}

function cleanupRuntimeScripts() {
  if (typeof document === "undefined") return;
  Array.from(document.querySelectorAll("script"))
    .filter((item) => String((item as HTMLScriptElement)?.src || "").includes("webapi.amap.com/maps"))
    .forEach((item) => item.remove());
}

function resetAmapRuntime() {
  if (typeof window === "undefined") return;
  cleanupRuntimeScripts();
  try {
    delete window.AMap;
  } catch {
    window.AMap = undefined;
  }
  try {
    delete window._AMapSecurityConfig;
  } catch {
    window._AMapSecurityConfig = undefined;
  }
}

function loadAmapScriptOnce(securityJsCode = ""): Promise<any> {
  return new Promise((resolve, reject) => {
    resetAmapRuntime();
    if (securityJsCode) {
      window._AMapSecurityConfig = { securityJsCode };
    }

    const src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=${AMAP_PLUGIN_LIST}&_ts=${Date.now()}`;
    const script = document.createElement("script");
    let timeoutId: number | null = null;

    const cleanup = () => {
      if (timeoutId != null) {
        window.clearTimeout(timeoutId);
        timeoutId = null;
      }
      script.removeEventListener("load", handleLoad);
      script.removeEventListener("error", handleError);
    };

    const handleLoad = () => {
      cleanup();
      if (window.AMap) {
        resolve(window.AMap);
      } else {
        reject(new Error("AMap script loaded, but window.AMap is unavailable."));
      }
    };

    const handleError = () => {
      cleanup();
      reject(new Error("Failed to load AMap script."));
    };

    timeoutId = window.setTimeout(() => {
      cleanup();
      reject(new Error("AMap script load timeout."));
    }, 15000);

    script.src = src;
    script.async = true;
    script.defer = true;
    script.setAttribute("data-amap-runtime-loader", "true");
    script.addEventListener("load", handleLoad, { once: true });
    script.addEventListener("error", handleError, { once: true });
    document.head.appendChild(script);
  });
}

function ensureAMapScriptLoaded() {
  if (typeof window === "undefined" || typeof document === "undefined") {
    return Promise.reject(new Error("Current environment does not support map loading."));
  }

  if (window.AMap) {
    return Promise.resolve(window.AMap);
  }

  if (amapLoaderPromise) {
    return amapLoaderPromise;
  }

  if (!AMAP_KEY) {
    return Promise.reject(new Error("Missing VITE_AMAP_KEY in frontend/.env.local."));
  }

  const securityJsCode = AMAP_SECURITY_JS_CODE || String(window.__AMAP_SECURITY_JS_CODE || "").trim();
  amapLoaderPromise = (async () => {
    try {
      return await loadAmapScriptOnce(securityJsCode);
    } catch (firstError) {
      if (!securityJsCode) {
        throw firstError;
      }
      return loadAmapScriptOnce("");
    }
  })().catch((error) => {
    amapLoaderPromise = null;
    throw error;
  });

  return amapLoaderPromise;
}

function extractRouteDataFromResult(result: any, origin: LngLatTuple, destination: LngLatTuple): RoutePlanResult {
  const route = Array.isArray(result?.routes) ? result.routes[0] : null;
  const output: LngLatTuple[] = [];

  if (route) {
    if (Array.isArray(route.steps)) {
      route.steps.forEach((step: any) => {
        if (Array.isArray(step?.path)) {
          step.path.forEach((point: any) => appendUniquePoint(output, point));
        }
      });
    }

    if (!output.length && Array.isArray(route.rides)) {
      route.rides.forEach((ride: any) => {
        if (Array.isArray(ride?.path)) {
          ride.path.forEach((point: any) => appendUniquePoint(output, point));
        }
      });
    }

    if (!output.length && Array.isArray(route.path)) {
      route.path.forEach((point: any) => appendUniquePoint(output, point));
    }
  }

  if (!output.length) {
    appendUniquePoint(output, origin);
    appendUniquePoint(output, destination);
  }

  const distanceCandidate = Number(route?.distance ?? result?.distance);
  const durationCandidate = Number(route?.time ?? route?.duration ?? result?.time ?? result?.duration);

  return {
    path: output,
    summary: {
      distanceMeters: Number.isFinite(distanceCandidate) ? distanceCandidate : null,
      durationSeconds: Number.isFinite(durationCandidate) ? durationCandidate : null,
    },
  };
}

function buildFallbackRoute(_origin: LngLatTuple, _destination: LngLatTuple, errorMessage = ""): RoutePlanResult {
  return {
    path: [],
    summary: {
      distanceMeters: null,
      durationSeconds: null,
      unavailable: true,
      errorMessage,
    },
  };
}

function buildRoadGeometryFallbackRoute(
  mode: RouteMode,
  drivingPlan: RoutePlanResult,
  origin: LngLatTuple,
  destination: LngLatTuple,
  errorMessage = "",
): RoutePlanResult {
  const drivingDistance = Number(drivingPlan?.summary?.distanceMeters);
  const resolvedDistance = Number.isFinite(drivingDistance) && drivingDistance > 0
    ? drivingDistance
    : distanceMeters(origin, destination);

  let estimatedDuration: number | null = null;
  if (Number.isFinite(resolvedDistance) && resolvedDistance > 0) {
    if (mode === "walking") {
      // ~4.7km/h
      estimatedDuration = Math.round(resolvedDistance / 1.3);
    } else if (mode === "riding") {
      // ~15km/h
      estimatedDuration = Math.round(resolvedDistance / 4.2);
    } else {
      estimatedDuration = drivingPlan?.summary?.durationSeconds ?? null;
    }
  }

  return {
    path: Array.isArray(drivingPlan?.path) && drivingPlan.path.length >= 2
      ? drivingPlan.path
      : [origin, destination],
    summary: {
      distanceMeters: Number.isFinite(resolvedDistance) ? resolvedDistance : null,
      durationSeconds: estimatedDuration,
      unavailable: true,
      errorMessage: errorMessage
        ? `${errorMessage}; 使用驾车道路形状近似展示`
        : "使用驾车道路形状近似展示",
    },
  };
}

function resolveAdaptiveZoom(distanceValue: number) {
  if (!Number.isFinite(distanceValue) || distanceValue <= 0) {
    return 8;
  }

  if (distanceValue < 800) return 13.5;
  if (distanceValue < 2000) return 12.8;
  if (distanceValue < 5000) return 12.2;
  if (distanceValue < 10000) return 11.4;
  if (distanceValue < 20000) return 10.8;
  if (distanceValue < 40000) return 10.1;
  if (distanceValue < 80000) return 9.4;
  if (distanceValue < 160000) return 8.7;
  if (distanceValue < 320000) return 8.0;
  if (distanceValue < 600000) return 7.3;
  if (distanceValue < 1000000) return 6.6;
  return 5.8;
}

function capZoomToDistance(
  map: any,
  distanceValue: number,
  requiredPoints: LngLatTuple[] = [],
  fitPadding: MapFitPadding = [80, 320, 80, 80],
) {
  if (!map || typeof map.getZoom !== "function" || typeof map.setZoom !== "function") {
    return;
  }

  const preferredZoom = resolveAdaptiveZoom(distanceValue);
  // Keep a readable upper zoom and a gentle floor for very long routes.
  const safeUpperZoom = Math.min(preferredZoom + 0.9, 13.4);
  const longRouteFloor = distanceValue >= 3_000_000
    ? 5.2
    : distanceValue >= 2_000_000
      ? 5.0
      : distanceValue >= 1_200_000
        ? 4.8
        : Number.NEGATIVE_INFINITY;
  const safeLowerZoom = Math.max(preferredZoom - 1.2, longRouteFloor);
  const currentZoom = Number(map.getZoom());
  if (!Number.isFinite(currentZoom)) {
    map.setZoom(Math.min(Math.max(preferredZoom, safeLowerZoom), safeUpperZoom));
    return;
  }

  if (currentZoom > safeUpperZoom + 0.01) {
    map.setZoom(safeUpperZoom);
    return;
  }

  if (currentZoom < safeLowerZoom - 0.01) {
    map.setZoom(safeLowerZoom);
    if (
      requiredPoints.length >= 2
      && !arePointsVisibleWithinPadding(map, requiredPoints, fitPadding)
    ) {
      map.setZoom(currentZoom);
    }
  }
}

function normalizeLookupKey(value: unknown) {
  return String(value ?? "").trim().toLowerCase();
}

function resolveScenicIdFromSpot(spot: Spot | null): string {
  const id = String(spot?.id || "").toLowerCase();
  const name = String(spot?.name || "").toLowerCase();
  const pinyin = String(spot?.pinyin || "").toLowerCase();

  if (id.includes("ancient") || name.includes("古镇") || pinyin.includes("guzhen")) {
    return "ancient-town";
  }
  if (id.includes("jiuzhaigou") || name.includes("九寨沟") || pinyin.includes("jiuzhaigou")) {
    return "jiuzhaigou";
  }
  return "jiuzhaigou";
}

function resolveMapSize(map: any): { width: number; height: number } | null {
  if (!map || typeof map.getSize !== "function") return null;
  const size = map.getSize();
  if (!size) return null;

  const width = Number(typeof size.getWidth === "function" ? size.getWidth() : size.width);
  const height = Number(typeof size.getHeight === "function" ? size.getHeight() : size.height);
  if (!Number.isFinite(width) || !Number.isFinite(height)) {
    return null;
  }
  return { width, height };
}

function arePointsVisibleWithinPadding(
  map: any,
  points: LngLatTuple[],
  padding: MapFitPadding,
) {
  if (!map || typeof map.lngLatToContainer !== "function") return true;
  const size = resolveMapSize(map);
  if (!size) return true;

  const [top, right, bottom, left] = padding;
  const minX = left;
  const maxX = size.width - right;
  const minY = top;
  const maxY = size.height - bottom;
  if (maxX <= minX || maxY <= minY) return true;

  return points.every((point) => {
    const pixel = map.lngLatToContainer(point);
    const x = Number(pixel?.getX?.() ?? pixel?.x);
    const y = Number(pixel?.getY?.() ?? pixel?.y);
    if (!Number.isFinite(x) || !Number.isFinite(y)) {
      return true;
    }
    return x >= minX && x <= maxX && y >= minY && y <= maxY;
  });
}

function buildSpotMarkersSignature(items: Spot[]) {
  return items
    .map((spot) => `${spot.id}:${Number(spot.lng).toFixed(5)}:${Number(spot.lat).toFixed(5)}`)
    .join("|");
}

function buildFacilitiesSignature(facilities: NearbyFacility[]) {
  return facilities
    .filter((item) => Array.isArray(item.location))
    .slice(0, 8)
    .map((item) => {
      const location = Array.isArray(item.location) ? item.location : [];
      return `${item.id}:${Number(location[0]).toFixed(5)},${Number(location[1]).toFixed(5)}:${item.distance}`;
    })
    .join("|");
}

export default function MapView() {
  const mapContainer = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<any>(null);
  const routePolyline = useRef<any>(null);
  const scenicMarkers = useRef<any[]>([]);
  const scenicMarkerByIdRef = useRef<Map<string, any>>(new Map());
  const spotByIdRef = useRef<Map<string, Spot>>(new Map());
  const markerClusterRef = useRef<any>(null);
  const selectedSpotIdRef = useRef("");
  const currentLocationMarkerRef = useRef<any>(null);
  const poiMarkers = useRef<any[]>([]);
  const infoWindowRef = useRef<any>(null);
  const routeServiceRef = useRef<Partial<Record<RouteMode, any>>>({});
  const routePlansCacheRef = useRef<Map<string, Record<RouteMode, RoutePlanResult | null>>>(new Map());
  const facilitiesCacheRef = useRef<Map<string, NearbyFacility[]>>(new Map());
  const facilitiesFetchTokenRef = useRef(0);
  const routeFetchTokenRef = useRef(0);
  const scenicMarkersSignatureRef = useRef("");
  const poiMarkersSignatureRef = useRef("");
  const routeViewStateRef = useRef("");
  const querySpotKeyRef = useRef<string>(
    typeof window !== "undefined"
      ? normalizeLookupKey(new URLSearchParams(window.location.search).get("spot"))
      : "",
  );
  const querySpotHandledRef = useRef(false);

  const [mapReady, setMapReady] = useState(false);
  const [mapError, setMapError] = useState("");
  const [spotsDataError, setSpotsDataError] = useState("");
  const [spots, setSpots] = useState<Spot[]>(() => normalizeSpotsForMap(SPOTS));
  const [selectedSpot, setSelectedSpot] = useState<Spot | null>(null);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [showSearchResults, setShowSearchResults] = useState(false);
  const [activeRouteMode, setActiveRouteMode] = useState<RouteMode>("driving");
  const [activeFacilityTab, setActiveFacilityTab] = useState<FacilityCategoryKey>("hotel");
  const [routePlans, setRoutePlans] = useState<Record<RouteMode, RoutePlanResult | null>>(createEmptyRoutePlans);
  const [nearbyFacilities, setNearbyFacilities] = useState<Record<FacilityCategoryKey, NearbyFacility[]>>(createEmptyFacilitiesMap);
  const [routeLoading, setRouteLoading] = useState(false);
  const [facilitiesLoading, setFacilitiesLoading] = useState(false);
  const [clusterEnabled, setClusterEnabled] = useState(true);
  const [mapZoom, setMapZoom] = useState(5);
  const setMarkerDebug = (_debug: any) => {};

  const normalizedKeyword = searchKeyword.trim().toLowerCase();
  const filteredSpots = useMemo(() => {
    if (!normalizedKeyword) return [];
    return spots
      .filter((spot) => [spot.name, spot.pinyin, spot.id]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(normalizedKeyword)))
      .slice(0, 8);
  }, [normalizedKeyword, spots]);
  const activeFacilities = useMemo(
    () => nearbyFacilities[activeFacilityTab] || [],
    [nearbyFacilities, activeFacilityTab],
  );
  const shouldUseCluster = clusterEnabled && mapZoom < CLUSTER_AUTO_DISABLE_ZOOM;

  useEffect(() => {
    let disposed = false;
    const controller = new AbortController();

    (async () => {
      try {
        const remoteSpots = await fetchWetlandSpots(controller.signal);
        if (disposed) return;
        setSpots(normalizeSpotsForMap(remoteSpots));
        setSpotsDataError("");
      } catch (error: any) {
        if (disposed || error?.name === "AbortError") {
          return;
        }
        setSpots(normalizeSpotsForMap(SPOTS));
        setSpotsDataError(error?.message || "failed to load remote wetland spots");
      }
    })();

    return () => {
      disposed = true;
      controller.abort();
    };
  }, []);

  useEffect(() => {
    if (!selectedSpot) return;
    const replacement = spots.find((item) => item.id === selectedSpot.id);
    if (!replacement) {
      setSelectedSpot(null);
      return;
    }
    if (
      replacement.lat !== selectedSpot.lat
      || replacement.lng !== selectedSpot.lng
      || replacement.name !== selectedSpot.name
      || replacement.type !== selectedSpot.type
      || replacement.description !== selectedSpot.description
    ) {
      setSelectedSpot(replacement);
    }
  }, [spots, selectedSpot]);

  useEffect(() => {
    selectedSpotIdRef.current = String(selectedSpot?.id || "");
  }, [selectedSpot]);

  const routeSummaries = useMemo<Record<RouteMode, RouteSummary | null>>(
    () => ({
      driving: routePlans.driving?.summary ?? null,
      walking: routePlans.walking?.summary ?? null,
      riding: routePlans.riding?.summary ?? null,
    }),
    [routePlans],
  );

  const clearRoutePolyline = useCallback(() => {
    if (routePolyline.current && mapInstance.current) {
      mapInstance.current.remove(routePolyline.current);
      routePolyline.current = null;
    }
    routeViewStateRef.current = "";
  }, []);

  const clearPoiMarkers = useCallback(() => {
    if (poiMarkers.current.length && mapInstance.current) {
      mapInstance.current.remove(poiMarkers.current);
    }
    poiMarkers.current = [];
    poiMarkersSignatureRef.current = "";
  }, []);

  const drawRoutePolyline = useCallback((path: LngLatTuple[]) => {
    if (!mapInstance.current || !window.AMap) return;

    clearRoutePolyline();
    if (!Array.isArray(path) || path.length < 2) return;

    routePolyline.current = new window.AMap.Polyline({
      path,
      strokeColor: "#c4813a",
      strokeWeight: 5,
      strokeOpacity: 0.9,
      lineJoin: "round",
      showDir: true,
    });

    const overlays = [routePolyline.current, ...poiMarkers.current].filter(Boolean);
    mapInstance.current.add(overlays);
  }, [clearRoutePolyline]);

  const drawPoiMarkers = useCallback((facilities: NearbyFacility[]) => {
    if (!mapInstance.current || !window.AMap) return;
    const markerItems = facilities
      .filter((item) => Array.isArray(item.location))
      .slice(0, 8);
    const nextSignature = buildFacilitiesSignature(markerItems);
    if (nextSignature === poiMarkersSignatureRef.current) {
      return;
    }

    clearPoiMarkers();
    if (!markerItems.length) {
      return;
    }

    const markers = markerItems.map((item, index) => {
        const marker = new window.AMap.Marker({
          position: item.location,
          anchor: "bottom-center",
          content: `<div class="tm-poi-marker">${index + 1}</div>`,
        });

        marker.on("click", () => {
          if (!infoWindowRef.current) {
            infoWindowRef.current = new window.AMap.InfoWindow({
              offset: new window.AMap.Pixel(0, -20),
            });
          }
          infoWindowRef.current.setContent(`
            <div style="padding:8px 10px;min-width:200px;">
              <strong style="display:block;font-size:14px;margin-bottom:4px;">${escapeHtml(item.name)}</strong>
              <div style="font-size:12px;line-height:1.6;color:#5f6258;">${escapeHtml(item.address || "周边设施")}</div>
              <div style="margin-top:6px;font-size:12px;color:#7c5b31;">距离：${escapeHtml(item.distance)}</div>
            </div>
          `);
          infoWindowRef.current.open(mapInstance.current, item.location);
        });

        return marker;
      });

    mapInstance.current.add(markers);
    poiMarkers.current = markers;
    poiMarkersSignatureRef.current = nextSignature;
  }, [clearPoiMarkers]);

  const getRouteService = useCallback((mode: RouteMode) => {
    if (!window.AMap) return null;
    if (routeServiceRef.current[mode]) {
      return routeServiceRef.current[mode];
    }

    let service: any = null;
    if (mode === "driving" && window.AMap.Driving) {
      service = new window.AMap.Driving({
        hideMarkers: true,
        autoFitView: false,
      });
    } else if (mode === "walking" && window.AMap.Walking) {
      service = new window.AMap.Walking({
        hideMarkers: true,
        autoFitView: false,
      });
    } else if (mode === "riding" && window.AMap.Riding) {
      service = new window.AMap.Riding({
        hideMarkers: true,
        autoFitView: false,
      });
    }

    if (!service) return null;
    routeServiceRef.current[mode] = service;
    return service;
  }, []);

  const requestRoute = useCallback((mode: RouteMode, origin: LngLatTuple, destination: LngLatTuple) => {
    return new Promise<RoutePlanResult>((resolve, reject) => {
      const service = getRouteService(mode);
      if (!service) {
        reject(new Error(`${mode} route service unavailable`));
        return;
      }

      let settled = false;
      const timer = window.setTimeout(() => {
        if (settled) return;
        settled = true;
        reject(new Error(`${mode} route timeout`));
      }, 15000);

      service.search(origin, destination, (status: string, result: any) => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timer);
        if (String(status).toLowerCase() === "complete") {
          resolve(extractRouteDataFromResult(result, origin, destination));
          return;
        }
        reject(new Error(`route failed: ${status || "unknown"}`));
      });
    });
  }, [getRouteService]);

  const searchNearbyKeyword = useCallback((keyword: string, center: LngLatTuple, radius = FACILITY_SEARCH_RADIUS_METERS) => {
    return new Promise<any[]>((resolve) => {
      if (!window.AMap || !window.AMap.PlaceSearch) {
        resolve([]);
        return;
      }

      const placeSearch = new window.AMap.PlaceSearch({
        pageSize: FACILITY_SEARCH_PAGE_SIZE,
        pageIndex: 1,
        extensions: "all",
      });

      placeSearch.searchNearBy(keyword, center, radius, (status: string, result: any) => {
        if (String(status).toLowerCase() !== "complete") {
          resolve([]);
          return;
        }
        const pois = Array.isArray(result?.poiList?.pois) ? result.poiList.pois : [];
        resolve(pois);
      });
    });
  }, []);

  const searchFacilitiesByCategory = useCallback(async (category: FacilityCategory, center: LngLatTuple) => {
    const collected: NearbyFacility[] = [];
    const seen = new Set<string>();

    for (const keyword of category.keywords) {
      const pois = await searchNearbyKeyword(keyword, center, FACILITY_SEARCH_RADIUS_METERS);
      for (let index = 0; index < pois.length; index += 1) {
        const poi = pois[index];
        const location = toNumericLngLatPair(poi?.location);
        const dedupeKey = `${poi?.id || ""}|${poi?.name || ""}|${location?.join(",") || ""}`;
        if (seen.has(dedupeKey)) continue;
        seen.add(dedupeKey);

        const distanceValue = Number(poi?.distance);
        const distance = Number.isFinite(distanceValue) && distanceValue > 0
          ? distanceValue
          : (location ? distanceMeters(center, location) : Number.NaN);

        const ratingValue = Number(poi?.biz_ext?.rating ?? poi?.rating);

        collected.push({
          id: String(poi?.id || `${category.key}-${keyword}-${index}`),
          name: String(poi?.name || `${keyword} service point`),
          type: category.key,
          distance: formatDistance(distance),
          rating: Number.isFinite(ratingValue) ? ratingValue : 0,
          address: String(poi?.address || poi?.district || "").trim() || undefined,
          location: location || undefined,
        });

        if (collected.length >= 8) {
          return collected;
        }
      }
    }

    return collected;
  }, [searchNearbyKeyword]);

  const flyToSpot = useCallback((spot: Spot) => {
    if (mapInstance.current) {
      const origin: LngLatTuple = [CURRENT_LOCATION_ON_MAP.lng, CURRENT_LOCATION_ON_MAP.lat];
      const destination: LngLatTuple = [spot.lng, spot.lat];
      const straightDistance = distanceMeters(origin, destination);
      const previewZoom = Math.max(8.2, Math.min(resolveAdaptiveZoom(straightDistance) + 0.6, 11.8));

      if (typeof mapInstance.current.setZoomAndCenter === "function") {
        mapInstance.current.setZoomAndCenter(previewZoom, [spot.lng, spot.lat]);
      } else if (typeof mapInstance.current.panTo === "function") {
        mapInstance.current.panTo([spot.lng, spot.lat]);
      } else if (typeof mapInstance.current.setCenter === "function") {
        mapInstance.current.setCenter([spot.lng, spot.lat]);
      }
    }
    setSelectedSpot(spot);
    setActiveFacilityTab("hotel");
  }, []);

  const handleOpenScenicDetail = useCallback((spot: Spot) => {
    const scenicId = resolveScenicIdFromSpot(spot);
    const detailPath = `/consult/${encodeURIComponent(scenicId)}`;

    if (typeof window === "undefined") return;

    try {
      if (window.top && window.top !== window && window.top.location) {
        window.top.location.href = detailPath;
        return;
      }
    } catch {
      // Ignore cross-window access errors and fallback to current window navigation.
    }

    window.location.href = detailPath;
  }, []);

  useEffect(() => {
    if (querySpotHandledRef.current) return;
    const queryKey = querySpotKeyRef.current;
    if (!queryKey) {
      querySpotHandledRef.current = true;
      return;
    }
    if (!mapReady || !spots.length) return;

    const matched = spots.find((item) => {
      const idKey = normalizeLookupKey(item.id);
      const nameKey = normalizeLookupKey(item.name);
      const pinyinKey = normalizeLookupKey(item.pinyin);
      return queryKey === idKey || queryKey === nameKey || queryKey === pinyinKey;
    });

    querySpotHandledRef.current = true;
    if (!matched) return;
    flyToSpot(matched);
    setSearchKeyword(matched.name);
    setShowSearchResults(false);
  }, [mapReady, spots, flyToSpot]);

  useEffect(() => {
    if (!mapContainer.current || mapInstance.current) return;
    let disposed = false;
    let mapForCleanup: any = null;
    let zoomListener: (() => void) | null = null;

    (async () => {
      try {
        const AMap = await ensureAMapScriptLoaded();
        if (disposed || !mapContainer.current) return;

        const map = new AMap.Map(mapContainer.current, {
          center: [104.1954, 35.8617],
          zoom: 5,
          mapStyle: "amap://styles/fresh",
          resizeEnable: true,
          showIndoorMap: false,
          dragEnable: true,
          animateEnable: true,
          jogEnable: true,
        });
        mapInstance.current = map;
        mapForCleanup = map;

        zoomListener = () => {
          const zoom = Number(map.getZoom?.());
          if (Number.isFinite(zoom)) {
            setMapZoom(zoom);
          }
        };
        if (typeof map.on === "function") {
          map.on("zoomend", zoomListener);
        }
        zoomListener();

        const currentMarker = new AMap.Marker({
          position: [CURRENT_LOCATION_ON_MAP.lng, CURRENT_LOCATION_ON_MAP.lat],
          anchor: "center",
          content: `
            <div class="tm-current-marker">
              <div class="marker-pulse"></div>
              <div class="tm-current-dot"></div>
            </div>
          `,
        });
        map.add(currentMarker);
        currentLocationMarkerRef.current = currentMarker;

        setMapReady(true);
      } catch (error: any) {
        setMapError(error?.message || "地图加载失败");
      }
    })();

    return () => {
      disposed = true;
      if (mapForCleanup && zoomListener && typeof mapForCleanup.off === "function") {
        mapForCleanup.off("zoomend", zoomListener);
      }
      clearRoutePolyline();
      clearPoiMarkers();

      if (markerClusterRef.current) {
        if (typeof markerClusterRef.current.setMap === "function") {
          markerClusterRef.current.setMap(null);
        } else if (typeof markerClusterRef.current.clearMarkers === "function") {
          markerClusterRef.current.clearMarkers();
        } else if (typeof markerClusterRef.current.setMarkers === "function") {
          markerClusterRef.current.setMarkers([]);
        }
        markerClusterRef.current = null;
      }
      if (scenicMarkers.current.length && mapInstance.current) {
        mapInstance.current.remove(scenicMarkers.current);
      }
      scenicMarkers.current = [];
      scenicMarkerByIdRef.current.clear();
      spotByIdRef.current.clear();
      currentLocationMarkerRef.current = null;
      scenicMarkersSignatureRef.current = "";
      routeServiceRef.current = {};
      routeFetchTokenRef.current += 1;

      if (mapInstance.current && typeof mapInstance.current.destroy === "function") {
        mapInstance.current.destroy();
      }
      mapInstance.current = null;
      setMapReady(false);
      setMapZoom(5);
    };
  }, [clearPoiMarkers, clearRoutePolyline]);

  useEffect(() => {
    if (!mapReady || !mapInstance.current || !window.AMap) return;

    const activeSpotId = String(selectedSpot?.id || "");
    selectedSpotIdRef.current = activeSpotId;
    const nextSignature = `${buildSpotMarkersSignature(spots)}::${activeSpotId}::${shouldUseCluster ? "cluster" : "plain"}`;
    if (nextSignature === scenicMarkersSignatureRef.current) {
      return;
    }

    if (markerClusterRef.current) {
      if (typeof markerClusterRef.current.setMap === "function") {
        markerClusterRef.current.setMap(null);
      } else if (typeof markerClusterRef.current.clearMarkers === "function") {
        markerClusterRef.current.clearMarkers();
      } else if (typeof markerClusterRef.current.setMarkers === "function") {
        markerClusterRef.current.setMarkers([]);
      }
      markerClusterRef.current = null;
    }
    if (scenicMarkers.current.length) {
      mapInstance.current.remove(scenicMarkers.current);
      scenicMarkers.current = [];
    }
    scenicMarkerByIdRef.current.clear();
    spotByIdRef.current.clear();

    const bindClusterClick = (clusterInstance: any) => {
      if (!clusterInstance || typeof clusterInstance.on !== "function") return;
      clusterInstance.on("click", (event: any) => {
        if (!mapInstance.current) return;
        const clickedClusterMarker = event?.marker;
        if (clickedClusterMarker && typeof clickedClusterMarker.setContent === "function") {
          const extData = clickedClusterMarker?.getExtData?.() || {};
          const clusterCount = Number(
            extData?.clusterCount
            ?? event?.count
            ?? event?.clusterData?.length
            ?? event?.cluster?.getMarkers?.()?.length
            ?? 0,
          ) || 0;
          try {
            clickedClusterMarker.setContent(createClusterMarkerContent(clusterCount, { isClicked: true }));
          } catch {
            // Marker may be recycled during quick re-cluster; ignore and keep zoom behavior.
          }
          window.setTimeout(() => {
            try {
              clickedClusterMarker.setContent(createClusterMarkerContent(clusterCount));
            } catch {
              // Marker can be detached after zoom, safe to ignore.
            }
          }, 380);
        }
        const center = toNumericLngLatPair(
          event?.lnglat
          || event?.marker?.getPosition?.()
          || event?.cluster?.getCenter?.(),
        );
        const currentZoom = Number(mapInstance.current.getZoom?.());
        const nextZoom = Math.min(Number.isFinite(currentZoom) ? currentZoom + 2 : 7, 18);
        if (center && typeof mapInstance.current.setZoomAndCenter === "function") {
          mapInstance.current.setZoomAndCenter(nextZoom, center, false, 360);
        } else if (typeof mapInstance.current.setZoom === "function") {
          mapInstance.current.setZoom(nextZoom);
        }
      });
    };

    const useV2Cluster = typeof window.AMap.MarkerCluster === "function";
    if (shouldUseCluster && useV2Cluster && spots.length > 1) {
      const clusterData = spots.map((spot) => {
        const spotId = String(spot.id);
        spotByIdRef.current.set(spotId, spot);
        return {
          lnglat: [spot.lng, spot.lat],
          spotId,
          spotName: spot.name,
        };
      });

      const sharedClusterOptions = {
        gridSize: 68,
        maxZoom: 17,
        minClusterSize: 3,
        averageCenter: true,
      };

      const renderMarker = (context: any) => {
        const marker = context?.marker;
        if (!marker || typeof marker.setContent !== "function") return;

        const dataItem = Array.isArray(context?.data) ? context.data[0] : context?.data;
        const fallbackExtData = marker?.getExtData?.() || {};
        const spotId = String(dataItem?.spotId || fallbackExtData?.spotId || "");
        const spotName = String(dataItem?.spotName || fallbackExtData?.spotName || "");

        const isActive = spotId === selectedSpotIdRef.current;
        applySpotMarkerLayout(marker);
        marker.setExtData({ spotId, spotName });
        marker.setContent(createSpotMarkerContent(spotName, isActive));
        if (typeof marker.setzIndex === "function") {
          marker.setzIndex(isActive ? 160 : 120);
        }
        if (typeof marker.off === "function") {
          marker.off("click");
        }
        if (typeof marker.on === "function") {
          marker.on("click", () => {
            const target = spotByIdRef.current.get(spotId);
            if (target) {
              flyToSpot(target);
            }
          });
        }
      };

      const renderClusterMarker = (context: any) => {
        if (!context?.marker || typeof context.marker.setContent !== "function") return;
        const clusterCount = Number(context.count) || 0;
        applyClusterMarkerLayout(context.marker);
        const existingExtData = context.marker?.getExtData?.() || {};
        if (typeof context.marker.setExtData === "function") {
          context.marker.setExtData({ ...existingExtData, clusterCount });
        }
        context.marker.setContent(createClusterMarkerContent(clusterCount));
      };

      let cluster: any = null;
      const clusterOptions = {
        ...sharedClusterOptions,
        zoomOnClick: false,
        renderMarker,
        renderClusterMarker,
        // Old AMap runtime typo compatibility:
        renderCluserMarker: renderClusterMarker,
      };
      try {
        cluster = new window.AMap.MarkerCluster(mapInstance.current, clusterData, clusterOptions);
      } catch (primaryError) {
        try {
          cluster = new window.AMap.MarkerCluster(mapInstance.current, clusterData, sharedClusterOptions);
        } catch (secondaryError) {
          console.warn("[travel-map] marker cluster init failed, fallback to plain markers", {
            primaryError,
            secondaryError,
          });
        }
      }

      if (cluster) {
        bindClusterClick(cluster);
        markerClusterRef.current = cluster;
        scenicMarkers.current = [];
        scenicMarkersSignatureRef.current = nextSignature;
        setMarkerDebug({
          phase: "cluster-v2-ready",
          created: clusterData.length,
          clustered: "yes-v2",
          error: "",
          domMarkers: 0,
          domSpots: 0,
          domClusters: 0,
        });
        return;
      }
    }

    let markers: any[] = [];
    try {
      markers = spots.map((spot) => {
        const spotId = String(spot.id);
        const marker = new window.AMap.Marker({
          position: [spot.lng, spot.lat],
          anchor: "bottom-center",
          bubble: true,
          cursor: "pointer",
          title: spot.name,
          zIndex: selectedSpotIdRef.current === spotId ? 160 : 120,
          content: createSpotMarkerContent(spot.name, selectedSpotIdRef.current === spotId),
          extData: {
            spotId,
            spotName: spot.name,
          },
        });
        marker.on("click", () => flyToSpot(spot));
        scenicMarkerByIdRef.current.set(spotId, marker);
        spotByIdRef.current.set(spotId, spot);
        return marker;
      });
    } catch (error: any) {
      setMarkerDebug({
        phase: "create-marker-failed",
        created: 0,
        clustered: "no",
        error: String(error?.message || error || "unknown"),
        domMarkers: 0,
        domSpots: 0,
        domClusters: 0,
      });
      return;
    }

    scenicMarkers.current = markers;
    scenicMarkersSignatureRef.current = nextSignature;

    const MarkerClustererCtor = window.AMap.MarkerClusterer;
    if (shouldUseCluster && typeof MarkerClustererCtor === "function" && markers.length > 1) {
      let cluster: any = null;
      try {
        cluster = new MarkerClustererCtor(mapInstance.current, markers, {
          gridSize: 68,
          maxZoom: 17,
          minClusterSize: 3,
          averageCenter: true,
          zoomOnClick: false,
          renderMarker: (context: any) => {
            const marker = context?.marker;
            if (!marker || typeof marker.setContent !== "function") return;
            const extData = marker?.getExtData?.() || {};
            const spotId = String(extData?.spotId || "");
            const spotName = String(extData?.spotName || "");
            const isActive = spotId === selectedSpotIdRef.current;
            applySpotMarkerLayout(marker);
            marker.setContent(createSpotMarkerContent(spotName, isActive));
          },
          renderCluserMarker: (context: any) => {
            if (!context?.marker || typeof context.marker.setContent !== "function") return;
            const clusterCount = Number(context.count) || 0;
            applyClusterMarkerLayout(context.marker);
            const existingExtData = context.marker?.getExtData?.() || {};
            if (typeof context.marker.setExtData === "function") {
              context.marker.setExtData({ ...existingExtData, clusterCount });
            }
            context.marker.setContent(createClusterMarkerContent(clusterCount));
          },
        });
      } catch (error) {
        cluster = null;
      }

      if (cluster) {
        bindClusterClick(cluster);
        markerClusterRef.current = cluster;
        setMarkerDebug({
          phase: "cluster-legacy-ready",
          created: markers.length,
          clustered: "yes-legacy",
          error: "",
          domMarkers: 0,
          domSpots: 0,
          domClusters: 0,
        });
        return;
      }
    }

    if (markers.length) {
      try {
        mapInstance.current.add(markers);
        setMarkerDebug({
          phase: "plain-marker-ready",
          created: markers.length,
          clustered: "no",
          error: "",
          domMarkers: 0,
          domSpots: 0,
          domClusters: 0,
        });
      } catch (error: any) {
        setMarkerDebug({
          phase: "plain-add-failed",
          created: markers.length,
          clustered: "no",
          error: String(error?.message || error || "unknown"),
          domMarkers: 0,
          domSpots: 0,
          domClusters: 0,
        });
      }
    } else {
      setMarkerDebug({
        phase: "no-markers",
        created: 0,
        clustered: "no",
        error: "",
        domMarkers: 0,
        domSpots: 0,
        domClusters: 0,
      });
    }
  }, [
    mapReady,
    spots,
    flyToSpot,
    selectedSpot?.id,
    shouldUseCluster,
  ]);

  useEffect(() => {
    const activeSpotId = String(selectedSpot?.id || "");
    selectedSpotIdRef.current = activeSpotId;
    if (!scenicMarkerByIdRef.current.size) return;

    scenicMarkerByIdRef.current.forEach((marker, spotId) => {
      const spot = spotByIdRef.current.get(spotId);
      if (!spot) return;
      const isActive = spotId === activeSpotId;
      marker.setContent(createSpotMarkerContent(spot.name, isActive));
      if (typeof marker.setzIndex === "function") {
        marker.setzIndex(isActive ? 160 : 120);
      }
    });
  }, [selectedSpot]);

  useEffect(() => {
    routeFetchTokenRef.current += 1;
    if (!selectedSpot || !mapReady) {
      setRoutePlans(createEmptyRoutePlans());
      setRouteLoading(false);
      clearRoutePolyline();
      return;
    }

    const routeCacheKey = buildRouteCacheKey(selectedSpot);
    const cachedPlans = routeCacheKey ? routePlansCacheRef.current.get(routeCacheKey) : null;
    setRoutePlans(cachedPlans || createEmptyRoutePlans());
    setRouteLoading(false);
  }, [selectedSpot, mapReady, clearRoutePolyline]);

  useEffect(() => {
    if (!selectedSpot || !mapReady) {
      return;
    }

    const routeCacheKey = buildRouteCacheKey(selectedSpot);
    if (!routeCacheKey) {
      setRouteLoading(false);
      return;
    }

    const cachedPlans = routePlansCacheRef.current.get(routeCacheKey) || createEmptyRoutePlans();
    const existingPlan = cachedPlans[activeRouteMode];
    if (existingPlan) {
      setRoutePlans((prev) => (
        prev[activeRouteMode] === existingPlan
          ? prev
          : { ...prev, [activeRouteMode]: existingPlan }
      ));
      setRouteLoading(false);
      return;
    }

    const origin: LngLatTuple = [CURRENT_LOCATION_ON_MAP.lng, CURRENT_LOCATION_ON_MAP.lat];
    const destination: LngLatTuple = [selectedSpot.lng, selectedSpot.lat];
    let cancelled = false;
    const requestToken = routeFetchTokenRef.current + 1;
    routeFetchTokenRef.current = requestToken;
    setRouteLoading(true);

    (async () => {
      try {
        const plan = await requestRoute(activeRouteMode, origin, destination);
        if (cancelled || requestToken !== routeFetchTokenRef.current) return;
        const nextPlans = {
          ...(routePlansCacheRef.current.get(routeCacheKey) || createEmptyRoutePlans()),
          [activeRouteMode]: plan,
        };
        routePlansCacheRef.current.set(routeCacheKey, nextPlans);
        setRoutePlans((prev) => ({ ...prev, [activeRouteMode]: plan }));
      } catch (error: any) {
        if (cancelled || requestToken !== routeFetchTokenRef.current) return;

        if (activeRouteMode !== "driving") {
          try {
            const drivingPlan = await requestRoute("driving", origin, destination);
            if (cancelled || requestToken !== routeFetchTokenRef.current) return;
            const geometryFallbackPlan = buildRoadGeometryFallbackRoute(
              activeRouteMode,
              drivingPlan,
              origin,
              destination,
              error?.message || `${activeRouteMode} route unavailable`,
            );
            const nextPlans = {
              ...(routePlansCacheRef.current.get(routeCacheKey) || createEmptyRoutePlans()),
              driving: drivingPlan,
              [activeRouteMode]: geometryFallbackPlan,
            };
            routePlansCacheRef.current.set(routeCacheKey, nextPlans);
            setRoutePlans((prev) => ({
              ...prev,
              driving: prev.driving || drivingPlan,
              [activeRouteMode]: geometryFallbackPlan,
            }));
            return;
          } catch {
            // Fall through to straight-line fallback only when driving route is unavailable too.
          }
        }

        const fallback = buildFallbackRoute(origin, destination, error?.message || "route unavailable");
        const nextPlans = {
          ...(routePlansCacheRef.current.get(routeCacheKey) || createEmptyRoutePlans()),
          [activeRouteMode]: fallback,
        };
        routePlansCacheRef.current.set(routeCacheKey, nextPlans);
        setRoutePlans((prev) => ({ ...prev, [activeRouteMode]: fallback }));
      } finally {
        if (!cancelled && requestToken === routeFetchTokenRef.current) {
          setRouteLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [selectedSpot, mapReady, activeRouteMode, requestRoute]);

  useEffect(() => {
    if (!selectedSpot || !mapReady) {
      facilitiesFetchTokenRef.current += 1;
      setNearbyFacilities(createEmptyFacilitiesMap());
      setFacilitiesLoading(false);
      clearPoiMarkers();
      return;
    }

    const spotId = buildRouteCacheKey(selectedSpot);
    const hydratedFacilities = createEmptyFacilitiesMap();
    FACILITY_CATEGORIES.forEach((category) => {
      const cacheKey = buildFacilityCacheKey(spotId, category.key);
      const cached = facilitiesCacheRef.current.get(cacheKey);
      if (cached) {
        hydratedFacilities[category.key] = cached;
      }
    });

    setNearbyFacilities(hydratedFacilities);
    setFacilitiesLoading(false);
  }, [selectedSpot, mapReady, clearPoiMarkers]);

  useEffect(() => {
    if (!selectedSpot || !mapReady) {
      return;
    }

    const spotId = buildRouteCacheKey(selectedSpot);
    if (!spotId) {
      setFacilitiesLoading(false);
      return;
    }

    const cacheKey = buildFacilityCacheKey(spotId, activeFacilityTab);
    const cachedFacilities = facilitiesCacheRef.current.get(cacheKey);
    if (cachedFacilities) {
      setNearbyFacilities((prev) => (
        prev[activeFacilityTab] === cachedFacilities
          ? prev
          : { ...prev, [activeFacilityTab]: cachedFacilities }
      ));
      setFacilitiesLoading(false);
      return;
    }

    const category = FACILITY_CATEGORIES.find((item) => item.key === activeFacilityTab);
    if (!category) {
      setFacilitiesLoading(false);
      return;
    }

    let cancelled = false;
    const fetchToken = facilitiesFetchTokenRef.current + 1;
    facilitiesFetchTokenRef.current = fetchToken;
    setFacilitiesLoading(true);
    const center: LngLatTuple = [selectedSpot.lng, selectedSpot.lat];

    (async () => {
      try {
        const facilities = await searchFacilitiesByCategory(category, center);
        if (cancelled || fetchToken !== facilitiesFetchTokenRef.current) return;
        facilitiesCacheRef.current.set(cacheKey, facilities);
        setNearbyFacilities((prev) => ({ ...prev, [activeFacilityTab]: facilities }));
      } catch {
        if (cancelled || fetchToken !== facilitiesFetchTokenRef.current) return;
        setNearbyFacilities((prev) => ({ ...prev, [activeFacilityTab]: [] }));
      } finally {
        if (!cancelled && fetchToken === facilitiesFetchTokenRef.current) {
          setFacilitiesLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [selectedSpot, mapReady, activeFacilityTab, searchFacilitiesByCategory]);

  useEffect(() => {
    if (!selectedSpot || !mapInstance.current) {
      clearRoutePolyline();
      return;
    }

    const origin: LngLatTuple = [CURRENT_LOCATION_ON_MAP.lng, CURRENT_LOCATION_ON_MAP.lat];
    const destination: LngLatTuple = [selectedSpot.lng, selectedSpot.lat];
    const plan = routePlans[activeRouteMode];
    const hasRenderableRoutePath = Array.isArray(plan?.path)
      && plan.path.length >= 2
      && !plan?.summary?.unavailable;
    const routePath = hasRenderableRoutePath ? plan.path : [];
    const routeDistance = Number(plan?.summary?.distanceMeters);
    const viewDistance = Number.isFinite(routeDistance) && routeDistance > 0
      ? routeDistance
      : distanceMeters(origin, destination);
    const fitPadding: MapFitPadding = [96, 232, 96, 92];

    drawRoutePolyline(routePath);

    const routeViewKey = `${selectedSpot.id}:${activeRouteMode}:${hasRenderableRoutePath ? routePath.length : 0}:${Math.round(viewDistance)}:${hasRenderableRoutePath ? "path" : "markers"}`;
    if (
      routeViewStateRef.current !== routeViewKey
      && typeof mapInstance.current.setFitView === "function"
    ) {
      const destinationMarker = scenicMarkerByIdRef.current.get(String(selectedSpot.id));
      const overlaysForFit = [
        currentLocationMarkerRef.current,
        destinationMarker,
        hasRenderableRoutePath ? routePolyline.current : null,
      ].filter(Boolean);
      if (overlaysForFit.length >= 2) {
        mapInstance.current.setFitView(overlaysForFit, false, fitPadding);
      }
      routeViewStateRef.current = routeViewKey;
    }

    capZoomToDistance(
      mapInstance.current,
      viewDistance,
      [origin, destination],
      fitPadding,
    );
  }, [activeRouteMode, routePlans, selectedSpot, drawRoutePolyline, clearRoutePolyline]);

  useEffect(() => {
    if (!selectedSpot || !mapInstance.current) {
      clearPoiMarkers();
      return;
    }

    drawPoiMarkers(activeFacilities);
  }, [selectedSpot, activeFacilities, drawPoiMarkers, clearPoiMarkers]);

  const handleZoomIn = () => {
    if (!mapInstance.current) return;
    if (typeof mapInstance.current.zoomIn === "function") {
      mapInstance.current.zoomIn();
    }
  };

  const handleZoomOut = () => {
    if (!mapInstance.current) return;
    if (typeof mapInstance.current.zoomOut === "function") {
      mapInstance.current.zoomOut();
    }
  };

  const handleRecenter = () => {
    if (!mapInstance.current) return;
    if (typeof mapInstance.current.setZoomAndCenter === "function") {
      mapInstance.current.setZoomAndCenter(5, [104.1954, 35.8617]);
    } else if (typeof mapInstance.current.setCenter === "function") {
      mapInstance.current.setCenter([104.1954, 35.8617]);
    }
    clearRoutePolyline();
    clearPoiMarkers();
    setSelectedSpot(null);
    setShowSearchResults(false);
    setRoutePlans(createEmptyRoutePlans());
    setNearbyFacilities(createEmptyFacilitiesMap());
  };

  const handleSearchSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!normalizedKeyword || filteredSpots.length === 0) return;
    flyToSpot(filteredSpots[0]);
    setShowSearchResults(false);
  };

  const handleGoBack = () => {
    if (typeof window === "undefined") return;
    if (window.history.length > 1) {
      window.history.back();
      return;
    }
    window.location.href = "/consult";
  };

  const handlePickSpot = (spot: Spot) => {
    flyToSpot(spot);
    setSearchKeyword(spot.name);
    setShowSearchResults(false);
  };

  return (
    <div className="w-full h-[100dvh] relative overflow-hidden bg-[#f5efe6]">
      <div ref={mapContainer} className="w-full h-full z-0" />

      {mapError ? (
        <div className="absolute top-4 left-4 z-[500] w-[min(92vw,420px)] rounded-xl border border-[#e8b7b7] bg-[#fff5f5] p-3 text-[#7f2b2b] shadow-md">
          <div className="flex items-start gap-2">
            <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
            <div>
              <p className="text-sm font-semibold">地图初始化失败</p>
              <p className="text-xs mt-1 leading-5">{mapError}</p>
            </div>
          </div>
        </div>
      ) : null}

      {!mapError && spotsDataError ? (
        <div className="absolute top-4 left-4 z-[500] w-[min(92vw,420px)] rounded-xl border border-[#f0d6b2] bg-[#fff9f2] p-3 text-[#7f5a2b] shadow-md">
          <div className="flex items-start gap-2">
            <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
            <div>
              <p className="text-sm font-semibold">湿地点位数据降级提示</p>
              <p className="text-xs mt-1 leading-5">{spotsDataError}</p>
            </div>
          </div>
        </div>
      ) : null}

      <div className="absolute left-4 top-4 z-[420] w-[min(92vw,360px)]">
        <form
          className="flex items-center gap-2 rounded-xl border border-[#e8dfd1] bg-[#fdfbf7]/98 p-2 shadow-md"
          onSubmit={handleSearchSubmit}
        >
          <button
            type="button"
            onClick={handleGoBack}
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-[#e8dfd1] bg-white text-[#5c7a5e] transition-colors hover:bg-[#f4eee4]"
            aria-label="返回"
            title="返回"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <input
            value={searchKeyword}
            onChange={(event) => {
              setSearchKeyword(event.target.value);
              setShowSearchResults(true);
            }}
            onFocus={() => setShowSearchResults(true)}
            className="h-9 flex-1 rounded-lg border border-[#e8dfd1] bg-white px-3 text-sm text-[#3d3228] outline-none focus:border-[#c4813a]"
            placeholder="搜索湿地点位（名称 / 拼音）"
          />
          <button
            type="submit"
            className="h-9 rounded-lg bg-[#5c7a5e] px-4 text-sm font-medium text-white transition-colors hover:bg-[#4d6950]"
          >
            搜索
          </button>
        </form>

        {showSearchResults && normalizedKeyword ? (
          <div className="mt-2 max-h-64 overflow-auto rounded-xl border border-[#e8dfd1] bg-[#fdfbf7]/98 p-2 shadow-md">
            {filteredSpots.length > 0 ? (
              filteredSpots.map((spot) => (
                <button
                  key={spot.id}
                  type="button"
                  onClick={() => handlePickSpot(spot)}
                  className="mb-1 block w-full rounded-lg px-3 py-2 text-left text-sm text-[#3d3228] transition-colors hover:bg-[#f1eadf]"
                >
                  <div className="font-medium">{spot.name}</div>
                  <div className="text-xs text-[#7a6d5d]">{spot.type}</div>
                </button>
              ))
            ) : (
              <div className="px-3 py-2 text-sm text-[#7a6d5d]">未找到匹配景区</div>
            )}
          </div>
        ) : null}
      </div>

      {selectedSpot ? (
        <ScenicPanel
          spot={selectedSpot}
          onClose={() => {
            clearRoutePolyline();
            clearPoiMarkers();
            setSelectedSpot(null);
          }}
          onOpenScenicDetail={handleOpenScenicDetail}
          facilitiesByCategory={nearbyFacilities}
          facilitiesLoading={facilitiesLoading}
          activeFacilityTab={activeFacilityTab}
          onFacilityTabChange={setActiveFacilityTab}
          activeRouteMode={activeRouteMode}
          onRouteModeChange={setActiveRouteMode}
          routeSummaries={routeSummaries}
          routeLoading={routeLoading}
        />
      ) : null}

      <div className="absolute right-6 top-1/2 -translate-y-1/2 flex flex-col gap-3 z-[400]">
        <button
          onClick={handleZoomIn}
          className="w-10 h-10 rounded-full bg-[#fdfbf7] shadow-md flex items-center justify-center text-[#3d3228] hover:text-[#c4813a] border border-[#e8dfd1] transition-colors"
        >
          <Plus className="w-5 h-5" />
        </button>
        <button
          onClick={handleZoomOut}
          className="w-10 h-10 rounded-full bg-[#fdfbf7] shadow-md flex items-center justify-center text-[#3d3228] hover:text-[#c4813a] border border-[#e8dfd1] transition-colors"
        >
          <Minus className="w-5 h-5" />
        </button>
        <button
          onClick={() => setClusterEnabled((prev) => !prev)}
          className={[
            "w-10 h-10 rounded-full shadow-md flex items-center justify-center border transition-colors",
            clusterEnabled
              ? "bg-emerald-50 text-emerald-700 border-emerald-300"
              : "bg-[#fdfbf7] text-[#3d3228] border-[#e8dfd1] hover:text-[#5c7a5e]",
          ].join(" ")}
          title={clusterEnabled ? "关闭点位聚合" : "开启点位聚合"}
        >
          <Layers className="h-[18px] w-[18px]" />
        </button>
        {clusterEnabled && !shouldUseCluster ? (
          <div className="w-[92px] rounded-lg border border-[#e8dfd1] bg-[#fdfbf7]/94 px-2 py-1 text-[10px] leading-4 text-[#6f6559]">
            当前缩放已自动展开全部点位
          </div>
        ) : null}
        <button
          onClick={handleRecenter}
          className="w-10 h-10 mt-1 rounded-full bg-[#fdfbf7] shadow-md flex items-center justify-center text-[#3d3228] hover:text-[#5c7a5e] border border-[#e8dfd1] transition-colors"
          title="重置视角"
        >
          <Compass className="w-5 h-5" />
        </button>
      </div>

      <div className="absolute bottom-8 left-8 bg-[#fdfbf7]/92 p-4 rounded-xl shadow-md border border-[#e8dfd1] z-[400]">
        <h4 className="font-serif font-semibold text-[#3d3228] mb-3 text-sm">
          图例说明
        </h4>
        <p className="mb-3 text-[11px] text-[#7a6d5d]">湿地点位总数：{spots.length}</p>
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-3">
            <div className="w-4 h-4 rounded-full bg-[#c4813a] border-2 border-white shadow-sm flex-shrink-0"></div>
            <span className="text-xs text-[#3d3228]">我的位置</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="tm-legend-pin flex-shrink-0"></div>
            <span className="text-xs text-[#3d3228]">湿地点位</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="tm-legend-cluster flex-shrink-0">
              <span className="tm-legend-cluster-halo"></span>
              <span className="tm-legend-cluster-core">8</span>
            </div>
            <span className="text-xs text-[#3d3228]">聚合点位</span>
          </div>
        </div>
      </div>

    </div>
  );
}
