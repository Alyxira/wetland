import React from "react";
import { Bed, Bike, Car, Coffee, Footprints, MapPin, Toilet, X } from "lucide-react";
import { Spot, CURRENT_LOCATION } from "../data/spots";
import {
  FACILITY_CATEGORIES,
  FacilityCategoryKey,
  NearbyFacility,
  RouteMode,
  RouteSummary,
  ROUTE_MODE_OPTIONS,
} from "@/lib/travelMapTypes";

interface ScenicPanelProps {
  spot: Spot | null;
  onClose: () => void;
  onOpenScenicDetail?: (spot: Spot) => void;
  facilitiesByCategory: Record<FacilityCategoryKey, NearbyFacility[]>;
  facilitiesLoading: boolean;
  activeFacilityTab: FacilityCategoryKey;
  onFacilityTabChange: (tab: FacilityCategoryKey) => void;
  activeRouteMode: RouteMode;
  onRouteModeChange: (mode: RouteMode) => void;
  routeSummaries: Record<RouteMode, RouteSummary | null>;
  routeLoading: boolean;
}

function formatRouteDistance(distanceMeters: number | null, fallbackKm: number, unavailable = false) {
  if (unavailable) return "路线不可达";
  if (Number.isFinite(distanceMeters) && distanceMeters! > 0) {
    const meters = distanceMeters!;
    if (meters < 1000) return `${Math.max(1, Math.round(meters))} 米`;
    const km = meters / 1000;
    return `${km.toFixed(km >= 100 ? 0 : 1)} 公里`;
  }
  return `${fallbackKm} 公里`;
}

function formatRouteDuration(durationSeconds: number | null, fallbackHours: number, unavailable = false) {
  if (unavailable) return "--";
  if (Number.isFinite(durationSeconds) && durationSeconds! > 0) {
    const totalMinutes = Math.max(1, Math.round(durationSeconds! / 60));
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    if (hours > 0 && minutes > 0) return `${hours}小时${minutes}分钟`;
    if (hours > 0) return `${hours}小时`;
    return `${minutes}分钟`;
  }
  return `${fallbackHours}小时`;
}

function resolveRouteModeIcon(mode: RouteMode) {
  switch (mode) {
    case "driving":
      return <Car className="h-[18px] w-[18px]" />;
    case "walking":
      return <Footprints className="h-[18px] w-[18px]" />;
    default:
      return <Bike className="h-[18px] w-[18px]" />;
  }
}

function resolveFacilityIcon(category: FacilityCategoryKey) {
  switch (category) {
    case "hotel":
      return <Bed className="h-4 w-4" />;
    case "food":
      return <Coffee className="h-4 w-4" />;
    case "parking":
      return <Car className="h-4 w-4" />;
    default:
      return <Toilet className="h-4 w-4" />;
  }
}

function resolveSpotCoverUrl(spot: Spot) {
  const raw = String(spot.imagePath || "").trim();
  if (!raw) return "";
  if (/^https?:\/\//i.test(raw)) return raw;
  if (raw.startsWith("/")) return raw;
  return `/${raw.replace(/^\.?\//, "")}`;
}

function formatFacilityRating(rating: number) {
  if (!Number.isFinite(rating) || rating <= 0) {
    return "-";
  }
  return rating.toFixed(1);
}

export function ScenicPanel({
  spot,
  onClose,
  onOpenScenicDetail,
  facilitiesByCategory,
  facilitiesLoading,
  activeFacilityTab,
  onFacilityTabChange,
  activeRouteMode,
  onRouteModeChange,
  routeSummaries,
  routeLoading,
}: ScenicPanelProps) {
  if (!spot) return null;

  const dx = spot.lng - CURRENT_LOCATION.lng;
  const dy = spot.lat - CURRENT_LOCATION.lat;
  const distanceKm = Math.max(1, Math.round(Math.sqrt(dx * dx + dy * dy) * 111));

  const fallbackHoursByMode: Record<RouteMode, number> = {
    driving: Math.max(1, Math.round(distanceKm / 80)),
    walking: Math.max(1, Math.round(distanceKm / 5)),
    riding: Math.max(1, Math.round(distanceKm / 15)),
  };

  const activeSummary = routeSummaries[activeRouteMode];
  const routeUnavailable = Boolean(activeSummary?.unavailable);
  const activeCategory = FACILITY_CATEGORIES.find((item) => item.key === activeFacilityTab) || FACILITY_CATEGORIES[0];
  const activeFacilities = facilitiesByCategory[activeFacilityTab] || [];
  const coverUrl = resolveSpotCoverUrl(spot);
  const spotType = String(spot.type || "").trim() || "内陆湿地";

  return (
    <div className="absolute top-5 right-5 z-[1000] flex h-[calc(100dvh-40px)] w-[min(380px,calc(100vw-40px))] flex-col overflow-hidden rounded-2xl border border-white/70 bg-[rgba(255,255,255,0.85)] shadow-[0_22px_56px_rgba(10,24,40,0.24)] backdrop-blur-xl max-md:top-2 max-md:right-2 max-md:left-2 max-md:h-[calc(100dvh-16px)] max-md:w-auto">
      <button
        onClick={onClose}
        className="absolute top-3 right-3 z-10 grid h-8 w-8 place-items-center rounded-full bg-white/80 text-slate-600 transition-colors hover:bg-white hover:text-slate-900"
        aria-label="关闭湿地详情卡片"
      >
        <X className="h-4 w-4" />
      </button>

      <div
        className="flex-1 min-h-0 overflow-y-auto overscroll-contain px-4 pt-4 pb-[calc(1.25rem+env(safe-area-inset-bottom))] [scrollbar-gutter:stable]"
        style={{ WebkitOverflowScrolling: "touch" }}
      >
        <header>
          {coverUrl ? (
            <div className="h-28 overflow-hidden rounded-xl">
              <img src={coverUrl} alt={spot.name} className="h-full w-full object-cover" loading="lazy" />
            </div>
          ) : null}

          <div className={coverUrl ? "mt-3" : "mt-1"}>
            <h2 className={`font-sans font-bold tracking-tight text-slate-900 ${coverUrl ? "text-2xl" : "text-[2rem] leading-tight"}`}>
              {spot.name}
            </h2>

            <div className="mt-2 inline-flex items-center rounded-full bg-slate-500/12 px-2.5 py-1 text-[11px] font-medium text-slate-600">
              {spotType}
            </div>

            <div className="mt-3">
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center rounded-full border border-slate-300/80 bg-white/70 px-3 text-xs font-medium text-slate-700 transition-colors hover:bg-white"
                onClick={() => onOpenScenicDetail?.(spot)}
              >
                进入景区详情
              </button>
            </div>
          </div>
        </header>

        <section className="mt-4 rounded-xl bg-white/68 p-3">
          <p className="text-[13px] leading-6 text-slate-600">
            {spot.description || "暂无景区简介。"}
          </p>
        </section>

        <section className="mt-4 rounded-xl bg-white/68 p-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-800">导航</h3>
            <span className="truncate text-[11px] text-slate-500">
              {CURRENT_LOCATION.name || "当前位置"} → {spot.name}
            </span>
          </div>

          <div className="mt-3 grid grid-cols-3 gap-2">
            {ROUTE_MODE_OPTIONS.map((mode) => {
              const isActive = activeRouteMode === mode.key;
              return (
                <button
                  key={mode.key}
                  type="button"
                  onClick={() => onRouteModeChange(mode.key)}
                  className={[
                    "group flex min-h-11 flex-col items-center justify-center gap-1 rounded-lg border transition-all",
                    isActive
                      ? "border-emerald-300 bg-emerald-50/90 text-emerald-700 shadow-[0_6px_16px_rgba(16,185,129,0.14)]"
                      : "border-slate-200/90 bg-white/80 text-slate-500 hover:border-slate-300 hover:text-slate-700",
                  ].join(" ")}
                  aria-label={mode.label}
                >
                  {resolveRouteModeIcon(mode.key)}
                  <span className="text-[11px] font-medium">{mode.label}</span>
                </button>
              );
            })}
          </div>

          <div className="mt-3 grid grid-cols-2 gap-2">
            <div className="rounded-lg bg-slate-900/[0.04] px-2.5 py-2">
              <div className="text-[10px] font-medium tracking-[0.08em] text-slate-500">距离</div>
              <div className="mt-0.5 text-sm font-semibold text-slate-800">
                {formatRouteDistance(activeSummary?.distanceMeters ?? null, distanceKm, routeUnavailable)}
              </div>
            </div>

            <div className="rounded-lg bg-slate-900/[0.04] px-2.5 py-2">
              <div className="text-[10px] font-medium tracking-[0.08em] text-slate-500">耗时</div>
              <div className="mt-0.5 text-sm font-semibold text-slate-800">
                {routeLoading
                  ? "计算中..."
                  : formatRouteDuration(activeSummary?.durationSeconds ?? null, fallbackHoursByMode[activeRouteMode], routeUnavailable)}
              </div>
            </div>
          </div>
          {routeUnavailable ? (
            <p className="mt-2 text-[11px] text-amber-700/90">
              当前点位可能位于边境或道路不可达区域，已关闭近似导航线
            </p>
          ) : null}
        </section>

        <section className="mt-4 rounded-xl bg-white/68 p-3">
          <h3 className="text-sm font-semibold text-slate-800">周边设施</h3>

          <div className="mt-3 flex items-center gap-2">
            {FACILITY_CATEGORIES.map((category) => {
              const isActive = category.key === activeFacilityTab;
              return (
                <button
                  key={category.key}
                  type="button"
                  onClick={() => onFacilityTabChange(category.key)}
                  className={[
                    "flex min-h-9 flex-1 items-center justify-center rounded-lg border bg-white/78 px-2 text-slate-500 transition-colors",
                    isActive
                      ? "border-emerald-300 text-emerald-700"
                      : "border-slate-200 hover:border-slate-300 hover:text-slate-700",
                  ].join(" ")}
                  aria-label={category.label}
                  title={category.label}
                >
                  {resolveFacilityIcon(category.key)}
                </button>
              );
            })}
          </div>

          <div key={activeFacilityTab} className="tm-facility-panel mt-3 grid gap-2">
            {facilitiesLoading ? (
              <div className="rounded-lg bg-white/70 px-3 py-3 text-xs text-slate-500">
                正在查询 {activeCategory.label}...
              </div>
            ) : activeFacilities.length ? (
              activeFacilities.map((facility) => (
                <article
                  key={`${activeCategory.key}-${facility.id}`}
                  className="rounded-lg border border-white/70 bg-white/76 px-3 py-2.5 shadow-[0_6px_18px_rgba(15,23,42,0.06)]"
                >
                  <div className="flex items-start gap-2.5">
                    <span className="mt-0.5 text-slate-500">{resolveFacilityIcon(activeCategory.key)}</span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-slate-800">{facility.name}</p>
                      {facility.address ? (
                        <p className="mt-0.5 truncate text-[11px] text-slate-500">{facility.address}</p>
                      ) : null}
                      <div className="mt-1.5 flex items-center gap-3 text-[11px] text-slate-500">
                        <span className="inline-flex items-center gap-1">
                          <MapPin className="h-3.5 w-3.5" />
                          {facility.distance}
                        </span>
                        <span>评分 {formatFacilityRating(facility.rating)}</span>
                      </div>
                    </div>
                  </div>
                </article>
              ))
            ) : (
              <div className="rounded-lg bg-white/70 px-3 py-3 text-xs text-slate-500">
                暂未检索到附近 {activeCategory.label}
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
