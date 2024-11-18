import cv2
import torch
import requests
import os
import time
from datetime import datetime, timedelta
from queue import Queue
import threading
import warnings

warnings.filterwarnings("ignore", category=FutureWarning)
warnings.filterwarnings("ignore", category=UserWarning)
warnings.filterwarnings("ignore", category=FutureWarning, module="torch")

# 加载YOLOv5模型
model = torch.hub.load('ultralytics/yolov5', 'yolov5s')  # 加载预训练模型（YOLOv5s）

# 摄像头设置
cap = cv2.VideoCapture(0)  # 打开默认摄像头
if not cap.isOpened():
    print("Error: Could not open webcam.")
    exit()

# 服务器的API URL
#server_url = "http://localhost:8000/api/photos/"  # 替换为实际的上传接口
server_url = "https://jincyang42.pythonanywhere.com/api/photos/"  # 替换为实际的上传接口

# 缓存和输出目录
output_dir = "./output"
cache_dir = "./cache"
os.makedirs(output_dir, exist_ok=True)
os.makedirs(cache_dir, exist_ok=True)

# 队列用于存储检测任务
task_queue = Queue()

# 上传图片函数
def upload_image(file_path):
    try:
        with open(file_path, 'rb') as img_file:
            files = {'image': img_file}
            data={ 
                "post" :2,
                "tags" : file_path
            }
            response = requests.post(server_url, files=files, timeout=10,data=data)
            if response.status_code == 201 or response.status_code == 200:
                print(f"Uploaded successfully: {file_path}")
                return True
            else:
                print(f"Failed to upload (status {response.status_code}): {file_path}")
                return False
    except Exception as e:
        print(f"Error uploading image: {e}")
        return False

def upload_cache():
    """遍历并尝试上传缓存目录的所有图片"""
    for file_name in os.listdir(cache_dir):
        file_path = os.path.join(cache_dir, file_name)
        if os.path.isfile(file_path):
            if upload_image(file_path):
                os.remove(file_path)
                
                print(f"Cache file uploaded successfully: {file_path}")
            else:
                print(f"Cache file upload failed: {file_path}")
            time.sleep(1)

# 后台线程处理队列任务
def process_tasks():
    while True:
        try :
            if not task_queue.empty():
                task = task_queue.get()
                file_path, cache_path = task['file_path'], task['cache_path']
                if not upload_image(file_path):  # 上传失败，保存到缓存
                    os.rename(file_path, cache_path)
                    print(f"Saved to cache: {cache_path}")
                    os.remove(file_path)
                else:
                    os.remove(file_path)  # 上传成功后删除文件
                    time.sleep(1) 
                    if(os.path.isfile(file_path)):
                        print(f"file is still there: {cache_path}") 
                    else :
                        print(f"file is deleted : {cache_path}") 
                    upload_cache()
            time.sleep(1)
        except Exception as e:
            print(f"process_tasks: {e}") 


# 定期清理缓存图片
def clean_cache(directory, days=7):
    while True:
        now = datetime.now()
        cutoff_time = now - timedelta(days=days)
        for file_name in os.listdir(directory):
            file_path = os.path.join(directory, file_name)
            if os.path.isfile(file_path):
                file_time = datetime.fromtimestamp(os.path.getmtime(file_path))
                if file_time < cutoff_time:
                    os.remove(file_path)
                    print(f"Deleted old file: {file_path}")
        time.sleep(3600)  # 每小时清理一次

# 后台线程启动
threading.Thread(target=process_tasks, daemon=True).start()
threading.Thread(target=clean_cache, args=(cache_dir,), daemon=True).start()

# 主循环
while True:
    ret, frame = cap.read()
    if not ret:
        print("Error: Failed to capture image.")
        break
    
    # 使用YOLOv5进行检测
    results = model(frame)
    detections = results.xyxy[0]  # 获取检测结果
    if len(detections) > 0:  # 判断是否检测到物体
        # 保存检测结果图片
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_path = os.path.join(output_dir, f"detection_{timestamp}.jpg")
        cache_path = os.path.join(cache_dir, f"detection_{timestamp}.jpg")
        
        # 绘制检测结果并保存
        results.render()
        detection_image = results.ims[0]
        cv2.imwrite(output_path, detection_image)
        time.sleep(2)  # 增加短暂延迟（根据需要调整）
       
        if(os.path.exists(output_path)): 
            print(f"Detection saved: {output_path}")
        else:
            print(f"Detection save fail: {output_path}")
            
        
        # 将任务加入队列
        task_queue.put({"file_path": output_path, "cache_path": cache_path})
    
    # 显示检测结果
    cv2.imshow("YOLOv5 Detection", detection_image)
    
    # 按 'q' 键退出
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
