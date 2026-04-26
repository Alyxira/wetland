export type RouteMode = "driving" | "walking" | "riding";

export interface RouteModeOption {
  key: RouteMode;
  label: string;
}

export interface RouteSummary {
  distanceMeters: number | null;
  durationSeconds: number | null;
  unavailable?: boolean;
  errorMessage?: string;
}

export type FacilityCategoryKey = "hotel" | "food" | "parking" | "service";

export interface FacilityCategory {
  key: FacilityCategoryKey;
  label: string;
  shortLabel: string;
  keywords: string[];
}

export interface NearbyFacility {
  id: string;
  name: string;
  type: FacilityCategoryKey;
  distance: string;
  rating: number;
  address?: string;
  location?: [number, number];
}

export const ROUTE_MODE_OPTIONS: RouteModeOption[] = [
  { key: "driving", label: "驾车" },
  { key: "walking", label: "步行" },
  { key: "riding", label: "骑行" },
];

export const FACILITY_CATEGORIES: FacilityCategory[] = [
  {
    key: "hotel",
    label: "酒店住宿",
    shortLabel: "酒店",
    keywords: ["酒店", "民宿", "客栈"],
  },
  {
    key: "food",
    label: "餐饮美食",
    shortLabel: "餐饮",
    keywords: ["餐厅", "美食", "小吃"],
  },
  {
    key: "parking",
    label: "停车服务",
    shortLabel: "停车",
    keywords: ["停车场"],
  },
  {
    key: "service",
    label: "游客公共服务",
    shortLabel: "公服",
    keywords: ["游客中心", "公共厕所", "公交站"],
  },
];
