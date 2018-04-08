# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.shortcuts import render

# Create your views here.
from blog.models import BlogPost  
from django.shortcuts import render_to_response  
  
# Create your views here.  
def Index(request):  
    blog_list = BlogPost.objects.all()  
    return render_to_response('index.html',{'blog_list':blog_list})  
