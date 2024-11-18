from django.contrib import admin
from .models import Post, Photo

@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    list_display = ['id', 'title', 'author', 'created_date', 'published_date']

@admin.register(Photo)
class PhotoAdmin(admin.ModelAdmin):
    list_display = ['id', 'post', 'image']  # 确保引用的字段是 Photo 模型中存在的字段

