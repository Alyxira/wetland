import sys
import json
import numpy as np
from osgeo import gdal

def extract_water_and_invert(tiff_path, coordinates):
    """
    提取水体并进行水质参数反演
    :param tiff_path: TIFF文件路径
    :param coordinates: 框选区域坐标 [min_x, min_y, max_x, max_y]
    :return: 水质参数反演结果
    """
    try:
        # 打开TIFF文件
        dataset = gdal.Open(tiff_path)
        if dataset is None:
            return {"error": "无法打开TIFF文件"}
        
        # 读取图像数据
        band = dataset.GetRasterBand(1)
        data = band.ReadAsArray()
        
        # 提取框选区域
        min_x, min_y, max_x, max_y = coordinates
        min_x = max(0, int(min_x))
        min_y = max(0, int(min_y))
        max_x = min(data.shape[1], int(max_x))
        max_y = min(data.shape[0], int(max_y))
        
        # 提取区域数据
        region_data = data[min_y:max_y, min_x:max_x]
        
        # 模拟水体提取和水质参数反演
        # 实际应用中，这里应该使用真实的算法
        water_mask = region_data > 100  # 简单的阈值判断
        water_area = np.sum(water_mask)
        
        # 模拟水质参数
        water_quality_params = {
            "chlorophyll": np.random.uniform(0.5, 5.0),
            "turbidity": np.random.uniform(0.1, 2.0),
            "ph": np.random.uniform(6.5, 8.5),
            "dissolved_oxygen": np.random.uniform(5.0, 10.0),
            "water_area": water_area
        }
        
        # 模拟时间序列数据
        time_series = []
        for i in range(12):  # 12个月的数据
            time_series.append({
                "month": i + 1,
                "chlorophyll": np.random.uniform(0.5, 5.0),
                "turbidity": np.random.uniform(0.1, 2.0),
                "ph": np.random.uniform(6.5, 8.5),
                "dissolved_oxygen": np.random.uniform(5.0, 10.0)
            })
        
        result = {
            "success": True,
            "water_quality": water_quality_params,
            "time_series": time_series
        }
        
        return result
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(json.dumps({"error": "参数错误"}))
        sys.exit(1)
    
    tiff_path = sys.argv[1]
    coordinates = json.loads(sys.argv[2])
    
    result = extract_water_and_invert(tiff_path, coordinates)
    print(json.dumps(result))
