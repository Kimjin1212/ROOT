from django.db import models

class Post(models.Model):
    author = models.CharField(max_length=100, default="Anonymous")
    title = models.CharField(max_length=255, blank=True)
    text = models.TextField(blank=True)
    created_date = models.DateTimeField(auto_now_add=True)
    published_date = models.DateTimeField(null=True, blank=True)
    tags = models.CharField(max_length=255, blank=True)

class Photo(models.Model): 
    post = models.ForeignKey(Post, related_name='photos', on_delete=models.CASCADE) 
    image = models.ImageField(upload_to='photos/')
    tags = models.CharField(max_length=255, blank=True)
