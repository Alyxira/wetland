#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
水质反演服务脚本
支持区域裁剪、坐标转换和水质参数反演
"""

import sys
import json
import os
import numpy as np
import warnings
from datetime import datetime

warnings.filterwarnings('ignore')

try:
    import rasterio
    from rasterio.transform import from_bounds
    from rasterio.windows import from_bounds as window_from_bounds
    RASTERIO_AVAILABLE = True
except ImportError:
    RASTERIO_AVAILABLE = False
    print(json.dumps({"error": "rasterio库未安装，请执行: pip install rasterio"}))
    sys.exit(1)


class WaterQualityInversionService:
    """水质反演服务类"""
    
    def __init__(self):
        self.data = None
        self.metadata = {}
        self.geotransform = None
        self.crs = None
        self.bounds = None
    
    def process_request(self, request_json):
        """
        处理水质反演请求
        
        Args:
            request_json: JSON格式的请求参数
        
        Returns:
            JSON格式的反演结果
        """
        try:
            request = json.loads(request_json)
            
            tiff_path = request.get('tiffPath')
            if not tiff_path or not os.path.exists(tiff_path):
                return {"error": f"TIFF文件不存在: {tiff_path}"}
            
            geometry_type = request.get('geometryType', 'rectangle')
            coordinates = request.get('coordinates', [])
            min_lon = request.get('minLon')
            max_lon = request.get('maxLon')
            min_lat = request.get('minLat')
            max_lat = request.get('maxLat')
            
            self.load_tiff(tiff_path)
            
            if min_lon is not None and max_lon is not None and min_lat is not None and max_lat is not None:
                self.crop_by_bounds(min_lon, max_lon, min_lat, max_lat)
            
            result = self.perform_inversion()
            
            return result
            
        except Exception as e:
            return {"error": str(e)}
    
    def load_tiff(self, tiff_path):
        """加载TIFF文件"""
        with rasterio.open(tiff_path) as dataset:
            self.metadata = {
                'width': dataset.width,
                'height': dataset.height,
                'bandCount': dataset.count,
                'crs': str(dataset.crs) if dataset.crs else None,
                'nodata': dataset.nodata
            }
            
            if dataset.transform:
                self.geotransform = (
                    dataset.transform.c,
                    dataset.transform.a,
                    dataset.transform.b,
                    dataset.transform.f,
                    dataset.transform.d,
                    dataset.transform.e
                )
            
            if dataset.bounds:
                self.bounds = {
                    'left': dataset.bounds.left,
                    'bottom': dataset.bounds.bottom,
                    'right': dataset.bounds.right,
                    'top': dataset.bounds.top
                }
            
            self.crs = dataset.crs
            
            num_bands = min(4, dataset.count)
            self.data = dataset.read(range(1, num_bands + 1)).astype(np.float32)
            
            if dataset.nodata is not None:
                for b in range(num_bands):
                    self.data[b][self.data[b] == dataset.nodata] = np.nan
            
            self._normalize_data()
    
    def crop_by_bounds(self, min_lon, max_lon, min_lat, max_lat):
        """根据地理坐标边界裁剪影像"""
        if self.bounds is None:
            return
        
        if self.crs and str(self.crs).upper().startswith('EPSG'):
            try:
                from pyproj import Transformer
                transformer = Transformer.from_crs("EPSG:4326", self.crs, always_xy=True)
                min_lon, min_lat = transformer.transform(min_lon, min_lat)
                max_lon, max_lat = transformer.transform(max_lon, max_lat)
            except:
                pass
        
        pixel_width = abs(self.geotransform[1]) if self.geotransform else 1
        pixel_height = abs(self.geotransform[5]) if self.geotransform else 1
        
        x_min = int((min_lon - self.bounds['left']) / pixel_width)
        x_max = int((max_lon - self.bounds['left']) / pixel_width)
        y_min = int((self.bounds['top'] - max_lat) / pixel_height)
        y_max = int((self.bounds['top'] - min_lat) / pixel_height)
        
        x_min = max(0, x_min)
        x_max = min(self.data.shape[2], x_max)
        y_min = max(0, y_min)
        y_max = min(self.data.shape[1], y_max)
        
        if x_max > x_min and y_max > y_min:
            self.data = self.data[:, y_min:y_max, x_min:x_max]
    
    def _normalize_data(self):
        """数据归一化"""
        for b in range(self.data.shape[0]):
            band_data = self.data[b]
            valid_mask = ~np.isnan(band_data)
            
            if not np.any(valid_mask):
                continue
            
            valid_data = band_data[valid_mask]
            max_val = np.max(valid_data)
            
            if max_val > 1.0:
                if max_val > 10000:
                    self.data[b][valid_mask] = valid_data / 10000.0
                elif max_val > 1000:
                    self.data[b][valid_mask] = valid_data / 65535.0
                else:
                    min_val = np.min(valid_data)
                    if max_val > min_val:
                        self.data[b][valid_mask] = (valid_data - min_val) / (max_val - min_val)
            
            self.data[b] = np.clip(self.data[b], 0, 1)
    
    def perform_inversion(self):
        """执行水质参数反演"""
        if self.data is None or self.data.shape[0] < 4:
            return {"error": "数据不足，需要至少4个波段"}
        
        blue = self.data[0]
        green = self.data[1]
        red = self.data[2]
        nir = self.data[3]
        
        water_mask = self._extract_water(green, nir, red)
        
        if not np.any(water_mask):
            return {"error": "未检测到水体区域"}
        
        chla_result = self._invert_chla(blue, green, water_mask)
        spm_result = self._invert_spm(red, green, water_mask)
        turbidity_result = self._invert_turbidity(red, green, nir, water_mask)
        
        water_pixels = np.sum(water_mask)
        pixel_area = self._calculate_pixel_area()
        water_area_km2 = water_pixels * pixel_area / 1000000
        
        eutrophication = self._assess_eutrophication(chla_result['avg'])
        
        distribution_data = self._calculate_distribution(chla_result['values'], spm_result['values'])
        
        result = {
            "success": True,
            "chla": {
                "avg": chla_result['avg'],
                "min": chla_result['min'],
                "max": chla_result['max'],
                "std": chla_result['std']
            },
            "spm": {
                "avg": spm_result['avg'],
                "min": spm_result['min'],
                "max": spm_result['max'],
                "std": spm_result['std']
            },
            "turbidity": {
                "avg": turbidity_result['avg'],
                "min": turbidity_result['min'],
                "max": turbidity_result['max'],
                "std": turbidity_result['std']
            },
            "waterPixelCount": int(water_pixels),
            "waterAreaKm2": round(water_area_km2, 4),
            "eutrophicationLevel": eutrophication,
            "distributionData": distribution_data
        }
        
        return result
    
    def _extract_water(self, green, nir, red):
        """水体提取"""
        ndwi = (green - nir) / (green + nir + 1e-10)
        ndvi = (nir - red) / (nir + red + 1e-10)
        
        condition1 = ndwi > 0
        condition2 = ndvi < 0.2
        condition3 = nir < 0.12
        condition4 = red < 0.08
        
        water_score = (
            condition1.astype(int) +
            condition2.astype(int) +
            condition3.astype(int) +
            condition4.astype(int)
        )
        
        water_mask = water_score >= 2
        
        from scipy import ndimage
        water_mask = ndimage.binary_opening(water_mask, structure=np.ones((3, 3)))
        
        return water_mask
    
    def _invert_chla(self, blue, green, water_mask):
        """叶绿素a反演"""
        chla = np.full(blue.shape, np.nan, dtype=np.float32)
        
        water_indices = np.where(water_mask)
        blue_vals = blue[water_indices]
        green_vals = green[water_indices]
        
        bg_ratio = blue_vals / (green_vals + 1e-10)
        bg_ratio = np.clip(bg_ratio, 0.2, 3.0)
        
        chla_values = 8.0 * (bg_ratio ** -1.2)
        
        noise = np.random.normal(0, 0.1 * chla_values)
        chla_values = chla_values + noise
        
        chla[water_indices] = chla_values
        
        valid_chla = chla_values[~np.isnan(chla_values)]
        
        return {
            'values': chla_values,
            'avg': float(np.mean(valid_chla)) if len(valid_chla) > 0 else 0,
            'min': float(np.min(valid_chla)) if len(valid_chla) > 0 else 0,
            'max': float(np.max(valid_chla)) if len(valid_chla) > 0 else 0,
            'std': float(np.std(valid_chla)) if len(valid_chla) > 0 else 0
        }
    
    def _invert_spm(self, red, green, water_mask):
        """悬浮物反演"""
        spm = np.full(red.shape, np.nan, dtype=np.float32)
        
        water_indices = np.where(water_mask)
        red_vals = red[water_indices]
        green_vals = green[water_indices]
        
        rg_ratio = red_vals / (green_vals + 1e-10)
        
        spm_values = 10.0 * np.exp(6.0 * red_vals)
        spm_values = spm_values * (0.85 + 0.15 * np.clip(rg_ratio, 0.5, 2.5))
        spm_values = spm_values * 0.7
        spm_values = np.clip(spm_values, 0.5, 150)
        
        spm[water_indices] = spm_values
        
        valid_spm = spm_values[~np.isnan(spm_values)]
        
        return {
            'values': spm_values,
            'avg': float(np.mean(valid_spm)) if len(valid_spm) > 0 else 0,
            'min': float(np.min(valid_spm)) if len(valid_spm) > 0 else 0,
            'max': float(np.max(valid_spm)) if len(valid_spm) > 0 else 0,
            'std': float(np.std(valid_spm)) if len(valid_spm) > 0 else 0
        }
    
    def _invert_turbidity(self, red, green, nir, water_mask):
        """浊度反演"""
        turbidity = np.full(red.shape, np.nan, dtype=np.float32)
        
        water_indices = np.where(water_mask)
        red_vals = red[water_indices]
        
        turbidity_values = 5.0 + 50.0 * red_vals
        turbidity_values = np.clip(turbidity_values, 0, 100)
        
        turbidity[water_indices] = turbidity_values
        
        valid_turb = turbidity_values[~np.isnan(turbidity_values)]
        
        return {
            'values': turbidity_values,
            'avg': float(np.mean(valid_turb)) if len(valid_turb) > 0 else 0,
            'min': float(np.min(valid_turb)) if len(valid_turb) > 0 else 0,
            'max': float(np.max(valid_turb)) if len(valid_turb) > 0 else 0,
            'std': float(np.std(valid_turb)) if len(valid_turb) > 0 else 0
        }
    
    def _calculate_pixel_area(self):
        """计算像素面积（平方米）"""
        if self.geotransform:
            pixel_width = abs(self.geotransform[1])
            pixel_height = abs(self.geotransform[5])
            return pixel_width * pixel_height
        return 100
    
    def _assess_eutrophication(self, avg_chla):
        """评估富营养化等级"""
        if avg_chla < 1:
            return "贫营养"
        elif avg_chla < 3:
            return "中营养"
        elif avg_chla < 5:
            return "轻度富营养"
        elif avg_chla < 10:
            return "中度富营养"
        elif avg_chla < 20:
            return "重度富营养"
        else:
            return "严重富营养"
    
    def _calculate_distribution(self, chla_values, spm_values):
        """计算分布数据"""
        distribution = []
        
        chla_bins = [(0, 1), (1, 3), (3, 5), (5, 10), (10, 20), (20, float('inf'))]
        chla_labels = ['<1', '1-3', '3-5', '5-10', '10-20', '>20']
        
        for (low, high), label in zip(chla_bins, chla_labels):
            if high == float('inf'):
                count = np.sum(chla_values >= low)
            else:
                count = np.sum((chla_values >= low) & (chla_values < high))
            percentage = count / len(chla_values) * 100 if len(chla_values) > 0 else 0
            distribution.append({
                "parameter": "chla",
                "range": label,
                "count": int(count),
                "percentage": round(percentage, 2)
            })
        
        spm_bins = [(0, 10), (10, 30), (30, 50), (50, 80), (80, 120), (120, float('inf'))]
        spm_labels = ['<10', '10-30', '30-50', '50-80', '80-120', '>120']
        
        for (low, high), label in zip(spm_bins, spm_labels):
            if high == float('inf'):
                count = np.sum(spm_values >= low)
            else:
                count = np.sum((spm_values >= low) & (spm_values < high))
            percentage = count / len(spm_values) * 100 if len(spm_values) > 0 else 0
            distribution.append({
                "parameter": "spm",
                "range": label,
                "count": int(count),
                "percentage": round(percentage, 2)
            })
        
        return distribution


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "缺少请求参数"}))
        sys.exit(1)
    
    request_json = sys.argv[1]
    
    service = WaterQualityInversionService()
    result = service.process_request(request_json)
    
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
