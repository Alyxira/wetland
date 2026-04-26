#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import os
import sys

try:
    import rasterio
    from rasterio.warp import transform_bounds
except ImportError:
    print(json.dumps({"error": "rasterio library is not installed"}))
    sys.exit(1)


def get_tiff_metadata(tiff_path):
    if not os.path.exists(tiff_path):
        return {"error": f"file not found: {tiff_path}"}

    try:
        with rasterio.open(tiff_path) as dataset:
            metadata = {
                "width": dataset.width,
                "height": dataset.height,
                "bandCount": dataset.count,
                "crs": str(dataset.crs) if dataset.crs else None,
                "nodata": dataset.nodata,
                "driver": dataset.driver,
                "dtypes": [str(dt) for dt in dataset.dtypes] if hasattr(dataset, "dtypes") else [],
            }

            if dataset.transform:
                metadata["geotransform"] = [
                    dataset.transform.c,
                    dataset.transform.a,
                    dataset.transform.b,
                    dataset.transform.f,
                    dataset.transform.d,
                    dataset.transform.e,
                ]

            if dataset.bounds:
                left = dataset.bounds.left
                bottom = dataset.bounds.bottom
                right = dataset.bounds.right
                top = dataset.bounds.top

                # Always return bounds in EPSG:4326 (lon/lat).
                if dataset.crs and str(dataset.crs).upper() != "EPSG:4326":
                    left, bottom, right, top = transform_bounds(
                        dataset.crs,
                        "EPSG:4326",
                        left,
                        bottom,
                        right,
                        top,
                        densify_pts=21,
                    )

                metadata["bounds"] = {
                    "left": left,
                    "bottom": bottom,
                    "right": right,
                    "top": top,
                }
                metadata["boundsCrs"] = "EPSG:4326"

            if dataset.res:
                metadata["resolution"] = [float(r) for r in dataset.res]

            return metadata
    except Exception as e:
        return {"error": str(e)}


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "missing input file path"}))
        sys.exit(1)

    tiff_path = sys.argv[1]
    result = get_tiff_metadata(tiff_path)
    print(json.dumps(result))


if __name__ == "__main__":
    main()
