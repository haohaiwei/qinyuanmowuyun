from django.conf.urls import *
import blog.views
urlpatterns = [
   url(r'^$', blog.views.test),
]
