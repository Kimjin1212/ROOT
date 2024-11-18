from rest_framework import serializers
from .models import Post, Photo

class PhotoSerializer(serializers.ModelSerializer):
    class Meta:
        model = Photo
        fields = ('id', 'image','post','tags')

class PostSerializer(serializers.ModelSerializer):
    photos = PhotoSerializer(many=True, read_only=True)  # 读取关联的图片
    images = serializers.ListField(
        child=serializers.ImageField(max_length=100000, allow_empty_file=False, use_url=False),
        write_only=True,
        required=False
    )  # 该字段用于上传图片 

    class Meta:
        model = Post
        fields = ('id', 'author', 'title', 'text', 'created_date', 'published_date', 'tags', 'photos', 'images')

    def create(self, validated_data):
        images = validated_data.pop('images', [])
        post = Post.objects.create(**validated_data) 

        for image in images:
            Photo.objects.create(post=post, image=image,tags=image.tags)

        return post

