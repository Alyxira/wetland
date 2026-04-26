#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import os
import sys

try:
    import rasterio
    from rasterio.features import shapes
    from rasterio.warp import transform_geom
except ImportError as exc:
    print(json.dumps({"error": f"missing dependency: {exc}"}))
    sys.exit(1)


def build_water_mask(mask_array, nodata):
    valid_mask = mask_array > 0
    if nodata is not None:
        valid_mask = valid_mask & (mask_array != nodata)
    return valid_mask


def iter_polygons(mask_array, transform, src_crs, nodata=None):
    water_mask = build_water_mask(mask_array, nodata)
    for geom, value in shapes(mask_array, mask=water_mask, transform=transform):
        if float(value) <= 0:
            continue
        if nodata is not None and float(value) == float(nodata):
            continue

        out_geom = geom
        if src_crs and str(src_crs).upper() != "EPSG:4326":
            out_geom = transform_geom(src_crs, "EPSG:4326", geom, precision=6)

        gtype = out_geom.get("type")
        if gtype == "Polygon":
            yield out_geom.get("coordinates", [])
        elif gtype == "MultiPolygon":
            for poly in out_geom.get("coordinates", []):
                yield poly


def convert_tiff_to_geojson(tiff_path):
    if not os.path.exists(tiff_path):
        return {"error": f"file not found: {tiff_path}"}

    with rasterio.open(tiff_path) as ds:
        band = ds.read(1)
        polygons = list(iter_polygons(band, ds.transform, ds.crs, ds.nodata))

    feature = {
        "type": "Feature",
        "geometry": {
            "type": "MultiPolygon",
            "coordinates": polygons,
        },
        "properties": {
            "source": os.path.basename(tiff_path),
        },
    }
    return feature


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "missing input tiff path"}))
        sys.exit(1)

    tiff_path = sys.argv[1]
    try:
        result = convert_tiff_to_geojson(tiff_path)
        print(json.dumps(result, ensure_ascii=False))
    except Exception as exc:
        print(json.dumps({"error": str(exc)}))
        sys.exit(1)


if __name__ == "__main__":
    main()
