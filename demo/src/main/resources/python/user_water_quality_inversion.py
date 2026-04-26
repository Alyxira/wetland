﻿#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import math
import os
import sys

import numpy as np

try:
    import rasterio
    from rasterio.features import geometry_mask
    from rasterio.warp import transform, transform_geom
except Exception as exc:
    print(json.dumps({"error": f"rasterio import failed: {exc}"}))
    sys.exit(1)


def _as_float(v):
    if v is None:
        return None
    try:
        return float(v)
    except Exception:
        return None


def _calc_stats(values):
    if values.size == 0:
        return {"avg": 0.0, "min": 0.0, "max": 0.0, "std": 0.0}
    return {
        "avg": float(np.nanmean(values)),
        "min": float(np.nanmin(values)),
        "max": float(np.nanmax(values)),
        "std": float(np.nanstd(values)),
    }


def _eutrophication(avg_chla):
    if avg_chla < 1:
        return "贫营养"
    if avg_chla < 3:
        return "中营养"
    if avg_chla < 5:
        return "轻度富营养"
    if avg_chla < 10:
        return "中度富营养"
    if avg_chla < 20:
        return "重度富营养"
    return "严重富营养"


def _build_geometry(req):
    geom = req.get("geometry") if isinstance(req.get("geometry"), dict) else None
    if geom and geom.get("coordinates"):
        return {
            "type": str(geom.get("type") or "Polygon"),
            "coordinates": geom.get("coordinates"),
        }

    coords = req.get("coordinates")
    if coords:
        return {
            "type": str(req.get("geometryType") or "Polygon"),
            "coordinates": coords,
        }
    return None


def _mask_from_geometry(ds, req):
    geom = _build_geometry(req)
    if geom is None:
        return None

    src_crs = ds.crs
    if src_crs is not None and str(src_crs).upper() != "EPSG:4326":
        geom = transform_geom("EPSG:4326", src_crs, geom, precision=15)

    mask = geometry_mask(
        [geom],
        out_shape=(ds.height, ds.width),
        transform=ds.transform,
        invert=True,
        all_touched=False,
    )
    return mask


def _mask_from_bounds(ds, req):
    min_lon = _as_float(req.get("minLon"))
    max_lon = _as_float(req.get("maxLon"))
    min_lat = _as_float(req.get("minLat"))
    max_lat = _as_float(req.get("maxLat"))
    if None in (min_lon, max_lon, min_lat, max_lat):
        return None

    poly = {
        "type": "Polygon",
        "coordinates": [[
            [min_lon, min_lat],
            [max_lon, min_lat],
            [max_lon, max_lat],
            [min_lon, max_lat],
            [min_lon, min_lat],
        ]],
    }
    if ds.crs is not None and str(ds.crs).upper() != "EPSG:4326":
        poly = transform_geom("EPSG:4326", ds.crs, poly, precision=15)

    return geometry_mask(
        [poly],
        out_shape=(ds.height, ds.width),
        transform=ds.transform,
        invert=True,
        all_touched=False,
    )


def _pixel_area_m2(ds):
    if ds.crs is None:
        return abs(ds.transform.a * ds.transform.e)

    crs_txt = str(ds.crs).upper()
    if "4326" in crs_txt:
        center_row = ds.height // 2
        center_col = ds.width // 2
        lon, lat = ds.xy(center_row, center_col)
        meters_per_deg_lat = 111320.0
        meters_per_deg_lon = 111320.0 * math.cos(math.radians(lat))
        px_w = abs(ds.transform.a) * meters_per_deg_lon
        px_h = abs(ds.transform.e) * meters_per_deg_lat
        return max(px_w * px_h, 0.0)

    return abs(ds.transform.a * ds.transform.e)


def run(req):
    tiff_path = req.get("tiffPath") or req.get("tiff_path") or req.get("imagePath")
    if not tiff_path or not os.path.exists(tiff_path):
        return {"error": f"TIFF file not found: {tiff_path}"}

    with rasterio.open(tiff_path) as ds:
        band_count = min(4, ds.count)
        if band_count < 4:
            return {"error": "not enough bands, require at least 4"}

        data = ds.read(list(range(1, band_count + 1))).astype(np.float32)
        nodata = ds.nodata
        if nodata is not None:
            data[data == nodata] = np.nan

        # Sentinel-2 reflectance normalization
        for b in range(band_count):
            valid = np.isfinite(data[b])
            if np.any(valid):
                max_v = float(np.nanmax(data[b][valid]))
                if max_v > 1.5:
                    data[b][valid] = data[b][valid] / 10000.0
                data[b] = np.clip(data[b], 0.0, 1.0)

        aoi_mask = _mask_from_geometry(ds, req)
        if aoi_mask is None:
            aoi_mask = _mask_from_bounds(ds, req)
        if aoi_mask is None:
            aoi_mask = np.ones((ds.height, ds.width), dtype=bool)

        blue = data[0]
        green = data[1]
        red = data[2]
        nir = data[3]

        ndwi = (green - nir) / (green + nir + 1e-10)
        ndvi = (nir - red) / (nir + red + 1e-10)

        water_mask = (ndwi > 0.0) & (ndvi < 0.2) & (nir < 0.15)
        valid_mask = np.isfinite(blue) & np.isfinite(green) & np.isfinite(red) & np.isfinite(nir)
        final_mask = aoi_mask & water_mask & valid_mask

        if not np.any(final_mask):
            return {"error": "no valid water pixels inside AOI"}

        blue_v = blue[final_mask]
        green_v = green[final_mask]
        red_v = red[final_mask]

        chla = 8.0 * np.power(np.clip(blue_v / (green_v + 1e-10), 0.2, 3.0), -1.2)
        chla = np.clip(chla, 0.01, 200.0)

        spm = 10.0 * np.exp(6.0 * red_v)
        spm = np.clip(spm, 0.1, 300.0)

        turbidity = np.clip(spm * 0.7, 0.0, 300.0)

        chla_stats = _calc_stats(chla)
        spm_stats = _calc_stats(spm)
        turb_stats = _calc_stats(turbidity)

        pixel_area = _pixel_area_m2(ds)
        water_pixels = int(np.sum(final_mask))
        water_area_km2 = float((water_pixels * pixel_area) / 1_000_000.0)

        distribution = [
            {"parameter": "chla", "range": "<1", "count": int(np.sum(chla < 1)), "percentage": round(float(np.mean(chla < 1) * 100), 2)},
            {"parameter": "chla", "range": "1-3", "count": int(np.sum((chla >= 1) & (chla < 3))), "percentage": round(float(np.mean((chla >= 1) & (chla < 3)) * 100), 2)},
            {"parameter": "spm", "range": "<10", "count": int(np.sum(spm < 10)), "percentage": round(float(np.mean(spm < 10) * 100), 2)},
            {"parameter": "spm", "range": "10-30", "count": int(np.sum((spm >= 10) & (spm < 30))), "percentage": round(float(np.mean((spm >= 10) & (spm < 30)) * 100), 2)},
        ]

        return {
            "success": True,
            "chla": chla_stats,
            "spm": spm_stats,
            "turbidity": turb_stats,
            "waterPixelCount": water_pixels,
            "waterAreaKm2": round(water_area_km2, 4),
            "eutrophicationLevel": _eutrophication(chla_stats["avg"]),
            "distributionData": distribution,
        }


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "missing request json"}, ensure_ascii=False))
        sys.exit(1)

    try:
        req = json.loads(sys.argv[1])
    except Exception as exc:
        print(json.dumps({"error": f"invalid request json: {exc}"}, ensure_ascii=False))
        sys.exit(0)

    try:
        result = run(req)
    except Exception as exc:
        result = {"error": str(exc)}

    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
