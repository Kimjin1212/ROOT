from rest_framework import viewsets, status
from rest_framework.response import Response
from rest_framework.parsers import MultiPartParser, FormParser
from rest_framework.decorators import action
from .models import Post, Photo
from .serializers import PostSerializer, PhotoSerializer
from django.utils.decorators import method_decorator
from django.views.decorators.csrf import csrf_exempt
from rest_framework.permissions import AllowAny
from django.http import HttpResponse 
from django.shortcuts import render
from rest_framework.views import APIView

class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
    parser_classes = (MultiPartParser, FormParser)
    permission_classes = [AllowAny]

    # def create(self, request, *args, **kwargs):
    #     # 解析数据
    #     serializer = self.get_serializer(data=request.data)
    #     serializer.is_valid(raise_exception=True)
    #     post = serializer.save()

    #     # 处理图片上传
    #     if 'images' in request.FILES:
    #         images = request.FILES.getlist('images')
    #         for image in images:
    #             Photo.objects.create(post=post, image=image)

    #     # 返回响应
    #     return Response(serializer.data, status=status.HTTP_201_CREATED)

    # @action(detail=False, methods=['get'])
    # def recent_posts(self, request):
    #     """获取最新帖子""" 
    #     recent = Post.objects.order_by('-created_date')[:10]
    #     serializer = self.get_serializer(recent, many=True)
    #     return Response(serializer.data)

class PhotoViewSet(viewsets.ModelViewSet): 
    queryset = Photo.objects.all()
    serializer_class = PhotoSerializer
    permission_classes = [AllowAny]

    @action(detail=False, methods=['get'])
    def getAll(self, request):
        """获取最新帖子"""
        recent = Post.objects.order_by('-created_date')[:10]
        serializer = self.get_serializer(recent, many=True)
        return Response(serializer.data)

    @action(detail=False, methods=['get'])
    def delete(self, photo_id):
        print(photo_id) 
        photo = Photo.objects.get(id=photo_id)
        photo.delete()
        return HttpResponse("{ state:'1'}")


    @action(detail=False, methods=['get'])
    def setTage(self, tages,photo_id):
        print(photo_id) 
        photo = Photo.objects.get(id=photo_id)
        photo.tags = tages
        photo.save()
        return HttpResponse("{ state:'1'}")

  
    def showphoto(self, request, *args, **kwargs):
        # 在这里进行 HTML 渲染
        photos = self.get_queryset()
        return render(request, 'photos_show.html', {'photos': photos})
    
class PhotoListView(APIView):
    def get(self, request, *args, **kwargs):  
        last_photos = Photo.objects.order_by('-id').all() 
        return render(request, 'photos_show.html', {'photos': last_photos})
