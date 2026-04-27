export interface Facility {
  id: string;
  name: string;
  distance: string;
  rating: number;
  type: '酒店住宿' | '停车场' | '餐厅' | '加油站';
}

export interface Spot {
  id: string;
  name: string;
  pinyin: string;
  lat: number;
  lng: number;
  type: string;
  description: string;
  imageHint: string;
  imagePath?: string;
  facilities: Facility[];
}

export const CURRENT_LOCATION = {
  lat: 30.5928,
  lng: 114.3055,
  name: "我的位置",
  pinyin: "Wuhan"
};

const API_ORIGIN = String(import.meta.env.VITE_API_ORIGIN || "").trim();
const WETLAND_SPOTS_ENDPOINT = API_ORIGIN
  ? `${API_ORIGIN}/api/consult/wetlands/spots`
  : "/api/consult/wetlands/spots";

export const SPOTS: Spot[] = [
  {
    id: "jiuzhaigou",
    name: "九寨沟",
    pinyin: "Jiuzhaigou",
    lat: 33.2600,
    lng: 103.9200,
    type: "世界自然遗产",
    description: "世界自然遗产地，以多彩湖泊和瀑布闻名于世，被誉为'人间仙境'。秋季的红叶与碧水交相辉映，是摄影爱好者的天堂。",
    imageHint: "colorful lakes in autumn forest",
    facilities: [
      { id: "f1", name: "星宇国际大酒店", distance: "1.2 km", rating: 4.8, type: "酒店住宿" },
      { id: "f2", name: "九寨沟天堂洲际大饭店", distance: "3.5 km", rating: 4.9, type: "酒店住宿" },
      { id: "f3", name: "隐居乡村客栈", distance: "0.8 km", rating: 4.5, type: "酒店住宿" },
      { id: "f4", name: "景区1号生态停车场", distance: "0.2 km", rating: 4.0, type: "停车场" },
      { id: "f5", name: "沟口立体停车场", distance: "0.5 km", rating: 4.2, type: "停车场" },
      { id: "f6", name: "阿布氇孜藏餐厅", distance: "1.0 km", rating: 4.7, type: "餐厅" },
      { id: "f7", name: "聚峰楼中餐厅", distance: "1.5 km", rating: 4.3, type: "餐厅" },
      { id: "f8", name: "中石油九寨沟加油站", distance: "2.1 km", rating: 4.0, type: "加油站" }
    ]
  },
  {
    id: "ruoergai",
    name: "若尔盖湿地",
    pinyin: "Ruoergai",
    lat: 33.5800,
    lng: 102.9600,
    type: "国家级自然保护区",
    description: "中国最大的高原沼泽湿地，黑颈鹤的重要栖息地，草原辽阔壮美。黄河九曲第一湾在这里婉转流淌，日落时分景色极为壮观。",
    imageHint: "vast green wetland with winding river",
    facilities: [
      { id: "f1", name: "若尔盖大草原帐篷宾馆", distance: "5.0 km", rating: 4.6, type: "酒店住宿" },
      { id: "f2", name: "黄河九曲第一湾大酒店", distance: "8.2 km", rating: 4.4, type: "酒店住宿" },
      { id: "f3", name: "湿地观景台停车场", distance: "0.1 km", rating: 4.1, type: "停车场" },
      { id: "f4", name: "游客中心停车场", distance: "2.0 km", rating: 4.3, type: "停车场" },
      { id: "f5", name: "草原牧歌藏餐馆", distance: "4.5 km", rating: 4.8, type: "餐厅" },
      { id: "f6", name: "中石化若尔盖加油站", distance: "12.0 km", rating: 3.9, type: "加油站" }
    ]
  },
  {
    id: "bayinbuluke",
    name: "巴音布鲁克湿地",
    pinyin: "Bayinbuluke",
    lat: 42.7400,
    lng: 83.5800,
    type: "世界自然遗产",
    description: "新疆天山腹地的高原湿地，以天鹅湖和九曲十八弯日落景色闻名。四周雪山环抱，绿草如茵，是多种珍禽异兽的乐园。",
    imageHint: "winding river with swans at sunset",
    facilities: [
      { id: "f1", name: "天鹅湖度假村", distance: "3.5 km", rating: 4.5, type: "酒店住宿" },
      { id: "f2", name: "巴音布鲁克宾馆", distance: "6.0 km", rating: 4.2, type: "酒店住宿" },
      { id: "f3", name: "九曲十八弯观景停车场", distance: "0.5 km", rating: 4.0, type: "停车场" },
      { id: "f4", name: "蒙古包风味餐厅", distance: "4.0 km", rating: 4.6, type: "餐厅" },
      { id: "f5", name: "天山牧民特色烤肉", distance: "5.5 km", rating: 4.7, type: "餐厅" },
      { id: "f6", name: "巴音镇加油站", distance: "8.0 km", rating: 4.1, type: "加油站" }
    ]
  },
  {
    id: "zhalong",
    name: "扎龙湿地",
    pinyin: "Zhalong",
    lat: 47.1700,
    lng: 124.2200,
    type: "国际重要湿地",
    description: "中国最大的以鹤类为主的珍禽自然保护区，丹顶鹤的故乡。广袤的芦苇荡在微风中摇曳，仙鹤在天际翱翔，充满生机。",
    imageHint: "red-crowned cranes in reed marsh",
    facilities: [
      { id: "f1", name: "鹤乡假日酒店", distance: "2.8 km", rating: 4.4, type: "酒店住宿" },
      { id: "f2", name: "湿地生态客栈", distance: "1.5 km", rating: 4.6, type: "酒店住宿" },
      { id: "f3", name: "保护区正门停车场", distance: "0.3 km", rating: 4.2, type: "停车场" },
      { id: "f4", name: "芦苇荡农家院", distance: "1.8 km", rating: 4.5, type: "餐厅" },
      { id: "f5", name: "全鱼宴特色餐厅", distance: "3.0 km", rating: 4.3, type: "餐厅" },
      { id: "f6", name: "扎龙镇加油站", distance: "5.0 km", rating: 4.0, type: "加油站" }
    ]
  },
  {
    id: "dongting",
    name: "洞庭湖湿地",
    pinyin: "Dongting Lake",
    lat: 29.3600,
    lng: 112.9100,
    type: "国际重要湿地",
    description: "中国第二大淡水湖，候鸟越冬的重要栖息地，芦苇荡连绵无际。“气蒸云梦泽，波撼岳阳城”，湖光山色令人心旷神怡。",
    imageHint: "vast misty lake with reed beds",
    facilities: [
      { id: "f1", name: "岳阳楼宾馆", distance: "15.0 km", rating: 4.7, type: "酒店住宿" },
      { id: "f2", name: "洞庭湖畔度假村", distance: "4.2 km", rating: 4.5, type: "酒店住宿" },
      { id: "f3", name: "观鸟台停车场", distance: "0.8 km", rating: 4.1, type: "停车场" },
      { id: "f4", name: "洞庭渔歌餐厅", distance: "3.5 km", rating: 4.8, type: "餐厅" },
      { id: "f5", name: "老渔民私房菜", distance: "5.1 km", rating: 4.6, type: "餐厅" },
      { id: "f6", name: "君山加油站", distance: "10.0 km", rating: 4.2, type: "加油站" }
    ]
  },
  {
    id: "poyang",
    name: "鄱阳湖湿地",
    pinyin: "Poyang Lake",
    lat: 29.1400,
    lng: 116.2700,
    type: "国际重要湿地",
    description: "中国最大的淡水湖，冬季有数十万候鸟在此越冬，蔚为壮观。水草丰美，鱼虾成群，是白鹤等珍稀鸟类的天堂。",
    imageHint: "flock of birds flying over wide lake",
    facilities: [
      { id: "f1", name: "鄱阳湖大酒店", distance: "12.0 km", rating: 4.5, type: "酒店住宿" },
      { id: "f2", name: "候鸟小镇民宿", distance: "2.5 km", rating: 4.8, type: "酒店住宿" },
      { id: "f3", name: "国家湿地公园停车场", distance: "0.5 km", rating: 4.3, type: "停车场" },
      { id: "f4", name: "吴城候鸟观赏区停车场", distance: "3.0 km", rating: 4.0, type: "停车场" },
      { id: "f5", name: "鄱湖鲜鱼馆", distance: "4.5 km", rating: 4.7, type: "餐厅" },
      { id: "f6", name: "水乡人家酒楼", distance: "6.0 km", rating: 4.4, type: "餐厅" },
      { id: "f7", name: "吴城镇中石化加油站", distance: "8.5 km", rating: 4.1, type: "加油站" }
    ]
  }
];

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}

interface WetlandMapSpotDto {
  wetlandId: number;
  id: string;
  name: string;
  pinyin: string;
  lat: number;
  lng: number;
  type: string;
  description: string;
  imageHint: string;
  imagePath?: string;
}

interface WetlandMapPayloadDto {
  sourceFile: string;
  totalRows: number;
  parsedRows: number;
  skippedRows: number;
  spots: WetlandMapSpotDto[];
}

function toFiniteNumber(value: unknown): number | null {
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

function normalizeRemoteSpot(input: WetlandMapSpotDto, index: number): Spot | null {
  const lat = toFiniteNumber(input?.lat);
  const lng = toFiniteNumber(input?.lng);
  if (lat == null || lng == null) return null;

  return {
    id: String(input?.id || `wetland-${index + 1}`),
    name: String(input?.name || `Wetland ${index + 1}`),
    pinyin: String(input?.pinyin || input?.id || `wetland-${index + 1}`),
    lat,
    lng,
    type: String(input?.type || "湿地"),
    description: String(input?.description || ""),
    imageHint: String(input?.imageHint || "wetland"),
    imagePath: String(input?.imagePath || "").trim() || undefined,
    facilities: [],
  };
}

export async function fetchWetlandSpots(signal?: AbortSignal): Promise<Spot[]> {
  try {
    const response = await fetch(WETLAND_SPOTS_ENDPOINT, {
      method: "GET",
      headers: { Accept: "application/json" },
      signal,
    });

    if (!response.ok) {
      throw new Error(`failed to fetch wetland spots: ${response.status}`);
    }

    const payload = (await response.json()) as ApiResponse<WetlandMapPayloadDto>;
    const rows = payload?.data?.spots;
    if (!payload?.success || !Array.isArray(rows)) {
      throw new Error(payload?.message || "invalid wetland spots payload");
    }

    const spots = rows
      .map((item, index) => normalizeRemoteSpot(item, index))
      .filter((item): item is Spot => item != null);

    if (!spots.length) {
      throw new Error("wetland spots list is empty");
    }
    return spots;
  } catch (err) {
    // If running as a purely static site (Cloudflare Pages) there may be no backend API.
    // Fall back to the bundled SPOTS so the UI remains usable.
    // Log the error for visibility in browser console.
    // eslint-disable-next-line no-console
    console.warn('[travel-map] fetchWetlandSpots failed, falling back to local SPOTS:', err);
    return SPOTS;
  }
}
