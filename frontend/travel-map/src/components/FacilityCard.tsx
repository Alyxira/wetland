import React from "react";
import { Bed, Car, Coffee, MapPin, Star, TentTree } from "lucide-react";
import { NearbyFacility } from "@/lib/travelMapTypes";

interface FacilityCardProps {
  facility: NearbyFacility;
}

function resolveFacilityIcon(type: NearbyFacility["type"]) {
  switch (type) {
    case "hotel":
      return <Bed className="w-4 h-4 text-accent" />;
    case "food":
      return <Coffee className="w-4 h-4 text-accent" />;
    case "parking":
      return <Car className="w-4 h-4 text-accent" />;
    default:
      return <TentTree className="w-4 h-4 text-accent" />;
  }
}

function formatRating(value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    return "暂无评分";
  }
  return value.toFixed(1);
}

export function FacilityCard({ facility }: FacilityCardProps) {
  return (
    <div className="p-3 mb-2 rounded-md bg-[#fdfbf7] border border-[#e8dfd1] shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-start gap-3">
        <div className="p-2 rounded-full bg-[#f5efe6]">
          {resolveFacilityIcon(facility.type)}
        </div>

        <div className="min-w-0 flex-1">
          <h4 className="text-sm font-medium text-foreground leading-5 break-words">
            {facility.name}
          </h4>

          {facility.address ? (
            <p className="mt-1 text-xs text-muted-foreground leading-4 break-words">
              {facility.address}
            </p>
          ) : null}

          <div className="flex items-center gap-3 mt-2">
            <span className="flex items-center text-xs text-muted-foreground">
              <MapPin className="w-3 h-3 mr-1" />
              {facility.distance}
            </span>
            <span className="flex items-center text-xs text-accent">
              <Star className="w-3 h-3 mr-1 fill-current" />
              {formatRating(facility.rating)}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
