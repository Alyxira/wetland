#!/usr/bin/env python
# -*- coding: utf-8 -*-

import base64
import json
import math
import os
import struct
import sys
import zlib

import numpy as np

try:
    import rasterio
    from rasterio.enums import Resampling
    from rasterio.warp import transform_bounds
    from rasterio.windows import from_bounds
except Exception as exc:
    print(json.dumps({"error": f"rasterio import failed: {exc}"}))
    sys.exit(1)


def mercator_bounds(z, x, y):
    n = 2 ** z
    lon_left = x / n * 360.0 - 180.0
    lon_right = (x + 1) / n * 360.0 - 180.0

    lat_top = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * y / n))))
    lat_bottom = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * (y + 1) / n))))

    def lonlat_to_merc(lon, lat):
        origin_shift = 20037508.342789244
        mx = lon * origin_shift / 180.0
        my = math.log(math.tan((90 + lat) * math.pi / 360.0)) / (math.pi / 180.0)
        my = my * origin_shift / 180.0
        return mx, my

    left, bottom = lonlat_to_merc(lon_left, lat_bottom)
    right, top = lonlat_to_merc(lon_right, lat_top)
    return left, bottom, right, top


def normalize(arr):
    finite = np.isfinite(arr)
    if not np.any(finite):
        return np.zeros_like(arr, dtype=np.float32)
    vals = arr[finite]
    lo = np.percentile(vals, 2)
    hi = np.percentile(vals, 98)
    if hi <= lo:
        lo = np.min(vals)
        hi = np.max(vals)
    if hi <= lo:
        out = np.zeros_like(arr, dtype=np.float32)
        out[finite] = 0.5
        return out
    out = (arr - lo) / (hi - lo)
    out = np.clip(out, 0.0, 1.0)
    out[~finite] = 0.0
    return out


def colormap(norm, ramp):
    if ramp == "gray":
        gray = (norm * 255).astype(np.uint8)
        return np.stack([gray, gray, gray], axis=-1)

    ramps = {
        "viridis": [(68, 1, 84), (59, 82, 139), (33, 145, 140), (94, 201, 97), (253, 231, 37)],
        "inferno": [(0, 0, 4), (87, 15, 109), (187, 55, 84), (249, 142, 8), (252, 255, 164)],
        "magma": [(0, 0, 4), (74, 15, 109), (149, 44, 129), (221, 95, 102), (252, 253, 191)],
    }
    stops = ramps.get(ramp, ramps["viridis"])

    pos = np.linspace(0, 1, len(stops))
    flat = norm.reshape(-1)
    rgb = np.zeros((flat.shape[0], 3), dtype=np.float32)
    for c in range(3):
        values = np.array([s[c] for s in stops], dtype=np.float32)
        rgb[:, c] = np.interp(flat, pos, values)
    return rgb.reshape(norm.shape[0], norm.shape[1], 3).astype(np.uint8)


def encode_png(rgb):
    h, w, _ = rgb.shape
    raw = b"".join([b"\x00" + rgb[i].tobytes() for i in range(h)])
    compressed = zlib.compress(raw, 9)

    def chunk(tag, data):
        return (
            struct.pack("!I", len(data))
            + tag
            + data
            + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    png = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack("!IIBBBBB", w, h, 8, 2, 0, 0, 0)
    png += chunk(b"IHDR", ihdr)
    png += chunk(b"IDAT", compressed)
    png += chunk(b"IEND", b"")
    return png


def render(req):
    image_path = req.get("imagePath")
    if not image_path or not os.path.exists(image_path):
        return {"error": f"image not found: {image_path}"}

    z = int(req.get("z"))
    x = int(req.get("x"))
    y = int(req.get("y"))
    band = max(1, int(req.get("band", 1)))
    ramp = str(req.get("colorRamp", "viridis")).lower()

    left, bottom, right, top = mercator_bounds(z, x, y)

    with rasterio.open(image_path) as ds:
        src_crs = ds.crs
        if src_crs is None:
            return {"error": "source CRS is missing"}

        if band > ds.count:
            band = ds.count

        if str(src_crs).upper() != "EPSG:3857":
            b_left, b_bottom, b_right, b_top = transform_bounds(
                "EPSG:3857", src_crs, left, bottom, right, top, densify_pts=21
            )
        else:
            b_left, b_bottom, b_right, b_top = left, bottom, right, top

        window = from_bounds(b_left, b_bottom, b_right, b_top, ds.transform)
        if window.width <= 0 or window.height <= 0:
            return {"error": "tile outside raster extent"}

        arr = ds.read(
            band,
            window=window,
            out_shape=(256, 256),
            resampling=Resampling.bilinear,
            boundless=True,
            fill_value=0,
        ).astype(np.float32)

        nodata = ds.nodata
        if nodata is not None:
            arr[arr == nodata] = np.nan

        norm = normalize(arr)
        rgb = colormap(norm, ramp)
        png = encode_png(rgb)
        return {"pngBase64": base64.b64encode(png).decode("ascii")}


def main():
    if len(sys.argv) >= 7:
        req = {
            "imagePath": sys.argv[1],
            "z": sys.argv[2],
            "x": sys.argv[3],
            "y": sys.argv[4],
            "band": sys.argv[5],
            "colorRamp": sys.argv[6],
        }
    elif len(sys.argv) >= 2:
        try:
            req = json.loads(sys.argv[1])
        except Exception as exc:
            print(json.dumps({"error": f"invalid request json: {exc}"}))
            return
    else:
        print(json.dumps({"error": "missing request args"}))
        return

    try:
        result = render(req)
    except Exception as exc:
        result = {"error": str(exc)}
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
